# GsGit Admin API v2 — новые серверные ручки (бриф для GPT)

> Это дополнение к `admin-app-brief-for-gpt.md`. На сервере `api.gsgit.org` добавлены
> новые эндпоинты. Все — под заголовком `X-Admin-Key` (тот же ключ, что и раньше).
> Ответы — JSON. Коды ошибок те же: `401 {"error":"bad admin key"}`, `400 {"error":"bad json"}`,
> `404 {"error":"not found"}`. Обрабатывай КАЖДЫЙ. Ничего из старого контракта не изменилось —
> `/admin/stats`, `/admin/devices`, `/admin/appconfig`, `/announce` работают как прежде,
> только `/admin/devices` теперь отдаёт больше полей (см. ниже), а `/announce` пишет историю.

Базовый URL: `https://api.gsgit.org`

---

## 1. Состояние сервера — `GET /admin/health`
```json
{ "status": "ok", "uptimeSec": 86420, "serverVersion": "1.1.0",
  "database": "ok", "firebase": "ok", "githubWebhooks": "ok",
  "pushQueue": 3, "serverTime": "2026-07-19T17:30:00.000Z" }
```
`firebase`/`githubWebhooks` = `"ok"`/`"off"` в зависимости от конфигурации. Используй для дашборда
как «реальное здоровье», а не факт ответа.

## 2. Метрики — `GET /admin/metrics?period=24h`
`period` ∈ `1h|24h|7d|30d` (или `?hours=N`).
```json
{ "period": "24h", "hours": 24, "registrations": 5, "activeDevices": 18,
  "pushesSent": 120, "pushesFailed": 3, "heldPushes": 2, "githubEvents": 40,
  "requests": 900, "responses4xx": 12, "responses5xx": 0 }
```
Данные — из почасовых бакетов (окно до 31 суток). Для графиков дёргай разные `period`.

## 3. История рассылок
- `GET /admin/announcements?limit=50&cursor=...` →
  ```json
  { "items": [ { "id": "ann_ab12cd", "title": "...", "url": "...",
                 "createdAt": "...", "targeted": 18, "delivered": 16,
                 "failed": 2, "status": "completed" } ], "nextCursor": null }
  ```
  (тело `body` и токены наружу не отдаются в списке).
- `GET /admin/announcements/{id}` — детали (с `body`, без токенов).
- `POST /admin/announcements/{id}/retry` — повтор неудачных → `{ "ok": true, "delivered": N, "stillFailed": M }`.
- `POST /admin/announcements/{id}/cancel` → `409` (броадкаст синхронный, отменять нечего) — покажи как «нельзя отменить».

## 4. Тест-пуш на одно устройство — `POST /admin/devices/{deviceId}/test-push`
Body (опц.): `{ "title": "...", "body": "...", "url": "..." }`.
Ответ `200 {"ok":true,"delivered":1}` или `502 {"ok":false,"delivered":0}` (FCM отклонил).
`deviceId` бери из `/admin/devices` (см. ниже) — это устойчивый непрозрачный id, НЕ `tokenTail`.

## 5. Диагностика устройств
`GET /admin/devices` теперь для каждого устройства возвращает:
```json
{ "deviceId": "bf97795da08edc22", "name": "HONOR BVL-N49", "appVersion": "1.0.85",
  "tzOffsetMin": 180, "quietHours": {"start":23,"end":8}, "heldCount": 0,
  "pushEnabled": true, "registeredAt": "...", "lastSeenAt": "...",
  "lastPushAt": "...", "lastPushStatus": "delivered", "tokenTail": "a1b2c3" }
```
Фильтры: `?login=<substr>`, `?status=active` (только с включёнными пушами).
Плюс по одному устройству:
- `GET /admin/devices/{deviceId}` — детали.
- `DELETE /admin/devices/{deviceId}` — удалить регистрацию (с подтверждением в UI!).
- `POST /admin/devices/{deviceId}/clear-held` → `{ "ok": true, "cleared": N }`.
- `POST /admin/devices/{deviceId}/disable-push` body `{ "enabled": false }` (false = выключить,
  true = включить) → `{ "ok": true, "pushEnabled": false }`.

## 6. История конфигурации и откат
- `GET /admin/appconfig/history?limit=&cursor=` → список ревизий
  `{ revision, changedAt, changedFields, reason }`.
- `GET /admin/appconfig/history/{revision}` → та же запись + `snapshot` (полный конфиг на тот момент).
- `POST /admin/appconfig/rollback` body `{ "revision": 3 }` → применяет снимок, отдаёт новый конфиг.
- `POST /admin/appconfig/validate` body с полями конфига → `{ "ok": bool, "errors": [ ... ] }`
  (проверяет `x.y.z` и что `minVersion` ≤ `latestVersion`). **Валидируй перед сохранением.**
- `POST /admin/appconfig` теперь принимает и `reason` (строка, попадает в ревизию).

## 7. Планирование техработ
- `POST /admin/maintenance/schedule` body `{ "startsAt": ISO, "endsAt": ISO, "message": "..." }`
  → окно само включит и снимет `maintenance`. Ответ `{ "ok": true, "schedule": {...} }`.
- `GET /admin/maintenance` → `{ "maintenanceNow": "...", "schedule": {...}|null }`.
- `DELETE /admin/maintenance/schedule` — снять расписание (и снять блокировку, если оно её включило).
- `POST /admin/maintenance/stop` — немедленно снять техработы и расписание.

## 8. Управление релизами
- `GET /admin/releases?limit=&cursor=` — список.
- `POST /admin/releases` body `{ version, changelog?, url?, sha256?, mandatory?, rollout? }` — upsert.
- `POST /admin/releases/{version}/publish` — **атомарно** проставит `latestVersion`,
  `changelog`, `downloadUrl` (и `minVersion`, если `mandatory`). Меньше риска записать
  несовместимые значения руками.

## 9. Журнал админ-действий — `GET /admin/audit?limit=100&cursor=...`
```json
{ "items": [ { "id": "aud_...", "at": "...", "ip": "1.2.3.4",
               "action": "appconfig.rollback", "result": "ok", "meta": {...} } ],
  "nextCursor": null }
```
Пишется на каждое мутирующее действие. Ключи и тела в журнал не попадают.

## 10. Серверные ошибки — `GET /admin/errors?service=push&limit=50`
```json
{ "items": [ { "id": "err_...", "service": "push", "code": "FCM_UNREGISTERED",
               "message": "device token no longer valid", "count": 4,
               "createdAt": "...", "lastAt": "..." } ] }
```
Агрегировано по `service+code`, без секретов и стектрейсов.

---

## Как поднять на сервере
Изменения только серверные, чисто аддитивные. На VPS:
```
cd /root/gsgit/server && git pull && docker compose up -d --build
```
Проверка: `curl -s -H "X-Admin-Key: $(cat /root/gsgit/server/data/admin.key)" https://api.gsgit.org/admin/health`
