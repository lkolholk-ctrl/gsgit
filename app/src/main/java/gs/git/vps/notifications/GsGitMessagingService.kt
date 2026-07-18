package gs.git.vps.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import gs.git.vps.MainActivity

/**
 * Приём моментальных пушей с api.gsgit.org (data-сообщения FCM:
 * type/title/body/repo/url — формирует server/api/server.js).
 *
 * Каждый тип события идёт в свой notification channel, поэтому пользователь
 * может выключать категории по отдельности в системных настройках уведомлений
 * (Settings → Apps → GsGit → Notifications). Названия каналов — английские.
 * Тап по нотификации открывает MainActivity с github-ссылкой — тот же роутинг,
 * что у NotificationSyncWorker.
 */
class GsGitMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        GsGitPush.onNewToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return
        val title = data["title"].orEmpty().ifBlank { data["repo"].orEmpty() }
        val body = data["body"].orEmpty()
        if (title.isBlank() && body.isBlank()) return
        val channel = channelFor(data["type"].orEmpty())
        showNotification(channel, title.ifBlank { "GitHub" }, body, data["url"].orEmpty())
    }

    private fun showNotification(channel: PushChannel, title: String, body: String, url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannels(this)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = url.takeIf { it.isNotBlank() }?.let(Uri::parse)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channel.id)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(gs.git.vps.R.drawable.ic_stat_gsgit)
            .setLargeIcon(
                android.graphics.BitmapFactory.decodeResource(resources, gs.git.vps.R.mipmap.ic_launcher)
            )
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Отдельный id на каждое событие, чтобы пуши не перетирали друг друга.
        manager.notify((url.ifBlank { title + body }).hashCode(), notification)
    }

    /** Категория пуша: id канала + английское имя + важность. */
    data class PushChannel(val id: String, val name: String, val importance: Int)

    companion object {
        private const val LEGACY_CHANNEL_ID = "gsgit_instant_push"

        private val CHANNELS: List<PushChannel> by lazy {
            val high = NotificationManager.IMPORTANCE_HIGH
            val normal = NotificationManager.IMPORTANCE_DEFAULT
            listOf(
                PushChannel("gsgit_issues", "Issues", high),
                PushChannel("gsgit_pulls", "Pull requests", high),
                PushChannel("gsgit_comments", "Comments", high),
                PushChannel("gsgit_ci", "CI & deployments", normal),
                PushChannel("gsgit_commits", "Commits & branches", normal),
                PushChannel("gsgit_releases", "Releases", normal),
                PushChannel("gsgit_discussions", "Discussions", normal),
                PushChannel("gsgit_social", "Stars, forks & members", normal),
                PushChannel("gsgit_security", "Account security", high),
                PushChannel("gsgit_other", "Other GitHub events", normal),
            )
        }

        fun channelFor(type: String): PushChannel {
            val id = when (type) {
                "issues" -> "gsgit_issues"
                "pull_request", "pull_request_review" -> "gsgit_pulls"
                "issue_comment", "discussion_comment" -> "gsgit_comments"
                "workflow_run", "check_suite", "deployment_status" -> "gsgit_ci"
                "push", "create", "delete" -> "gsgit_commits"
                "release" -> "gsgit_releases"
                "discussion" -> "gsgit_discussions"
                "star", "watch", "fork", "member" -> "gsgit_social"
                "security" -> "gsgit_security"
                else -> "gsgit_other"
            }
            return CHANNELS.first { it.id == id }
        }

        /**
         * Создаёт все каналы разом, чтобы полный список тумблеров был виден в
         * системных настройках сразу, а не по мере прихода событий. Идемпотентно;
         * зовётся из App.onCreate и перед показом нотификации.
         */
        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            CHANNELS.forEach { c ->
                manager.createNotificationChannel(NotificationChannel(c.id, c.name, c.importance))
            }
            // Старый единый канал больше не используется — убираем из настроек.
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
    }
}
