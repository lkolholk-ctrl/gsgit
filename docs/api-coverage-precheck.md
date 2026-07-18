# Предварительный чек покрытия GitHub API (перед аудитом 2.0.0)

Дата: 2026-07-18, версия приложения 1.0.83.
Это экспертная оценка «по именам функций» против REST API GitHub (v2022-11-28).
Точный построчный дифф по OpenAPI-спеке — отдельным этапом в 2.0.0
(см. roadmap-2.0-api-audit.md). Здесь — где густо, где пусто и куда копать.

**Итого: 467 функций в 36 доменных файлах.**

Легенда: ✅ полно · 🟡 есть заметные дыры · 🔴 главный кандидат на доработку · ⚰️ на удаление

## Сводная таблица

| Домен | Функций | Вердикт | Короткий диагноз |
|---|---|---|---|
| Workflows + Actions + Secrets | 79 | ✅ | Лучший домен проекта, покрыт почти весь Actions API |
| Users | 34 | ✅ | Профиль, ключи, почта, соцсети, блокировки — всё |
| Notifications | 27 | ✅ | Вплоть до thread subscriptions и mark-done |
| Repos | 42 | ✅ | Очень полно; см. мелочи ниже |
| Issues | 23 | 🔴 | Много чтения, мало управления — главная цель 2.0 |
| PullRequests | 22 | 🟡 | REST полон, не хватает GraphQL-фич |
| Security | 15 | 🟡 | Всё видим, ничего не можем сделать (read-only) |
| ProjectsV2 | 14 | 🟡 | Нельзя добавить существующий issue/PR в проект |
| GitData | 14 | ✅ | Полный низкоуровневый Git (refs/trees/blobs/tags) |
| Webhooks | 14 | ✅ | Включая deliveries и redelivery |
| Projects (classic) | 13 | ⚰️ | API убит GitHub'ом — выпилить в 2.0 |
| Releases | 13 | ✅ | Мелочь: нет generate-notes |
| Contents | 13 | ✅ | Свой богатый флоу коммитов/загрузок |
| Discussions | 12 | 🟡 | Нет edit/delete комментария |
| Gists | 12 | ✅ | Мелочь: нет ревизий и списка форков |
| Branches | 12 | 🟡 | Protection есть, но крупными мазками |
| Orgs | 11 | 🟡 | Нет invitations и outside collaborators |
| Teams | 11 | ✅ | |
| Commits | 10 | ✅ | Мелочь: нет «PRs по коммиту» |
| RepoMeta | 10 | ✅ | |
| Reactions | 9 | 🟡 | Нет реакций на discussions/commit comments/releases |
| Packages | 8 | ✅ | |
| Rulesets | 7 | ✅ | |
| RepoFeatures | 7 | ✅ | Autolinks, codespaces, LFS |
| Auth | 7 | ✅ | Device flow + управление токенами App |
| Apps | 6 | ✅ | Ровно под нужды GsGit App |
| Search | 5 | ✅ | Все 7 типов поиска (репо/юзеры в др. доменах) |
| Diagnostics | 5 | ✅ | |
| Collaborators | 4 | ✅ | Мелочь: нет get-permission юзера |
| Events | 2 | ✅ | Минимум, но осознанный |
| Прочие (Code*, Home, ActionsCapabilities) | 6 | ✅ | Служебные |

## Разбор проблемных доменов

### 🔴 Issues (23) — приоритет №1 в 2.0

Чтение отличное (детали, комменты, события, таймлайн), управление дырявое:

- **Лейблы**: можно `addLabelsToIssue` — а **снять лейбл с иссуи нельзя**.
  Нет и `updateLabel` (переименовать/перекрасить), нет replace-all.
- **Ассайни**: есть только список возможных (`getAssignees`).
  **Назначить/снять исполнителя нельзя вообще.**
- **Майлстоуны**: создать можно, **изменить/закрыть/удалить нельзя**.
  Нет и привязки/отвязки майлстоуна к иссуе (если не идёт через updateIssueMeta — проверить).
- Нет: перенос иссуи в другой репо (transfer), pin/unpin, sub-issues (новый API 2024+).

### 🟡 PullRequests (22)

REST-часть добротная (ревью, комменты, чеки, merge). Дыры — там, где GitHub
вынес функционал в GraphQL:

- **Draft ⇄ Ready for review** — только GraphQL (`convertPullRequestToDraft` /
  `markPullRequestReadyForReview`). У нас нет.
- **Auto-merge** включить/выключить — GraphQL. Нет.
- **Resolve / unresolve review thread** — GraphQL. Нет (в вебе это главная кнопка ревью!).
- Мелочь REST: нет list requested reviewers отдельным вызовом, нет codeowners-errors.

### 🟡 Security (15)

Смотреть можем всё (Dependabot, code scanning, secret scanning, advisories),
**действовать — нет**: dismiss/reopen алертов Dependabot и code scanning
отсутствуют. Для секьюрити-инбокса «прочитал и закрыл» — половина ценности.

### 🟡 ProjectsV2 (14)

CRUD проекта, поля, драфты, значения — есть. Главная дыра:
**нельзя добавить существующий issue или PR в проект**
(`addProjectV2ItemById`). Сейчас можно только создать draft-заметку.
Плюс не трогаем views и project workflows (read есть? — сверить).

### 🟡 Discussions (12)

Создать/ответить/пометить ответом — есть. **Редактирование и удаление
комментария — нет.** Опечатка в ответе = навсегда.

### 🟡 Branches (12)

Protection читаем/пишем целиком объектом. Нет тонких ручек: управление
required status checks по одному, restrictions (users/teams/apps),
enforce_admins отдельно. Нет merge queue API.

### 🟡 Мелкие дыры одной строкой

- Reactions: нет реакций на discussions / commit comments / releases.
- Repos: нет Pages API, stats/* (participation, code frequency), custom properties.
- Orgs: нет приглашений в организацию и outside collaborators.
- Releases: нет `generate-notes` (автогенерация чейнджлога — удобно!).
- Collaborators: нет проверки уровня прав конкретного юзера.
- Commits: нет «pull requests, содержащие коммит».

### ⚰️ Projects (classic, 13 функций)

REST classic Projects похоронен GitHub'ом (410 Gone). Создание мы уже
перевели на V2; в 2.0 выпилить весь домен и вкладку Classic из UI.

## Что это значит для 2.0.0

Порядок работ из роадмапа подтверждается с уточнением:

1. **Issues** — управление лейблами/ассайни/майлстоунами (самая заметная юзеру дыра).
2. **PullRequests** — GraphQL-тройка: draft toggle, auto-merge, resolve threads.
3. **ProjectsV2** — addProjectV2ItemById (+ views).
4. **Security** — dismiss/reopen алертов.
5. **Discussions** — edit/delete комментариев.
6. Остальное — по механическому диффу со спекой (мелочь и полировка).

И отдельно: домены ✅ («полно») всё равно проходят спек-дифф в 2.0 —
эта оценка по именам функций не видит недостающие ПОЛЯ в моделях
и небезопасный парсинг. Она отвечает на вопрос «какие ручки есть»,
а не «все ли данные мы из них достаём».
