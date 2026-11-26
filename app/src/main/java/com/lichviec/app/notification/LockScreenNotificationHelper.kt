package com.lichviec.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lichviec.app.MainActivity
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.formatMinute
import java.time.LocalDate

/**
 * Hiển thị thông báo lịch hôm nay ngay trên màn hình khóa (VISIBILITY_PUBLIC)
 * kèm nút mở nhanh AI Ghi chú hoặc Thêm việc.
 */
object LockScreenNotificationHelper {

    private const val CHANNEL_ID = "lichviec_lockscreen_agenda"
    private const val NOTIFICATION_ID = 2026

    fun updateLockScreenSummary(
        context: Context,
        allTasks: List<TaskItem>,
        enabled: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!enabled) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lịch hôm nay trên màn hình khóa",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Hiển thị nhanh các việc chưa làm và sắp làm ngay trên màn hình khóa"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val today = LocalDate.now()
        val todayTasks = allTasks
            .filter { it.appliesToDate(today) }
            .sortedWith(
                compareBy<TaskItem> { it.statusOn(today) == TaskStatus.DONE }
                    .thenBy { it.startMinute ?: Int.MAX_VALUE }
            )

        val doneCount = todayTasks.count { it.statusOn(today) == TaskStatus.DONE }
        val pending = todayTasks.filter { it.statusOn(today) != TaskStatus.DONE }

        val title = if (pending.isEmpty()) {
            "Lịch hôm nay • Xong $doneCount/${todayTasks.size}"
        } else {
            "Còn ${pending.size} việc hôm nay ($doneCount/${todayTasks.size} xong)"
        }

        val firstTaskText = pending.firstOrNull()?.let { task ->
            val timeStr = if (task.startMinute != null) "${formatMinute(task.startMinute)} • " else ""
            "Tiếp theo: $timeStr${task.title}"
        } ?: "Chạm để thêm việc mới hoặc ghi chú AI"

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        if (pending.isEmpty()) {
            inboxStyle.addLine("Đã hoàn thành hết công việc hôm nay.")
        } else {
            pending.take(5).forEach { task ->
                val timeStr = if (task.startMinute != null) "${formatMinute(task.startMinute)} " else "• "
                val tag = if (task.statusOn(today) == TaskStatus.UPCOMING) "[Sắp làm] " else ""
                inboxStyle.addLine("$timeStr$tag${task.title}")
            }
        }

        val openTodayIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, 0)
        }
        val openTodayPending = PendingIntent.getActivity(
            context,
            201,
            openTodayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAiIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, 2)
        }
        val openAiPending = PendingIntent.getActivity(
            context,
            202,
            openAiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addTaskIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, 0)
            putExtra(MainActivity.EXTRA_OPEN_ADD_DIALOG, true)
        }
        val addTaskPending = PendingIntent.getActivity(
            context,
            203,
            addTaskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(firstTaskText)
            .setStyle(inboxStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openTodayPending)
            .addAction(0, "+ Note AI", openAiPending)
            .addAction(0, "+ Thêm việc", addTaskPending)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }
}
