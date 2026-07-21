// GsGit push backend — приём вебхуков GsGit GitHub App и моментальные FCM-пуши.
// Ноль зависимостей: только встроенные модули Node.js 22 (http, crypto, fs).
//
// Эндпоинты:
//   GET  /healthz     — проверка живости (для Uptime Kuma)
//   POST /webhook     — приёмник вебхуков GitHub (подпись X-Hub-Signature-256 обязательна)
//   POST /register    — регистрация устройства: Authorization: Bearer <GsGit App user token>,
//                       body {"fcmToken": "...", "device": "...", "quietStart": 23,
//                       "quietEnd": 8, "tzOffsetMin": 180}; логин берём из GET /user
//   POST /unregister  — удаление устройства: body {"fcmToken": "..."}
//   POST /announce    — броадкаст всем устройствам: header X-Admin-Key, body {title, body, url?}
//
// Админ-панель (всё под X-Admin-Key):
//   GET  /admin/stats, /admin/health, /admin/metrics, /admin/audit, /admin/errors
//   GET  /admin/devices            — список; GET/DELETE /admin/devices/{deviceId}
//   POST /admin/devices/{deviceId}/{test-push|clear-held|disable-push}
//   GET  /admin/announcements[/{id}]; POST .../{id}/{retry|cancel}
//   GET  /appconfig; POST /admin/appconfig; GET /admin/appconfig/history[/{rev}]
//   POST /admin/appconfig/{rollback|validate}
//   POST /admin/maintenance/{schedule|stop}; GET /admin/maintenance; DELETE .../schedule
//   GET/POST /admin/releases; POST /admin/releases/{version}/publish
//
// Фоновые механики (тик раз в 5 минут):
//   - тихие часы: пуши в окне quietStart..quietEnd копятся и после окна уходят одним дайджестом
//   - напоминания о ревью: review_requested без ревью сутки → пуш ревьюеру
//   - недельная сводка: счётчики активности per-login, отправка в воскресенье 20:00 МСК

'use strict';

const http = require('http');
const https = require('https');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.PORT || 8080);
const WEBHOOK_SECRET = process.env.GITHUB_WEBHOOK_SECRET || '';
const FCM_PROJECT_ID = process.env.FCM_PROJECT_ID || '';
const SERVICE_ACCOUNT_FILE = process.env.SERVICE_ACCOUNT_FILE || '/data/service-account.json';
const DEVICES_FILE = process.env.DEVICES_FILE || '/data/devices.json';
const GITHUB_API = process.env.GITHUB_API || 'https://api.github.com';
const GITHUB_WEB = process.env.GITHUB_WEB || 'https://github.com';
// OAuth GsGit GitHub App: client_id публичный, а client_secret живёт ТОЛЬКО на
// сервере (устройство его не видит). Нужен для обновления user-token'а —
// GitHub требует секрет для grant_type=refresh_token.
const GH_APP_CLIENT_ID = process.env.GITHUB_APP_CLIENT_ID || 'Iv23li20TIiHVSmfq2ai';
const GH_APP_CLIENT_SECRET = process.env.GITHUB_APP_CLIENT_SECRET || '';
const MAX_BODY = 1024 * 1024; // GitHub шлёт до ~25 МБ, но нам столько не нужно — режем на 1 МБ

if (!WEBHOOK_SECRET) console.warn('[warn] GITHUB_WEBHOOK_SECRET не задан — /webhook будет отвечать 503');
if (!FCM_PROJECT_ID) console.warn('[warn] FCM_PROJECT_ID не задан — пуши отправляться не будут');
if (!GH_APP_CLIENT_SECRET) console.warn('[warn] GITHUB_APP_CLIENT_SECRET не задан — /auth/refresh будет отвечать 503');

// ─── Общий JSON-стор с дебаунс-записью ───────────────────────────────────────

function loadJson(file, fallback) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch (_) { return fallback; }
}

const saveTimers = {};
function saveJson(file, value) {
  clearTimeout(saveTimers[file]);
  saveTimers[file] = setTimeout(() => {
    // Ошибка записи не должна ронять процесс: данные остаются в памяти,
    // а причину видно в логах.
    try {
      const tmp = file + '.tmp';
      fs.mkdirSync(path.dirname(file), { recursive: true });
      fs.writeFileSync(tmp, JSON.stringify(value, null, 2));
      fs.renameSync(tmp, file);
    } catch (e) {
      console.error('[store] save failed:', e.message);
    }
  }, 250);
}

// ─── Хранилище устройств ─────────────────────────────────────────────────────
// { "<github login>": [ { t: "<fcm token>", name, qs, qe, tz, held: [...] } ] }
// qs/qe — тихие часы (0-23, qs===qe → выключены), tz — смещение от UTC в минутах,
// held — пуши, задержанные тихими часами. Легаси-формат (массив строк) мигрируется на лету.

const DATA_DIR = path.dirname(DEVICES_FILE);
const REMINDERS_FILE = process.env.REMINDERS_FILE || path.join(DATA_DIR, 'reminders.json');
const STATS_FILE = process.env.STATS_FILE || path.join(DATA_DIR, 'stats.json');
const ADMIN_KEY_FILE = process.env.ADMIN_KEY_FILE || path.join(DATA_DIR, 'admin.key');

let devices = loadJson(DEVICES_FILE, {});
for (const login of Object.keys(devices)) {
  devices[login] = (devices[login] || []).map((d) => (typeof d === 'string' ? { t: d } : d));
}

function saveDevices() { saveJson(DEVICES_FILE, devices); }

function allDeviceObjects() {
  const out = [];
  for (const login of Object.keys(devices)) for (const d of devices[login]) out.push(d);
  return out;
}

function addDevice(login, fcmToken, meta) {
  const key = login.toLowerCase();
  const list = devices[key] || (devices[key] = []);
  let entry = list.find((d) => d.t === fcmToken);
  const isNew = !entry;
  if (!entry) { entry = { t: fcmToken, registeredAt: Date.now() }; list.push(entry); }
  entry.name = meta.name || entry.name || '';
  entry.qs = Number.isInteger(meta.qs) ? meta.qs : (entry.qs ?? null);
  entry.qe = Number.isInteger(meta.qe) ? meta.qe : (entry.qe ?? null);
  entry.tz = Number.isInteger(meta.tz) ? meta.tz : (entry.tz ?? 0);
  if (typeof meta.appVersion === 'string' && meta.appVersion) entry.appVersion = meta.appVersion;
  if (entry.pe === undefined) entry.pe = true;
  entry.lastSeenAt = Date.now();
  saveDevices();
  return isNew;
}

function removeDeviceToken(fcmToken) {
  let removed = false;
  for (const login of Object.keys(devices)) {
    const next = devices[login].filter((d) => d.t !== fcmToken);
    if (next.length !== devices[login].length) removed = true;
    if (next.length === 0) delete devices[login];
    else devices[login] = next;
  }
  if (removed) saveDevices();
  return removed;
}

// ─── Админ-ключ для /announce: из env или автогенерация в /data ──────────────

let ADMIN_KEY = process.env.ADMIN_KEY || '';
if (!ADMIN_KEY) {
  try {
    ADMIN_KEY = fs.readFileSync(ADMIN_KEY_FILE, 'utf8').trim();
  } catch (_) {
    ADMIN_KEY = crypto.randomBytes(24).toString('hex');
    try {
      fs.mkdirSync(DATA_DIR, { recursive: true });
      fs.writeFileSync(ADMIN_KEY_FILE, ADMIN_KEY);
    } catch (e) { console.error('[admin] key save failed:', e.message); }
  }
}
console.log(`[admin] announce key: ${ADMIN_KEY.slice(0, 6)}… (полный — в ${ADMIN_KEY_FILE})`);

