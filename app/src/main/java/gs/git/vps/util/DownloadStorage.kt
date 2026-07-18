package gs.git.vps.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * App-owned download location that remains writable under scoped storage.
 *
 * Public Downloads must be created through MediaStore or the system file
 * picker. Callers that need a user-selected public destination should use the
 * picker; background jobs use this location so they never fail on Android 10+.
 */
object DownloadStorage {
    private const val DIRECTORY_NAME = "GsGit"
    private val unsafeFileChars = Regex("""[\\/:*?"<>|\p{Cntrl}]+""")

    fun directory(context: Context): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(root, DIRECTORY_NAME).apply { mkdirs() }
    }

    fun file(context: Context, displayName: String): File {
        val safeName = displayName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(unsafeFileChars, "_")
            .trim()
            .ifBlank { "download" }
            .take(180)
        return File(directory(context), safeName)
    }

    /**
     * Публикует готовый файл в СИСТЕМНЫЕ Загрузки (Download/GsGit) без диалогов:
     * на API 29+ — через MediaStore (разрешения не нужны), ниже — прямой копией
     * в публичную папку. Возвращает Uri опубликованного файла или null.
     */
    fun publishToDownloads(context: Context, source: File, mimeType: String): Uri? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + DIRECTORY_NAME)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val copied = resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                } != null
                if (copied) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri
                } else {
                    resolver.delete(uri, null, null)
                    null
                }
            } else null
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME)
            dir.mkdirs()
            val dest = File(dir, source.name)
            source.copyTo(dest, overwrite = true)
            Uri.fromFile(dest)
        }
    } catch (_: Exception) {
        null
    }
}
