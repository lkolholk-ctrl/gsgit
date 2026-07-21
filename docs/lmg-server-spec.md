# LiquidMusicGlass — серверная часть (брокер ICM). Инструкция для Claude Fable 5

Полная спецификация backend'а LMG: от архитектуры до каждого эндпоинта. Ставится на **тот же VPS**, что и
сервер GsGit (`api.gsgit.org`), и подключается к **той же админ-панели** (новая вкладка «LMG» рядом с
GsGit/GlassFiles). Стек — **как у GsGit-сервера: чистый Node.js без зависимостей** (`http`, `https`, `crypto`,
`fs`), JSON-файловые сторы с дебаунсом, admin-key.

---

## 0. Зачем это (проблема, которую чиним)

Клиент LMG (`com.liquidmusicglass.api.icm`) сейчас ходит в партнёрский API **`https://byicloud.online/api/partner`**
напрямую и **шлёт `X-Partner-Key` (секрет партнёра) с устройства**, а также сам дёргает s2s-only
`/session/issue`. В коде прямо: `IcmAuthRepository:327 «Set premium status. In production this should come from
your backend»`. Значит:

1. **Партнёрский ключ обязан жить на сервере**, а не в APK (его видно в декомпиляции → бан партнёрки).
2. **Premium/подписка — server-authoritative** (не доверять флагу на устройстве).
3. `/session/issue`, `/link/email/*` — только server-to-server.

Решение — **сервер-брокер**: держит ключ, минтит сессии, владеет premium, и **реверс-проксирует** остальной
ICM API, подставляя `X-Partner-Key` на своей стороне. Клиент общается только с нашим сервером.

> Что НЕ трогаем: **Yandex Music** остаётся клиентским (нужна личная Plus-подписка юзера + RU-IP, чужую не
> проксируем). **AutoMix / TFLite** — на устройстве. Lyrics (`lrclib.net`) — можно оставить клиентским.

---

## 1. Архитектура

- Node.js, без npm-зависимостей (как `server/api/server.js` у GsGit). Один процесс, свой порт.
- JSON-сторы в `/data` с дебаунс-сохранением (паттерн GsGit: `saveJson`, `saveScope`, `delay(400)`).
- Разворачивается на том же VPS **отдельным сервисом** в `docker-compose` (свой контейнер + свой поддомен/путь,
  например `api.lmg.gsgit.org` или `api.gsgit.org` c префиксом `/lmg`). База URL — конфигурируемая.
- Общий admin-key механизм с GsGit (или свой файл `/data/lmg-admin.key`), чтобы админка ходила по `x-admin-key`.

**Домены сервера (модули):** `auth` (Telegram/email + session), `icm-proxy` (реверс-прокси партнёрки),
`premium` (подписки, server-authoritative), `admin` (health/metrics/devices/config/premium), `push` (опц.).

---

## 2. Секреты / env (ничего в код, только env на VPS)

```
LMG_PORT=8090
ICM_PARTNER_KEY=<ключ с https://byicloud.online/partners>   # ГЛАВНЫЙ секрет, только тут
ICM_BASE_URL=https://byicloud.online/api/partner            # апстрим партнёрки
TELEGRAM_BOT_TOKEN=<токен @byicmbot>                        # для проверки Telegram-логина
LMG_ADMIN_KEY=<48-hex>                                      # или переиспользовать /data/admin.key GsGit
LMG_JWT_SECRET=<random>                                     # подпись наших сессионных куки/токенов (если свои)
```

---

## 3. Флоу авторизации (Telegram → partner_user_id → session)

Клиент уже умеет Telegram-линк (`byicmbot`, callback был на `liquid.glassfiles.ru/auth/telegram`).
Переносим/дублируем на наш сервер:

1. Юзер жмёт «войти через Telegram» → открывается `t.me/byicmbot?start=<nonce>`.
2. Бот/виджет отдаёт Telegram-данные на наш `POST /lmg/auth/telegram` — **сервер валидирует подпись**
   (HMAC-SHA256 от `TELEGRAM_BOT_TOKEN` по правилам Telegram Login Widget).
3. Сервер вычисляет `partner_user_id` (по доке ICM — хэш telegram id), сохраняет пользователя, и **сам**
   вызывает апстрим `POST /session/issue` с `X-Partner-Key` → получает `partner_session_token` (JWT).
4. Возвращает клиенту: `{ partner_user_id, partner_session_token, expires_in, is_premium, premium_expires_at }`.
5. Клиент кэширует токен до `expires_in` (как сейчас в `refreshSessionIfNeeded`), **partner key на устройстве
   больше не нужен и удаляется**.

Email-линк (`/link/email/*`) — так же через сервер (s2s), эндпоинты ниже.

---

