'use strict';
// ─────────────────────────────────────────────────────────────────────────────
// LiquidMusicGlass — серверный брокер ICM (byicloud.online partner API).
// Чистый Node.js без зависимостей (как сервер GsGit). Держит партнёрский ключ,
// минтит сессии, владеет premium, реверс-проксирует партнёрку. Клиент общается
// только с этим сервером — X-Partner-Key на устройстве больше не нужен.
//
// Запуск:  node server.js   (env см. .env.example)
// ─────────────────────────────────────────────────────────────────────────────

const http = require('http');
const https = require('https');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { URL } = require('url');

const SERVER_VERSION = '1.0.2';

// ─── env / секреты (в код ничего не зашивать) ────────────────────────────────
const PORT = Number(process.env.LMG_PORT || 8090);
const ICM_BASE_URL = process.env.ICM_BASE_URL || 'https://byicloud.online/api/partner';
const ICM_PARTNER_KEY = process.env.ICM_PARTNER_KEY || '';          // ГЛАВНЫЙ секрет
// ICM пускает партнёрку по whitelist origin. Наш домен уже занесён — шлём его на upstream.
const ICM_ORIGIN = process.env.ICM_ORIGIN || 'https://liquid.glassfiles.ru';
const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '';    // опционально: HMAC-валидация Telegram Login
const DATA_DIR = process.env.LMG_DATA_DIR || '/data';
const ADMIN_KEY_FILE = process.env.LMG_ADMIN_KEY_FILE || path.join(DATA_DIR, 'lmg-admin.key');
let ADMIN_KEY = process.env.LMG_ADMIN_KEY || '';
const MAX_BODY = 2 * 1024 * 1024;

if (!ICM_PARTNER_KEY) console.warn('[warn] ICM_PARTNER_KEY не задан — сессии/прокси к ICM работать не будут');
if (!TELEGRAM_BOT_TOKEN) console.warn('[info] TELEGRAM_BOT_TOKEN не задан — HMAC-проверка Telegram отключена, доверяем origin-whitelist ICM');

// ─── файловые сторы + дебаунс-сохранение (паттерн GsGit) ─────────────────────
try { fs.mkdirSync(DATA_DIR, { recursive: true }); } catch (_) {}
if (!ADMIN_KEY) {
  try { ADMIN_KEY = fs.readFileSync(ADMIN_KEY_FILE, 'utf8').trim(); } catch (_) {}
  if (!ADMIN_KEY) {
    ADMIN_KEY = crypto.randomBytes(24).toString('hex');
    try { fs.writeFileSync(ADMIN_KEY_FILE, ADMIN_KEY); } catch (_) {}
    console.log('[admin] сгенерирован новый admin-key ->', ADMIN_KEY_FILE);
  }
}

const FILES = {
  users: path.join(DATA_DIR, 'lmg-users.json'),
  config: path.join(DATA_DIR, 'lmg-config.json'),
  metrics: path.join(DATA_DIR, 'lmg-metrics.json'),
  errors: path.join(DATA_DIR, 'lmg-errors.json'),
};
function loadJson(file, fallback) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch (_) { return fallback; }
}
const saveScope = { timers: {} };
function saveJson(file, obj) {
  clearTimeout(saveScope.timers[file]);
  saveScope.timers[file] = setTimeout(() => {
    try { fs.writeFileSync(file, JSON.stringify(obj)); } catch (e) { console.error('[save]', file, e.message); }
  }, 400);
}

// users: { [partnerUserId]: { tgId, email, createdAt, lastSeenAt, isPremium, premiumExpiresAt, plan } }
let users = loadJson(FILES.users, {});
// config: server-driven (feature flags / maintenance / версии) — как appconfig GsGit
let config = loadJson(FILES.config, {
  maintenance: '', minVersion: '', latestVersion: '', changelog: '', downloadUrl: '',
  waveEnabled: true, importEnabled: true,
});
let metrics = loadJson(FILES.metrics, { hours: {} });
let errorsLog = loadJson(FILES.errors, []);

const START = Date.now();

