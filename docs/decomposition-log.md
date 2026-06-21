# Журнал декомпозиции data-слоя (GitHubManager)

Цель шага 1 «Порядка работ» (см. CLAUDE.md): нарезать god-файл `data/github/GitHubManager.kt`
(9008 строк, 452 функции, 713 ad-hoc `JSONObject(...)`-разборов) по доменам через
extension-функции `GitHubManager+<Domain>.kt`, не меняя сигнатуры вызовов.

Сначала доводим до идеала **ОДИН эталонный data-домен**, фиксируем как конвенцию, потом реплицируем.

## Конвенция эталона (что показываем на первом домене)

1. Методы домена → `GitHubManager+<Domain>.kt`, `internal`-extension-функции объекта.
   Вызовы `GitHubManager.x(...)` на местах **не меняются**.
2. Ядро (`request`, `parseNextPage`, `updateApiUrl`, `TAG`) → пометить `internal` (было `private`),
   `request`/`parseNextPage` остаются в `GitHubManager.kt`.
3. Модели домена (`data class GH*`) → `model/GHX.kt` (новая директория `data/github/model/`).
4. Парсинг — через чистые `parseGHX(json)`-функции (без IO), переиспользуемые, отделённые от сети.
5. Билд зелёный, поведение идентично (рефактор, не переписывание).

## Эталонный домен: Releases (выбран пользователем)

Состав в `GitHubManager.kt` (до правок):
- Функции: строки 1982–2225 (13 шт.): getReleases, createRelease, createReleaseDetailed,
  getReleaseByTag, updateRelease, updateReleaseDetailed, publishRelease, deleteRelease,
  deleteReleaseAsset, downloadReleaseAsset, downloadReleaseAssetWithProgress,
  uploadReleaseAsset, uploadReleaseAssetDetailed.
- Хелперы: parseRelease (2227–2244), parseReleaseAsset (2246–2254), getContentType (2256–2268).
  `getContentType` используется только в Releases — переносим вместе.
- Модели: GHRelease (7978–7989), GHAsset (7991–7999) — выносим в `model/`.

Зависимости домена от ядра (сделать `internal`):
- `request` (стр.110, private suspend), `parseNextPage` (242), `updateApiUrl` (41), `TAG` (21).
- `getApiUrl` (23), `getToken` (104) — уже public, трогать не нужно.

## Лог действий

- [ходовой осмотр] Составлена карта: 452 функции, домены по убыванию — repo(83), run(30),
  workflow(27), project(27), comment(26), pullrequest(25)... releases(13). Моделей `parseGH*`=0,
  ad-hoc разборов=713. Директории `model/` нет.
- [baseline] `./gradlew compileDebugKotlin` ДО правок — **exit 0, зелёный**. Окружение: JDK 17,
  android-sdk=/opt/android-sdk. Можно безопасно резать.
- [releases] Нарезка эталонного домена Releases:
  1. Созданы модели `model/GHRelease.kt` (GHRelease + GHAsset) в подпакете
     `gs.git.vps.data.github.model`.
  2. Создан `GitHubManager+Releases.kt` — 13 `internal`-extension-функций объекта; вызовы
     `GitHubManager.x(...)` на местах не менялись. Парсинг приведён к конвенции:
     `parseGHRelease(json)` / `parseGHAsset(json)` — чистые функции без IO.
     `getContentType` → локальная `releaseContentType`; TAG → локальная `RELEASES_TAG`
     (приватный `TAG` ядра не трогаем).
     Прямой `openConnection()` оставлен только в upload/download ассетов — это бинарные стримы,
     которые текстовое ядро `request()` не обслуживает (законное исключение, помечено в шапке файла).
  3. Из `GitHubManager.kt` удалён блок Releases (стр. 1982–2269) и модели GHRelease/GHAsset.
     Файл: 9008 → 8697 строк.
  4. Ядро помечено `internal` (было `private`): `request`, `parseNextPage`, `updateApiUrl`.
     `getApiUrl`/`getToken` уже были public.
  5. Импорты моделей переключены на `.model` в потребителях: GitHubReleasesModule.kt,
     GitHubRepoModule.kt (wildcard не покрывает подпакет — добавлен явный импорт),
     ReleaseDownloadWorker.kt. В GitHubManager.kt ссылок на модели не осталось.
  6. Контрольная компиляция `compileDebugKotlin` — **exit 0, зелёная**, по домену предупреждений нет.

### Итог по эталону Releases ✅

- `GitHubManager+Releases.kt` — 303 строки (< 600, DoD выполнен).
- `model/GHRelease.kt` — 29 строк.
- `GitHubManager.kt` — 8697 строк (было 9008; −311).
- Ноль прямого HTTP мимо ядра, кроме обоснованного стрима ассетов. Парсинг через `parseGHX`.
- Поведение идентично (рефактор, сигнатуры не менялись). Билд зелёный.

**Конвенция зафиксирована — этот домен служит образцом для остальных.**
Следующие кандидаты (компактные): Gists(13), Webhooks(12), Secrets(8), Search(7), Notifications(6).
Перед массовой репликацией CLAUDE.md рекомендует довести до идеала ещё ОДИН эталон — UI-экран.

Коммит: `6d77c93 refactor(data): extract Releases domain from GitHubManager god-file`.

## Домен Gists (по эталону Releases)

- Созданы `model/GHGist.kt` (GHGist + GHGistComment) и `GitHubManager+Gists.kt`
  (12 `internal`-extension-функций: getGists, createGist, getGistContent, deleteGist,
  starGist, unstarGist, isGistStarred, forkGist, updateGist, getGistComments,
  addGistComment, deleteGistComment).
- Inline-парсинг выделен в чистые `parseGHGist`/`parseGHGistComment`.
  Замечание: `getGistComments` читает поле `user_login` (как и было) — поведение сохранено, не менял.
- Из `GitHubManager.kt` удалён блок Gists (функции + модели). Файл: 8697 → 8599 строк.
- В потребитель `GitHubGistsAndDialogsModule.kt` (wildcard `data.github.*`) добавлен явный
  импорт `.model.GHGist`/`.GHGistComment`.
- Контрольная компиляция `compileDebugKotlin` — **exit 0, зелёная**. DoD выполнен
  (`GitHubManager+Gists.kt` < 600 строк, ноль прямого HTTP, парсинг через parseGHX).

Коммит: `0a9a0d1 refactor(data): extract Gists domain from GitHubManager god-file`.

## Домен Webhooks (по эталону Releases)

- Особенность: функции были разбросаны по двум местам — org-хуки (getOrgHooks/createOrgHook)
  и repo-вебхуки. Собраны в один `GitHubManager+Webhooks.kt` (14 `internal`-extension-функций).
- Модели `model/GHWebhook.kt`: GHWebhook, GHWebhookConfig, GHWebhookDelivery.
- Парсеры приведены к конвенции: parseGHWebhook/parseGHWebhookConfig/parseGHWebhookDelivery.
  Приватный хелпер `parseHeaderMap` использовался только в этом домене — перенесён вместе
  (остался private в файле домена).
- Ядро: `encPath` помечен `internal` (его звали org-хуки).
- Из `GitHubManager.kt` удалены 3 блока (org-хуки, repo-вебхуки+парсеры, модели) и пустой
  заголовок-разделитель «Webhooks». Файл: 8599 → 8399 строк.
- Импорты потребителей на `.model`: GitHubWebhooksModule.kt, GitHubSettingsModule.kt.
- Контрольная компиляция — (результат ниже).

### ⚠️ Важное уточнение конвенции (всплыло на Webhooks)

При выносе метода `object`-а в **extension-функцию** `fun GitHubManager.fn()` вызов
`GitHubManager.fn(...)` компилируется ТОЛЬКО если функция в области видимости:
- файл в пакете `gs.git.vps.data.github` — видит автоматически;
- иначе файл-потребитель ОБЯЗАН импортировать `import gs.git.vps.data.github.*`
  (wildcard подтягивает top-level extension-функции пакета) либо точечно `import ...github.fn`.

Раньше (метод был членом object) импорт не требовался — поэтому «вызовы не меняются» верно
лишь при наличии wildcard-импорта. Половина экранов уже на wildcard; остальным он добавлен.

**Подвох инкрементальной сборки gradle:** `compileDebugKotlin` может дать ложный exit 0,
не перекомпилировав downstream-потребителя (так Releases-коммит 6d77c93 скрыл сломанный
`ReleaseDownloadWorker` без импорта). Вывод: после нарезки домена проверять, что ВСЕ
файлы, зовущие перенесённые функции, имеют wildcard `data.github.*`; при сомнении — clean build.

