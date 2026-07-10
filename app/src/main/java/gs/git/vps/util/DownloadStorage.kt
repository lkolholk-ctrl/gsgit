package gs.git.vps.util

import android.content.Context
import android.os.Environment
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
}
