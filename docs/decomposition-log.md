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