// ─── Конфиг приложения (техработы/обновления) — правится из админки в самом GsGit ───

const APPCONFIG_FILE = process.env.APPCONFIG_FILE || path.join(DATA_DIR, 'appconfig.json');
const APPCONFIG_FIELDS = ['maintenanceSoon', 'maintenance', 'latestVersion', 'minVersion', 'changelog', 'downloadUrl'];
let appConfig = Object.assign({
  maintenanceSoon: '',
  maintenance: '',
  latestVersion: '',
  minVersion: '',
  changelog: '',
  downloadUrl: 'https://github.com/lkolholk-ctrl/gsgit/releases/latest',
}, loadJson(APPCONFIG_FILE, {}));
function saveAppConfig() { saveJson(APPCONFIG_FILE, appConfig); }

// ─── Расширенная админка: доп-хранилища и хелперы (строго аддитивно) ──────────
// Всё под тем же X-Admin-Key. Ничего из боевых путей (/webhook, /register,
// пуши, старые /admin/*) не меняет по контракту — только добавляет.

const SERVER_VERSION = '1.1.2';
const startedAt = Date.now();

const ANNOUNCEMENTS_FILE = process.env.ANNOUNCEMENTS_FILE || path.join(DATA_DIR, 'announcements.json');
const APPCONFIG_HISTORY_FILE = process.env.APPCONFIG_HISTORY_FILE || path.join(DATA_DIR, 'appconfig-history.json');
const RELEASES_FILE = process.env.RELEASES_FILE || path.join(DATA_DIR, 'releases.json');
const AUDIT_FILE = process.env.AUDIT_FILE || path.join(DATA_DIR, 'audit.json');
const METRICS_FILE = process.env.METRICS_FILE || path.join(DATA_DIR, 'metrics.json');
const ERRORS_FILE = process.env.ERRORS_FILE || path.join(DATA_DIR, 'errors.json');
const MAINT_FILE = process.env.MAINT_FILE || path.join(DATA_DIR, 'maintenance.json');

let announcements = loadJson(ANNOUNCEMENTS_FILE, []);        // newest last, cap 500
let appConfigHistory = loadJson(APPCONFIG_HISTORY_FILE, []); // {revision, changedAt, changedFields, reason, snapshot}
let releases = loadJson(RELEASES_FILE, []);                  // {version, changelog, url, sha256, mandatory, rollout, publishedAt}
let auditLog = loadJson(AUDIT_FILE, []);                     // newest last, cap 1000
let metrics = loadJson(METRICS_FILE, { hours: {} });        // hours: { 'YYYY-MM-DDTHH': {..counters..} }
let errorsLog = loadJson(ERRORS_FILE, {});                  // 'service|code' -> {id, service, code, message, count, createdAt, lastAt}
let maintSchedule = loadJson(MAINT_FILE, null);            // {startsAt, endsAt, message, applied, prevMaintenance}

const VERSION_RE = /^\d+\.\d+\.\d+([.-][0-9A-Za-z.-]+)?$/;

function clientIp(req) {
  return String(req.headers['x-forwarded-for'] || '').split(',')[0].trim() ||
    req.socket?.remoteAddress || '';
}
function adminOk(req) {
  const given = String(req.headers['x-admin-key'] || '');
  return !!given && given === ADMIN_KEY;
}
async function jsonBody(req) {
  const raw = await readBody(req);
  try { return JSON.parse(raw.toString('utf8')); } catch (_) { return undefined; }
}
function cmpVersions(a, b) {
  const pa = String(a).split(/[.\-]/).map((x) => parseInt(x, 10) || 0);
  const pb = String(b).split(/[.\-]/).map((x) => parseInt(x, 10) || 0);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] || 0) - (pb[i] || 0);
    if (d) return d > 0 ? 1 : -1;
  }
  return 0;
}
// Пагинация newest-first по массиву, который хранится oldest-first.
function paginateDesc(arr, limit, cursor) {
  const lim = Math.min(Math.max(Number(limit) || 50, 1), 200);
  const desc = arr.slice().reverse();
  const start = Math.max(Number(cursor) || 0, 0);
  const items = desc.slice(start, start + lim);
  const nextCursor = start + lim < desc.length ? String(start + lim) : null;
  return { items, nextCursor };
}

// — метрики: почасовые бакеты, окно до ~31 суток —
function hourKey(d = new Date()) { return d.toISOString().slice(0, 13); }
function recordMetric(field, by = 1) {
  const k = hourKey();
  const b = metrics.hours[k] || (metrics.hours[k] = {});
  b[field] = (b[field] || 0) + by;
  saveJson(METRICS_FILE, metrics);
}
function pruneMetrics() {
  const cutoff = Date.now() - 31 * 86_400_000;
  let changed = false;
  for (const k of Object.keys(metrics.hours)) {
    if (Date.parse(k + ':00:00Z') < cutoff) { delete metrics.hours[k]; changed = true; }
  }
  if (changed) saveJson(METRICS_FILE, metrics);
}
function metricsFor(hoursBack) {
  const cutoff = Date.now() - hoursBack * 3_600_000;
  const sum = {};
  for (const [k, b] of Object.entries(metrics.hours)) {
    if (Date.parse(k + ':00:00Z') >= cutoff) {
      for (const f of Object.keys(b)) sum[f] = (sum[f] || 0) + b[f];
    }
  }
  return sum;
}

// — серверные ошибки: агрегируем по service+code, без секретов —
function recordError(service, code, message) {
  const key = service + '|' + code;
  const e = errorsLog[key] || (errorsLog[key] = {
    id: 'err_' + crypto.randomBytes(4).toString('hex'), service, code, count: 0, createdAt: Date.now(),
  });
  e.count += 1;
  e.message = excerpt(message, 200);
  e.lastAt = Date.now();
  saveJson(ERRORS_FILE, errorsLog);
}

// — журнал админ-действий: время, IP, действие, результат (без ключей и тел) —
function audit(req, action, result, meta) {
  auditLog.push({
    id: 'aud_' + crypto.randomBytes(4).toString('hex'),
    at: Date.now(), ip: clientIp(req), action, result: result || 'ok', meta: meta || {},
  });
  if (auditLog.length > 1000) auditLog = auditLog.slice(-1000);
  saveJson(AUDIT_FILE, auditLog);
}

// — устойчивый непрозрачный id устройства (НЕ tokenTail: его недостаточно) —
function deviceIdOf(token) {
  return crypto.createHash('sha256').update(String(token)).digest('hex').slice(0, 16);
}
function findDevByToken(token) {
  for (const login of Object.keys(devices)) for (const d of devices[login]) if (d.t === token) return d;
  return null;
}
function findByDeviceId(id) {
  for (const login of Object.keys(devices)) for (const d of devices[login]) {
    if (deviceIdOf(d.t) === id) return { login, dev: d };
  }
  return null;
}

// — история конфига: снимок → ревизия на каждое изменение (для отката) —
function pushConfigRevision(changedFields, reason) {
  const revision = (appConfigHistory[appConfigHistory.length - 1]?.revision || 0) + 1;
  appConfigHistory.push({
    revision, changedAt: Date.now(),
    changedFields: changedFields || [], reason: excerpt(reason || '', 200),
    snapshot: { ...appConfig },
  });
  if (appConfigHistory.length > 200) appConfigHistory = appConfigHistory.slice(-200);
  saveJson(APPCONFIG_HISTORY_FILE, appConfigHistory);
  return revision;
}

