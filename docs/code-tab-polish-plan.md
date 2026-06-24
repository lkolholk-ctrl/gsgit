# Code tab — полиш (аудит + план)

> Аудит после завершения UI-плана (P1/P2/P3 + канон + lite-editor). Дисциплина: один блок → сборка →
> проверка. Файлы: CodeBrowser/CodeChangesPanel/CodeCommitSheet/CodeTabShell, lite-editor в
> GitHubCodeEditorModule, проводка в GitHubRepoModule, CodeDraftStore, GitHubManager+CodeCommit.

## 🔴 BUG (потеря данных)
- **B1. `discard` стирал весь черновик без подтверждения** (SC-бар → onDiscardAll → codeDraft.clear()).
- **B2. Редактор не авто-сохранял в фон** — onSaveDraft только на back/save; смерть процесса при
  правке → потеря. Спека: «авто-сейв на уход в фон».

## 🟠 GAP (vs спека)
- **G1.** Нет фильтра файлов в Code-браузере (спека: «Фильтр файлов»).
- **G2.** Конфликт-коммит = голый тост; спека хочет перефетч + предупреждение + ретрай (ретрай уже
  перефетчит ref внутри commitCodeDraft).

## 🟡 SMELL / полиш
- **S1.** Commit-sheet — AlertDialog, по плану хотели bottom-sheet.
- **S2.** Смена ветки при открытом редакторе → стейл-контент.
- **S3.** onOpenPath строит GHContent с пустыми sha/downloadUrl.
- **S4.** codeRecents только в памяти (теряются при смерти процесса).
- **S5.** Открытие бинарника/картинки в редакторе → мусор (нет гарда по типу/размеру).
- **S6.** CodeDraftStore без лимита (большой черновик раздувает SharedPreferences).
- **S7.** Иконки-кнопки без contentDescription (a11y).
- **S8.** Error-тост показывает сырое тех-сообщение.

## ✅ Уже хорошо
Browser loading/empty/error+retry, editor спиннер, panel empty-state, committing-стейт, канон-стиль.

## План по фазам
- **Фаза 1 — баги/данные:** ✅ B1 (конфирм на discard-all), ✅ B2 (lifecycle авто-сейв редактора).
- **Фаза 2 — спека + ценность:** G1 (фильтр файлов), S5 (гард бинарников), G2 (конфликт-UX).
- **Фаза 3 — полиш:** S1 (commit bottom-sheet), S2/S3/S4/S6/S7/S8, явный «вверх» в крошках.
