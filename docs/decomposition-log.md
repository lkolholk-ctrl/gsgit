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