## 4. Эндпоинты НАШЕГО сервера

### 4.1 Auth / session (сервер держит ключ)

| Метод | Путь | Делает |
|---|---|---|
| POST | `/lmg/auth/telegram` | Валидирует Telegram-подпись → `partner_user_id` → апстрим `/session/issue` → отдаёт сессию |
| POST | `/lmg/auth/email/request` | Прокси на апстрим `/link/email/request` (s2s, с ключом) |
| POST | `/lmg/auth/email/verify` | Прокси `/link/email/verify` (s2s) → сессия |
| POST | `/lmg/auth/email/password/reset` | Прокси `/link/email/password/reset` (s2s) |
| POST | `/lmg/auth/email/password/change` | Прокси `/link/email/password/change` (s2s) |
| POST | `/lmg/session/refresh` | Если токен клиента протух — сервер минтит новый через `/session/issue` |
| POST | `/lmg/auth/logout` | Гасит сессию на нашей стороне |

**`/session/issue` тело апстриму:** `{ partner_user_id, hide_explicit }`, заголовок `X-Partner-Key`.
Ответ: `partner_session_token`, `expires_in`, `partner_user_id`, `scopes` (см. `IcmSessionResponse`).

### 4.2 ICM reverse-proxy (весь остальной партнёрский API)

Один универсальный обработчик: **`ALL /lmg/icm/*`** → форвардит на `${ICM_BASE_URL}/*`, подставляя
`X-Partner-Key`, пробрасывая клиентский `Authorization: Bearer <partner_session_token>`,
`X-Partner-User-Id`, query-строку, тело и метод; возвращает ответ апстрима как есть (включая `202` async и
`X-Request-Id`). Клиент просто меняет `BASE_URL` на `https://<наш-сервер>/lmg/icm`.

