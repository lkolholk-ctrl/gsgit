# Задача для Claude Code: enforcement анти-тампера + консолидация токена

Цель: сейчас native-проверки (Frida/root/Magisk/debugger/emulator) вызываются ТОЛЬКО в
`GitHubDiagnosticsModule` и работают информационно. Нужно превратить их в реальный gate.
Плюс убрать дублирование хранилищ токена.

> Заметка по реальности: клиентский анти-тампер обходится решительным атакующим. Это
> повышает порог, а не делает защиту абсолютной. Цель — не отдавать токен в скомпрометированной среде.

## Затрагиваемые файлы (уже существуют)

- `security/NativeSecurity.kt` — `isEnvironmentSafe()`, `runSecurityChecks()`, `encryptToken`/`decryptToken`.
- `data/github/GitHubAuth.kt` — `getToken`/`saveToken` (сейчас через TokenRepository + legacy-миграция).
- `data/security/TokenRepository.kt` — Jetpack `EncryptedSharedPreferences` (deprecated).
- `App.kt` (`gs.git.vps.App`) — точка старта приложения.

## Часть 1 — Gate на старте

1. В `App.onCreate()` вызвать `NativeSecurity.runSecurityChecks()` один раз, результат закэшировать
   (например в `@Volatile var environmentSafe: Boolean` в `App` или отдельном `SecurityGate`-объекте).
2. Политика при небезопасной среде — **сделать настраиваемой**, по умолчанию `BLOCK_SENSITIVE`:
   - `BLOCK_SENSITIVE` (дефолт): не отдавать токен, показать предупреждающий экран/баннер, UI работает в read-only без авторизации.
   - `WARN_ONLY`: показать баннер, но не блокировать (для отладки/совместимости).
   - `WIPE`: вызвать `logout()` (стереть токен) при детекте — только если осознанно нужно.
   Политику хранить в обычных prefs, дефолт `BLOCK_SENSITIVE`.
3. Не крашить приложение при детекте — деградировать мягко (ложные срабатывания у легитимных rooted-юзеров реальны).

## Часть 2 — Gate перед выдачей токена (главное)

В `GitHubAuth.getToken(context)` **перед возвратом токена** проверять среду:

```kotlin
fun getToken(context: Context): String {
    if (!SecurityGate.isSafe(context)) return ""   // policy BLOCK/WIPE — не отдаём токен
    ... // существующая логика TokenRepository + миграция
}
```
- Так даже если злоумышленник дошёл до вызова API, в скомпрометированной среде токен не утечёт.
- `decryptToken` тоже не вызывать при небезопасной среде.

## Часть 3 — Консолидация хранилища токена

Сейчас два механизма: `TokenRepository` (Jetpack `EncryptedSharedPreferences`, **deprecated**) +
legacy native-encrypted. Свести на **native-путь** (device-bound, hardware-derived ключ, сильнее):

1. `saveToken`: `NativeSecurity.encryptToken(token)` → Base64 → хранить в prefs (или DataStore).
2. `getToken`: читать → Base64 decode → `NativeSecurity.decryptToken(bytes)` (после gate-проверки).
3. Оставить одноразовую миграцию со старого `EncryptedSharedPreferences`-ключа, затем удалить зависимость
   `androidx.security:security-crypto` из gradle и сам `TokenRepository` (или сделать его тонкой обёрткой над native).
4. Проверить, что `logout()` чистит оба пути на время переходного периода.

## Часть 4 — Логи

В `proguard-rules.pro` сейчас стрипаются только `Log.v/d/i`. Проверить, что `Log.w/e`
(в `GitHubAuth`, `GitHubManager`) не пишут токен/чувствительное. Либо добавить `w` в `assumenosideeffects`.

## Acceptance criteria

- [ ] `runSecurityChecks()` вызывается на старте, результат закэширован.
- [ ] `getToken()` и `decryptToken` не отдают токен при `!isSafe` (политика BLOCK/WIPE).
- [ ] Политика настраиваемая, дефолт BLOCK_SENSITIVE, без краша на детекте.
- [ ] Токен хранится одним механизмом (native), deprecated Jetpack-либа удалена после миграции.
- [ ] `Log.w/e` не содержат секретов.
- [ ] Билд зелёный, обычный (безопасный) сценарий логина/работы не сломан.

## Порядок выполнения

Часть 2 (gate в getToken) → Часть 1 (старт) → Часть 3 (консолидация) → Часть 4 (логи).
Один коммит на часть, билд после каждой.