Файлы, которым добавлен wildcard на этом этапе: GitHubWebhooksModule, GitHubSettingsModule,
GitHubActionsModule, GitHubReleasesModule, ReleaseDownloadWorker.

### Итог по Webhooks ✅

- **Чистая сборка `./gradlew clean compileDebugKotlin` — BUILD SUCCESSFUL (1m11s)**, exit 0.
  Только предсуществующие deprecation-warnings про `Icons.*` (не связаны с декомпозицией).
  Это первый честный (без инкрементального кэша) зелёный после всех трёх доменов.
- `GitHubManager.kt`: 9008 → 8399 строк (−609 за три домена). Вынесено: Releases (13),
  Gists (12), Webhooks (14) = 39 функций, 7 моделей, 3 файла домена + 3 файла моделей.

Коммит: `c3aad45 refactor(data): extract Webhooks domain + fix extension-fn imports`.

## Домен Notifications (по эталону Releases)

- `GitHubManager+Notifications.kt`: 15 `internal`-extension-функций (getNotifications,
  listNotifications, markNotificationRead, markThreadRead, markAllNotificationsRead,
  markRepoNotificationsRead, getThreadSubscription, setThreadSubscription,
  deleteThreadSubscription, markThreadDone, getNotification, isWatching, isWatchingRepo,
  watchRepo, unwatchRepo) + private extension `fetchNotifications`.
- Inline-парсинг → чистые `parseGHNotification`/`parseGHThreadSubscription`. Приватный хелпер
  `githubApiUrlToWebUrl` использовался только тут — перенесён (остался private в файле).
- Модели `model/GHNotification.kt`: GHNotification (с computed-алиасами), GHThreadSubscription.
- Из `GitHubManager.kt` удалены функции и модели. Файл: 8399 → 8201 строк.
- Wildcard добавлен потребителям функций: GitHubNotificationsModule, GitHubRepoSettingsModule,
  NotificationSyncWorker. Явные импорты моделей переключены на `.model` в GitHubSettingsModule,
  GitHubNotificationsModule.
- Контрольная компиляция `compileDebugKotlin` — **BUILD SUCCESSFUL (24s), exit 0**.
  Все потребители функций (wildcard) и типов (model-import) проверены grep'ом заранее.

### Промежуточный итог (4 домена) ✅

`GitHubManager.kt`: 9008 → 8201 строк (−807). Вынесено 4 домена: Releases(13), Gists(12),
Webhooks(14), Notifications(15+1) = 55 функций, 9 моделей. 4 файла домена + 4 файла моделей.
Конвенция стабильна и задокументирована.

Коммит: `422982d refactor(data): extract Notifications domain from GitHubManager god-file`.

## Домен Search (по эталону Releases)

- Вынесен **advanced search**: searchCode, searchIssuesAdvanced, searchCommitsAdvanced,
  searchTopics, searchLabels (5 функций) + private extension `getRepositoryId`.
  `searchRepos`/`searchUsers` ОСТАВЛЕНЫ в god-файле — возвращают GHRepo/GHUser и переедут
  в домены Repos/Users (используют их parse-функции).
- Модели `model/GHSearchResult.kt`: GHCodeResult, GHSearchIssueResult, GHSearchCommitResult,
  GHTopicSearchResult, GHLabelSearchResult.
- Парсинг → чистые parseGHCodeResult/parseGHSearchIssueResult/parseGHSearchCommitResult/
  parseGHTopicSearchResult/parseGHLabelSearchResult (последний берёт repository параметром).
  Приватные хелперы repoNameFromIssueSearch/parseLabelNames/parseTopicNameArray использовались
  только тут — перенесены (остались private в файле).
- Из `GitHubManager.kt` удалены функции и модели. Файл: 8201 → **7989 строк** (ниже 8000).
- Потребители GitHubAdvancedSearchModule, GitHubExploreModule: импорты моделей → `.model`,
  добавлен wildcard.
- Контрольная компиляция `compileDebugKotlin` — **BUILD SUCCESSFUL (11s), exit 0**.

### Промежуточный итог (5 доменов) ✅

`GitHubManager.kt`: 9008 → 7989 строк (−1019). Вынесено: Releases(13), Gists(12), Webhooks(14),
Notifications(16), Search(5+1) = 61 функция, 14 моделей. 5 файлов домена + 5 файлов моделей.

Коммит: `9a71bfd refactor(data): extract Search domain from GitHubManager god-file`.

## Домен Secrets (по эталону Releases)

- Особенность: домен разбит на 2 несмежных блока (repo-secrets и env-secrets+variables),
  между ними — отдельный домен Environments (оставлен на месте). Secret-scanning (6339+) —
  это домен Security, НЕ трогали.
- `GitHubManager+Secrets.kt`: 11 `internal`-extension-функций (repo actions secrets + public-key,
  environment secrets, repo actions variables). Значения шифруются `GitHubSecretCrypto.encryptSecret`.
- Модели `model/GHSecret.kt`: GHActionPublicKey, GHActionSecret, GHActionVariable, GHEnvironmentSecret.
- Inline-парсинг → чистые parseGHActionSecret/parseGHActionPublicKey/parseGHActionVariable/
  parseGHEnvironmentSecret. TAG → локальный SECRETS_TAG.
- Из `GitHubManager.kt` удалены 2 блока функций (с заголовком «Environment Secrets») и 4 модели.
  Файл: 7989 → 7859 строк.
- Потребители: GitHubActionsModule (импорты моделей → `.model`), GitHubSecurityModule
  (fully-qualified `...github.GHActionSecret` → `.model`, добавлен wildcard для функций).
- Контрольная компиляция `compileDebugKotlin` — **BUILD SUCCESSFUL (17s), exit 0**.

### Промежуточный итог (6 доменов) ✅

`GitHubManager.kt`: 9008 → 7859 строк (−1149). Вынесено: Releases(13), Gists(12), Webhooks(14),
Notifications(16), Search(6), Secrets(11) = 72 функции, 18 моделей. 6 файлов домена + 6 моделей.

**Дальше — по запросу пользователя — крупные домены** (Repos ~83, Actions+Workflows ~50+,
Projects ~27, Issues ~24, PullRequests ~25). Они объёмные и с перекрёстными зависимостями
парсеров — резать осторожно, по под-доменам, с проверкой потребителей.

## ПЛАН: крупный домен Repos (следующий, лучше со свежим контекстом)

В god-файле ~70 функций с «Repo» в имени, НО многие принадлежат другим доменам — НЕ тащить их
в Repos, оставить для соответствующих доменов:
- → **Actions**: getRepoSelfHostedRunners, deleteRepoSelfHostedRunner, createRepoRunnerRegistrationToken,
  createRepoRunnerRemoveToken, getRepoActionsPermissions, setRepoActionsPermissions,
  getRepoActionsWorkflowPermissions, setRepoActionsWorkflowPermissions, getRepoActionsRetention,
  setRepoActionsRetention, getRepositoryArtifacts.
- → **Projects**: getRepoProjects, createRepoProject, getRepoProjectsV2.
- → **Security**: getRepositorySecurityAdvisor*, getRepositorySecuritySettings, setPrivateVulnerabilityReporting.
- → **Teams**: getRepoTeams, addRepoTeam, updateRepoTeamPermission, removeRepoTeam.
- → **Apps**: getAppInstallationRepositories, addRepositoryToAppInstallation, removeRepositoryFromAppInstallation.

**Чистое ядро Repos (резать сюда), ~35-40 функций:** getRepos, getRepo, createRepo*,
deleteRepo, getRepoContents, starRepo/unstarRepo/forkRepo, getRepoLicense, traffic-*
(views/clones/referrers/paths), getRepoStargazers/Watchers/Events, getUserRepos/StarredRepos/
WatchedRepos, getOrgRepos, getUserRepositoryInvitations, getRepoInteractionLimit/set/remove,
getRepoSettings/updateRepoSettings, getRepoTopics, getRepoTags, getRepoDeployKeys/create/delete,
transferRepo, getRepoInvitations/update/delete.
Модели: GHRepo, GHContent, GHLicenseDetail, GHTraffic*, GHRepoPerson, GHRepoEvent, GHTag,
GHDeployKey, GHRepoSettings, GHRepoInvitation, GHUserRepositoryInvitation, GHInteractionLimitEntry…
ВНИМАНИЕ: `parseRepo`/`parseUser` используются и в оставшихся searchRepos/searchUsers — при
выносе пометить эти parse-функции `internal` (как ядро), не ломать чужие вызовы.

