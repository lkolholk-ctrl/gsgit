# План расширения модулей (после нарезки)

Заполнять/уточнять по мере нарезки. Идея: после декомпозиции у каждого домена есть понятные
**точки расширения**, и новый функционал добавляется по шаблону, а не врезается в god-класс.

## Общий шаблон расширения (любой data-домен)

Добавить новый эндпоинт GitHub API = :
1. Модель `model/GHXxx.kt` (data class) + `parseGHXxx(json)`.
2. Метод-extension в `GitHubManager+<Domain>.kt`, вызывает `request(...)`, возвращает `ApiResult`/модель.
3. Если list — пагинация через `parseNextPage`.
4. core (`request`, хелперы) НЕ трогается.

Добавить новый экран/фичу в UI = :
1. Собрать из существующих компонентов `ui/components/` + нужный под-Composable.
2. Стейт в новый/существующий `ViewModel`, вызовы только в VM.
3. Диалоги — отдельные файлы. `@Preview` на stateless-куски.

## По доменам — текущее и куда расширять

> COVERAGE заполнит Claude Code, прочитав каждый сервис после нарезки. Ниже — ориентиры.

**Repos (самый большой)**
- Точки роста: branch protection rules, repo rulesets, topics, autolinks, custom properties, traffic/views API.
- Расширяемость: вынести подгруппы (settings, collaborators, contents) в под-файлы домена.

**Actions + Workflows**
- Рост: workflow re-run failed jobs, artifacts download, cache list/delete, environments, variables (не только secrets), runner groups.
- Watch: long-poll статусов ранов — единый поллер, не дубли по экранам.

**Pull Requests + Reviews + Checks**
- Рост: requested reviewers teams, auto-merge enable/disable, draft↔ready, review threads resolve, suggested changes commit.
- Watch: mergeability вычисляется асинхронно на стороне GitHub — корректно ретраить `mergeable == null`.

**Issues + Comments + Labels + Milestones**
- Рост: sub-issues, issue types, lock/unlock, transfer, timeline events, label из шаблонов.

**Users + Orgs + Teams**
- Рост: org roles, team membership sync, fine-grained PAT awareness, org audit log (если scope есть).

**Projects (v2 / GraphQL)**
- ВАЖНО: Projects v2 — это GraphQL, не REST. Если домен растёт — выделить отдельный `GraphQLClient`
  поверх того же `request()` (один POST `/graphql`), не смешивать с REST-парсингом.

**Gists / Releases / Webhooks / Discussions / Search / Secrets**
- Releases: assets upload (multipart/binary) — отдельный путь в ядре под бинарный body.
- Search: единый rate-limit (отдельный лимит у GitHub) — учитывать.
- Discussions: тоже частично GraphQL — см. заметку про Projects.
- Webhooks: deliveries list + redeliver.

## Кросс-модульные расширения (делать один раз, переиспользовать)

- **GraphQL-клиент** поверх `request()` — для Projects v2 и Discussions.
- **Бинарный upload/download** в ядре — для release assets и сырых файлов (заодно убирает протечку в CodeEditor).
- **Единый поллер статусов** (Actions runs, checks) — вместо повторов по экранам.
- **Слой кэша моделей** (поверх etag) — если понадобится офлайн/быстрый старт.

## Порядок

Сначала нарезка + аудит (CLAUDE.md, audit-playbook.md). План расширения — для шага «после»:
по каждому домену Claude Code дописывает реальный COVERAGE и предлагает 2-3 ближайшие фичи.
