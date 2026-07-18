package gs.git.vps.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import gs.git.vps.MainActivity
import gs.git.vps.data.github.*
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.data.github.model.GHNotification

class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val CHANNEL_ID = "github_sync_channel"
        const val NOTIFICATION_ID = 9500
        private const val KEY_LAST_NOTIFIED_SIGNATURE = "last_notified_signature"
        private const val KEY_NEXT_API_POLL_AT = "notification_next_api_poll_at"

        fun schedule(context: Context, intervalMins: Int) {
            try {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val wifiOnly = context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)
                    .getBoolean("sync_wifi_only", false)
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(
                        if (wifiOnly) androidx.work.NetworkType.UNMETERED
                        else androidx.work.NetworkType.CONNECTED
                    )
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

        if (!GitHubManager.isLoggedIn(context)) return Result.success()

        val now = System.currentTimeMillis()
        if (now < prefs.getLong(KEY_NEXT_API_POLL_AT, 0L)) return Result.success()

        val page = GitHubManager.getNotificationsPage(
            context = context,
            all = false,
            participating = false,
            page = 1,
            perPage = 50,
        )
        prefs.edit().putLong(
            KEY_NEXT_API_POLL_AT,
            now + page.pollIntervalSeconds.coerceAtLeast(1) * 1_000L,
        ).apply()
        if (page.error.isNotBlank()) {
            return if (page.code == -1 || page.code >= 500) Result.retry() else Result.success()
        }
        val notifications = page.notifications

        if (notifications.isNotEmpty()) {
            val signature = notifications
                .sortedBy { it.id }
                .joinToString("|") { "${it.id}:${it.updatedAt}" }
            if (signature != prefs.getString(KEY_LAST_NOTIFIED_SIGNATURE, "")) {
                if (showNotification(notifications)) {
                    prefs.edit().putString(KEY_LAST_NOTIFIED_SIGNATURE, signature).apply()
                }
            }
        }

        return Result.success()
    }

    private fun showNotification(notifications: List<GHNotification>): Boolean {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GitHub Notification Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val latest = notifications.first()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = latest.htmlUrl.takeIf { it.isNotBlank() }?.let(Uri::parse)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            latest.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GitHub notifications (${notifications.size})")
            .setContentText("Latest from ${latest.repoName}: ${latest.title}")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("${notifications.size} unread GitHub threads")
                    .also { style ->
                        notifications.take(5).forEach { item ->
                            style.addLine("${item.repoName.substringAfter('/')}: ${item.title}")
                        }
                        if (notifications.size > 5) style.setSummaryText("+${notifications.size - 5} more")
                    }
            )
            .setSmallIcon(gs.git.vps.R.drawable.ic_stat_gsgit)
            .setLargeIcon(
                android.graphics.BitmapFactory.decodeResource(context.resources, gs.git.vps.R.mipmap.ic_launcher)
            )
            .setNumber(notifications.size)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        return true
    }
}
