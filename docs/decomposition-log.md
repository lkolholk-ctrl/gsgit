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