// ─── мини-клиент HTTPS (без зависимостей) ────────────────────────────────────
function request(url, options, body) {
  return new Promise((resolve, reject) => {
    const req = https.request(url, options, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => resolve({ status: res.statusCode || 0, headers: res.headers, body: Buffer.concat(chunks) }));
    });
    req.on('error', reject);
    req.setTimeout(20000, () => req.destroy(new Error('timeout')));
    if (body) req.write(body);
    req.end();
  });
}

// ─── реальный IP клиента (за nginx/CF) ───────────────────────────────────────
function clientIp(req) {
  const xff = req.headers['x-forwarded-for'];
  if (xff) return String(xff).split(',')[0].trim();
  return (req.socket && req.socket.remoteAddress) || 'unknown';
}
// ─── простой rate-limit по IP (скользящее окно, in-memory) ───────────────────
// Защищает открытый минт сессии: lg_-id — bearer-секрет, нельзя давать перебирать/спамить.
const rlHits = new Map();  // ip → массив timestamps
function rateLimited(ip, limit, windowMs) {
  const now = Date.now();
  const arr = (rlHits.get(ip) || []).filter((t) => now - t < windowMs);
  arr.push(now);
  rlHits.set(ip, arr);
  if (rlHits.size > 5000) { for (const [k, v] of rlHits) { if (!v.length || now - v[v.length - 1] > windowMs) rlHits.delete(k); } }
  return arr.length > limit;
}

// ─── хелперы ─────────────────────────────────────────────────────────────────
function send(res, status, obj, extraHeaders) {
  const data = Buffer.isBuffer(obj) ? obj : Buffer.from(JSON.stringify(obj));
  res.writeHead(status, Object.assign({
    'Content-Type': Buffer.isBuffer(obj) ? 'application/octet-stream' : 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization,content-type,x-admin-key,x-partner-user-id,x-device-id,x-app-version,x-lmg-fast',
    'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
  }, extraHeaders || {}));
  res.end(data);
}
function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = []; let size = 0;
    req.on('data', (c) => { size += c.length; if (size > MAX_BODY) { req.destroy(); reject(new Error('too large')); } else chunks.push(c); });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}
async function jsonBody(req) {
  try { return JSON.parse((await readBody(req)).toString('utf8')); } catch (_) { return undefined; }
}
function adminOk(req) { return ADMIN_KEY && req.headers['x-admin-key'] === ADMIN_KEY; }
function hourKey(t = Date.now()) { return new Date(t).toISOString().slice(0, 13); }
function recordMetric(field, by = 1) {
  const k = hourKey();
  const b = metrics.hours[k] || (metrics.hours[k] = {});
  b[field] = (b[field] || 0) + by;
  saveJson(FILES.metrics, metrics);
}
function metricsFor(hoursBack) {
  const cutoff = Date.now() - hoursBack * 3600_000; const sum = {};
  for (const [k, b] of Object.entries(metrics.hours)) {
    if (Date.parse(k + ':00:00Z') >= cutoff) for (const f of Object.keys(b)) sum[f] = (sum[f] || 0) + b[f];
  }
  return sum;
}
function recordError(code, message) {
  const now = Date.now();
  const found = errorsLog.find((e) => e.code === code);
  if (found) { found.count++; found.lastAt = now; found.message = message; }
  else errorsLog.unshift({ code, message, count: 1, firstAt: now, lastAt: now });
  errorsLog = errorsLog.slice(0, 100);
  saveJson(FILES.errors, errorsLog);
}
function periodHours(p) { return p === '1h' ? 1 : p === '7d' ? 168 : p === '30d' ? 720 : 24; }

