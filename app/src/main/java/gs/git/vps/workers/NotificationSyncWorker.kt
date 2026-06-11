package gs.git.vps.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import gs.git.vps.MainActivity
import gs.git.vps.data.github.GitHubManager

class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "github_sync_channel"
        const val NOTIFICATION_ID = 9500

        fun schedule(context: Context, intervalMins: Int) {
            try {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()

                val syncRequest = androidx.work.PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                    intervalMins.toLong(), java.util.concurrent.TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "github_notification_sync",
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    syncRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun cancel(context: Context) {
            try {
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("github_notification_sync")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("sync_background_enabled", false)
        if (!enabled) return Result.success()

        val wifiOnly = prefs.getBoolean("sync_wifi_only", false)
        if (wifiOnly) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)
            val isWifi = capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
            if (!isWifi) return Result.success()
        }

        if (!GitHubManager.isLoggedIn(context)) return Result.success()

        val notifications = try {
            GitHubManager.listNotifications(context, all = false, participating = false)
        } catch (e: Exception) {
            return Result.retry()
        }

        if (notifications.isNotEmpty()) {
            val count = notifications.size
            showNotification(count, notifications.first().repoName, notifications.first().title)
        }

        return Result.success()
    }

    private fun showNotification(count: Int, repoName: String, latestTitle: String) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GitHub Notification Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("New GitHub Notifications ($count)")
            .setContentText("Latest from $repoName: $latestTitle")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
