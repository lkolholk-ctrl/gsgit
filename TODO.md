# GsGit: План работы по сборке и исправлению безопасности

## Текущие задачи

- [x] **Исправление ложного детектирования среды (False Positive)**
  - Изменен алгоритм расшифровки системных строк в `security.c` (используется статический `BUILD_KEY` вместо нестабильного `g_hw_key()`).
  - Уникальный аппаратный ключ устройства сохранен для безопасного шифрования токена GitHub.

- [x] **Связь с репозиторием Git и Пуш изменений**
  - Файлы `security.c` и `TODO.md` скопированы на VPS.
  - Изменения успешно закоммичены и отправлены в GitHub-репозиторий `lkolholk-ctrl/GlassFiles-1`.

- [x] **Синхронизация и сборка на VPS**
  - Подключились по SSH к `myvps`.
  - Настроили путь к Android SDK (`sdk.dir=/opt/android-sdk` в `local.properties`).
  - Выполнили сборку релизного APK с помощью `./gradlew assembleRelease`.

- [x] **Доставка сборки на телефон**
  - APK-файл успешно скачан с VPS на телефон в папку загрузок под именем `gsgit-release-fixed.apk`.

## Известные баги (найдены при декомпозиции data-слоя) — ИСПРАВЛЕНЫ ✅

Это предсуществующие баги в логике (при нарезке доменов сохранялись «как было», чинились
отдельным behavioural-коммитом). Оба закрыты, чистая сборка зелёная.

- [x] **Teams: литеральный `$encodedOrg` в URL** — `data/github/GitHubManager+Teams.kt`
  - Функции: `getTeamMembers`, `addTeamMember`, `removeTeamMember`, `getTeamDiscussions`,
    `createTeamDiscussion`.
  - Было: URL собирался через `"/orgs/${'$'}encodedOrg/teams/${'$'}encodedTeam/..."` — `${'$'}` давал
    ЛИТЕРАЛЬНЫЙ `$`, в путь уходил текст `$encodedOrg`/`$encodedTeam`, вычисленные
    `val encodedOrg`/`encodedTeam`/`encodedUser` не подставлялись → эндпоинт битый.
  - Фикс: `${'$'}encodedOrg` → `$encodedOrg` (и team/user). Теперь URL подставляет URL-encoded значения.

- [x] **createCommitStatus: писал android `Context` вместо `statusContext`** —
  `data/github/GitHubManager+Commits.kt`
  - Было: `put("context", context)` клало объект android `Context`, параметр `statusContext`
    игнорировался → поле `context` у commit-status было неверным.
  - Фикс: `put("context", statusContext)`.