// ─── Telegram Login Widget: проверка подписи ─────────────────────────────────
// secret_key = SHA256(bot_token); data_check_string = отсортированные "k=v"\n
// (без hash); сверяем HMAC-SHA256. См. core.telegram.org/widgets/login.
function verifyTelegramAuth(data) {
  if (!TELEGRAM_BOT_TOKEN || !data || !data.hash) return false;
  const secret = crypto.createHash('sha256').update(TELEGRAM_BOT_TOKEN).digest();
  const check = Object.keys(data).filter((k) => k !== 'hash').sort()
    .map((k) => `${k}=${data[k]}`).join('\n');
  const hmac = crypto.createHmac('sha256', secret).update(check).digest('hex');
  if (hmac.length !== String(data.hash).length) return false;
  const ok = crypto.timingSafeEqual(Buffer.from(hmac), Buffer.from(String(data.hash)));
  // защита от старых данных (24ч)
  const fresh = !data.auth_date || (Date.now() / 1000 - Number(data.auth_date)) < 86400;
  return ok && fresh;
}
// partner_user_id, стабильный на пользователя. Сервер — источник истины.
function partnerUserIdForTelegram(tgId) {
  return 'tg_' + crypto.createHash('sha256').update('lmg:tg:' + tgId).digest('hex').slice(0, 24);
}
function partnerUserIdForEmail(email) {
  return 'em_' + crypto.createHash('sha256').update('lmg:em:' + String(email).toLowerCase()).digest('hex').slice(0, 24);
}

// ─── ICM upstream: выпуск сессии (s2s, с партнёрским ключом) ─────────────────
async function issueUpstreamSession(partnerUserId, hideExplicit) {
  const body = JSON.stringify({ partner_user_id: partnerUserId, hide_explicit: !!hideExplicit });
  const r = await request(`${ICM_BASE_URL}/session/issue`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'User-Agent': 'LiquidMusicGlass-Server/' + SERVER_VERSION,
      'X-Partner-Key': ICM_PARTNER_KEY,
      'Origin': ICM_ORIGIN,
      'Referer': ICM_ORIGIN + '/',
      'Content-Length': Buffer.byteLength(body),
    },
  }, body);
  let data; try { data = JSON.parse(r.body.toString('utf8')); } catch (_) { data = {}; }
  return { status: r.status, data };
}
// Учёт устройств (паритет с GsGit, но без пушей): читаем заголовки, БЕЗ геолокации.
// Клиент шлёт X-Device-Id (стабильный per-install), X-App-Version; платформа — из User-Agent.
function touchDevice(pid, req) {
  const u = users[pid]; if (!u) return;
  const rawId = req.headers['x-device-id'];
  const ua = String(req.headers['user-agent'] || '');
  // если клиент не прислал id — деривим стабильный из UA, чтобы не плодить записи
  const devId = (rawId && String(rawId).slice(0, 64))
    || 'ua_' + crypto.createHash('sha256').update(ua).digest('hex').slice(0, 16);
  const now = Date.now();
  const devices = u.devices || (u.devices = {});
  const d = devices[devId] || (devices[devId] = { firstSeen: now });
  d.lastSeen = now;
  d.appVersion = String(req.headers['x-app-version'] || d.appVersion || '');
  d.platform = /Android/i.test(ua) ? 'android' : (ua.split(/[/ ]/)[0] || 'unknown').slice(0, 32);
  // держим не больше 10 последних устройств на юзера
  const ids = Object.keys(devices);
  if (ids.length > 10) {
    ids.sort((a, b) => (devices[a].lastSeen || 0) - (devices[b].lastSeen || 0));
    while (Object.keys(devices).length > 10) delete devices[ids.shift()];
  }
}
function userView(pid) {
  const u = users[pid] || {};
  const now = Date.now();
  // Premium = локальный грант брокера ИЛИ реальная подписка ICM (кэш от
  // fetchUpstreamSubscription). Полевой баг: у юзера подписка ICM до 06.08,
  // а брокер отвечал "free", потому что смотрел только в свою базу.
  const localP = !!u.isPremium && (!u.premiumExpiresAt || now < u.premiumExpiresAt);
  const icmP = !!u.icmPremium && (!u.icmPremiumExpiresAt || now < u.icmPremiumExpiresAt);
  const premium = localP || icmP;
  const exp = Math.max(localP ? (u.premiumExpiresAt || 0) : 0, icmP ? (u.icmPremiumExpiresAt || 0) : 0)
    || u.icmPremiumExpiresAt || u.premiumExpiresAt || 0;
  const plan = premium
    ? ((localP && u.plan && u.plan !== 'free') ? u.plan : (u.icmPlan && u.icmPlan !== 'free' ? u.icmPlan : 'premium'))
    : 'free';
  return { partner_user_id: pid, is_premium: premium, premium_expires_at: exp, plan };
}

