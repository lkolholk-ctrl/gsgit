# LMG — скачивания в публичные «Загрузки» (для Fable)

Сейчас загрузки пишутся в приватную `context.filesDir/downloads/<trackId>.<ext>` — не видны в файловом
менеджере, удаляются вместе с апк. Нужно: **в системные «Загрузки», папка `Download/LiquidMusicGlass/`** —
видимые файлы, переживают переустановку.

**Важно (Android 10+ / у юзера 16):** прямой `File(Environment.DIRECTORY_DOWNLOADS)` НЕ работает (scoped
storage; в манифесте `WRITE_EXTERNAL_STORAGE maxSdkVersion=32`). Единственный правильный путь на 13+ —
**MediaStore.Downloads** через `ContentResolver`. localPath в БД станет `content://` URI, а не файловым путём.

## Файлы
- `engine/AudioDownloadManager.kt` — запись (`performDownload`) + `clearAllDownloads`
- `engine/StreamingDataSource.kt` — офлайн-резолв (строки ~112–117)
- `data/local/db/DownloadedTrackEntity.kt` — поле `localPath` (теперь URI)
- `ui/screens/LibraryScreen.kt` — `File(entity.localPath)` (размер, `Uri.fromFile`)

## 1. Запись через MediaStore (`performDownload`)
Вместо финального `File(downloadsDir, "$trackId$ext")`:
```kotlin
val mime = if (ext == ".m4a") "audio/mp4" else if (ext == ".flac") "audio/flac" else "audio/mpeg"
val name = buildString {                         // человекочитаемое имя вместо trackId
    append(track.artistName?.let { "$it - " } ?: "")
    append(track.title.ifBlank { trackId })
    append(ext)
}.replace(Regex("[/\\\\:*?\"<>|]"), "_")
val finalUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)
        put(MediaStore.Downloads.MIME_TYPE, mime)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LiquidMusicGlass")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
    resolver.openOutputStream(uri)!!.use { out -> /* стримим сюда вместо tempFile */ }
    values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    uri
} else {
    // Android ≤ 9: старый путь c WRITE_EXTERNAL_STORAGE
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LiquidMusicGlass").apply { mkdirs() }
    val f = File(dir, name); /* дописать из tempFile */; Uri.fromFile(f)
}
// в БД:
localPath = finalUri.toString()
```
Скачивание веди сразу в `openOutputStream(uri)` (либо в temp в кэше, потом скопировать в uri). `IS_PENDING`
скрывает недокачанный файл от других приложений, снимаем после `flush`.

## 2. Офлайн-резолв (`StreamingDataSource.kt` ~112–117)
Сейчас ищет `File(filesDir, "downloads/$trackId.{mp3,m4a,flac}")`. Заменить на **чтение `localPath` из БД**
(`DownloadedTrackDao.getByTrackId(trackId)`), и если есть — вернуть `Uri.parse(entity.localPath)`.
ExoPlayer умеет `content://` из коробки. Файловую проверку по расширениям убрать.

## 3. Библиотека (`LibraryScreen.kt`)
- `File(entity.localPath).length()` → размер через `contentResolver.openFileDescriptor(Uri.parse(localPath),"r")?.statSize`
  или query `OpenableColumns.SIZE`.
- `Uri.fromFile(File(entity.localPath))` → `Uri.parse(entity.localPath)`.

## 4. `clearAllDownloads`
Удалять по сохранённым URI из БД: `contentResolver.delete(Uri.parse(localPath), null, null)` для каждого,
затем чистить БД. Приватный `filesDir/downloads` и старый листинг public-папки можно оставить как fallback-очистку.

## 5. Обложки
`localCoverPath` можно оставить приватным (обложки в «Загрузках» не нужны) — трогать не обязательно.

## Definition of Done
- Скачанный трек появляется в системных **«Загрузки» → LiquidMusicGlass**, виден в файловом менеджере.
- Офлайн-воспроизведение играет из этого файла, размер в библиотеке верный.
- Файлы переживают переустановку апк; `clearAllDownloads` их удаляет.
- На Android 10+ — без прямых File-путей в public (только MediaStore). Сборка зелёная.

> Разрешения: на 13+ запись в свои же MediaStore.Downloads разрешения не требует. Чтение чужих аудио —
> `READ_MEDIA_AUDIO` (уже есть). `WRITE_EXTERNAL_STORAGE maxSdkVersion=32` оставить для legacy-ветки.
