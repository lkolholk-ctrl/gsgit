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
**Модель входа (уточнена по реальной ссылке).** `partner_user_id` **генерит САМА апка** и передаёт его В НАЧАЛЕ
входа — ICM лишь привязывает Telegram-аккаунт к этому id на своей стороне. Пример стартовой ссылки:
```
https://byicloud.online/partner/msng/link
  ?partner_user_id=lg_<random16hex>          ← генерим 1 раз, храним в prefs (стабильный id юзера)
  &redirect_uri=https://liquid.glassfiles.ru/auth/telegram   ← CF-воркер, вернёт deep-link в апку
  &state=<uuid>                              ← анти-replay, СВЕРИТЬ при возврате
  &app_name=Liquid Music Glass
```
Формат id: `"lg_" + 16 hex` (напр. `lg_c618bd5b4d8b4436`). Это **bearer-секрет** — только по TLS, НЕ логировать.
Deep-link назад несёт `state` (и признак успеха), НЕ несёт токен — токен апка получает уже от нашего сервера.

Поток:
1. Один раз: `partnerUserId = "lg_" + randomHex(16)` → prefs. `state = UUID` (на каждый вход новый).
2. Открыть `partner/msng/link?partner_user_id=...&redirect_uri=...&state=...&app_name=...`.
3. Вернулись deep-link'ом → **сверить `state`** (не совпал → отменить, не входить).
4. **`POST <LMG_SERVER>/lmg/session/issue`** с `{ "partner_user_id": "<lg_...>", "hide_explicit": <bool> }`.
   Ответ: `{ partner_session_token, expires_in, scopes, partner_user_id, is_premium, premium_expires_at, plan }`.
   → `partner_session_token` в `IcmApi.sessionToken`, `partner_user_id` в `IcmApi.partnerUserId`.
5. Рефреш протухшего токена: **`POST <LMG_SERVER>/lmg/session/refresh`** с `{ partner_user_id }` (тот же id из prefs).

**Удалить** с устройства `IcmApi.issueSession()`/прямой `/session/issue`, весь s2s (`s2sOnly`,
`X-Partner-Key` на session/issue). Данные Telegram-виджета (`id/hash/auth_date`) не нужны — их апка и не видит.

> Сервер лимитит минт (20/мин на IP на `/session/issue`, 30/мин на `/refresh`) → на 429 сделать паузу+ретрай,
> не долбить в цикле. `partner_user_id` при логах/аналитике маскировать.

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
