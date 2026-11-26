package com.lichviec.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lichviec.app.MainActivity

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Sắp đến giờ làm việc"
        val timeText = intent.getStringExtra(EXTRA_TIME) ?: ""
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 1001)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nhắc nhở công việc",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo nhắc trước khi đến giờ công việc trên màn hình khóa"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Sắp đến giờ: $title")
            .setContentText(if (timeText.isNotBlank()) "Khung giờ: $timeText" else "Chuẩn bị bắt đầu công việc")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()

        manager.notify(notifId, notification)
    }

    companion object {
        const val CHANNEL_ID = "lichviec_reminders"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