// ─── ICM upstream: РЕАЛЬНАЯ подписка юзера (/me/subscription) ────────────────
// Брокер владеет premium, но источник фактической подписки — ICM: минтим
// сессию s2s, спрашиваем /me/subscription с Bearer, кэшируем на юзере 10 мин
// (клиент опрашивает при каждом логине/старте — не долбим партнёрку).
// Не-linked юзер (403) = подписки нет. Ошибки сети кэш не трогают.
const SUB_CACHE_MS = 10 * 60_000;
async function fetchUpstreamSubscription(pid, force) {
  const u = users[pid];
  if (!u || !ICM_PARTNER_KEY) return;
  const now = Date.now();
  if (!force && u.subCheckedAt && now - u.subCheckedAt < SUB_CACHE_MS) return;
  try {
    const s = await issueUpstreamSession(pid, false);
    if (s.status < 200 || s.status >= 300 || !s.data.partner_session_token) return;
    const r = await request(`${ICM_BASE_URL}/me/subscription`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'LiquidMusicGlass-Server/' + SERVER_VERSION,
        'X-Partner-Key': ICM_PARTNER_KEY,
        'Authorization': 'Bearer ' + s.data.partner_session_token,
        'X-Partner-User-Id': pid,
        'Origin': ICM_ORIGIN,
        'Referer': ICM_ORIGIN + '/',
      },
    });
    let d; try { d = JSON.parse(r.body.toString('utf8')); } catch (_) { d = null; }
    if (r.status === 200 && d && typeof d.active === 'boolean') {
      // active И не истекло (как гейт скачивания в клиенте): подписка с
      // active=true, но days_left=0 premium давать не должна.
      const active = d.active && (d.days_left == null || d.days_left > 0 || !d.expires_at);
      // expires_at → миллисекунды (клиент сравнивает с currentTimeMillis):
      // секунды → ×1000; фолбэк — expires_at_iso.
      let exp = Number(d.expires_at || 0);
      if (exp && exp < 1e12) exp *= 1000;
      if (!exp && d.expires_at_iso) { const t = Date.parse(d.expires_at_iso); if (!isNaN(t)) exp = t; }
      u.icmPremium = active;
      u.icmPremiumExpiresAt = exp;
      u.icmPlan = d.plan_type || (active ? 'premium' : 'free');
      // Регионы подписки (US/NZ) — как есть из ICM: профиль в клиенте
      // показывает regions[0], без них рисовался ложный «Global (WW)».
      u.icmRegions = Array.isArray(d.regions) ? d.regions : [];
      u.subCheckedAt = now;
      saveJson(FILES.users, users);
    } else if (r.status === 403 || r.status === 404) {
      // user_not_linked / subscription_required / user_not_found → подписки нет
      u.icmPremium = false;
      u.subCheckedAt = now;
      saveJson(FILES.users, users);
    }
    // 5xx/сеть: кэш не трогаем — прошлое знание лучше ложного "free"
  } catch (_) { /* сеть упала — молча, кэш живёт */ }
}