Из-за объёма (~40 функций, перекрёстные парсеры) Repos лучше резать в свежем контексте
строго по этому плану; журнал позволяет восстановить состояние мгновенно.

## РЕАЛИЗОВАНО: домен Repos ✅

Нарезан по плану выше (свежий контекст). `GitHubManager.kt`: 7859 → 7208 строк (−651).

1. **Модели** вынесены в `model/`: `GHRepo.kt` (GHRepo + GHPermissions + GHRepoCreateResult),
   `GHContent.kt`, `GHTraffic.kt` (GHTrafficSeries/Point/Referrer/Path), `GHRepoActivity.kt`
   (GHRepoPerson + GHRepoEvent), `GHRepoMeta.kt` (GHTag, GHDeployKey, GHRepoSettings,
   GHRepoInvitation, GHUserRepositoryInvitation, GHInteractionLimitEntry), `GHLicenseDetail.kt`.
   `canWrite`/`canAdmin` (extension на GHRepo) ОСТАВЛЕНЫ в `GitHubManager.kt` — они в пакете
   `gs.git.vps.data.github`, перенос в `.model` сломал бы импорты у потребителей без выгоды.
2. **`GitHubManager+Repos.kt`** — 40 `internal`-extension-функций (569 строк): getRepos/getRepo,
   createRepo*/deleteRepo, getRepoContents, star/unstar/forkRepo, getRepoLicense, traffic-*,
   stargazers/watchers/events, getUserRepos/Starred/Watched/OrgRepos,
   getUserRepositoryInvitations(+accept/decline), interaction-limits, settings/topics/tags,
   deploy-keys, transferRepo, repo-invitations. Хелперы парсинга `parseTrafficSeries`/
   `parseRepoPerson` перенесены сюда как private. Вызовы `GitHubManager.x(...)` не менялись.
3. **Шаренные хелперы ядра → `internal`** (были private): `parseRepo` (нужен ещё `searchRepos`,
   остался в core), `repoPath`, `refQuery` (нужен ещё `getFileContent`). `request`/`encPath`/
   `parseNextPage` уже были internal.
4. **Потребители** (compiler-driven): wildcard-импорты не покрывают подпакет `.model` —
   в 20 экранов добавлены явные импорты моделей; в 4 экрана без wildcard добавлены явные импорты
   перенесённых extension-функций (getRepoContents/getRepoInvitations/updateRepoInvitation/
   deleteRepoInvitation/getUserRepos/getStarredRepos/getRepoLicense). В `GitHubManager.kt`
   добавлены импорты GHRepo/GHPermissions/GHRepoEvent/GHInteractionLimitEntry/GHLicenseDetail
   (используются оставшимися searchRepos/parseRepo/getUserReceived|PublicEvents/getLicense/native-лимитами).
5. Контрольная компиляция `compileDebugKotlin` — **BUILD SUCCESSFUL, exit 0**. Поведение идентично (рефактор).

### Итог (7 доменов) ✅
Вынесено: Releases, Gists, Webhooks, Notifications, Search, Secrets, **Repos**. `GitHubManager.kt`: 9008 → 7208.

## РЕАЛИЗОВАНО: домен Actions+Workflows ✅

Самый крупный домен, нарезан по под-доменам в ДВА файла, один коммит.
`GitHubManager.kt`: 7208 → 6196 строк (−1012). Вынесено 60 функций + 12 парсеров.

1. **`GitHubManager+Workflows.kt`** (585 строк) — CI-исполнение: workflows, runs, jobs,
   dispatch(+schema/yaml-парсер), artifacts, check-runs, usage, review/approve. Парсеры
   parseWorkflow/parseWorkflowRun/parseJobs/parseArtifact(s)/parseActionsUsage/
   parseWorkflowDispatchSchema(+yaml*) перенесены как private. Приватный хелпер
   `getRedirectLocationOrText` стал `private suspend fun GitHubManager.` (звал getToken).
   Бинарные стримы (zip-логи/артефакты) остались на прямом openConnection() — законное
   исключение (текстовое ядро request() их не обслуживает), помечено в шапке.
2. **`GitHubManager+Actions.kt`** (361 строка) — инфраструктура: deployments, environments,
   caches, self-hosted runners (repo/org/enterprise), actions-permissions/retention.
   Парсеры parseEnvironment/parseActionRunner перенесены как private.
3. **Модели → `model/`**: `GHWorkflow.kt` (GHActionResult, GHWorkflow, *DispatchInput/Schema,
   GHWorkflowRun, GHJob, GHStep, GHArtifact, GHCheckRun, GHCheckAnnotation, GHActionsUsage,
   GHWorkflowRunReview); `GHActions.kt` (GHPendingDeployment, GHDeployment, GHActionsCacheUsage/
   Entry, GHActionRunner, GHActionRunnerGroup, GHRunnerToken, GHActionsPermissions,
   GHWorkflowPermissions, GHActionsRetention, GHEnvironment, GHEnvironmentProtectionRule,
   GHDeploymentBranchPolicy).
4. **Шаренные хелперы → `internal`**: `apiErrorMessage` (звался из getCheckRunAnnotations).
   `request`/`repoPath`/`encPath`/`parseNextPage`/getApiUrl/getToken — уже доступны.
   НЕ тащили: getPullRequestCheckRuns (домен PR, тоже зовёт GHCheckRun → импорт в core),
   интерливленные Org-audit/SCIM/SAML/OAuth/device-flow (чужие домены, между runner-функциями),
   codespaces. В core добавлены импорты GHActionResult/GHCheckRun.
5. **Потребители** (compiler-driven): 7 экранов — импорты моделей; 4 экрана без wildcard —
   импорты перенесённых функций. Поймана ловушка: `model.GHEnvironmentSecret` как подстрока
   ломала проверку наличия импорта `GHEnvironment` — поправлено вручную.
6. `compileDebugKotlin` — **BUILD SUCCESSFUL, exit 0**. Поведение идентично (рефактор).

### Итог (9 доменов, 2 файла Actions) ✅
Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos, **Workflows, Actions**.
`GitHubManager.kt`: 9008 → 6196.

## РЕАЛИЗОВАНО: домен Projects ✅

Нарезан по эталону, разбит по под-доменам в ДВА файла (как Actions+Workflows).
`GitHubManager.kt`: 6196 → 5418 строк (−778). Вынесено 27 функций + 9 парсеров + 1 private-helper.

1. **`GitHubManager+Projects.kt`** (156 строк) — classic Projects (REST): getRepoProjects,
   getProject, createRepoProject, getOrgProjects, createOrgProject, updateProject, deleteProject,
   getProjectColumns, createProjectColumn, getProjectCards, createProjectCard, moveProjectCard,
   deleteProjectCard (13 функций) + парсеры parseProject/parseProjectColumn/parseProjectCard.
2. **`GitHubManager+ProjectsV2.kt`** (539 строк) — Projects V2 (GraphQL): getRepoProjectsV2,
   getProjectV2Detail, updateProjectV2, create/update/deleteProjectV2Field,
   add/updateProjectV2DraftIssue, delete/archiveProjectV2Item, update/clearProjectV2ItemFieldValue,
   moveProjectV2Item (14 функций) + private-helper projectV2SingleSelectOptionsJson + парсеры
   parseProjectV2Detail/Field/View/Workflow/Item/ItemFieldValue. Объём раздут GraphQL-литералами
   (один getProjectV2Detail — ~120 строк query); разбиение classic/V2 удержало оба файла < 600.
3. **Модели → `model/GHProject.kt`** (11 data class): GHProject, GHProjectColumn, GHProjectCard,
   GHProjectV2, GHProjectV2Detail, GHProjectV2Field, GHProjectV2FieldOption, GHProjectV2View,
   GHProjectV2Workflow, GHProjectV2Item, GHProjectV2ItemFieldValue.
4. **Ядро: `graphql` помечен `internal`** (был private) — его зовут все V2-функции. `request`/
   `encPath` уже internal. Helper `projectV2SingleSelectOptionsJson` (не использует this) →
   top-level private в V2-файле.
