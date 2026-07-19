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
const MAX_BODY = 1024 * 1024; // GitHub шлёт до ~25 МБ, но нам столько не нужно — режем на 1 МБ

if (!WEBHOOK_SECRET) console.warn('[warn] GITHUB_WEBHOOK_SECRET не задан — /webhook будет отвечать 503');
if (!FCM_PROJECT_ID) console.warn('[warn] FCM_PROJECT_ID не задан — пуши отправляться не будут');

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
  if (!entry) { entry = { t: fcmToken }; list.push(entry); }
  entry.name = meta.name || entry.name || '';
  entry.qs = Number.isInteger(meta.qs) ? meta.qs : (entry.qs ?? null);
  entry.qe = Number.isInteger(meta.qe) ? meta.qe : (entry.qe ?? null);
  entry.tz = Number.isInteger(meta.tz) ? meta.tz : (entry.tz ?? 0);
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
    removeDeviceToken(deviceToken);
    return false;
  }
  if (res.status !== 200) {
    console.error(`[fcm] ${res.status}: ${res.body.slice(0, 300)}`);
    return false;
  }
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
}, 5 * 60_000);

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
      const extra = commits.length > 1 ? ` (+${commits.length - 1} ещё)` : '';
      return {
        title: `${repo} · ${branch}`,
        body: `${p.sender?.login} запушил: ${head}${extra}`,
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
        body: `${p.sender?.login} создал ${p.ref_type === 'tag' ? 'тег' : 'ветку'} ${p.ref}`,
      };
    case 'delete':
      return {
        title: repo,
        body: `${p.sender?.login} удалил ${p.ref_type === 'tag' ? 'тег' : 'ветку'} ${p.ref}`,
      };
    case 'member':
      if (p.action !== 'added') return null;
      return {
        title: `${repo} · коллабораторы`,
        body: `${p.sender?.login} добавил ${p.member?.login} в репозиторий`,
      };
    case 'fork':
      return {
        title: repo,
        body: `🍴 ${p.sender?.login} сделал форк → ${p.forkee?.full_name || ''}`,
      };
    case 'star':
      if (p.action !== 'created') return null;
      return { title: repo, body: `⭐ ${p.sender?.login} поставил звезду` };
    case 'watch': // легаси-вариант события звезды
      if (p.action !== 'started') return null;
      return { title: repo, body: `⭐ ${p.sender?.login} поставил звезду` };
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

function send(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(body);
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url || '/', 'http://localhost');

    if (req.method === 'GET' && url.pathname === '/healthz') {
      return send(res, 200, { ok: true, devices: Object.keys(devices).length });
    }

    // Публичный конфиг приложения: техработы, версии, чейнджлог.
    if (req.method === 'GET' && url.pathname === '/appconfig') {
      return send(res, 200, appConfig);
    }

    // Правка конфига из встроенной админки GsGit (X-Admin-Key).
    if (req.method === 'POST' && url.pathname === '/admin/appconfig') {
      const given = String(req.headers['x-admin-key'] || '');
      if (!given || given !== ADMIN_KEY) return send(res, 401, { error: 'bad admin key' });
      const raw = await readBody(req);
      let body;
      try { body = JSON.parse(raw.toString('utf8')); } catch (_) {
        return send(res, 400, { error: 'bad json' });
      }
      for (const f of APPCONFIG_FIELDS) {
        if (typeof body[f] === 'string') appConfig[f] = body[f];
      }
      saveAppConfig();
      console.log('[admin] appconfig updated');
      return send(res, 200, appConfig);
    }

    // Список зарегистрированных push-устройств (для админ-панели).
    if (req.method === 'GET' && url.pathname === '/admin/devices') {
      const given = String(req.headers['x-admin-key'] || '');
      if (!given || given !== ADMIN_KEY) return send(res, 401, { error: 'bad admin key' });
      const list = Object.keys(devices).sort().map((login) => ({
        login,
        count: devices[login].length,
        devices: devices[login].map((d) => ({
          name: d.name || '',
          tzOffsetMin: d.tz ?? 0,
          quietHours: (Number.isInteger(d.qs) && Number.isInteger(d.qe) && d.qs !== d.qe)
            ? { start: d.qs, end: d.qe } : null,
          heldCount: (d.held || []).length,
          tokenTail: String(d.t || '').slice(-6),
        })),
      }));
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
      });
      console.log(`[register] ${login}${isNewDevice ? ' (new device)' : ''}`);
      // Security-пуш: о входе с нового устройства предупреждаем все ОСТАЛЬНЫЕ
      // устройства этого аккаунта (сам новичок уведомление не получает).
      if (isNewDevice) {
        const others = (devices[login.toLowerCase()] || []).filter((d) => d.t !== body.fcmToken).map((d) => d.t);
        const deviceName = excerpt(body.device, 48) || 'новое устройство';
        for (const t of others) {
          sendPush(t, {
            type: 'security',
            title: 'GsGit · вход в аккаунт',
            body: `Пуши включены на новом устройстве: ${deviceName}. Если это не ты — отзови доступ GsGit App в настройках GitHub.`,
            repo: '',
            url: 'https://github.com/settings/apps/authorizations',
          }).catch(() => {});
        }
      }
      return send(res, 200, { ok: true, login });
    }

    if (req.method === 'POST' && url.pathname === '/announce') {
      // Броадкаст всем устройствам. Ключ — X-Admin-Key (env ADMIN_KEY или /data/admin.key).
      const given = String(req.headers['x-admin-key'] || '');
      if (!given || given !== ADMIN_KEY) return send(res, 401, { error: 'bad admin key' });
      const raw = await readBody(req);
      let body;
      try { body = JSON.parse(raw.toString('utf8')); } catch (_) {
        return send(res, 400, { error: 'bad json' });
      }
      if (!body.title || !body.body) return send(res, 400, { error: 'title and body required' });
      let delivered = 0;
      for (const dev of allDeviceObjects()) {
        const ok = await sendPush(dev.t, {
          type: 'announce',
          title: String(body.title),
          body: String(body.body),
          repo: '',
          url: String(body.url || ''),
        }).catch(() => false);
        if (ok) delivered++;
      }
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
