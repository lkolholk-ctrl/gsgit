// GsGit push backend — приём вебхуков GsGit GitHub App и моментальные FCM-пуши.
// Ноль зависимостей: только встроенные модули Node.js 22 (http, crypto, fs).
//
// Эндпоинты:
//   GET  /healthz     — проверка живости (для Uptime Kuma)
//   POST /webhook     — приёмник вебхуков GitHub (подпись X-Hub-Signature-256 обязательна)
//   POST /register    — регистрация устройства: Authorization: Bearer <GsGit App user token>,
//                       body {"fcmToken": "..."}; логин берём из GET /user на GitHub API
//   POST /unregister  — удаление устройства: body {"fcmToken": "..."}

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

// ─── Хранилище устройств: { "<github login>": ["<fcm token>", ...] } ─────────

let devices = {};
try {
  devices = JSON.parse(fs.readFileSync(DEVICES_FILE, 'utf8'));
} catch (_) {
  devices = {};
}

let saveTimer = null;
function saveDevices() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    const tmp = DEVICES_FILE + '.tmp';
    fs.mkdirSync(path.dirname(DEVICES_FILE), { recursive: true });
    fs.writeFileSync(tmp, JSON.stringify(devices, null, 2));
    fs.renameSync(tmp, DEVICES_FILE);
  }, 250);
}

function addDevice(login, fcmToken) {
  const key = login.toLowerCase();
  const list = devices[key] || (devices[key] = []);
  if (!list.includes(fcmToken)) list.push(fcmToken);
  saveDevices();
}

function removeDeviceToken(fcmToken) {
  let removed = false;
  for (const login of Object.keys(devices)) {
    const next = devices[login].filter((t) => t !== fcmToken);
    if (next.length !== devices[login].length) removed = true;
    if (next.length === 0) delete devices[login];
    else devices[login] = next;
  }
  if (removed) saveDevices();
  return removed;
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

function describeEvent(event, p) {
  const repo = p.repository?.full_name || '';
  switch (event) {
    case 'push': {
      const count = (p.commits || []).length;
      const branch = String(p.ref || '').replace('refs/heads/', '');
      return { title: repo, body: `${p.sender?.login} pushed ${count} commit(s) to ${branch}` };
    }
    case 'issues':
      return { title: repo, body: `issue #${p.issue?.number} ${p.action}: ${p.issue?.title || ''}` };
    case 'issue_comment':
      return { title: repo, body: `${p.sender?.login} commented on #${p.issue?.number}` };
    case 'pull_request':
      return { title: repo, body: `PR #${p.pull_request?.number} ${p.action}: ${p.pull_request?.title || ''}` };
    case 'pull_request_review':
      return { title: repo, body: `${p.sender?.login} ${p.review?.state || 'reviewed'} PR #${p.pull_request?.number}` };
    case 'workflow_run': {
      if (p.action !== 'completed') return null;
      const run = p.workflow_run || {};
      return { title: repo, body: `workflow "${run.name}" ${run.conclusion} on ${run.head_branch}` };
    }
    case 'check_suite': {
      if (p.action !== 'completed' || p.check_suite?.conclusion === 'success') return null;
      return { title: repo, body: `checks ${p.check_suite?.conclusion} on ${p.check_suite?.head_branch}` };
    }
    case 'release':
      if (p.action !== 'published') return null;
      return { title: repo, body: `release ${p.release?.tag_name} published` };
    default:
      return null; // прочие события молча игнорируем
  }
}

async function handleWebhook(event, payload) {
  const note = describeEvent(event, payload);
  if (!note) return { delivered: 0, reason: 'event ignored' };
  const logins = targetLogins(payload);
  const tokens = new Set();
  for (const l of logins) for (const t of devices[l] || []) tokens.add(t);
  let delivered = 0;
  for (const token of tokens) {
    const ok = await sendPush(token, {
      type: event,
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
      addDevice(login, body.fcmToken);
      console.log(`[register] ${login}`);
      return send(res, 200, { ok: true, login });
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
