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

## Часть 3 — Хранилище токена

`TokenRepository` на `EncryptedSharedPreferences` остаётся основным хранилищем: он использует
Android Keystore и AES-256. Native `encryptToken`/`decryptToken` в текущей реализации — legacy-
совместимость, а не замена AES-хранилища. После gate-проверки допускается только одноразовая
миграция старого native-зашифрованного ключа в `TokenRepository`; затем legacy-ключ удаляется.

## Часть 4 — Логи

В `proguard-rules.pro` сейчас стрипаются только `Log.v/d/i`. Проверить, что `Log.w/e`
(в `GitHubAuth`, `GitHubManager`) не пишут токен/чувствительное. Либо добавить `w` в `assumenosideeffects`.

## Acceptance criteria

- [x] `runSecurityChecks()` вызывается на старте, результат закэширован.
- [x] `getToken()` и `decryptToken` не отдают токен при `!isSafe` (политика BLOCK/WIPE).
- [x] Политика настраиваемая, дефолт BLOCK_SENSITIVE в release и WARN_ONLY в debug, без краша на детекте.
- [x] Токен хранится в EncryptedSharedPreferences (AES-256 / Android Keystore); legacy native-ключ мигрируется один раз.
- [x] `Log.w/e` не содержат тел ответов и текстов исключений; в release они удаляются R8.
- [ ] Билд зелёный, обычный (безопасный) сценарий логина/работы не сломан.

## Порядок выполнения

Часть 2 (gate в getToken) → Часть 1 (старт) → Часть 3 (консолидация) → Часть 4 (логи).
Один коммит на часть, билд после каждой.