// ─── роутинг ─────────────────────────────────────────────────────────────────
const server = http.createServer(async (req, res) => {
  const method = req.method || 'GET';
  const url = new URL(req.url || '/', 'http://localhost');
  const p = url.pathname;
  recordMetric('req');

  if (method === 'OPTIONS') return send(res, 204, {});

  try {
    // ── health ──
    if (method === 'GET' && p === '/healthz') return send(res, 200, { ok: true });

    // ── config для клиента (server-driven) ──
    if (method === 'GET' && p === '/lmg/config') return send(res, 200, config);

    // ── Telegram-логин → partner_user_id → upstream session ──
    if (method === 'POST' && p === '/lmg/auth/telegram') {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'auth not configured' });
      const body = await jsonBody(req);
      if (!body || !body.id) { recordMetric('authFail'); return send(res, 400, { error: 'no telegram id' }); }
      // Если задан бот-токен — проверяем HMAC. Иначе доверяем origin-whitelist ICM (наш домен занесён).
      if (TELEGRAM_BOT_TOKEN && !verifyTelegramAuth(body)) { recordMetric('authFail'); return send(res, 401, { error: 'bad telegram signature' }); }
      const tgId = String(body.id);
      const pid = partnerUserIdForTelegram(tgId);
      const { status, data } = await issueUpstreamSession(pid, !!body.hide_explicit);
      if (status < 200 || status >= 300 || !data.partner_session_token) {
        recordError('SESSION_' + status, 'upstream session/issue failed'); recordMetric('authFail');
        return send(res, 502, { error: 'session issue failed' });
      }
      const now = Date.now();
      const u = users[pid] || (users[pid] = { createdAt: now });
      u.tgId = tgId; u.lastSeenAt = now;
      u.name = [body.first_name, body.last_name].filter(Boolean).join(' ') || body.username || '';
      saveJson(FILES.users, users);
      recordMetric('sessionIssued');
      return send(res, 200, Object.assign({
        partner_session_token: data.partner_session_token,
        expires_in: data.expires_in || 0,
        scopes: data.scopes || [],
      }, userView(pid)));
    }

    // ── issue: минт сессии по partner_user_id, пришедшему из ICM deep-link ──
    // (linked/<icm_user_id>/state → app POST-ит сюда icm_user_id как partner_user_id).
    // Юзер регистрируется на лету, HMAC не нужен — доверяем origin-whitelist ICM.
    if (method === 'POST' && p === '/lmg/session/issue') {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'not configured' });
      if (rateLimited(clientIp(req), 20, 60_000)) { recordMetric('rateLimited'); return send(res, 429, { error: 'too many requests' }); }
      const body = await jsonBody(req);
      const pid = body && body.partner_user_id;
      if (!pid) return send(res, 400, { error: 'no partner_user_id' });
      const { status, data } = await issueUpstreamSession(pid, !!(body && body.hide_explicit));
      if (status < 200 || status >= 300 || !data.partner_session_token) {
        recordError('SESSION_' + status, 'upstream session/issue failed'); recordMetric('authFail');
        return send(res, 502, { error: 'session issue failed' });
      }
      const now = Date.now();
      const u = users[pid] || (users[pid] = { createdAt: now });
      u.lastSeenAt = now;
      touchDevice(pid, req);
      saveJson(FILES.users, users);
      recordMetric('sessionIssued');
      // Прогрев кэша подписки в фоне (НЕ await — не удлиняем логин): клиент
      // спросит /me/subscription сразу после — ответ уже будет тёплым.
      fetchUpstreamSubscription(pid).catch(() => {});
      return send(res, 200, Object.assign({
        partner_session_token: data.partner_session_token,
        expires_in: data.expires_in || 0,
        scopes: data.scopes || [],
      }, userView(pid)));
    }

    // ── refresh: перевыпустить сессию по partner_user_id ──
    if (method === 'POST' && p === '/lmg/session/refresh') {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'not configured' });
      if (rateLimited(clientIp(req), 30, 60_000)) { recordMetric('rateLimited'); return send(res, 429, { error: 'too many requests' }); }
      const body = await jsonBody(req);
      const pid = body && body.partner_user_id;
      if (!pid) return send(res, 400, { error: 'no partner_user_id' });
      if (!users[pid]) users[pid] = { createdAt: Date.now() };   // авто-регистрация, если сервер перезапускали
      const { status, data } = await issueUpstreamSession(pid, !!body.hide_explicit);
      if (status < 200 || status >= 300 || !data.partner_session_token) { recordError('SESSION_' + status, 'refresh failed'); return send(res, 502, { error: 'refresh failed' }); }
      users[pid].lastSeenAt = Date.now(); saveJson(FILES.users, users);
      recordMetric('sessionIssued');
      fetchUpstreamSubscription(pid).catch(() => {});   // прогрев кэша, не await
      return send(res, 200, Object.assign({ partner_session_token: data.partner_session_token, expires_in: data.expires_in || 0, scopes: data.scopes || [] }, userView(pid)));
    }

    // ── email-линк (s2s, проксируем с ключом) ──
    if (method === 'POST' && p.startsWith('/lmg/auth/email/')) {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'not configured' });
      const sub = p.slice('/lmg/auth/email/'.length); // request|verify|password/reset|password/change
      const raw = await readBody(req);
      const r = await request(`${ICM_BASE_URL}/link/email/${sub}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Partner-Key': ICM_PARTNER_KEY, 'Origin': ICM_ORIGIN, 'Referer': ICM_ORIGIN + '/', 'Content-Length': raw.length, 'User-Agent': 'LiquidMusicGlass-Server/' + SERVER_VERSION },
      }, raw);
      return send(res, r.status, r.body, { 'Content-Type': r.headers['content-type'] || 'application/json' });
    }

    // ── premium / подписка (server-authoritative) ──
    // Реальная подписка тянется из ICM (кэш 10 мин) и мержится с локальными
    // грантами брокера в userView.
    if (method === 'GET' && p === '/lmg/me/subscription') {
      const pid = req.headers['x-partner-user-id'];
      if (!pid || !users[pid]) return send(res, 401, { error: 'unknown user' });
      users[pid].lastSeenAt = Date.now(); touchDevice(pid, req);
      await fetchUpstreamSubscription(pid);
      saveJson(FILES.users, users);
      return send(res, 200, Object.assign(userView(pid), {
        regions: users[pid].icmRegions || [],
      }));
    }

    // ── ICM reverse-proxy: ВСЁ остальное партнёрки ──
    // /lmg/icm/<path>?<query>  →  ICM_BASE_URL/<path>?<query> + X-Partner-Key
    if (p.startsWith('/lmg/icm/')) {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'not configured' });
      const upstreamPath = p.slice('/lmg/icm'.length); // сохраняет ведущий '/'
      const target = ICM_BASE_URL + upstreamPath + (url.search || '');
      const raw = ['GET', 'HEAD'].includes(method) ? null : await readBody(req);
      const headers = {
        'Accept': 'application/json',
        'User-Agent': 'LiquidMusicGlass/1.0',
        'X-Partner-Key': ICM_PARTNER_KEY,
        'Origin': ICM_ORIGIN,
        'Referer': ICM_ORIGIN + '/',
      };
      if (req.headers['authorization']) headers['Authorization'] = req.headers['authorization'];
      if (req.headers['x-partner-user-id']) headers['X-Partner-User-Id'] = req.headers['x-partner-user-id'];
      if (req.headers['x-lmg-fast']) headers['X-LMG-Fast'] = req.headers['x-lmg-fast'];
      { const dpid = req.headers['x-partner-user-id']; if (dpid && users[dpid]) { users[dpid].lastSeenAt = Date.now(); touchDevice(dpid, req); saveJson(FILES.users, users); } }
      if (raw && raw.length) { headers['Content-Type'] = req.headers['content-type'] || 'application/json'; headers['Content-Length'] = raw.length; }
      let r;
      try { r = await request(target, { method, headers }, raw); }
      catch (e) { recordError('ICM_NET', 'upstream unreachable'); recordMetric('icmFail'); return send(res, 502, { error: 'icm unreachable' }); }
      recordMetric('icm');
      if (r.status >= 500) { recordMetric('icm5xx'); recordError('ICM_' + r.status, 'upstream 5xx ' + upstreamPath.split('?')[0]); }
      else if (r.status >= 400) recordMetric('icm4xx');
      const passHeaders = { 'Content-Type': r.headers['content-type'] || 'application/json' };
      if (r.headers['x-request-id']) passHeaders['X-Request-Id'] = r.headers['x-request-id'];
      return send(res, r.status, r.body, passHeaders);
    }

    // ── ADMIN ──
    if (p.startsWith('/admin/lmg/')) {
      if (!adminOk(req)) return send(res, 401, { error: 'bad admin key' });

      if (method === 'GET' && p === '/admin/lmg/health') {
        let icmOk = false;
        try { const r = await request(`${ICM_BASE_URL}/health`, { method: 'GET', headers: { 'X-Partner-Key': ICM_PARTNER_KEY, 'Origin': ICM_ORIGIN, 'Referer': ICM_ORIGIN + '/', 'Accept': 'application/json' } }); icmOk = r.status >= 200 && r.status < 500; } catch (_) {}
        return send(res, 200, {
          serverVersion: SERVER_VERSION,
          uptimeMs: Date.now() - START,
          users: Object.keys(users).length,
          partnerKeySet: !!ICM_PARTNER_KEY,
          icmOrigin: ICM_ORIGIN,
          telegramHmac: !!TELEGRAM_BOT_TOKEN,
          icmUpstream: icmOk ? 'ok' : 'down',
          serverTime: new Date().toISOString(),
        });
      }
      if (method === 'GET' && p === '/admin/lmg/metrics') {
        return send(res, 200, metricsFor(periodHours(url.searchParams.get('period'))));
      }
      if (method === 'GET' && p === '/admin/lmg/users') {
        const list = Object.entries(users).map(([pid, u]) => ({ partner_user_id: pid, tgId: u.tgId || null, email: u.email || null, name: u.name || '', isPremium: !!u.isPremium, premiumExpiresAt: u.premiumExpiresAt || 0, lastSeenAt: u.lastSeenAt || 0, devices: Object.keys(u.devices || {}).length }))
          .sort((a, b) => b.lastSeenAt - a.lastSeenAt);
        return send(res, 200, { count: list.length, items: list });
      }
      // ── устройства (плоский список по всем юзерам, как в GsGit) ──
      if (method === 'GET' && p === '/admin/lmg/devices') {
        const list = [];
        for (const [pid, u] of Object.entries(users)) {
          for (const [devId, d] of Object.entries(u.devices || {})) {
            list.push({ deviceId: devId, partner_user_id: pid, name: u.name || '', platform: d.platform || 'unknown', appVersion: d.appVersion || '', firstSeen: d.firstSeen || 0, lastSeen: d.lastSeen || 0 });
          }
        }
        list.sort((a, b) => b.lastSeen - a.lastSeen);
        return send(res, 200, { count: list.length, items: list });
      }
      {
        const m = p.match(/^\/admin\/lmg\/users\/([A-Za-z0-9_]+)$/);
        if (m && method === 'GET') { const u = users[m[1]]; return u ? send(res, 200, Object.assign({ partner_user_id: m[1] }, u)) : send(res, 404, { error: 'not found' }); }
        if (m && method === 'DELETE') { if (!users[m[1]]) return send(res, 404, { error: 'not found' }); delete users[m[1]]; saveJson(FILES.users, users); return send(res, 200, { ok: true }); }
      }
      {
        const m = p.match(/^\/admin\/lmg\/users\/([A-Za-z0-9_]+)\/premium$/);
        if (m && method === 'POST') {
          const u = users[m[1]]; if (!u) return send(res, 404, { error: 'not found' });
          const body = await jsonBody(req) || {};
          u.isPremium = body.premium !== false;
          u.premiumExpiresAt = Number.isFinite(body.until) ? body.until : (u.isPremium ? 0 : 0);
          u.plan = u.isPremium ? (body.plan || 'premium') : 'free';
          saveJson(FILES.users, users);
          return send(res, 200, userView(m[1]));
        }
      }
      if (p === '/admin/lmg/config') {
        if (method === 'GET') return send(res, 200, config);
        if (method === 'POST') {
          const body = await jsonBody(req); if (body === undefined) return send(res, 400, { error: 'bad json' });
          for (const k of Object.keys(config)) if (k in body) config[k] = body[k];
          saveJson(FILES.config, config);
          return send(res, 200, config);
        }
      }
      if (method === 'GET' && p === '/admin/lmg/errors') return send(res, 200, { items: errorsLog });
      if (method === 'POST' && p === '/admin/lmg/session/test') {
        const { status, data } = await issueUpstreamSession('admin_probe_' + Date.now(), false);
        return send(res, 200, { upstreamStatus: status, gotToken: !!data.partner_session_token });
      }
      return send(res, 404, { error: 'not found' });
    }

    return send(res, 404, { error: 'not found' });
  } catch (e) {
    recordError('SRV', e.message || 'error');
    return send(res, 500, { error: 'server error' });
  }
});

server.listen(PORT, () => console.log(`[lmg] server ${SERVER_VERSION} on :${PORT} (icm=${ICM_BASE_URL}, origin=${ICM_ORIGIN})`));