// — планировщик техработ: окно [startsAt, endsAt) включает/снимает maintenance —
function applyMaintenanceSchedule() {
  if (!maintSchedule) return;
  const start = Date.parse(maintSchedule.startsAt), end = Date.parse(maintSchedule.endsAt);
  if (Number.isNaN(start) || Number.isNaN(end)) { maintSchedule = null; saveJson(MAINT_FILE, maintSchedule); return; }
  const now = Date.now();
  if (now >= start && now < end && !maintSchedule.applied) {
    maintSchedule.prevMaintenance = appConfig.maintenance;
    appConfig.maintenance = maintSchedule.message || 'scheduled maintenance';
    maintSchedule.applied = true;
    saveAppConfig(); saveJson(MAINT_FILE, maintSchedule);
    console.log('[maint] scheduled window started');
  } else if (now >= end && maintSchedule.applied) {
    appConfig.maintenance = maintSchedule.prevMaintenance || '';
    saveAppConfig();
    maintSchedule = null; saveJson(MAINT_FILE, maintSchedule);
    console.log('[maint] scheduled window ended');
  }
}

// ─── Мини-клиент HTTPS (без зависимостей) ────────────────────────────────────

function request(url, options, body) {
  return new Promise((resolve, reject) => {
    const req = https.request(url, options, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => resolve({ status: res.statusCode || 0, body: data }));
    });
    req.on('error', reject);
    req.setTimeout(15000, () => req.destroy(new Error('timeout')));
    if (body) req.write(body);
    req.end();
  });
}

// ─── FCM: OAuth-токен из service account + отправка сообщений ────────────────

let fcmToken = { value: '', expiresAt: 0 };

function b64url(input) {
  return Buffer.from(input).toString('base64url');
}

async function getFcmAccessToken() {
  if (fcmToken.value && Date.now() < fcmToken.expiresAt - 60_000) return fcmToken.value;
  const sa = JSON.parse(fs.readFileSync(SERVICE_ACCOUNT_FILE, 'utf8'));
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claims = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600,
  }));
  const signer = crypto.createSign('RSA-SHA256');
  signer.update(`${header}.${claims}`);
  const jwt = `${header}.${claims}.${signer.sign(sa.private_key, 'base64url')}`;

  const form = new URLSearchParams({
    grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
    assertion: jwt,
  }).toString();
  const res = await request('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  }, form);
  if (res.status !== 200) throw new Error(`oauth ${res.status}: ${res.body.slice(0, 200)}`);
  const json = JSON.parse(res.body);
  fcmToken = { value: json.access_token, expiresAt: Date.now() + json.expires_in * 1000 };
  return fcmToken.value;
}

async function sendPush(deviceToken, data) {
  const dev = findDevByToken(deviceToken);
  // Устройство выключено админом (disable-push) — молча пропускаем.
  if (dev && dev.pe === false) return false;
  const accessToken = await getFcmAccessToken();
  const message = JSON.stringify({
    message: {
      token: deviceToken,
      data,
      android: { priority: 'high' },
    },
  });
  const res = await request(
    `https://fcm.googleapis.com/v1/projects/${FCM_PROJECT_ID}/messages:send`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
    },
    message,
  );
  if (res.status === 404 || res.body.includes('UNREGISTERED')) {
    // Токен устройства умер (переустановка/чистка) — выкидываем из базы.
    recordError('push', 'FCM_UNREGISTERED', 'device token no longer valid');
    recordMetric('pushFail');
    removeDeviceToken(deviceToken);
    return false;
  }
  if (res.status !== 200) {
    console.error(`[fcm] ${res.status}: ${res.body.slice(0, 300)}`);
    recordError('push', 'FCM_' + res.status, 'fcm rejected the message');
    recordMetric('pushFail');
    if (dev) { dev.lastPushAt = Date.now(); dev.lastPushStatus = 'failed'; saveDevices(); }
    return false;
  }
  recordMetric('pushSent');
  if (dev) { dev.lastPushAt = Date.now(); dev.lastPushStatus = 'delivered'; saveDevices(); }
  return true;
}

// ─── Тихие часы: пуш либо уходит сразу, либо копится до конца окна ──────────

function inQuietWindow(dev, now = new Date()) {
  const qs = dev.qs, qe = dev.qe;
  if (!Number.isInteger(qs) || !Number.isInteger(qe) || qs === qe) return false;
  const local = (now.getUTCHours() * 60 + now.getUTCMinutes() + (dev.tz || 0) + 1440) % 1440;
  const h = Math.floor(local / 60);
  return qs < qe ? h >= qs && h < qe : h >= qs || h < qe;
}

async function deliverOrHold(dev, data) {
  if (inQuietWindow(dev)) {
    dev.held = dev.held || [];
    dev.held.push({ ts: Date.now(), title: data.title, body: data.body, url: data.url || '' });
    if (dev.held.length > 60) dev.held = dev.held.slice(-60);
    saveDevices();
    return true; // «доставлено» в очередь
  }
  return sendPush(dev.t, data);
}

async function flushHeld() {
  for (const dev of allDeviceObjects()) {
    if (!dev.held || dev.held.length === 0 || inQuietWindow(dev)) continue;
    const held = dev.held;
    dev.held = [];
    saveDevices();
    const lines = held.slice(0, 8).map((h) => `- ${h.title}: ${excerpt(h.body, 70)}`);
    if (held.length > 8) lines.push(`…and ${held.length - 8} more`);
    await sendPush(dev.t, {
      type: 'digest',
      title: `While you were away - ${held.length} update${held.length === 1 ? '' : 's'}`,
      body: lines.join('\n'),
      repo: '',
      url: held[0]?.url || '',
    }).catch(() => {});
  }
}

// ─── Напоминания о ревью: сутки без ответа → нудж ревьюеру ──────────────────

let reminders = loadJson(REMINDERS_FILE, {});
function saveReminders() { saveJson(REMINDERS_FILE, reminders); }

function trackReminders(event, p) {
  const repo = p.repository?.full_name || '';
  const pr = p.pull_request?.number;
  if (!repo || !pr) return;
  if (event === 'pull_request' && p.action === 'review_requested' && p.requested_reviewer?.login) {
    const reviewer = p.requested_reviewer.login.toLowerCase();
    reminders[`${repo}#${pr}:${reviewer}`] = {
      repo, pr, reviewer,
      title: p.pull_request?.title || '',
      url: p.pull_request?.html_url || '',
      requestedAt: Date.now(),
      nudged: false,
    };
    saveReminders();
  } else if (event === 'pull_request_review' && p.action === 'submitted') {
    const reviewer = p.sender?.login?.toLowerCase();
    if (reviewer && delete reminders[`${repo}#${pr}:${reviewer}`]) saveReminders();
  } else if (event === 'pull_request' && p.action === 'closed') {
    let changed = false;
    for (const k of Object.keys(reminders)) {
      if (k.startsWith(`${repo}#${pr}:`)) { delete reminders[k]; changed = true; }
    }
    if (changed) saveReminders();
  }
}

async function checkReminders() {
  const now = Date.now();
  let changed = false;
  for (const [key, r] of Object.entries(reminders)) {
    if (now - r.requestedAt > 14 * 86_400_000) { delete reminders[key]; changed = true; continue; }
    if (r.nudged || now - r.requestedAt < 24 * 3_600_000) continue;
    r.nudged = true;
    changed = true;
    const hours = Math.round((now - r.requestedAt) / 3_600_000);
    for (const dev of devices[r.reviewer] || []) {
      deliverOrHold(dev, {
        type: 'reminder',
        title: `${r.repo} - PR #${r.pr} awaits your review`,
        body: `"${excerpt(r.title, 80)}" has been waiting ${hours}h for your review.`,
        repo: r.repo,
        url: r.url,
      }).catch(() => {});
    }
  }
  if (changed) saveReminders();
}

