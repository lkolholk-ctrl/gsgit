# Эталон: UI-экран (Jetpack Compose)

Применяется к `ui/screens/`. Цель — god-экраны (RepoModule 8.1k, ActionsModule 6k, CodeEditor 4.4k)
разбить так, чтобы новый функционал собирался из готовых кусков.

## Чек-лист «идеального» экрана

- [ ] **Экран — тонкий Composable.** Он только собирает под-Composable'ы и прокидывает state/events.
- [ ] **Стейт поднят** в `ViewModel` или state-holder. В Composable нет загрузки данных и бизнес-логики.
- [ ] **UDF:** state идёт вниз, события (`onX`) — вверх через лямбды. Никаких прямых вызовов API из Composable.
- [ ] **Под-Composable'ы вынесены** в отдельные файлы, каждый < ~150-200 строк, по возможности `stateless`.
- [ ] **Диалоги/bottom-sheet'ы — отдельные файлы** (`RepoDeleteDialog.kt`…), не инлайнить в экран.
- [ ] **`@Preview`** на stateless-компоненты (берут данные параметрами, а не из VM).
- [ ] **Переиспользование** — общие куски идут в `ui/components/`, не копипастятся между экранами.
- [ ] **Ноль `openConnection()` / сетевого кода** в файле экрана (см. известные протечки в CLAUDE.md).

## Рецепт разбиения god-экрана

1. Выделить state: что экран реально держит (списки, фильтры, флаги диалогов, загрузка) → в `XxxUiState` + `XxxViewModel`.
2. Перенести вызовы `GitHubManager.*` из Composable в VM (Composable дёргает `viewModel.action()`).
3. Резать UI на секции: header / list item / toolbar / каждый диалог → отдельные `@Composable` в своих файлах.
4. Корневой `XxxScreen(state, onEvent)` — только композиция этих секций.
5. После каждого шага — собрать билд, открыть экран, проверить что ведёт себя как раньше.

## Структура файлов экрана (пример)

```
screens/repo/
  RepoScreen.kt          // тонкий корень
  RepoViewModel.kt       // стейт + вызовы GitHubManager
  RepoUiState.kt
  components/RepoHeader.kt
  components/RepoFileRow.kt
  dialogs/RepoDeleteDialog.kt
  dialogs/RepoRenameDialog.kt
```

## Definition of Done для экрана

Файл корня < ~300 строк, каждый под-Composable < ~200, стейт в VM, ноль сети в Composable,
ключевые stateless-куски имеют `@Preview`, билд зелёный, экран ведёт себя идентично.
