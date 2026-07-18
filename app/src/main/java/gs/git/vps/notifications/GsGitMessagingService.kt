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
 * Тап по нотификации открывает MainActivity с github-ссылкой —
 * тот же роутинг, что у NotificationSyncWorker.
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
        showNotification(title.ifBlank { "GitHub" }, body, data["url"].orEmpty())
    }

    private fun showNotification(title: String, body: String, url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "GitHub instant push", NotificationManager.IMPORTANCE_HIGH)
            )
        }

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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(gs.git.vps.R.drawable.ic_stat_gsgit)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Отдельный id на каждое событие, чтобы пуши не перетирали друг друга.
        manager.notify((url.ifBlank { title + body }).hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "gsgit_instant_push"
    }
}
