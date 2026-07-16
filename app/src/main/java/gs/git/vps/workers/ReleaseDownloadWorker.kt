package gs.git.vps.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHAsset
import gs.git.vps.util.DownloadStorage

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
        val notificationId = NOTIFICATION_ID + (assetId % 100_000L).toInt()

        val asset = GHAsset(
            name = assetName,
            size = assetSize,
            downloadUrl = assetUrl,
            downloadCount = 0,
            id = assetId
        )

        createNotificationChannel()

        setForeground(createForegroundInfo(notificationId, assetName, 0))

        val destFile = DownloadStorage.file(applicationContext, assetName)

        val success = GitHubManager.downloadReleaseAssetWithProgress(
            context = applicationContext,
            asset = asset,
            destFile = destFile
        ) { bytesDownloaded, totalBytes ->
            val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
            notificationManager.notify(
                notificationId,
                createNotification(assetName, progress)
            )
        }

        if (success) {
            showCompletedNotification(notificationId, assetName)
            return Result.success(workDataOf("dest_path" to destFile.absolutePath))
        } else {
            showFailedNotification(notificationId, assetName)
            return Result.failure()
        }
    }

    private fun createForegroundInfo(notificationId: Int, assetName: String, progress: Int): ForegroundInfo {
        // WorkManager forwards this value to Service.startForeground(). The
        // two-argument constructor uses type=0 ("none"), which Android rejects
        // for targetSdk 34+ even when dataSync is declared in the manifest.
        return ForegroundInfo(
            notificationId,
            createNotification(assetName, progress),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
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

    private fun showCompletedNotification(notificationId: Int, assetName: String) {
        val title = "Download completed"
        val text = "Saved $assetName to GsGit downloads"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 1, notification)
    }

    private fun showFailedNotification(notificationId: Int, assetName: String) {
        val title = "Download failed"
        val text = "Could not download $assetName"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 2, notification)
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