Проксируемые пути (полный инвентарь из клиента — их НЕ надо описывать по одному, прокси общий, но вот что
реально дёргается, для проверки покрытия и rate-limit'ов):

**Контент:**
- `GET /health`
- `GET /search?q=&region=&source=&limit=`
- `POST /track` (+ `?async=1`, ответ `202` = pending job) · `GET /track/{trackId}/lyrics` ·
  `GET /track/{trackId}/meta` · `GET /track/job/{jobId}` · `POST /tracks/meta`
- `GET /album/{albumId}` · `GET /artist/{artistId}`
- `POST /cover-sign` (подпись URL обложки)

**Wave (радио/DJ-станция):**
- `POST /wave/session/start` · `GET /wave/session/{sessionId}/next` · `POST /wave/session/{sessionId}/feedback`
- `GET /wave/next` · `GET /wave/genre/{...}` · `GET /wave/mood/{...}` · `POST /wave/feedback/{alias}`

**Библиотека / профиль (user-scoped, Bearer):**
- `GET /me/profile` · `GET/PUT /me/preferences` · `GET/PUT /me/region` · `GET /me/subscription`
- `GET /me/playlists` · `GET /me/playlists/{id}` · `POST /me/playlists/preview` ·
  `POST /me/playlists/import` · `GET /me/playlists/import/{jobId}`
- `GET /library/likes` · `POST/DELETE /library/likes` · `GET /library/subscriptions`
- `GET /library/wave/popular-artists` · `POST /library/wave/onboarding` · `POST /library/wave/feedback` ·
  `POST /library/wave/playback` · `POST /library/wave/reset`

> **Проверить по `https://byicloud.online/partners/api-docs`:** нужен ли `X-Partner-Key` на **всех**
> user-scoped вызовах, или Bearer-JWT достаточно. Если ключ нужен везде (в коде так: «always required for
> source validation») — оставляем полный прокси (Model B, как здесь). Если только `/session/issue` требует
> ключ — можно проксировать лишь auth, а контент клиент дёргает напрямую (Model A, легче). По коду сейчас —
> **Model B правильнее**.

### 4.3 Premium / подписка (server-authoritative)

Premium НЕ доверяем устройству. Наш сервер — источник истины:

| Метод | Путь | Делает |
|---|---|---|
| GET | `/lmg/me/subscription` | Возвращает `{ is_premium, premium_expires_at, plan }` из нашего стора (не с устройства) |
| POST | `/lmg/premium/verify` | (если появятся покупки) валидация чека Google Play / промо → выставить premium |

Клиентский `IcmAuthRepository.setPremium(...)` в проде должен читать premium **только отсюда**.

### 4.4 Admin (для админ-панели, `x-admin-key`)

Зеркалим стиль GsGit-админки:

| Метод | Путь | Делает |
|---|---|---|
| GET | `/admin/lmg/health` | `serverVersion`, uptime, апстрим ICM ok (пинг `/health`), premium-key задан |
| GET | `/admin/lmg/metrics?period=1h/24h/7d/30d` | сессии выданы, запросы к ICM, 4xx/5xx, ошибки апстрима |
| GET | `/admin/lmg/users` | список пользователей (telegram id / email / premium / last seen) |
| GET/DELETE | `/admin/lmg/users/{id}` | карточка / удалить |
| POST | `/admin/lmg/users/{id}/premium` | выдать/снять premium вручную (тело `{ until }`) |
| GET | `/admin/lmg/config` · POST `/admin/lmg/config` | server-driven конфиг LMG (feature-флаги, maintenance, min/latest version — как appconfig GsGit) |
| GET | `/admin/lmg/errors` | лог ошибок апстрима (без секретов) |
| POST | `/admin/lmg/session/test` | тестовый `/session/issue` (проверить, что ключ рабочий) |

### 4.5 Push (опционально, если нужны уведомления LMG)

По желанию — та же FCM-схема, что у GsGit (`/lmg/register`, `sendPush`). Для музыки полезно: «новая волна
готова», «плейлист импортирован». Можно отложить.

---

## 5. Клиентские правки в LMG (что просить сделать в приложении)

1. **`IcmApi.BASE_URL`** → `https://<наш-сервер>/lmg/icm` (вместо `byicloud.online/api/partner`).
2. **Убрать партнёрский ключ с устройства:** `IcmAuthRepository.getPartnerKey()`/`apiKey` и заголовок
   `X-Partner-Key` из `buildRequest` — удалить. Ключ теперь только на сервере.
3. **`/session/issue` с устройства — убрать.** Вместо `issueSession(...)` вызывать наш `POST /lmg/auth/telegram`
   (или `/lmg/session/refresh`), получать `partner_session_token` и класть в `sessionToken`.
4. **Email-линк** (`/link/email/*`) — перенаправить на `/lmg/auth/email/*`.
5. **Premium** — читать из `/lmg/me/subscription`, не из локального флага.
6. Остальное (`sessionToken` Bearer на user-scoped вызовы) **не меняется** — прокси прозрачен.
7. **CertificatePinner** в `IcmApi` — перевесить пин на наш домен (или снять на время, потом вернуть).

---

## 6. Интеграция с админ-панелью

Добавить в админку **третью вкладку «LMG»** рядом с GsGit/GlassFiles (переключатель backend'а уже есть):
- Обзор: `/admin/lmg/health` + `/admin/lmg/metrics` (карточки как у GsGit).
- Пользователи: список + выдача premium (`/admin/lmg/users`, `/admin/lmg/users/{id}/premium`).
- Конфиг: `/admin/lmg/config` (feature-флаги/maintenance/версии).
- Ошибки: `/admin/lmg/errors`.
- Кнопка «тест session» → `/admin/lmg/session/test`.

Инструкцию по самой админке (UI-вкладка) напишу отдельно — там реализует GPT.

---

## 7. Деплой (тот же VPS)

- Новый сервис в `docker-compose.yml` рядом с GsGit-сервером (свой контейнер, порт `LMG_PORT`).
- env из раздела 2 (`ICM_PARTNER_KEY` — только на сервере, в `.env`, не в код).
- Reverse-proxy (nginx/caddy на VPS) — маршрут `api.lmg.gsgit.org` или `api.gsgit.org/lmg` → контейнер LMG.
- `curl https://<домен>/admin/lmg/health -H "x-admin-key: ..."` → `serverVersion` + `icmUpstream: ok`.

---

## 8. Порядок работ (чтобы Fable не утонул)

1. Скелет сервера (роутер, `/data` сторы, admin-key, `/admin/lmg/health`).
2. Auth: `/lmg/auth/telegram` (валидация подписи) + s2s `/session/issue` → сессия клиенту.
3. Reverse-proxy `ALL /lmg/icm/*` с подстановкой `X-Partner-Key`.
4. Premium-стор + `/lmg/me/subscription` + admin выдача.
5. Admin: metrics/users/config/errors.
6. Клиентские правки (раздел 5), сверить с `byicloud.online/partners/api-docs`.
7. Деплой + smoke (`/admin/lmg/health`, тестовый `/session/issue`).

## 9. Что уточнить у byicloud (по api-docs) перед финалом

- Нужен ли `X-Partner-Key` на user-scoped вызовах (определяет Model A vs B — см. 4.2).
- Точные тела/ответы `/session/issue`, `/link/email/*`, `/me/subscription` (взять из `IcmModels.kt` в клиенте —
  там уже есть `IcmSessionRequest/Response` и пр., сериализация совпадает).
- TTL сессии (`expires_in`) и правила refresh.
- Rate-limit'ы партнёрки (в клиенте есть `IcmRateGate` — сверить лимиты, чтобы прокси не ловил 429).
