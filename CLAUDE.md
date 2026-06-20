# GsGit — Claude Code project memory

Полноценный GitHub-клиент под Android. ~60.8k строк Kotlin, один модуль, Jetpack Compose.
Package root: `gs.git.vps`. HTTP — голый `HttpURLConnection` (Ktor НЕ используем, не предлагать).

> Этот файл грузится каждую сессию. Держать кратким. Детали эталонов — в `.claude/rules/`.

## Карта (где что лежит)

- `data/github/` — слой GitHub API. Ядро: `GitHubManager.kt` (god-файл, 9k строк).
- `ui/screens/` — Compose-экраны (тут god-классы: RepoModule 8.1k, ActionsModule 6k, CodeEditor 4.4k).
- `ui/components/`, `ui/liquid/`, `ui/theme/` — переиспользуемые UI-примитивы, glassmorphism, тема.
- `security/`, `data/security/` — нативная защита (XOR, hardware keys, O-MVLL, NDK r26).
- `logging/`, `notifications/`, `workers/`, `util/` — инфраструктура.

## Архитектурные инварианты (НЕ нарушать)

1. **Единый сетевой слой.** Любой вызов GitHub API идёт через `GitHubManager.request()`.
   Прямой `openConnection()` вне ядра ЗАПРЕЩЁН в новом коде. Ядро уже даёт: ETag-кэш,
   retry на rate-limit с ожиданием reset, backoff на 5xx, retry на сетевых, proxy, error-tracking.
2. **Единый тип возврата** — `ApiResult`. Ошибки — через `recordApiError`, не глотать молча.
3. **Пагинация** — только через `parseNextPage(headers)`. Не парсить Link-заголовок руками заново.
4. **Никаких секретов в коде/репо.** Токены/ключи — только через защищённое хранилище.
5. **Обфускация — в самом конце**, на уже нарезанные модули. Не обфусцировать god-классы.

## Известные протечки (чинить при касании этих файлов)

Прямой `openConnection()` мимо ядра — завести в `GitHubManager.request()`:
- `ui/screens/GitHubRepoModule.kt` — сырой README (нужен кастомный Accept → добавить параметр в ядро)
- `ui/screens/GitHubCodeEditorModule.kt` — сырой файл (дублирует proxy-логику ядра)
- `ui/screens/GitHubDiagnosticsModule.kt` — пинг `/zen`
- `data/github/GitHubRepoSettingsManager.kt` — собственный мини-request (свести на ядро)
- `ui/screens/GitHubSettingsModule.kt` — вызов LLM `chat/completions` (это НЕ GitHub; оставить, но вынести в отдельный AI-клиент, не в Composable)

## Конвенция декомпозиции

**Data-слой** (`GitHubManager`): резать по доменам GitHub API через **extension-функции объекта**
в файлах `GitHubManager+<Domain>.kt` (Repos, Issues, PullRequests, Actions, Workflows, Projects,
Users, Orgs, Teams, Gists, Releases, Webhooks, Commits, Branches, Search, Discussions, Secrets…).
`request()` и хелперы → `internal`. **Сигнатуры вызовов не меняются** — `GitHubManager.x(...)` как было.
См. `.claude/rules/data-service.md`.

**UI-слой** (god-экраны): тонкий экран + стейт в state-holder/ViewModel, под-Composable'ы и диалоги
в отдельные файлы, UDF, ноль сети/бизнес-логики в Composable. См. `.claude/rules/ui-screen.md`.

## Порядок работ

1. Data-слой по доменам (быстро, риск ноль) — `GitHubManager` + `GitHubRepoSettingsManager`.
2. UI god-экраны (RepoModule → ActionsModule → CodeEditor).
3. Тройная обфускация на готовые куски.

## Правила сессии

- **Один модуль за коммит.** Не трогать 60k за раз.
- После каждого модуля — **собрать билд и убедиться, что поведение не изменилось** (рефактор, не переписывание).
- Между разными модулями — `/clear`, чтобы контекст не засорялся.
- Сначала доводим до идеала ДВА эталонных модуля (один data, один UI), фиксируем как конвенцию,
  потом реплицируем на остальные строго по эталону.