5. **Потребитель один** — GitHubProjectsModule.kt (проверено grep'ом): 10 импортов моделей
   переключены на `.model`, добавлены явные импорты 24 перенесённых extension-функций
   (экран без wildcard). Других вызывающих файлов нет — риск ловушки инкрементальной сборки нулевой.
6. **Чистая сборка `./gradlew clean compileDebugKotlin` — BUILD SUCCESSFUL (44s), exit 0.**
   Только предсуществующие deprecation-warnings про `Icons.*` (не связаны с декомпозицией).

### Итог (10 доменов) ✅
Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos, Workflows, Actions, **Projects**.
`GitHubManager.kt`: 9008 → 5418. Следующие крупные домены: Issues+Comments+Labels (~24),
PullRequests+Reviews+Checks (~25), Users+Orgs+Teams, Commits+Branches+Tags, Discussions.

## РЕАЛИЗОВАНО: домен Issues ✅

Нарезан по эталону Releases. `GitHubManager.kt`: 5418 → 5128 строк (−290).
Вынесено 23 функции + 7 чистых `parseGHX`-парсеров в один файл.

1. **`GitHubManager+Issues.kt`** (271 строка) — issues (getIssues, createIssue, getIssueDetail,
   close/reopen/lock/unlockIssue, updateIssueMeta), комментарии (getIssueComments, addComment,
   update/deleteIssueComment), события (getIssueEvents, getIssueEventsForIssue, getIssueEvent),
   timeline (getIssueTimeline), метки/вехи/assignees (getLabels, createLabel, deleteLabel,
   addLabelsToIssue, getMilestones, createMilestone, getAssignees). Inline-парсинг (раньше был
   размазан по методам — часть из тех 713 ad-hoc разборов) выделен в чистые
   `parseGHIssue/parseGHComment/parseGHIssueEvent/parseGHIssueDetail/parseGHLabel/parseGHMilestone/
   parseGHTimelineEvent`. Приватный хелпер ядра `parseIssueEvent` переименован в `parseGHIssueEvent`
   и перенесён сюда (использовался только Issues).
2. **Модели → `model/GHIssue.kt`** (7 data class): GHIssue, GHIssueEvent, GHIssueDetail, GHComment,
   GHLabel, GHMilestone, GHTimelineEvent. `GHUserLite` (возвращает getAssignees) и `GHReaction`
   ОСТАВЛЕНЫ в core — первый шарится широко, второй переедет в домен Reactions.
3. **Реакции НЕ трогали:** getIssueReactions/addIssueReaction/… и PR-review-comment-реакции
   используют общий `GHReaction` и общий DELETE `/reactions/{id}` — это отдельный домен Reactions,
   режется позже целиком. Из Issues исключены сознательно (как Search оставил searchRepos).
4. **Ядро не трогали** — `request`/`parseNextPage` уже `internal`. Новых `internal`-пометок не
   потребовалось. `extraHeaders` (mockingbird-preview для timeline) — штатный параметр `request()`.
5. **Протечка в core:** `getDiscussionComments` (остаётся в god-файле до домена Discussions) тоже
   возвращает `GHComment` → в `GitHubManager.kt` добавлен `import …model.GHComment`.
6. **Потребители** (compiler-driven): GitHubRepoModule (wildcard есть) — добавлены 7 явных
   импортов моделей; GitHubDiscussionsModule — `import …github.GHComment` → `…github.model.GHComment`;
   GitHubGistsAndDialogsModule зовёт Issues-функцию, но wildcard уже есть и моделей не использует.
7. **Чистая сборка `./gradlew clean compileDebugKotlin` — BUILD SUCCESSFUL, exit 0.**
   Поведение идентично (рефактор, сигнатуры не менялись).

### Итог (11 доменов) ✅
Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos, Workflows, Actions, Projects, **Issues**.
`GitHubManager.kt`: 9008 → 5128. Следующие крупные домены: PullRequests+Reviews+Checks (~25),
Users+Orgs+Teams, Commits+Branches+Tags, Discussions, Reactions.

## РЕАЛИЗОВАНО: домен PullRequests+Reviews+Checks ✅

Нарезан по эталону Releases. `GitHubManager.kt`: 5128 → 4768 строк (−360).
Вынесено 22 функции + 7 чистых `parseGHX`-парсеров (+ private `parseUsers`) в один файл.

1. **`GitHubManager+PullRequests.kt`** (303 строки) — PR (getPullRequests, getPullRequestDetail,
   createPullRequest, updatePullRequest, mergePullRequest, updatePullRequestBranch,
   getPullRequestMergedStatus, getPullRequestFiles), ревью (getPullRequestReviews,
   getPullRequestReview, updatePullRequestReview, deletePullRequestReview, submitPullRequestReview,
   request/removePullRequestReviewers), review-комментарии (get/create/update/delete +
   getPullRequestReviewComment), checks (getPullRequestCheckRuns, getPullRequestCheckSuites).
   Разбросанный inline-парсинг сведён к чистым `parseGHPullRequest` (единый для list/detail —
   list-ответ не содержит detail-полей, opt-дефолты дают тот же результат), `parseGHPullReview`,
   `parseGHPullFile`, `parseGHReviewComment`, `parseGHCheckRun`, `parseGHCheckSuite`. Приватные
   хелперы `parseUsers`/`parsePullReview` (PR-only) перенесены; `parseGitRef`/`parseOAuthTokenInfo`
   (чужие домены, стояли рядом) ОСТАВЛЕНЫ в core. Прямой `URLEncoder.encode(ref)` в check-функциях
   заменён на ядровый `encPath(ref)` (идентично).
2. **Модели → `model/GHPullRequest.kt`** (6 data class): GHPullRequest, GHPullMergeStatus,
   GHPullReview, GHPullFile, GHReviewComment, GHCheckSuite. `GHCheckRun` остался в `model/GHWorkflow.kt`
   (переехал с Actions) — импортируется, не дублируется.
3. **Ядро не трогали** — `request`/`parseNextPage`/`encPath` уже `internal`. Новых пометок не
   потребовалось.
4. **Сознательно НЕ тащили** (как Issues оставил реакции): реакции на review-комментарии
   (getPullRequestReviewCommentReaction*) — домен Reactions (общий GHReaction + DELETE /reactions/{id});
   getCommitComments/createCommitComment (тоже возвращают GHReviewComment) — домен Commits;
   compareCommits — домен Commits. Из-за этого в `GitHubManager.kt` добавлен
   `import …model.GHReviewComment` (нужен оставшемуся getCommitComments), а неиспользуемый
   `import …model.GHCheckRun` удалён.
5. **Потребители** (compiler-driven): GitHubRepoModule (wildcard есть) — +6 явных импортов моделей;
   GitHubSecurityModule (wildcard) — +import GHPullRequest и правка fully-qualified
   `gs.git.vps.data.github.GHPullRequest?` → короткое имя (FQN ловил старый пакет); GitHubDiffModule
   (без wildcard) — 2 импорта моделей на `.model` + 6 явных импортов перенесённых функций;
   GitHubCheckRunsModule (без wildcard) — GHCheckSuite на `.model` + 2 явных импорта функций;
   GitHubCompareModule (без wildcard) — +import createPullRequest; GitHubGistsAndDialogsModule
   (wildcard) — без правок.
6. **Чистая сборка `./gradlew clean compileDebugKotlin` — BUILD SUCCESSFUL, exit 0.**
   Только предсуществующие deprecation-warnings про `Icons.*` (не связаны с декомпозицией).
   Поведение идентично (рефактор, сигнатуры не менялись).

### Итог (12 доменов) ✅
Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos, Workflows, Actions, Projects,
Issues, **PullRequests**. `GitHubManager.kt`: 9008 → 4768. Следующие крупные домены:
Users+Orgs+Teams, Commits+Branches+Tags, Discussions, Reactions.

## РЕАЛИЗОВАНО: домен Users+Orgs+Teams ✅

Крупный гетерогенный домен, нарезан по эталону Releases в ТРИ файла (как Actions/Projects), один коммит.
`GitHubManager.kt`: 4768 → 3984 строк (−784). Вынесено 57 функций + чистые `parseGHX`-парсеры.

1. **`GitHubManager+Users.kt`** (374 строки, 35 функций) — текущий пользователь и его кэш
   (getUser/getCachedUser/clearGitHubUserCache), поиск пользователей (searchUsers, был оставлен
   доменом Search «до Users»), профиль и контрибуции (getUserProfile/getUserContributions/
   getCurrentUserProfile/updateCurrentUserProfile), follow (is/follow/unfollowUser), почты, SSH/GPG-
   ключи, соц-аккаунты, подписчики/подписки/блок-лист, interaction-limits (native). Парсинг сведён к
   `parseGHUser` (единый для `/user` и search-item — у search нет счётчиков, opt-дефолты совпадают),
   `parseGHUserProfile`, `parseGHEmailEntry`; приватный `JSONObject.cleanString` перенесён.
   TAG → локальный `USERS_TAG` (private `TAG` ядра не трогаем).
2. **`GitHubManager+Orgs.kt`** (194 строки, 11 функций) — организации/членство (getOrganizations,
   getOrg, getOrgMembership, updateOrgMembership, getOrgMembers, removeOrgMember, updateOrg) и
   org-админ (getOrgAuditLog, getOrgScimUsers, getOrgSamlAuthorizations, removeOrgSamlAuthorization).
   Парсеры parseGHOrg/parseGHAuditLogEntry/parseGHScimUser/parseGHSamlAuthorization.
3. **`GitHubManager+Teams.kt`** (185 строк, 11 функций) — команды репо/орг (getRepoTeams/getOrgTeams/
   add/update/removeRepoTeam), участники (getTeamMembers/add/removeTeamMember), обсуждения
   (get/create/deleteTeamDiscussion). parseRepoTeam→`parseGHRepoTeam`, `normalizeRepoTeamPermission`
   перенесены как private.
4. **Модели → `model/`**: `GHUser.kt` (GHUser, GHUserProfile, GHContributionDay, GHEmailEntry,
   GHUserKeyEntry, GHSocialAccountEntry, GHFollowerEntry, GHBlockedEntry), `GHOrg.kt` (GHOrg,
   GHOrgMembership, GHAuditLogEntry, GHScimUsersPage, GHScimUser, GHSamlAuthorization),
   `GHTeam.kt` (GHRepoTeam, GHOrgTeam, GHTeamDiscussion).
5. **Ядро: `PREFS`/`KEY_USER` помечены `internal`** (были private) — кэш текущего юзера читают и
   оставшиеся в core функции (commit-flow), и вынесенные user-функции. `request`/`encPath`/
   `parseNextPage` уже internal. Доменные файлы в пакете `data.github` видят оставшиеся в core
   `GHUserLite`/`GHCollaborator` без импорта (их сознательно НЕ выносили — широко шарятся,
   см. прецедент Issues).
6. **Сознательно НЕ тащили** (как Search оставил searchRepos): активность getUserReceived/
   PublicEvents (домен Events), getUserPackages/getOrgPackages (Packages), collaborators
   (репо-доступ), validateToken/getCopilotToken/OAuth-app/device-flow (Auth), getCommunityProfile
   (Repos/Security). Их модели (GHOAuthTokenInfo, GHDeviceCode/TokenResult, GHCollaborator,
   GHUserLite) оставлены в core.
7. **Потребители** (10 экранов, compiler-driven): импорты моделей переключены на `.model`
   (Profile, EnterpriseAdmin, AdvancedSearch, Teams, Packages, Settings, Explore — 22 импорта);
   Home (wildcard, без явного импорта) — добавлен `import …model.GHUser`; четырём экранам без
   wildcard, зовущим перенесённые функции (Profile, EnterpriseAdmin, Teams, Packages), добавлен
   `import …github.*`. В core ссылок на вынесенные модели не осталось.
8. **Найден предсуществующий баг (НЕ чинил — это рефактор):** в getTeamMembers/addTeamMember/
   removeTeamMember/getTeamDiscussions/createTeamDiscussion URL собирается через `${'$'}encodedOrg`
   — это ЛИТЕРАЛЬНЫЙ `$encodedOrg` в пути (vals encoded* по факту не подставляются). Сохранено
   как было, помечено в шапке `GitHubManager+Teams.kt` для отдельного фикса.
9. **Чистая сборка `./gradlew clean compileDebugKotlin` — BUILD SUCCESSFUL (58s), exit 0.**
   Поведение идентично (рефактор, сигнатуры не менялись).

### Итог (13 доменов, 3 файла Users/Orgs/Teams) ✅
Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos, Workflows, Actions, Projects,
Issues, PullRequests, **Users, Orgs, Teams**. `GitHubManager.kt`: 9008 → 3984. Следующие крупные
домены: Commits+Branches+Tags, Discussions, Reactions, Events, Packages, Collaborators, Auth.

## РЕАЛИЗОВАНО: домен Commits+Branches ✅

Нарезан по эталону Releases в ДВА файла (порцелайн), один коммит. `GitHubManager.kt`: 3984 → 3494
строк (−490). Вынесено 22 функции + чистые `parseGHX`-парсеры.

1. **`GitHubManager+Commits.kt`** (227 строк, 10 функций) — коммиты (getCommits, getFileCommits),
   детали/diff (getCommitDiff), сравнение (compareCommits), commit-статусы (getCommitStatuses,
   createCommitStatus), commit-комментарии (get/create/update/deleteCommitComment). Парсинг сведён
   к `parseGHCommit` (единый для getCommits/getFileCommits), `parseDiffFiles` (общий для diff/compare),
   `parseGHCommitStatus`. compareCommits строит GHCommit иначе (sha не обрезан, author-логика) —
   оставлен инлайн. commit-comments тоже инлайн (своё поле-маппинг: originalLine=position, inReplyToId=null).
2. **`GitHubManager+Branches.kt`** (216 строк, 12 функций) — ветки (getBranches, getBranchHeadSha,
   create/deleteBranch, merge/renameBranch) и защита веток (getBranchProtection, required-signatures
   get/enable/disable, update/deleteBranchProtection). Парсер `parseGHBranchProtection`.
3. **Модели → `model/`**: `GHCommit.kt` (GHCommit, GHCommitDetail, GHDiffFile, GHCompareResult,
   GHCommitStatus), `GHBranchProtection.kt` (GHBranchProtection, GHRequiredStatusChecks,
   GHRequiredPRReviews, GHBranchRestrictions). GHBlameRange (blame) и GHCollaborator оставлены в core.
4. **Ядро не трогали** — `request`/`parseNextPage`/`encPath`/`repoPath`/`refQuery` уже internal.
5. **Сознательно НЕ тащили** (отдельные домены): blame (getFileBlame), git-data объекты
   (getGitRef/createGitRef/getGitTree/createGitTree/getGitBlob/createGitBlob/getGitTag/createGitTag/
   getGitCommit/createGitCommit), file-contents/upload/commitWorkspaceChanges → Contents/GitData;
   collaborators → отдельный домен. Ветки берут git-refs из core (тот же пакет, без импорта).
6. **Найден предсуществующий баг (НЕ чинил — рефактор):** в createCommitStatus тело пишет
   `put("context", context)`, где `context` — android `Context`, а не параметр `statusContext`
   (он игнорируется). Сохранено как было, помечено в шапке `GitHubManager+Commits.kt`.
7. **Потребители** (compiler-driven): импорты моделей → `.model` (CodeEditor, Compare, Releases, Diff);
   wildcard-экранам без явного импорта добавлены `.model`-импорты (RepoModule: GHCommit/CommitDetail/
   CommitStatus; BranchProtection: GHBranchProtection/RequiredStatusChecks/RequiredPRReviews);
   трём экранам без wildcard, зовущим перенесённые функции (CodeEditor, Compare, Diff), добавлен
   `import …github.*`.
8. **Чистая сборка `./gradlew clean compileDebugKotlin` — BUILD SUCCESSFUL (43s), exit 0.**
   Поведение идентично (рефактор, сигнатуры не менялись).

### Итог (15 доменов) ✅
Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos, Workflows, Actions, Projects,
Issues, PullRequests, Users, Orgs, Teams, **Commits, Branches**. `GitHubManager.kt`: 9008 → 3494.
Следующие домены: GitData (refs/trees/blobs/git-objects)+Contents, Discussions, Reactions, Events,
Packages, Collaborators, Auth, Repo-settings/Autolinks/LFS/Codespaces, Diagnostics/RateLimit.

## РЕАЛИЗОВАНО: домен GitData ✅

Низкоуровневый Git Data API. `GitHubManager.kt`: 3494 → 3078 строк (−416). Вынесено 13 функций
+ 4 чистых парсера в один файл.

1. **`GitHubManager+GitData.kt`** (~330 строк, 13 функций) — refs (getGitRef, getMatchingGitRefs,
   createGitRef, updateGitRef, deleteGitRef), trees (getGitTree, createGitTree), blobs (getGitBlob,
   createGitBlob), tag-объекты (getGitTag, createGitTag), commit-объекты (getGitCommit,
   createGitCommit). Inline-парсинг сведён к `parseGitRef`/`parseGitTree`/`parseGitTagDetail`/
   `parseGitCommit`. createGitCommit/updateGitRef сохраняют PGP-подпись (PgpKeyManager) и reflog
   (LocalTimeTravelManager) как было.
2. **Модели → `model/GHGitObjects.kt`**: GHGitRef, GHGitTree, GHGitTreeItem, GHGitBlob,
   GHGitTagDetail, GHGitCommit (только потребитель — GitHubRepoModule).
3. **Ядро не трогали** — `request` уже internal; PREFS/KEY_USER (PGP-подпись) уже internal.
4. **Contents оставлены в core** (getFileContent/upload*/commitWorkspaceChanges/clone/uploadProjectFolder)
   — отдельный домен следующим; они зовут эти GitData-функции из того же пакета без импорта.
5. **Потребитель** GitHubRepoModule (wildcard) — добавлены 5 явных `.model`-импортов гит-объектов.
6. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (43s), exit 0.**

### Итог (16 доменов) ✅
… + **GitData**. `GitHubManager.kt`: 9008 → 3078.

## РЕАЛИЗОВАНО: домен Contents ✅

Высокоуровневые операции с файлами/деревом. `GitHubManager.kt`: 3078 → 2571 строк (−507).
Вынесено 13 функций (12 extension + 1 private collectFiles) в один файл.

1. **`GitHubManager+Contents.kt`** (~430 строк) — getFileContent, getFileBlame (GraphQL),
   cloneRepo (zipball), uploadFile/uploadFileWithResult/uploadFileFromPath/uploadMultipleFiles,
   commitWorkspaceChanges (+PGP-подпись), deleteFile, downloadFile, uploadDirectory/collectFiles,
   uploadProjectFolder (Contents API seed + Git Data pipeline). TAG → локальный CONTENTS_TAG.
2. **Модели → `model/GHContent.kt`** (дополнен): GHBlameRange, GHFileSaveResult.
3. **Законное исключение**: прямой `openConnection()` в cloneRepo/downloadFile (бинарные стримы
   zip/raw) — текстовое ядро `request()` их не обслуживает; помечено в шапке файла.
4. **Зависимость от GitData**: uploadProjectFolder/uploadMultipleFiles зовут getGitRef/createGitBlob/
   createGitCommit/updateGitRef из домена GitData — тот же пакет, без импорта. PREFS/KEY_USER (PGP)
   и refQuery/repoPath/encPath/graphql — уже internal в ядре.
5. **Потребители** (5 экранов, все wildcard — функции покрыты): GitHubRepoModule — добавлен
   `import …model.GHBlameRange`. GHFileSaveResult кросс-файлово не используется (вывод типа).
6. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (41s), exit 0.**

### Итог (17 доменов) ✅
… + GitData, **Contents**. `GitHubManager.kt`: 9008 → 2571 (−71%).

## РЕАЛИЗОВАНО: домен Reactions ✅

`GitHubManager.kt`: 2571 → 2486 строк (−85). 9 функций + parseGHReaction в один файл.

1. **`GitHubManager+Reactions.kt`** (~95 строк) — реакции на issues (get/add/delete),
   на комментарии issues (get/add/delete), на комментарии ревью PR (get/add/delete).
   Inline-парсинг → `parseGHReaction`. Удаление у всех видов — общий DELETE /reactions/{id}.
2. **Модель → `model/GHReaction.kt`** (GHReaction). Потребитель — GitHubRepoModule (wildcard,
   добавлен `import …model.GHReaction`).
3. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (42s), exit 0.**

### Итог (18 доменов) ✅ — `GitHubManager.kt`: 9008 → 2486.

## РЕАЛИЗОВАНО: домен Discussions ✅

Целиком GraphQL. `GitHubManager.kt`: 2486 → 2144 строк (−342). 12 функций + 3 приватных
helper'а/парсера в один файл.

1. **`GitHubManager+Discussions.kt`** (~320 строк) — getDiscussions, getDiscussionCategories,
   getDiscussionDetail, create/update/deleteDiscussion, getDiscussionComments, addDiscussionComment,
   mark/unmarkDiscussionCommentAsAnswer, add/removeDiscussionUpvote. Приватные getRepositoryNodeId
   (резолв node-id репо для createDiscussion), parseDiscussionNodes/parseDiscussion перенесены сюда.
   GraphQL-литералы `${'$'}` сохранены как есть (корректный `$` GraphQL-переменных).
2. **Модели → `model/GHDiscussion.kt`**: GHDiscussion, GHDiscussionCategory. Комментарии обсуждений
   возвращают GHComment (домен Issues) — импорт из `.model`.
3. **Ядро**: из `GitHubManager.kt` удалён ставший лишним `import …model.GHComment` (его держал только
   getDiscussionComments). `graphql` уже internal.
4. **Потребитель** GitHubDiscussionsModule (без wildcard): импорты моделей → `.model`, добавлен
   `import …github.*` для перенесённых функций.
5. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (39s), exit 0.**

### Итог (19 доменов) ✅ — `GitHubManager.kt`: 9008 → 2144.

## РЕАЛИЗОВАНО: домен Packages ✅

`GitHubManager.kt`: 2144 → 2002 строк (−141). 8 функций + helper'ы/парсеры в один файл.

1. **`GitHubManager+Packages.kt`** (~150 строк) — getUserPackages/getOrgPackages (с раскруткой
   packageType="all" по githubPackageTypes), getPackage, deletePackage, getPackageVersions,
   deletePackageVersion, restorePackage, restorePackageVersion. Приватные packageOwnerPath,
   githubPackageTypes (val), parsePackages/parsePackage/parsePackageVersion перенесены сюда.
2. **Модели → `model/GHPackage.kt`**: GHPackage, GHPackageVersion. Потребитель — GitHubPackagesModule
   (wildcard; импорты моделей → `.model`).
3. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (38s), exit 0.**

### Итог (20 доменов) ✅ — `GitHubManager.kt`: 9008 → 2002 (−78%).

## РЕАЛИЗОВАНО: домен Collaborators ✅

`GitHubManager.kt`: 2002 → 1953 строк (−49). 4 функции + parseGHCollaborator в один файл.

1. **`GitHubManager+Collaborators.kt`** (~55 строк) — getCollaborators, addCollaborator,
   removeCollaborator, updateCollaboratorPermission. Inline-парсинг (role из permissions) →
   `parseGHCollaborator`.
2. **Модель → `model/GHCollaborator.kt`**. Шарится: getTeamMembers (домен Teams) → в Teams-файл
   добавлен `import …model.GHCollaborator` (раньше брал из core того же пакета).
3. **Потребители**: GitHubCollaboratorsModule (без wildcard) — импорт модели → `.model` + добавлен
   `import …github.*`; GitHubTeamsModule (wildcard) — импорт модели → `.model`.
4. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (39s), exit 0.**

### Итог (21 домен) ✅ — `GitHubManager.kt`: 9008 → 1953.

## РЕАЛИЗОВАНО: домен Security ✅

Самый крупный из оставшихся, нарезан в ДВА файла (rulesets отдельно от alerts/advisories),
один коммит. `GitHubManager.kt`: 1953 → 1346 строк (−607). Вынесено 29 функций + парсеры.

1. **`GitHubManager+Rulesets.kt`** (~230 строк, 7 функций) — repository rulesets (getRulesets,
   getRulesetDetail, create/update/deleteRuleset) и rule-suites (getRuleSuites, getRuleSuite).
   Хелперы buildRulesetPayload/parseRulesetDetail/parseRuleSuite/parseRulesetRules/
   parseRulesetBypassActors перенесены как private.
2. **`GitHubManager+Security.kt`** (~310 строк, 22 функции) — alerts (dependabot/code-scanning/
   secret-scanning, get-list+get-one ×3), repository security advisories (CRUD), community profile,
   security settings (get + setAutomatedSecurityFixes/setVulnerabilityAlerts/
   setPrivateVulnerabilityReporting). Парсеры parseDependabotAlert/parseCodeScanningAlert/
   parseSecretScanningAlert/parseRepositorySecurityAdvisory/parseCommunityFiles/
   parseSecurityAdvisoryVulnerabilities/parseEnabledFlag/parsePausedFlag перенесены как private.
3. **Модели → `model/`**: `GHRuleset.kt` (GHRuleset, GHRulesetDetail, GHRulesetRule,
   GHRulesetBypassActor, GHRuleSuite), `GHSecurity.kt` (GHDependabotAlert, GHCodeScanningAlert,
   GHSecretScanningAlert, GHRepositorySecurityAdvisory, GHAdvisoryVulnerability,
   GHRepositorySecuritySettings, GHCommunityProfile, GHCommunityProfileFile).
4. **`parseStringArray`** — общий генерик-хелпер (JSONArray→List<String>): нужен core
   (parseAppInstallation) И обоим Security-файлам. Оставлен в core как private member; в каждом из
   двух доменных файлов — private-дубль (3-строчный генерик, не доменная логика). Помечено в шапках.
5. **Потребители** (GitHubSecurityModule, GitHubRepoSettingsModule — оба wildcard): импорты моделей
   переключены на `.model` (12 импортов). Функции покрыты wildcard.
6. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (49s), exit 0.**

### Итог (23 домена, 2 файла Security) ✅ — `GitHubManager.kt`: 9008 → 1346 (−85%).

## РЕАЛИЗОВАНО: домен Auth ✅

`GitHubManager.kt`: 1346 → 1228 строк (−118). 6 функций + parseOAuthTokenInfo в один файл.

1. **`GitHubManager+Auth.kt`** (~110 строк) — OAuth-app токены (check/reset/deleteOAuthAppToken,
   deleteOAuthAppGrant) и device-flow (initiateDeviceFlow, pollDeviceToken). Запросы — через
   ядровый `requestBasic` (помечен `internal`, был private; других потребителей в core не осталось).
   parseOAuthTokenInfo перенесён как private.
2. **Модели → `model/GHAuth.kt`**: GHOAuthTokenInfo, GHDeviceCode, GHDeviceTokenResult.
3. **Оставлено в core**: validateToken/getCopilotToken — завязаны на вложенный
   GitHubManager.TokenValidation и базовое токен-хранилище (часть auth-ядра).
4. **Потребители** (GitHubSettingsModule, GitHubEnterpriseAdminModule — wildcard): импорты моделей → `.model`.
5. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (38s), exit 0.**

### Итог (24 домена) ✅ — `GitHubManager.kt`: 9008 → 1228.

## РЕАЛИЗОВАНО: домен Apps ✅

`GitHubManager.kt`: 1228 → 1092 строк (−136). 4 функции + 3 парсера в один файл.

1. **`GitHubManager+Apps.kt`** (~130 строк) — getAppInstallations, getAppInstallationRepositories,
   add/removeRepositoryToAppInstallation. parseAppInstallation + его хелперы parseStringArray/
   parseStringMap перенесены сюда (в core они больше не нужны — это был последний потребитель).
2. **Модели → `model/GHApps.kt`**: GHAppInstallation, GHAppInstallationsPage, GHAppInstallationReposPage.
3. **Оставлены в core (shared internal)**: parseRepo (getAppInstallationRepositories зовёт его —
   member, доступен), apiErrorMessage. `parseStringArray` теперь нет в core вообще.
4. **Потребитель** GitHubAppsModule (без wildcard): импорт модели → `.model`, добавлен `import …github.*`.
5. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (39s), exit 0.**

### Итог (25 доменов) ✅ — `GitHubManager.kt`: 9008 → 1092 (−88%).

## РЕАЛИЗОВАНО: домен Diagnostics ✅

`GitHubManager.kt`: 1092 → 783 строк (−309). 4 функции + 3 парсера в один файл.

1. **`GitHubManager+Diagnostics.kt`** (~300 строк) — getRateLimitSummaryNative, getRateLimitGraphQL,
   getGitHubStatus (статус github.com), runApiDiagnostics (комплексная проверка токена/доступов
   с локальными addResult/addSkip). Парсеры parseLogin/diagnosticStatus/parseRateLimitSummary
   перенесены как private. TAG → локальный DIAG_TAG. ApiResult (вложенный в core) импортирован.
2. **Модели → `model/GHDiagnostics.kt`**: GHApiDiagnostics, GHApiRateSummary, GHRateLimitGraphQL,
   GHApiDiagnosticCheck, GHStatusComponent, GHStatusSummary.
3. **Законное исключение**: openConnection в getGitHubStatus — githubstatus.com внешний сервис,
   не GitHub API. apiErrorMessage остаётся в core (internal, шарится).
4. **Потребители**: GitHubDiagnosticsModule (импорты моделей → `.model` + wildcard),
   GitHubActionsTroubleshootModule (+wildcard для getRateLimitSummaryNative).
5. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (45s), exit 0.**

### Итог (26 доменов) ✅ — `GitHubManager.kt`: 9008 → 783 (−91%).

## РЕАЛИЗОВАНО: домен RepoMeta ✅

`GitHubManager.kt`: 783 → 647 строк (−136). 10 функций в один файл.

1. **`GitHubManager+RepoMeta.kt`** (~135 строк) — getReadme, getGitHubMeta (/meta), renderMarkdown,
   getLanguages, getEmojis, getGitignoreTemplates, getGitignoreTemplate, getLicenses, getLicense,
   getContributors.
2. **Модели → `model/GHRepoMeta2.kt`**: GHMeta, GHLicense, GHContributor (GHLicenseDetail уже в
   model/GHLicenseDetail.kt домена Repos).
3. **Потребители** (Diagnostics, Explore, RepoCreate, RepoModule, Markdown, RepoSettingsScreen):
   импорты моделей → `.model`, wildcard где нужно.
4. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (45s), exit 0.**

### Итог (27 доменов) ✅ — `GitHubManager.kt`: 9008 → 647 (−93%).

## РЕАЛИЗОВАНО: домен Events ✅

`GitHubManager.kt`: 647 → 601 строк (−46). 2 функции + parseGHRepoEvent в один файл.

1. **`GitHubManager+Events.kt`** (~50 строк) — getUserReceivedEvents, getUserPublicEvents.
   Inline-парсинг (был идентичен) → `parseGHRepoEvent`. GHRepoEvent — модель домена Repos.
2. **Ядро**: удалён ставший лишним `import …model.GHRepoEvent` (Events был последним потребителем).
3. **Потребители** GitHubHomeModule, GitHubProfileModule (wildcard) — функции покрыты.
4. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (46s), exit 0.**

### Итог (28 доменов) ✅ — `GitHubManager.kt`: 9008 → 601.

## РЕАЛИЗОВАНО: домен RepoFeatures ✅

`GitHubManager.kt`: 601 → 503 строк (−97). 7 функций (мелкие фиче-эндпоинты) в один файл.

1. **`GitHubManager+RepoFeatures.kt`** (~95 строк) — autolinks (get/create/delete), codespaces
   (get/delete), Git LFS (enable/disable). Три мелких эндпоинт-группы, не тянущие на отдельный
   god-файл, объединены.
2. **Модели → `model/GHRepoFeatures.kt`**: GHAutolink, GHCodespace.
3. **Потребители** GitHubRepoSettingsModule, GitHubActionsModule (wildcard) — импорты моделей → `.model`.
4. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL (45s), exit 0.**

### Итог (29 доменов) ✅ — `GitHubManager.kt`: 9008 → 503.

## РЕАЛИЗОВАНО: финальные одиночки (Home + Repos-довесок) ✅

`GitHubManager.kt`: 503 → 420 строк (−83). Хвостовые функции разнесены по местам.

1. **searchRepos, isStarred** дописаны в существующий `GitHubManager+Repos.kt` (базовые Repos-эндпоинты,
   зовут ядровый `parseRepo`).
2. **getQuickGlanceStats** → новый `GitHubManager+Home.kt` (дашборд-агрегат), модель QuickGlanceStats
   → `model/GHHome.kt`.
3. Потребитель GitHubHomeModule — добавлен `import …model.QuickGlanceStats`.
4. **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL, exit 0.**

## ✅ ДЕКОМПОЗИЦИЯ DATA-СЛОЯ ЗАВЕРШЕНА

`GitHubManager.kt`: **9008 → 420 строк (−95%)**. Из god-файла на 452 функции остался чистый
сетевой слой (~420 строк): управление API-URL, rate-limit/etag, валидация токена + делегаты в
GitHubAuth, ядро `request`/`requestBasic`/`graphql`/`repoPath`/`encPath`/`parseNextPage`/`refQuery`/
`recordApiError`/`responseHeaders`, шаренные `parseRepo`/`apiErrorMessage`, вложенные ApiResult/
TokenValidation. Всё остальное — в 33 доменных файлах `GitHubManager+*.kt` и 35 файлах моделей `model/`.

Домены (порядок нарезки): Releases, Gists, Webhooks, Notifications, Search, Secrets, Repos,
Workflows, Actions, Projects, Issues, PullRequests, Users, Orgs, Teams, Commits, Branches, GitData,
Contents, Reactions, Discussions, Packages, Collaborators, Security (Rulesets+Alerts), Auth, Apps,
Diagnostics, RepoMeta, Events, RepoFeatures, Home.

Конвенция выдержана: extension-функции объекта, `internal`-ядро, модели в `model/`, парсинг через
`parseGHX`, сигнатуры вызовов не менялись, после каждого домена — зелёная сборка. Два предсуществующих
бага задокументированы в TODA.md (team-URL, createCommitStatus) — НЕ чинились (рефактор).

---

# UI-декомпозиция: RepoModule (Фаза 1 — разнесение по файлам)

Эталонный UI god-экран `ui/screens/GitHubRepoModule.kt` (8181 строк, 101 @Composable). Фаза 1 —
чистое разнесение кластеров по соседним файлам того же пакета `gs.git.vps.ui.screens`, поведение
НЕ меняется (только перемещение кода). Ключевой нюанс: `private` top-level в Kotlin file-scoped —
кластер переносится со всеми private-хелперами; шаренные с остающимся кодом → `internal`.
Билд после каждого файла. Фаза 2 (ViewModel/стейт) — отдельно потом.

## Файл 1: RepoReadmeRenderer.kt ✅

Вынесен весь README-движок (1964 строки): загрузка/парсинг markdown+HTML в блоки, рендер,
HTML-документ-вьюха, image-loader, резолв ссылок + типы ReadmeRenderBlock/ReadmeInlineSegment/
ReadmeFetchResult + README_*-константы. `GitHubRepoModule.kt`: 8181 → 6372 строк (−1809).

- Символы, зовущиеся из остающегося RepoDetailScreen, помечены `internal`: ReadmeTab,
  ReadmeHtmlDocument, fetchReadmeForRender, resolveReadmeLink, openReadmeUrl, README_*-константы
  (GitHubMarkdownDocument/parseReadmeBlocks/ReadmeBlockView/ReadmeRenderBlock/ReadmeFetchResult
  уже были internal — используются и другими экранами).
- Импорты скопированы из god-файла целиком (неиспользуемые — лишь warning).
- **Чистая сборка `clean compileDebugKotlin` — BUILD SUCCESSFUL, exit 0.** Экран не менялся.

## Файл 2: RepoFileScreens.kt ✅

CommitDiffScreen + BlameViewScreen + FileHistoryScreen (380 строк). `GitHubRepoModule.kt`:
6372 → 6143. Blame/FileHistory (зовутся из RepoDetailScreen) помечены `internal`.
Урок: диапазон выноса composable начинать с `@Composable`-строки и заканчивать на `}` функции,
не захватывая аннотацию следующей. Чистая сборка — BUILD SUCCESSFUL.

## Файл 3: RepoTelemetryTab.kt ✅
TelemetryTab + TelemetryGraph + CompactField + GrepResult (710 строк). main: 6143 → 5583.
Флипов не нужно (TelemetryTab уже internal, остальное локально). BUILD SUCCESSFUL.

## Файл 4: RepoTimeTravelTab.kt ✅
TimeTravelTab (571 строк). main: 5583 → 5162. BUILD SUCCESSFUL.

## Файл 5: RepoGitDataScreens.kt ✅
RepoInsightsScreen + GitDataToolsScreen + Git*-карточки (1038 строк). main: 5162 → 4275.
Флипы→internal: RepoInsightsScreen, GitDataToolsScreen, GitDataKv (шарится с IssueEventDetailDialog),
RepoInsightsTab (enum, использовался блоком). Урок: проверять и enum/class/val, используемые блоком,
а не только private fun. BUILD SUCCESSFUL.

## Файл 6: RepoPullsScreens.kt ✅
PullsTab + PullRequestDetailScreen + Pull*-карточки/диалоги/AI-summary (1310 строк, 2 диапазона).
main: 4275 → 3114. Флипы→internal: PullRequestDetailScreen, PullBadge (шарится). BUILD SUCCESSFUL.

## Файл 7: RepoIssuesScreens.kt ✅
IssueEventsScreen + IssueDetailScreen + Issue*-карточки/диалоги/реакции/timeline (1251 строк,
2 диапазона). main: 3114 → 2013. Флип→internal: IssueEventsScreen. BUILD SUCCESSFUL.

## Файл 8: RepoTabs.kt ✅
FilesTab, CommitsTab (+ git-граф), IssuesTab, ReleasesTab, fileIcon/fileTint (700 строк, 2 диапазона).
main: 2013 → 1462. Флип→internal: FilesTreeRow (data class, используется FilesTab).

## ✅ ФАЗА 1 RepoModule ЗАВЕРШЕНА

`GitHubRepoModule.kt`: **8181 → 1462 строки (−82%)**. Вынесено 8 файлов:
RepoReadmeRenderer (1964), RepoPullsScreens (1310), RepoIssuesScreens (1251), RepoGitDataScreens
(1038), RepoTabs (700), RepoTelemetryTab (710), RepoTimeTravelTab (571), RepoFileScreens (380).
В god-файле остался корень RepoDetailScreen (роутер табов) + GitHubAdminRequiredScreen + общие
мелочи (Chip, FilesTreeRow, RepoInsightsTab, HeadingItem/ToCInterface, Color.toHex, константы).

Чистое перемещение, поведение не менялось; каждый файл — отдельный коммит с зелёным clean
compileDebugKotlin. Shared-символы помечены internal. Фаза 2 (RepoDetailViewModel/стейт) — отдельно.

## РЕШЕНИЕ: Фаза 2 RepoModule отложена (НЕ дорезать автоматически)

Фаза 1 (разнесение по файлам) завершена и достаточна: god-экран 8181 → 1462 строки, переиспользуемые
куски вынесены. Оставшийся `RepoDetailScreen` (~1180 стр) держит свой стейт и inline-вызовы
GitHubManager — это «толстый экран», не god-файл.

**Фазу 2 (RepoDetailViewModel/state-holder, UDF, вынос сети из Composable) делаем ТОЛЬКО вместе со
средним редизайном экрана репозитория** — решение пользователя. Причина: подъём стейта рискован для
рантайма Compose (рекомпозиция/lifecycle/side-effects), верифицируется только вживую на устройстве;
во время редизайна UI всё равно тестируется руками, поэтому риск окупается. Отдельно «ради строк» —
плохой risk/reward.

→ Будущим сессиям: НЕ начинать Фазу 2 RepoModule по своей инициативе. Только по явному запросу
в контексте редизайна.

---

# UI-декомпозиция: ActionsModule (Фаза 1 — разнесение по файлам)

`ui/screens/GitHubActionsModule.kt` (5966 строк, 69 @Composable). Та же конвенция, что у RepoModule:
чистое перемещение кластеров по соседним файлам, shared private→internal, билд после каждого.

## Файл 1: ActionsDeploymentsPanels.kt ✅
DeploymentsPanel + EnvironmentsPanel + CodespacesPanel (473 строки). main: 5966 → 5668.
Shared-хелперы панелей → internal: ActionsPanelHeader, EmptyActionsText, LoadingActionsText.
BUILD SUCCESSFUL.

## Файл 2: ActionsRunHelpers.kt ✅
Хелперы рана (1120 строк): статусы/иконки/цвета job/step/check, длительности, обработка+экспорт
логов, диагностика падений, форматтеры артефактов, мелкие фильтр-composable. main: 5668 → 4724.
Двусторонний flip→internal: ~40 хелперов (используются main) + типы/константы из main, используемые
блоком (JobListItem, MatrixJobGroup, JobLogMeta, ArtifactGroup, FailureEvidence, ACTIONS_JOB_LOG_*,
isJobActive, displayRunStatus). Конвенция: после выноса проверять ОБЕ стороны зависимостей.
BUILD SUCCESSFUL.

## Файл 3: ActionsRunDetailScreen.kt ✅
WorkflowRunDetailScreen + секции (summary/jobs/pipeline/checks/artifacts), job/step-карточки,
лог-вьюер, диалоги (1992 строки). main: 4724 → 2908. Двусторонний flip→internal отработал автоматом.
BUILD SUCCESSFUL.

## Файл 4: ActionsConfigPanels.kt ✅
Config-панели (артефакты/кэши/variables/secrets/runners/settings) + инфраструктура (1099 строк).
main: 2908 → 1984. Двусторонний flip→internal. BUILD SUCCESSFUL.

## Файл 5: ActionsRunDispatch.kt ✅
DynamicDispatchInputs + dispatch-поля + ModernRunCard (карточка запуска) + сохранение инпутов
(528 строк). main: 1984 → 1631. Run-status хелперы оставлены в core (шарятся). BUILD SUCCESSFUL.
