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

const SERVER_VERSION = '1.0.0';

// ─── env / секреты (в код ничего не зашивать) ────────────────────────────────
const PORT = Number(process.env.LMG_PORT || 8090);
const ICM_BASE_URL = process.env.ICM_BASE_URL || 'https://byicloud.online/api/partner';
const ICM_PARTNER_KEY = process.env.ICM_PARTNER_KEY || '';          // ГЛАВНЫЙ секрет
const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '';    // валидация Telegram Login
const DATA_DIR = process.env.LMG_DATA_DIR || '/data';
const ADMIN_KEY_FILE = process.env.LMG_ADMIN_KEY_FILE || path.join(DATA_DIR, 'lmg-admin.key');
let ADMIN_KEY = process.env.LMG_ADMIN_KEY || '';
const MAX_BODY = 2 * 1024 * 1024;

if (!ICM_PARTNER_KEY) console.warn('[warn] ICM_PARTNER_KEY не задан — сессии/прокси к ICM работать не будут');
if (!TELEGRAM_BOT_TOKEN) console.warn('[warn] TELEGRAM_BOT_TOKEN не задан — /lmg/auth/telegram будет 503');

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

// ─── хелперы ─────────────────────────────────────────────────────────────────
function send(res, status, obj, extraHeaders) {
  const data = Buffer.isBuffer(obj) ? obj : Buffer.from(JSON.stringify(obj));
  res.writeHead(status, Object.assign({
    'Content-Type': Buffer.isBuffer(obj) ? 'application/octet-stream' : 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization,content-type,x-admin-key,x-partner-user-id',
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
      'Content-Length': Buffer.byteLength(body),
    },
  }, body);
  let data; try { data = JSON.parse(r.body.toString('utf8')); } catch (_) { data = {}; }
  return { status: r.status, data };
}
function userView(pid) {
  const u = users[pid] || {};
  const premium = !!u.isPremium && (!u.premiumExpiresAt || Date.now() < u.premiumExpiresAt);
  return { partner_user_id: pid, is_premium: premium, premium_expires_at: u.premiumExpiresAt || 0, plan: u.plan || 'free' };
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
      if (!TELEGRAM_BOT_TOKEN || !ICM_PARTNER_KEY) return send(res, 503, { error: 'auth not configured' });
      const body = await jsonBody(req);
      if (!body || !verifyTelegramAuth(body)) { recordMetric('authFail'); return send(res, 401, { error: 'bad telegram signature' }); }
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

    // ── refresh: перевыпустить сессию по partner_user_id ──
    if (method === 'POST' && p === '/lmg/session/refresh') {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'not configured' });
      const body = await jsonBody(req);
      const pid = body && body.partner_user_id;
      if (!pid || !users[pid]) return send(res, 401, { error: 'unknown user' });
      const { status, data } = await issueUpstreamSession(pid, !!body.hide_explicit);
      if (status < 200 || status >= 300 || !data.partner_session_token) { recordError('SESSION_' + status, 'refresh failed'); return send(res, 502, { error: 'refresh failed' }); }
      users[pid].lastSeenAt = Date.now(); saveJson(FILES.users, users);
      recordMetric('sessionIssued');
      return send(res, 200, Object.assign({ partner_session_token: data.partner_session_token, expires_in: data.expires_in || 0, scopes: data.scopes || [] }, userView(pid)));
    }

    // ── email-линк (s2s, проксируем с ключом) ──
    if (method === 'POST' && p.startsWith('/lmg/auth/email/')) {
      if (!ICM_PARTNER_KEY) return send(res, 503, { error: 'not configured' });
      const sub = p.slice('/lmg/auth/email/'.length); // request|verify|password/reset|password/change
      const raw = await readBody(req);
      const r = await request(`${ICM_BASE_URL}/link/email/${sub}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Partner-Key': ICM_PARTNER_KEY, 'Content-Length': raw.length, 'User-Agent': 'LiquidMusicGlass-Server/' + SERVER_VERSION },
      }, raw);
      return send(res, r.status, r.body, { 'Content-Type': r.headers['content-type'] || 'application/json' });
    }

    // ── premium / подписка (server-authoritative) ──
    if (method === 'GET' && p === '/lmg/me/subscription') {
      const pid = req.headers['x-partner-user-id'];
      if (!pid || !users[pid]) return send(res, 401, { error: 'unknown user' });
      users[pid].lastSeenAt = Date.now(); saveJson(FILES.users, users);
      return send(res, 200, userView(pid));
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
      };
      if (req.headers['authorization']) headers['Authorization'] = req.headers['authorization'];
      if (req.headers['x-partner-user-id']) headers['X-Partner-User-Id'] = req.headers['x-partner-user-id'];
      if (req.headers['x-lmg-fast']) headers['X-LMG-Fast'] = req.headers['x-lmg-fast'];
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
        try { const r = await request(`${ICM_BASE_URL}/health`, { method: 'GET', headers: { 'X-Partner-Key': ICM_PARTNER_KEY, 'Accept': 'application/json' } }); icmOk = r.status >= 200 && r.status < 500; } catch (_) {}
        return send(res, 200, {
          serverVersion: SERVER_VERSION,
          uptimeMs: Date.now() - START,
          users: Object.keys(users).length,
          partnerKeySet: !!ICM_PARTNER_KEY,
          telegramConfigured: !!TELEGRAM_BOT_TOKEN,
          icmUpstream: icmOk ? 'ok' : 'down',
          serverTime: new Date().toISOString(),
        });
      }
      if (method === 'GET' && p === '/admin/lmg/metrics') {
        return send(res, 200, metricsFor(periodHours(url.searchParams.get('period'))));
      }
      if (method === 'GET' && p === '/admin/lmg/users') {
        const list = Object.entries(users).map(([pid, u]) => ({ partner_user_id: pid, tgId: u.tgId || null, email: u.email || null, name: u.name || '', isPremium: !!u.isPremium, premiumExpiresAt: u.premiumExpiresAt || 0, lastSeenAt: u.lastSeenAt || 0 }))
          .sort((a, b) => b.lastSeenAt - a.lastSeenAt);
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

server.listen(PORT, () => console.log(`[lmg] server ${SERVER_VERSION} on :${PORT} (icm=${ICM_BASE_URL})`));