// ─── Недельная сводка активности ─────────────────────────────────────────────

let stats = loadJson(STATS_FILE, { lastSent: '', byLogin: {} });
function saveStats() { saveJson(STATS_FILE, stats); }

function bumpStat(login, field, by = 1) {
  if (!login) return;
  const key = String(login).toLowerCase();
  const s = stats.byLogin[key] || (stats.byLogin[key] = {});
  s[field] = (s[field] || 0) + by;
  saveStats();
}

function trackStats(event, p) {
  const sender = p.sender?.login;
  switch (event) {
    case 'push': bumpStat(sender, 'commits', (p.commits || []).length); break;
    case 'pull_request':
      if (p.action === 'opened') bumpStat(sender, 'prsOpened');
      if (p.action === 'closed' && p.pull_request?.merged) bumpStat(p.pull_request?.user?.login, 'prsMerged');
      break;
    case 'issues': if (p.action === 'opened') bumpStat(sender, 'issuesOpened'); break;
    case 'pull_request_review': if (p.action === 'submitted') bumpStat(sender, 'reviews'); break;
    case 'release': if (p.action === 'published') bumpStat(sender, 'releases'); break;
    case 'star': case 'watch':
      if (p.action === 'created' || p.action === 'started') bumpStat(p.repository?.owner?.login, 'stars');
      break;
  }
}

function weeklySummaryLine(s) {
  const parts = [];
  if (s.commits) parts.push(`${s.commits} commit${s.commits === 1 ? '' : 's'}`);
  if (s.prsMerged) parts.push(`${s.prsMerged} PR${s.prsMerged === 1 ? '' : 's'} merged`);
  if (s.prsOpened) parts.push(`${s.prsOpened} PR${s.prsOpened === 1 ? '' : 's'} opened`);
  if (s.issuesOpened) parts.push(`${s.issuesOpened} issue${s.issuesOpened === 1 ? '' : 's'} opened`);
  if (s.reviews) parts.push(`${s.reviews} review${s.reviews === 1 ? '' : 's'}`);
  if (s.releases) parts.push(`${s.releases} release${s.releases === 1 ? '' : 's'}`);
  if (s.stars) parts.push(`${s.stars} new star${s.stars === 1 ? '' : 's'}`);
  return parts.join(' - ');
}

async function checkWeekly() {
  // Воскресенье 17:00 UTC = 20:00 МСК. lastSent хранит дату, чтобы не слать дважды.
  const now = new Date();
  if (now.getUTCDay() !== 0 || now.getUTCHours() !== 17) return;
  const today = now.toISOString().slice(0, 10);
  if (stats.lastSent === today) return;
  stats.lastSent = today;
  for (const [login, s] of Object.entries(stats.byLogin)) {
    const line = weeklySummaryLine(s);
    if (!line) continue;
    for (const dev of devices[login] || []) {
      sendPush(dev.t, {
        type: 'weekly',
        title: 'Your GitHub week',
        body: line,
        repo: '',
        url: '',
      }).catch(() => {});
    }
  }
  stats.byLogin = {};
  saveStats();
}

setInterval(() => {
  flushHeld().catch((e) => console.error('[digest]', e.message));
  checkReminders().catch((e) => console.error('[reminder]', e.message));
  checkWeekly().catch((e) => console.error('[weekly]', e.message));
  try { applyMaintenanceSchedule(); } catch (e) { console.error('[maint]', e.message); }
  try { pruneMetrics(); } catch (e) { console.error('[metrics]', e.message); }
}, 5 * 60_000);

// Если сервер перезапустился внутри запланированного окна — применить сразу.
applyMaintenanceSchedule();

// ─── Вебхуки GitHub → адресаты и текст уведомления ───────────────────────────

