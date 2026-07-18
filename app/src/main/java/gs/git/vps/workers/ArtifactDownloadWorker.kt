package gs.git.vps.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.downloadArtifactWithProgress
import gs.git.vps.util.DownloadStorage
import java.io.File

/**
 * Скачивание артефакта Actions в фоне: нотификация с прогрессом и кнопкой
 * Cancel, распаковка zip рядом (с защитой от zip-slip), финальная нотификация
 * с «открыть» через FileProvider. Переживает уход с экрана — работой владеет
 * WorkManager, а не Composable.
 */
class ArtifactDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "artifact_download_channel"
        private const val NOTIFICATION_BASE_ID = 9200

        private const val KEY_OWNER = "owner"
        private const val KEY_REPO = "repo"
        private const val KEY_ARTIFACT_ID = "artifact_id"
        private const val KEY_ARTIFACT_NAME = "artifact_name"
        private const val KEY_SIZE = "size_bytes"
        private const val KEY_UNZIP = "unzip"

        /** Единственная точка запуска. Повторный энкью того же артефакта заменяет прежний. */
        fun enqueue(
            context: Context,
            owner: String,
            repo: String,
            artifactId: Long,
            artifactName: String,
            sizeBytes: Long,
            unzip: Boolean = true,
        ) {
            val request = OneTimeWorkRequestBuilder<ArtifactDownloadWorker>()
                .setInputData(workDataOf(
                    KEY_OWNER to owner,
                    KEY_REPO to repo,
                    KEY_ARTIFACT_ID to artifactId,
                    KEY_ARTIFACT_NAME to artifactName,
                    KEY_SIZE to sizeBytes,
                    KEY_UNZIP to unzip,
                ))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "artifact_download_$artifactId",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val owner = inputData.getString(KEY_OWNER) ?: return Result.failure()
        val repo = inputData.getString(KEY_REPO) ?: return Result.failure()
        val artifactId = inputData.getLong(KEY_ARTIFACT_ID, 0L)
        val name = inputData.getString(KEY_ARTIFACT_NAME) ?: "artifact"
        val size = inputData.getLong(KEY_SIZE, 0L)
        val unzip = inputData.getBoolean(KEY_UNZIP, true)
        val notificationId = NOTIFICATION_BASE_ID + (artifactId % 100_000L).toInt()

        createChannel()
        setForeground(foregroundInfo(notificationId, name, 0))

        val zipName = if (name.endsWith(".zip")) name else "$name.zip"
        val destZip = DownloadStorage.file(applicationContext, zipName)

        var lastShown = -1
        val ok = GitHubManager.downloadArtifactWithProgress(
            context = applicationContext,
            owner = owner,
            repo = repo,
            artifactId = artifactId,
            expectedSize = size,
            destFile = destZip,
        ) { downloaded, total ->
            val progress = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
            if (progress != lastShown) { // не спамим нотификацией на каждые 8 КБ
                lastShown = progress
                notificationManager.notify(notificationId, progressNotification(name, progress))
            }
        }

        if (!ok) {
            notificationManager.notify(notificationId + 1, finalNotification(
                title = "Download failed",
                text = name,
                icon = android.R.drawable.stat_notify_error,
                contentIntent = null,
            ))
            return Result.failure()
        }

        var extractedCount = 0
        if (unzip) {
            val extractDir = File(DownloadStorage.directory(applicationContext), name.removeSuffix(".zip"))
            extractedCount = runCatching { extractZip(destZip, extractDir) }.getOrDefault(0)
        }

        // Публикуем zip в СИСТЕМНЫЕ Загрузки (Download/GsGit) — молча, без пикеров,
        // как это делает браузер. Приватную копию после успеха удаляем.
        val publicUri = DownloadStorage.publishToDownloads(applicationContext, destZip, "application/zip")
        if (publicUri != null) destZip.delete()

        val where = "Downloads/GsGit/${destZip.name}"
        val text = if (extractedCount > 0) {
            "Saved to $where · $extractedCount file(s) extracted"
        } else {
            "Saved to $where"
        }
        notificationManager.notify(notificationId + 1, finalNotification(
            title = "Artifact downloaded",
            text = text,
            icon = android.R.drawable.stat_sys_download_done,
            contentIntent = openIntent(publicUri, destZip),
        ))
        return Result.success(workDataOf("zip_path" to (publicUri?.toString() ?: destZip.absolutePath)))
    }

    /** Распаковка с защитой от zip-slip. Возвращает число извлечённых файлов. */
    private fun extractZip(zipFile: File, outDir: File): Int {
        outDir.mkdirs()
        val safeRoot = outDir.canonicalFile
        var count = 0
        java.util.zip.ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val target = File(safeRoot, entry.name).canonicalFile
                if (!target.path.startsWith(safeRoot.path + File.separator)) {
                    throw java.io.IOException("Unsafe archive entry: ${entry.name}")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                    count++
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return count
    }

    /** Тап по финальной нотификации: открыть опубликованный файл из системных Загрузок. */
    private fun openIntent(publicUri: android.net.Uri?, fallbackZip: File): PendingIntent? = runCatching {
        val uri = publicUri ?: FileProvider.getUriForFile(
            applicationContext, "${applicationContext.packageName}.fileprovider", fallbackZip,
        )
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/zip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        PendingIntent.getActivity(
            applicationContext,
            fallbackZip.hashCode(),
            view,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    private fun foregroundInfo(id: Int, name: String, progress: Int): ForegroundInfo =
        ForegroundInfo(id, progressNotification(name, progress), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    private fun progressNotification(name: String, progress: Int): Notification =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $name")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
            )
            .build()

    private fun finalNotification(title: String, text: String, icon: Int, contentIntent: PendingIntent?): Notification =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Artifact downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }
}
