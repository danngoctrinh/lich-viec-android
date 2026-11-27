package com.lichviec.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lichviec.app.data.TaskRepository
import com.lichviec.app.widget.LichViecWidgetProvider
import java.time.LocalDate

/**
 * Tự động khôi phục toàn bộ báo thức nhắc việc, thông báo màn hình khóa và widget
 * sau khi điện thoại khởi động lại hoặc sau khi cập nhật phiên bản ứng dụng.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val repo = TaskRepository(context)
            val tasks = repo.loadTasks()
            val settings = repo.loadSettings()

            ReminderScheduler.scheduleForDate(
                context = context,
                tasks = tasks,
                date = LocalDate.now(),
                minutesBefore = settings.reminderBeforeMinutes
            )
            LockScreenNotificationHelper.updateLockScreenSummary(
                context = context,
                allTasks = tasks,
                enabled = settings.lockScreenSummaryEnabled,
                hideDetails = settings.hideLockScreenDetails
            )
            LichViecWidgetProvider.updateAllWidgets(context)
        }
    }
}
