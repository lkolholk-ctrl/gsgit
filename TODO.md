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

## Известные баги (найдены при декомпозиции data-слоя, НЕ исправлены)

Это предсуществующие баги в логике — при нарезке доменов сохранены «как было» (рефактор,
не переписывание). Чинить отдельным behavioural-коммитом, проверив на реальном API.

- [ ] **Teams: литеральный `$encodedOrg` в URL** — `data/github/GitHubManager+Teams.kt`
  - Функции: `getTeamMembers`, `addTeamMember`, `removeTeamMember`, `getTeamDiscussions`,
    `createTeamDiscussion`.
  - Симптом: URL собирается через `"/orgs/${'$'}encodedOrg/teams/${'$'}encodedTeam/..."`.
    `${'$'}` даёт ЛИТЕРАЛЬНЫЙ символ `$`, поэтому в путь уходит текст `$encodedOrg`/`$encodedTeam`,
    а вычисленные `val encodedOrg`/`encodedTeam`/`encodedUser` по факту не подставляются →
    эндпоинт битый (управление участниками/обсуждениями команд не работает).
  - Фикс: заменить `${'$'}encodedOrg` → `$encodedOrg` (и аналогично team/user). После — собрать
    и проверить экран Teams. (Замечание: компилятор предупреждает «variable never used» на этих val.)

- [ ] **createCommitStatus: пишет android `Context` вместо `statusContext`** —
  `data/github/GitHubManager+Commits.kt`
  - Симптом: тело запроса `put("context", context)` кладёт объект android `Context`, а параметр
    `statusContext` игнорируется → у создаваемого commit-status поле `context` неверное.
  - Фикс: `put("context", statusContext)`. После — проверить создание commit-status.
