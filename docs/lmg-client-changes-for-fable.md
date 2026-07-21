# LiquidMusicGlass — клиентские правки под серверный брокер ICM (для Fable)

Серверная часть готова и развёрнута (брокер держит партнёрский ключ ICM, минтит сессии, владеет premium,
реверс-проксирует партнёрку). Задача в APK: **перевести клиент с прямого `byicloud.online` на наш сервер и
убрать секрет с устройства.** Прокси прозрачный — относительные пути (`/search`, `/track`, `/wave`, `/me/*`)
НЕ меняются, меняется только база и заголовки.

`<LMG_SERVER>` = базовый URL нашего сервера (например `https://api.lmg.gsgit.org` или `https://api.gsgit.org/lmg`).

## Файлы
- `app/src/main/kotlin/com/liquidmusicglass/api/icm/IcmApi.kt`
- `app/src/main/kotlin/com/liquidmusicglass/api/icm/IcmAuthRepository.kt`

## 1. База API → наш прокси
`IcmApi.kt`:
```kotlin
// было
const val BASE_URL = "https://byicloud.online/api/partner"
// стало
const val BASE_URL = "https://<LMG_SERVER>/lmg/icm"
```
Все `execute("/search"|"/track"|"/wave/..."|"/me/...")` теперь автоматически идут на прокси — трогать их НЕ надо.

## 2. Убрать партнёрский ключ с устройства
`IcmApi.buildRequest(...)` — удалить блок, добавляющий `X-Partner-Key` (ключ теперь подставляет сервер):
```kotlin
// УДАЛИТЬ:
val partnerKey = IcmAuthRepository.getPartnerKey().takeIf { it.isNotBlank() } ?: apiKey
if (!partnerKey.isNullOrBlank()) builder.header("X-Partner-Key", partnerKey)
```
`X-Partner-User-Id` — ОСТАВИТЬ (прокси его пробрасывает, сервер использует для /me/subscription).
В `IcmAuthRepository` убрать хранение/использование `apiKey`/`getPartnerKey()` (партнёрский ключ в APK не хранить).

## 3. Выпуск сессии — через сервер (не s2s с устройства)
**Модель входа (согласована):** Telegram Login Widget живёт на странице ICM, приложение полей виджета
(`id/hash/auth_date`) НЕ видит — после логина ICM возвращает deep-link `linked/<icm_user_id>/state`.
Значит на устройстве нет ни бот-токена, ни HMAC. Клиент берёт `icm_user_id` из deep-link и шлёт его
как `partner_user_id` на наш сервер; сервер (origin уже в whitelist ICM) минтит upstream-сессию.

Заменить прямой s2s `POST /session/issue` на:
- После возврата из ICM deep-link (`linked/<icm_user_id>/state`) вызывать
  **`POST <LMG_SERVER>/lmg/session/issue`** с телом `{ "partner_user_id": "<icm_user_id>", "hide_explicit": <bool> }`.
  Ответ: `{ partner_session_token, expires_in, scopes, partner_user_id, is_premium, premium_expires_at, plan }`.
  → `partner_session_token` в `IcmApi.sessionToken`, `partner_user_id` (= icm_user_id) в `IcmApi.partnerUserId` (сохранить в prefs).
- Рефреш протухшего токена: **`POST <LMG_SERVER>/lmg/session/refresh`** с `{ partner_user_id }` (тот же сохранённый id).
- **Удалить** с устройства `IcmApi.issueSession()`/прямой `/session/issue`, весь s2s (`s2sOnly`,
  `X-Partner-Key` на session/issue) и любые данные Telegram-виджета — они больше не нужны.

> Сверить одно: что `icm_user_id` из deep-link — это и есть `partner_user_id` для upstream `/session/issue`
> (в партнёрке ICM это наш идентификатор пользователя). Если ICM отдаёт другой линк-id — передавай именно его.

## 4. Email-линк → на сервер
`/link/email/{request|verify|password/reset|password/change}` → **`<LMG_SERVER>/lmg/auth/email/{...}`**
(тело то же; сервер сам добавит партнёрский ключ). Убрать прямые вызовы byicloud `/link/email/*`.

## 5. Premium — только с сервера
`IcmAuthRepository.setPremium(...)` в проде читать из **`GET <LMG_SERVER>/lmg/me/subscription`**
(заголовок `X-Partner-User-Id: <partner_user_id>`). Ответ `{ is_premium, premium_expires_at, plan }`.
Локальный флаг premium использовать только как кэш, источник истины — сервер.

## 5a. Заголовки устройства (для админ-панели «Устройства», паритет с GsGit)
На КАЖДЫЙ запрос к `<LMG_SERVER>/lmg/*` (в `IcmApi.buildRequest`) добавить два заголовка — без геолокации:
```kotlin
builder.header("X-Device-Id", stableInstallId)   // стабильный per-install UUID (SharedPreferences, генерится 1 раз)
builder.header("X-App-Version", BuildConfig.VERSION_NAME)
```
`stableInstallId` — обычный `UUID.randomUUID()`, сохранённый при первом запуске. Платформу сервер сам берёт
из `User-Agent`. Ничего больше слать не надо — сервер сведёт устройства по юзеру и покажет их в админке.

## 6. CertificatePinner
В `IcmApi` пин сейчас на `byicloud.online`. Перевесить на домен `<LMG_SERVER>` (или временно снять и вернуть
после того, как домен/сертификат зафиксируется).

## 7. (Опционально) серверный конфиг
`GET <LMG_SERVER>/lmg/config` → `{ maintenance, minVersion, latestVersion, changelog, downloadUrl, waveEnabled, importEnabled }`.
Можно использовать под feature-флаги/maintenance, как appConfig в GsGit.

## Definition of Done
- В APK нет `X-Partner-Key` и нет прямых обращений к `byicloud.online` (только `<LMG_SERVER>/lmg/...`).
- Логин через Telegram/email проходит через сервер, `sessionToken` приходит от сервера.
- Premium читается с сервера. CertificatePinner на нашем домене. Сборка зелёная, воспроизведение играет.

> Сверить формы ответов с `IcmModels.kt` (там уже есть `IcmSessionResponse` и пр. — сериализация совпадает).
