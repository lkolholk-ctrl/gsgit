# Эталон: data-сервис (GitHub API)

Применяется к файлам в `data/github/`. Цель — каждый домен расширяется добавлением одного файла,
core не трогается.

## Чек-лист «идеального» сервиса

- [ ] **Ноль прямого `openConnection()`** — всё через `GitHubManager.request()`.
- [ ] **Один файл = один домен** GitHub API (`GitHubManager+Issues.kt` и т.п.), extension-функции объекта.
- [ ] **Единый возврат** `ApiResult` (или доменный sealed-тип поверх него). Никаких `null` как «ошибки» без причины.
- [ ] **Единый error-handling** — `recordApiError` уже внутри ядра; не дублировать try/catch вокруг каждого вызова.
- [ ] **Пагинация** через `parseNextPage`, единообразно во всех list-методах.
- [ ] **JSON→модель только через переиспользуемые `parse`-функции.** Один тип = одна `fun parseGHX(json: JSONObject): GHX`.
      Запрет на ad-hoc `JSONObject(...)` разбор внутри каждого метода (сейчас таких ~713 — их и сокращаем).
- [ ] **Модели (data class) в отдельных файлах** (`model/GHPullRequest.kt`…), отдельно от сетевой логики.
- [ ] **Энкодинг путей** только через `repoPath()` / `encPath()`. Не клеить URL руками.
- [ ] **Чистые функции парсинга отделены от IO** — чтобы парсинг можно было тестировать без сети.

## Рецепт нарезки (нулевой риск)

1. `request`, `requestBasic`, `repoPath`, `encPath`, `responseHeaders`, `parseNextPage`,
   etag-кэш, rate-limit — пометить `internal`, оставить в `GitHubManager.kt` (это ядро).
2. Каждую группу методов вынести как extension-функции:
   ```kotlin
   // GitHubManager+PullRequests.kt
   internal suspend fun GitHubManager.getPullRequests(...): List<GHPullRequest> { ... request(...) ... }
   ```
   Вызовы `GitHubManager.getPullRequests(...)` на местах **остаются без изменений**.
3. После выноса каждого домена — собрать билд, прогнать затронутый экран, убедиться что поведение идентично.
4. `GitHubRepoSettingsManager.kt` — свести его request на общее ядро, затем нарезать так же.

## Домены (по убыванию объёма)

Repos, Actions+Workflows, Projects, Issues+Comments+Labels, PullRequests+Reviews+Checks,
Users+Orgs+Teams, Webhooks, Releases, Commits+Branches+Tags, Gists, Search, Discussions, Secrets,
Reactions, Stars+Forks, Notifications, Trees+Blobs+Contents.

## Definition of Done для домена

Файл < ~600 строк, ноль прямого HTTP, единый `ApiResult`, парсинг через общие `parse`-функции,
билд зелёный, поведение не изменилось.