function verifySignature(raw, signatureHeader) {
  if (!signatureHeader || !signatureHeader.startsWith('sha256=')) return false;
  const expected = 'sha256=' + crypto.createHmac('sha256', WEBHOOK_SECRET).update(raw).digest('hex');
  const a = Buffer.from(signatureHeader);
  const b = Buffer.from(expected);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

// Кого будить: владелец репозитория + автор/назначенные из payload.
function targetLogins(payload) {
  const logins = new Set();
  const add = (l) => { if (l) logins.add(String(l).toLowerCase()); };
  add(payload.repository?.owner?.login);
  add(payload.issue?.user?.login);
  add(payload.pull_request?.user?.login);
  for (const a of payload.issue?.assignees || []) add(a.login);
  for (const a of payload.pull_request?.assignees || []) add(a.login);
  for (const r of payload.pull_request?.requested_reviewers || []) add(r.login);
  // Не будим того, кто сам совершил действие.
  const sender = payload.sender?.login?.toLowerCase();
  if (sender) logins.delete(sender);
  return [...logins];
}

// Обрезка длинных текстов для тела нотификации (BigTextStyle на клиенте раскрывает до ~450).
function excerpt(text, max = 300) {
  const t = String(text || '').replace(/\r/g, '').trim();
  return t.length > max ? t.slice(0, max - 1) + '…' : t;
}

function describeEvent(event, p) {
  const repo = p.repository?.full_name || '';
  switch (event) {
    case 'push': {
      const commits = p.commits || [];
      const branch = String(p.ref || '').replace('refs/heads/', '');
      const head = excerpt(commits[commits.length - 1]?.message?.split('\n')[0], 120);
      const extra = commits.length > 1 ? ` (+${commits.length - 1} more)` : '';
      return {
        title: `${repo} · ${branch}`,
        body: `${p.sender?.login} pushed: ${head}${extra}`,
      };
    }
    case 'issues':
      return {
        title: `${repo} · issue #${p.issue?.number} ${p.action}`,
        body: excerpt(`${p.issue?.title || ''}\n\n${p.issue?.body || ''}`),
      };
    case 'issue_comment':
      return {
        title: `${repo} · ${p.sender?.login} → #${p.issue?.number} ${excerpt(p.issue?.title, 60)}`,
        body: excerpt(p.comment?.body),
      };
    case 'pull_request':
      return {
        title: `${repo} · PR #${p.pull_request?.number} ${p.action}`,
        body: excerpt(`${p.pull_request?.title || ''}\n${p.pull_request?.head?.ref} → ${p.pull_request?.base?.ref}`),
      };
    case 'pull_request_review': {
      const state = p.review?.state || 'reviewed';
      return {
        title: `${repo} · ${p.sender?.login} ${state} PR #${p.pull_request?.number}`,
        body: excerpt(p.review?.body || p.pull_request?.title),
      };
    }
    case 'workflow_run': {
      const run = p.workflow_run || {};
      // Старты ранов — отдельный тип ci_start: клиент кладёт его в канал
      // «CI starts», выключенный по умолчанию (opt-in в настройках уведомлений).
      if (p.action === 'in_progress') {
        return {
          type: 'ci_start',
          title: `${repo} · ${run.head_branch}`,
          body: `▶️ workflow «${run.name}» started (run #${run.run_number})`,
        };
      }
      if (p.action !== 'completed') return null;
      const mark = run.conclusion === 'success' ? '✅' : run.conclusion === 'failure' ? '❌' : '▫️';
      return {
        title: `${repo} · ${run.head_branch}`,
        body: `${mark} workflow «${run.name}» ${run.conclusion} (run #${run.run_number})`,
      };
    }
    case 'check_suite': {
      if (p.action !== 'completed' || p.check_suite?.conclusion === 'success') return null;
      return {
        title: `${repo} · ${p.check_suite?.head_branch}`,
        body: `❌ checks ${p.check_suite?.conclusion}`,
      };
    }
    case 'release':
      if (p.action !== 'published') return null;
      return {
        title: `${repo} · release ${p.release?.tag_name}`,
        body: excerpt(p.release?.name || p.release?.body || 'published'),
      };
    case 'discussion':
      if (p.action !== 'created' && p.action !== 'answered') return null;
      return {
        title: `${repo} · discussion ${p.action}`,
        body: excerpt(`${p.discussion?.title || ''}\n\n${p.discussion?.body || ''}`),
      };
    case 'discussion_comment':
      if (p.action !== 'created') return null;
      return {
        title: `${repo} · ${p.sender?.login} → ${excerpt(p.discussion?.title, 60)}`,
        body: excerpt(p.comment?.body),
      };
    case 'create': // новая ветка или тег
      return {
        title: repo,
        body: `${p.sender?.login} created ${p.ref_type === 'tag' ? 'tag' : 'branch'} ${p.ref}`,
      };
    case 'delete':
      return {
        title: repo,
        body: `${p.sender?.login} deleted ${p.ref_type === 'tag' ? 'tag' : 'branch'} ${p.ref}`,
      };
    case 'member':
      if (p.action !== 'added') return null;
      return {
        title: `${repo} · collaborators`,
        body: `${p.sender?.login} added ${p.member?.login} to the repository`,
      };
    case 'fork':
      return {
        title: repo,
        body: `🍴 ${p.sender?.login} forked → ${p.forkee?.full_name || ''}`,
      };
    case 'star':
      if (p.action !== 'created') return null;
      return { title: repo, body: `⭐ ${p.sender?.login} starred the repo` };
    case 'watch': // легаси-вариант события звезды
      if (p.action !== 'started') return null;
      return { title: repo, body: `⭐ ${p.sender?.login} starred the repo` };
    case 'deployment_status': {
      const st = p.deployment_status?.state;
      if (!st || st === 'pending' || st === 'queued' || st === 'in_progress') return null;
      const mark = st === 'success' ? '✅' : '❌';
      return {
        title: `${repo} · deploy ${p.deployment?.environment || ''}`,
        body: `${mark} ${st}`,
      };
    }
    default:
      return null; // прочие события молча игнорируем
  }
}

async function handleWebhook(event, payload) {
  trackReminders(event, payload);
  trackStats(event, payload);
  const note = describeEvent(event, payload);
  if (!note) return { delivered: 0, reason: 'event ignored' };
  const logins = targetLogins(payload);
  const seen = new Set();
  const targets = [];
  for (const l of logins) for (const d of devices[l] || []) {
    if (!seen.has(d.t)) { seen.add(d.t); targets.push(d); }
  }
  let delivered = 0;
  for (const dev of targets) {
    const ok = await deliverOrHold(dev, {
      type: note.type || event,
      title: note.title,
      body: note.body,
      repo: payload.repository?.full_name || '',
      url: payload.pull_request?.html_url || payload.issue?.html_url ||
           payload.workflow_run?.html_url || payload.release?.html_url ||
           payload.repository?.html_url || '',
    });
    if (ok) delivered++;
  }
  return { delivered, targets: logins };
}

// ─── Регистрация устройств ───────────────────────────────────────────────────

async function githubLogin(userToken) {
  const res = await request(`${GITHUB_API}/user`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${userToken}`,
      'User-Agent': 'gsgit-push-backend',
      Accept: 'application/vnd.github+json',
    },
  });
  if (res.status !== 200) return null;
  try { return JSON.parse(res.body).login || null; } catch (_) { return null; }
}

// ─── HTTP-сервер ─────────────────────────────────────────────────────────────

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY) { req.destroy(); reject(new Error('body too large')); return; }
      chunks.push(c);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

// CORS: админ-панель (веб) живёт на другом origin и обращается к этому API из
// браузера. Авторизация — через заголовок X-Admin-Key (не cookie), поэтому
// wildcard-origin безопасен: чужой сайт ключа не знает и запрос не пройдёт.
const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, X-Admin-Key',
  'Access-Control-Max-Age': '86400',
};

function send(res, status, obj) {
  try {
    recordMetric('req');
    if (status >= 500) recordMetric('e5xx');
    else if (status >= 400) recordMetric('e4xx');
  } catch (_) { /* метрики не должны влиять на ответ */ }
  const body = JSON.stringify(obj);
  res.writeHead(status, { 'Content-Type': 'application/json', ...CORS_HEADERS });
  res.end(body);
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url || '/', 'http://localhost');

    // Префлайт CORS для браузерной админ-панели.
    if (req.method === 'OPTIONS') {
      res.writeHead(204, CORS_HEADERS);
      return res.end();
    }

    if (req.method === 'GET' && url.pathname === '/healthz') {
      return send(res, 200, { ok: true, devices: Object.keys(devices).length });
    }

    // Публичный конфиг приложения: техработы, версии, чейнджлог.
    if (req.method === 'GET' && url.pathname === '/appconfig') {
      return send(res, 200, appConfig);
    }

    // Правка конфига из админки (X-Admin-Key). Каждое изменение → ревизия (для отката).
    if (req.method === 'POST' && url.pathname === '/admin/appconfig') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const body = await jsonBody(req);
      if (body === undefined) return send(res, 400, { error: 'bad json' });
      const changed = [];
      for (const f of APPCONFIG_FIELDS) {
        if (typeof body[f] === 'string' && body[f] !== appConfig[f]) { appConfig[f] = body[f]; changed.push(f); }
      }
      saveAppConfig();
      const revision = changed.length ? pushConfigRevision(changed, body.reason) : (appConfigHistory[appConfigHistory.length - 1]?.revision || 0);
      audit(req, 'appconfig.update', 'ok', { changedFields: changed, revision });
      console.log('[admin] appconfig updated (rev', revision + ')');
      return send(res, 200, appConfig);
    }

    // Список зарегистрированных push-устройств (для админ-панели).
    if (req.method === 'GET' && url.pathname === '/admin/devices') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const qLogin = String(url.searchParams.get('login') || '').toLowerCase();
      const qStatus = String(url.searchParams.get('status') || '');
      const mapDev = (d) => ({
        deviceId: deviceIdOf(d.t),
        name: d.name || '',
        appVersion: d.appVersion || '',
        tzOffsetMin: d.tz ?? 0,
        quietHours: (Number.isInteger(d.qs) && Number.isInteger(d.qe) && d.qs !== d.qe)
          ? { start: d.qs, end: d.qe } : null,
        heldCount: (d.held || []).length,
        pushEnabled: d.pe !== false,
        registeredAt: d.registeredAt ? new Date(d.registeredAt).toISOString() : '',
        lastSeenAt: d.lastSeenAt ? new Date(d.lastSeenAt).toISOString() : '',
        lastPushAt: d.lastPushAt ? new Date(d.lastPushAt).toISOString() : '',
        lastPushStatus: d.lastPushStatus || '',
        tokenTail: String(d.t || '').slice(-6),
      });
      const list = Object.keys(devices).sort()
        .filter((login) => !qLogin || login.includes(qLogin))
        .map((login) => ({
          login,
          count: devices[login].length,
          devices: devices[login].filter((d) => qStatus !== 'active' || d.pe !== false).map(mapDev),
        }))
        .filter((g) => g.devices.length > 0);
      return send(res, 200, { logins: list.length, devices: list });
    }

    // Сводная статистика (для дашборда админ-панели).
    if (req.method === 'GET' && url.pathname === '/admin/stats') {
      const given = String(req.headers['x-admin-key'] || '');
      if (!given || given !== ADMIN_KEY) return send(res, 401, { error: 'bad admin key' });
      const allDevices = allDeviceObjects();
      const held = allDevices.reduce((n, d) => n + (d.held || []).length, 0);
      return send(res, 200, {
        logins: Object.keys(devices).length,
        devices: allDevices.length,
        quietEnabled: allDevices.filter((d) => Number.isInteger(d.qs) && Number.isInteger(d.qe) && d.qs !== d.qe).length,
        heldPushes: held,
        maintenance: appConfig.maintenance ? 'on' : 'off',
        latestVersion: appConfig.latestVersion,
        minVersion: appConfig.minVersion,
      });
    }

    // ── Расширенная админка (всё под X-Admin-Key, строго аддитивно) ────────────

    // 1. Живость сервера (реальная, не факт ответа).
    if (req.method === 'GET' && url.pathname === '/admin/health') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      let fbOk = false;
      try { fbOk = !!FCM_PROJECT_ID && fs.existsSync(SERVICE_ACCOUNT_FILE); } catch (_) {}
      const held = allDeviceObjects().reduce((n, d) => n + (d.held || []).length, 0);
      return send(res, 200, {
        status: 'ok',
        uptimeSec: Math.floor((Date.now() - startedAt) / 1000),
        serverVersion: SERVER_VERSION,
        database: 'ok',
        firebase: fbOk ? 'ok' : 'off',
        githubWebhooks: WEBHOOK_SECRET ? 'ok' : 'off',
        pushQueue: held,
        serverTime: new Date().toISOString(),
      });
    }

    // 2. Метрики за период (1h/24h/7d/30d или ?hours=N).
    if (req.method === 'GET' && url.pathname === '/admin/metrics') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const period = String(url.searchParams.get('period') || '24h');
      const map = { '1h': 1, '24h': 24, '7d': 168, '30d': 720 };
      const hours = Number(url.searchParams.get('hours')) || map[period] || 24;
      const m = metricsFor(hours);
      const active = allDeviceObjects();
      return send(res, 200, {
        period, hours,
        registrations: m.reg || 0,
        activeDevices: active.length,
        pushesSent: m.pushSent || 0,
        pushesFailed: m.pushFail || 0,
        heldPushes: active.reduce((n, d) => n + (d.held || []).length, 0),
        githubEvents: m.gh || 0,
        requests: m.req || 0,
        responses4xx: m.e4xx || 0,
        responses5xx: m.e5xx || 0,
      });
    }

    // 3. История рассылок (newest-first; токены наружу не отдаём).
    if (req.method === 'GET' && url.pathname === '/admin/announcements') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const { items, nextCursor } = paginateDesc(announcements, url.searchParams.get('limit'), url.searchParams.get('cursor'));
      const pub = items.map(({ failedTokens, body, ...rest }) => ({ ...rest, createdAt: new Date(rest.createdAt).toISOString() }));
      return send(res, 200, { items: pub, nextCursor });
    }
    {
      const m = url.pathname.match(/^\/admin\/announcements\/([A-Za-z0-9_]+)$/);
      if (m && req.method === 'GET') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const a = announcements.find((x) => x.id === m[1]);
        if (!a) return send(res, 404, { error: 'not found' });
        const { failedTokens, ...pub } = a;
        return send(res, 200, { ...pub, createdAt: new Date(a.createdAt).toISOString() });
      }
    }
    {
      const m = url.pathname.match(/^\/admin\/announcements\/([A-Za-z0-9_]+)\/retry$/);
      if (m && req.method === 'POST') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const a = announcements.find((x) => x.id === m[1]);
        if (!a) return send(res, 404, { error: 'not found' });
        const tokens = (a.failedTokens || []).slice();
        let delivered = 0; const stillFailed = [];
        for (const t of tokens) {
          const ok = await sendPush(t, { type: 'announce', title: a.title, body: a.body, repo: '', url: a.url || '' }).catch(() => false);
          if (ok) delivered++; else stillFailed.push(t);
        }
        a.delivered += delivered; a.failed = stillFailed.length; a.failedTokens = stillFailed;
        saveJson(ANNOUNCEMENTS_FILE, announcements);
        audit(req, 'announce.retry', 'ok', { id: a.id, delivered });
        return send(res, 200, { ok: true, delivered, stillFailed: stillFailed.length });
      }
    }
    {
      const m = url.pathname.match(/^\/admin\/announcements\/([A-Za-z0-9_]+)\/cancel$/);
      if (m && req.method === 'POST') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        // Броадкаст выполняется синхронно — «начатую» рассылку отменять нечего.
        return send(res, 409, { error: 'broadcasts complete synchronously - nothing to cancel' });
      }
    }

    // 4. Тест-пуш на одно устройство по deviceId.
    {
      const m = url.pathname.match(/^\/admin\/devices\/([a-f0-9]{16})\/test-push$/);
      if (m && req.method === 'POST') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const found = findByDeviceId(m[1]);
        if (!found) return send(res, 404, { error: 'device not found' });
        const body = (await jsonBody(req)) || {};
        const ok = await sendPush(found.dev.t, {
          type: 'announce',
          title: String(body.title || 'GsGit test push'),
          body: String(body.body || 'Test notification from the admin panel.'),
          repo: '', url: String(body.url || ''),
        }).catch(() => false);
        audit(req, 'device.test-push', ok ? 'ok' : 'failed', { login: found.login, deviceId: m[1] });
        return send(res, ok ? 200 : 502, { ok, delivered: ok ? 1 : 0 });
      }
    }

    // 5. Диагностика устройств: clear-held / disable-push / детали / удаление.
    {
      const m = url.pathname.match(/^\/admin\/devices\/([a-f0-9]{16})\/clear-held$/);
      if (m && req.method === 'POST') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const found = findByDeviceId(m[1]);
        if (!found) return send(res, 404, { error: 'device not found' });
        const cleared = (found.dev.held || []).length;
        found.dev.held = []; saveDevices();
        audit(req, 'device.clear-held', 'ok', { login: found.login, deviceId: m[1], cleared });
        return send(res, 200, { ok: true, cleared });
      }
    }
    {
      const m = url.pathname.match(/^\/admin\/devices\/([a-f0-9]{16})\/disable-push$/);
      if (m && req.method === 'POST') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const found = findByDeviceId(m[1]);
        if (!found) return send(res, 404, { error: 'device not found' });
        const body = (await jsonBody(req)) || {};
        const enabled = body.enabled === true; // по умолчанию выключаем
        found.dev.pe = enabled; saveDevices();
        audit(req, 'device.push-toggle', 'ok', { login: found.login, deviceId: m[1], enabled });
        return send(res, 200, { ok: true, pushEnabled: enabled });
      }
    }
    {
      const m = url.pathname.match(/^\/admin\/devices\/([a-f0-9]{16})$/);
      if (m && (req.method === 'GET' || req.method === 'DELETE')) {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const found = findByDeviceId(m[1]);
        if (!found) return send(res, 404, { error: 'device not found' });
        if (req.method === 'DELETE') {
          removeDeviceToken(found.dev.t);
          audit(req, 'device.delete', 'ok', { login: found.login, deviceId: m[1] });
          return send(res, 200, { ok: true, removed: true });
        }
        const d = found.dev;
        return send(res, 200, {
          deviceId: m[1], login: found.login, name: d.name || '', appVersion: d.appVersion || '',
          tzOffsetMin: d.tz ?? 0,
          quietHours: (Number.isInteger(d.qs) && Number.isInteger(d.qe) && d.qs !== d.qe) ? { start: d.qs, end: d.qe } : null,
          heldCount: (d.held || []).length, pushEnabled: d.pe !== false,
          registeredAt: d.registeredAt ? new Date(d.registeredAt).toISOString() : '',
          lastSeenAt: d.lastSeenAt ? new Date(d.lastSeenAt).toISOString() : '',
          lastPushAt: d.lastPushAt ? new Date(d.lastPushAt).toISOString() : '',
          lastPushStatus: d.lastPushStatus || '', tokenTail: String(d.t || '').slice(-6),
        });
      }
    }

    // 6. История конфигурации и откат.
    if (req.method === 'GET' && url.pathname === '/admin/appconfig/history') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const { items, nextCursor } = paginateDesc(appConfigHistory, url.searchParams.get('limit'), url.searchParams.get('cursor'));
      const pub = items.map((r) => ({ revision: r.revision, changedAt: new Date(r.changedAt).toISOString(), changedFields: r.changedFields, reason: r.reason }));
      return send(res, 200, { items: pub, nextCursor });
    }
    {
      const m = url.pathname.match(/^\/admin\/appconfig\/history\/(\d+)$/);
      if (m && req.method === 'GET') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const r = appConfigHistory.find((x) => x.revision === Number(m[1]));
        if (!r) return send(res, 404, { error: 'not found' });
        return send(res, 200, { revision: r.revision, changedAt: new Date(r.changedAt).toISOString(), changedFields: r.changedFields, reason: r.reason, snapshot: r.snapshot });
      }
    }
    if (req.method === 'POST' && url.pathname === '/admin/appconfig/rollback') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const body = await jsonBody(req);
      if (body === undefined) return send(res, 400, { error: 'bad json' });
      const r = appConfigHistory.find((x) => x.revision === Number(body.revision));
      if (!r) return send(res, 404, { error: 'revision not found' });
      for (const f of APPCONFIG_FIELDS) if (typeof r.snapshot[f] === 'string') appConfig[f] = r.snapshot[f];
      saveAppConfig();
      const rev = pushConfigRevision(APPCONFIG_FIELDS.slice(), `rollback to revision ${r.revision}`);
      audit(req, 'appconfig.rollback', 'ok', { toRevision: r.revision, newRevision: rev });
      return send(res, 200, appConfig);
    }
    if (req.method === 'POST' && url.pathname === '/admin/appconfig/validate') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const body = await jsonBody(req);
      if (body === undefined) return send(res, 400, { error: 'bad json' });
      const errors = [];
      for (const f of ['latestVersion', 'minVersion']) {
        if (typeof body[f] === 'string' && body[f] && !VERSION_RE.test(body[f])) errors.push(`${f} is not x.y.z`);
      }
      if (typeof body.minVersion === 'string' && typeof body.latestVersion === 'string' &&
          VERSION_RE.test(body.minVersion) && VERSION_RE.test(body.latestVersion) &&
          cmpVersions(body.minVersion, body.latestVersion) > 0) {
        errors.push('minVersion is greater than latestVersion');
      }
      return send(res, 200, { ok: errors.length === 0, errors });
    }

    // 7. Планирование техработ.
    if (req.method === 'POST' && url.pathname === '/admin/maintenance/schedule') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const body = await jsonBody(req);
      if (body === undefined) return send(res, 400, { error: 'bad json' });
      const start = Date.parse(body.startsAt), end = Date.parse(body.endsAt);
      if (Number.isNaN(start) || Number.isNaN(end) || end <= start) return send(res, 400, { error: 'bad startsAt/endsAt' });
      maintSchedule = { startsAt: new Date(start).toISOString(), endsAt: new Date(end).toISOString(), message: String(body.message || 'scheduled maintenance'), applied: false, prevMaintenance: '' };
      saveJson(MAINT_FILE, maintSchedule);
      applyMaintenanceSchedule();
      audit(req, 'maintenance.schedule', 'ok', { startsAt: maintSchedule.startsAt, endsAt: maintSchedule.endsAt });
      return send(res, 200, { ok: true, schedule: maintSchedule });
    }
    if (req.method === 'GET' && url.pathname === '/admin/maintenance') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      return send(res, 200, { maintenanceNow: appConfig.maintenance || '', schedule: maintSchedule });
    }
    if (req.method === 'DELETE' && url.pathname === '/admin/maintenance/schedule') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      if (maintSchedule && maintSchedule.applied) { appConfig.maintenance = maintSchedule.prevMaintenance || ''; saveAppConfig(); }
      maintSchedule = null; saveJson(MAINT_FILE, maintSchedule);
      audit(req, 'maintenance.unschedule', 'ok', {});
      return send(res, 200, { ok: true });
    }
    if (req.method === 'POST' && url.pathname === '/admin/maintenance/stop') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      appConfig.maintenance = ''; saveAppConfig();
      maintSchedule = null; saveJson(MAINT_FILE, maintSchedule);
      pushConfigRevision(['maintenance'], 'maintenance stopped');
      audit(req, 'maintenance.stop', 'ok', {});
      return send(res, 200, { ok: true });
    }

    // 8. Управление релизами (атомарная публикация в appConfig).
    if (req.method === 'GET' && url.pathname === '/admin/releases') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const { items, nextCursor } = paginateDesc(releases, url.searchParams.get('limit'), url.searchParams.get('cursor'));
      return send(res, 200, { items, nextCursor });
    }
    if (req.method === 'POST' && url.pathname === '/admin/releases') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const body = await jsonBody(req);
      if (body === undefined) return send(res, 400, { error: 'bad json' });
      if (typeof body.version !== 'string' || !VERSION_RE.test(body.version)) return send(res, 400, { error: 'bad version' });
      const existing = releases.find((r) => r.version === body.version);
      const rec = existing || { version: body.version, createdAt: Date.now(), publishedAt: '' };
      rec.changelog = String(body.changelog ?? rec.changelog ?? '');
      rec.url = String(body.url ?? rec.url ?? '');
      rec.sha256 = String(body.sha256 ?? rec.sha256 ?? '');
      rec.mandatory = body.mandatory === true || rec.mandatory === true;
      rec.rollout = Number.isFinite(body.rollout) ? body.rollout : (rec.rollout ?? 100);
      if (!existing) releases.push(rec);
      saveJson(RELEASES_FILE, releases);
      audit(req, 'release.upsert', 'ok', { version: rec.version });
      return send(res, 200, rec);
    }
    {
      const m = url.pathname.match(/^\/admin\/releases\/([0-9A-Za-z.\-]+)\/publish$/);
      if (m && req.method === 'POST') {
        if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
        const rec = releases.find((r) => r.version === m[1]);
        if (!rec) return send(res, 404, { error: 'release not found' });
        appConfig.latestVersion = rec.version;
        if (rec.changelog) appConfig.changelog = rec.changelog;
        if (rec.url) appConfig.downloadUrl = rec.url;
        if (rec.mandatory) appConfig.minVersion = rec.version;
        saveAppConfig();
        rec.publishedAt = new Date().toISOString();
        saveJson(RELEASES_FILE, releases);
        const rev = pushConfigRevision(['latestVersion', 'changelog', 'downloadUrl'], `publish release ${rec.version}`);
        audit(req, 'release.publish', 'ok', { version: rec.version, revision: rev });
        return send(res, 200, { ok: true, appConfig });
      }
    }

    // 9. Журнал админ-действий.
    if (req.method === 'GET' && url.pathname === '/admin/audit') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const { items, nextCursor } = paginateDesc(auditLog, url.searchParams.get('limit'), url.searchParams.get('cursor'));
      const pub = items.map((a) => ({ id: a.id, at: new Date(a.at).toISOString(), ip: a.ip, action: a.action, result: a.result, meta: a.meta }));
      return send(res, 200, { items: pub, nextCursor });
    }

    // 10. Серверные ошибки (агрегированные, без ключей/токенов/стектрейсов).
    if (req.method === 'GET' && url.pathname === '/admin/errors') {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const service = String(url.searchParams.get('service') || '');
      const limit = Math.min(Math.max(Number(url.searchParams.get('limit')) || 50, 1), 200);
      let items = Object.values(errorsLog);
      if (service) items = items.filter((e) => e.service === service);
      items.sort((a, b) => b.lastAt - a.lastAt);
      items = items.slice(0, limit).map((e) => ({
        id: e.id, service: e.service, code: e.code, message: e.message, count: e.count,
        createdAt: new Date(e.createdAt).toISOString(), lastAt: new Date(e.lastAt).toISOString(),
      }));
      return send(res, 200, { items });
    }

    // Обновление user-token'а GsGit GitHub App через backend: устройство шлёт
    // свой refresh_token, сервер добавляет client_secret (которого на устройстве
    // нет) и меняет его в GitHub на свежую пару. Токены НЕ логируем.
    if (req.method === 'POST' && url.pathname === '/auth/refresh') {
      if (!GH_APP_CLIENT_SECRET) return send(res, 503, { error: 'oauth not configured' });
      const body = await jsonBody(req);
      const refreshToken = body && typeof body.refresh_token === 'string' ? body.refresh_token.trim() : '';
      if (!refreshToken) return send(res, 400, { error: 'refresh_token required' });
      const form = new URLSearchParams({
        client_id: GH_APP_CLIENT_ID,
        client_secret: GH_APP_CLIENT_SECRET,
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
      }).toString();
      let gh;
      try {
        gh = await request(`${GITHUB_WEB}/login/oauth/access_token`, {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(form),
            'User-Agent': 'GsGit-Server',
          },
        }, form);
      } catch (_) {
        recordError('auth', 'REFRESH_NET', 'github unreachable during token refresh');
        recordMetric('authRefreshFail');
        return send(res, 502, { error: 'github unreachable' });
      }
      let data;
      try { data = JSON.parse(gh.body); } catch (_) { data = {}; }
      if (data.error || !data.access_token) {
        recordError('auth', 'REFRESH_' + (data.error || gh.status), 'github refused token refresh');
        recordMetric('authRefreshFail');
        return send(res, 401, { error: data.error || 'refresh rejected' });
      }
      recordMetric('authRefresh');
      console.log('[auth] user token refreshed');
      return send(res, 200, {
        access_token: data.access_token,
        expires_in: data.expires_in || 0,
        refresh_token: data.refresh_token || '',
        refresh_token_expires_in: data.refresh_token_expires_in || 0,
        token_type: data.token_type || 'bearer',
      });
    }

    if (req.method === 'POST' && url.pathname === '/webhook') {
      if (!WEBHOOK_SECRET) return send(res, 503, { error: 'webhook secret not configured' });
      const raw = await readBody(req);
      if (!verifySignature(raw, req.headers['x-hub-signature-256'])) {
        return send(res, 401, { error: 'bad signature' });
      }
      const event = String(req.headers['x-github-event'] || '');
      let payload;
      try { payload = JSON.parse(raw.toString('utf8')); } catch (_) {
        return send(res, 400, { error: 'bad json' });
      }
      recordMetric('gh');
      const result = await handleWebhook(event, payload);
      console.log(`[webhook] ${event} ${payload.action || ''} -> ${JSON.stringify(result)}`);
      return send(res, 200, result);
    }

    if (req.method === 'POST' && url.pathname === '/register') {
      const auth = String(req.headers.authorization || '');
      if (!auth.startsWith('Bearer ')) return send(res, 401, { error: 'missing token' });
      const raw = await readBody(req);
      let body;
      try { body = JSON.parse(raw.toString('utf8')); } catch (_) {
        return send(res, 400, { error: 'bad json' });
      }
      if (!body.fcmToken) return send(res, 400, { error: 'fcmToken required' });
      const login = await githubLogin(auth.slice(7).trim());
      if (!login) return send(res, 401, { error: 'github token rejected' });
      const isNewDevice = addDevice(login, body.fcmToken, {
        name: body.device,
        qs: body.quietStart,
        qe: body.quietEnd,
        tz: body.tzOffsetMin,
        appVersion: body.appVersion,
      });
      recordMetric('reg');
      console.log(`[register] ${login}${isNewDevice ? ' (new device)' : ''}`);
      // Security-пуш: о входе с нового устройства предупреждаем все ОСТАЛЬНЫЕ
      // устройства этого аккаунта (сам новичок уведомление не получает).
      if (isNewDevice) {
        const others = (devices[login.toLowerCase()] || []).filter((d) => d.t !== body.fcmToken).map((d) => d.t);
        const deviceName = excerpt(body.device, 48) || 'a new device';
        for (const t of others) {
          sendPush(t, {
            type: 'security',
            title: 'GsGit · account sign-in',
            body: `Push notifications were enabled on a new device: ${deviceName}. If this wasn't you, revoke GsGit App access in your GitHub settings.`,
            repo: '',
            url: 'https://github.com/settings/apps/authorizations',
          }).catch(() => {});
        }
      }
      return send(res, 200, { ok: true, login });
    }

    if (req.method === 'POST' && url.pathname === '/announce') {
      // Броадкаст всем устройствам. Ключ — X-Admin-Key (env ADMIN_KEY или /data/admin.key).
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });
      const body = await jsonBody(req);
      if (body === undefined) return send(res, 400, { error: 'bad json' });
      if (!body.title || !body.body) return send(res, 400, { error: 'title and body required' });
      const targets = allDeviceObjects();
      let delivered = 0;
      const failedTokens = [];
      for (const dev of targets) {
        const ok = await sendPush(dev.t, {
          type: 'announce',
          title: String(body.title),
          body: String(body.body),
          repo: '',
          url: String(body.url || ''),
        }).catch(() => false);
        if (ok) delivered++; else failedTokens.push(dev.t);
      }
      const rec = {
        id: 'ann_' + crypto.randomBytes(5).toString('hex'),
        title: String(body.title), body: String(body.body), url: String(body.url || ''),
        createdAt: Date.now(), targeted: targets.length, delivered, failed: failedTokens.length,
        status: 'completed', failedTokens,
      };
      announcements.push(rec);
      if (announcements.length > 500) announcements = announcements.slice(-500);
      saveJson(ANNOUNCEMENTS_FILE, announcements);
      audit(req, 'announce.send', 'ok', { id: rec.id, delivered, failed: rec.failed });
      console.log(`[announce] "${body.title}" -> ${delivered} devices`);
      return send(res, 200, { ok: true, delivered });
    }

    if (req.method === 'POST' && url.pathname === '/unregister') {
      const raw = await readBody(req);
      let body;
      try { body = JSON.parse(raw.toString('utf8')); } catch (_) {
        return send(res, 400, { error: 'bad json' });
      }
      if (!body.fcmToken) return send(res, 400, { error: 'fcmToken required' });
      return send(res, 200, { ok: true, removed: removeDeviceToken(body.fcmToken) });
    }

    send(res, 404, { error: 'not found' });
  } catch (e) {
    console.error('[error]', e.message);
    send(res, 500, { error: 'internal' });
  }
});

server.listen(PORT, () => console.log(`gsgit push backend on :${PORT}`));
