package gs.git.vps.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHAsset
import java.io.File

class ReleaseDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "release_download_channel"
        const val NOTIFICATION_ID = 9001
        
        const val KEY_ASSET_NAME = "asset_name"
        const val KEY_ASSET_SIZE = "asset_size"
        const val KEY_ASSET_URL = "asset_url"
        const val KEY_ASSET_ID = "asset_id"
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val assetName = inputData.getString(KEY_ASSET_NAME) ?: return Result.failure()
        val assetSize = inputData.getLong(KEY_ASSET_SIZE, 0L)
        val assetUrl = inputData.getString(KEY_ASSET_URL) ?: ""
        val assetId = inputData.getLong(KEY_ASSET_ID, 0L)

        val asset = GHAsset(
            name = assetName,
            size = assetSize,
            downloadUrl = assetUrl,
            downloadCount = 0,
            id = assetId
        )

        createNotificationChannel()

        // Android 12+ запрещает старт FGS из фона (ForegroundServiceStartNotAllowedException) —
        // в этом случае качаем обычным воркером с теми же notify()-уведомлениями, не падаем.
        try {
            setForeground(createForegroundInfo(assetName, 0))
        } catch (e: Exception) {
            Log.w("ReleaseDownload", "setForeground rejected, continuing as background worker: ${e.message}")
        }

        val destFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GlassFiles_Git/$assetName"
        )

        val success = GitHubManager.downloadReleaseAssetWithProgress(
            context = applicationContext,
            asset = asset,
            destFile = destFile
        ) { bytesDownloaded, totalBytes ->
            val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
            notificationManager.notify(
                NOTIFICATION_ID,
                createNotification(assetName, progress)
            )
        }

        if (success) {
            showCompletedNotification(assetName, destFile.absolutePath)
            return Result.success(workDataOf("dest_path" to destFile.absolutePath))
        } else {
            showFailedNotification(assetName)
            return Result.failure()
        }
    }

    private fun createForegroundInfo(assetName: String, progress: Int): ForegroundInfo {
        val notification = createNotification(assetName, progress)
        // Android 14+ требует явный тип FGS, совпадающий с foregroundServiceType из манифеста
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(assetName: String, progress: Int): Notification {
        val title = "Downloading $assetName"
        val progressText = "$progress%"
        
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun showCompletedNotification(assetName: String, path: String) {
        val title = "Download completed"
        val text = "Saved $assetName to Downloads"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showFailedNotification(assetName: String) {
        val title = "Download failed"
        val text = "Could not download $assetName"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Release Downloads"
            val descriptionText = "Shows progress of release asset downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
