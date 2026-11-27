package com.lichviec.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lichviec.app.MainActivity
import com.lichviec.app.R
import com.lichviec.app.data.TaskRepository
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.formatMinute
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LichViecWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, LichViecWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateSingleWidget(context, manager, id)
                }
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repo = TaskRepository(context)
            val settings = repo.loadSettings()
            val today = LocalDate.now()
            val todayTasks = repo.loadTasks()
                .filter { it.appliesToDate(today) }
                .sortedWith(
                    compareBy<com.lichviec.app.data.TaskItem> { it.statusOn(today) == TaskStatus.DONE }
                        .thenBy { it.startMinute ?: Int.MAX_VALUE }
                )

            val doneCount = todayTasks.count { it.statusOn(today) == TaskStatus.DONE }
            val pending = todayTasks.filter { it.statusOn(today) != TaskStatus.DONE }

            val dateStr = today.format(DateTimeFormatter.ofPattern("dd/MM"))
            val previewText = when {
                settings.hideLockScreenDetails -> "Còn ${pending.size} việc hôm nay (đang ẩn nội dung chi tiết)."
                pending.isEmpty() -> {
                    if (todayTasks.isEmpty()) "Chưa có công việc hôm nay.\nChạm \"+ Note AI\" để ghi chú nhanh."
                    else "Đã hoàn thành hết ${todayTasks.size} việc hôm nay."
                }
                else -> pending.take(4).joinToString("\n") { task ->
                    val timePrefix = if (task.startMinute != null) "${formatMinute(task.startMinute)} " else ""
                    val mark = if (task.statusOn(today) == TaskStatus.UPCOMING) "[Sắp] " else ""
                    "$timePrefix$mark${task.title}"
                }
            }

            val views = RemoteViews(context.packageName, R.layout.widget_quick_note)
            views.setTextViewText(R.id.widget_title, "Hôm nay $dateStr")
            views.setTextViewText(R.id.widget_progress, "$doneCount/${todayTasks.size} xong")
            views.setTextViewText(R.id.widget_tasks_preview, previewText)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TAB, 0)
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                100,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPending)

            val aiNoteIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TAB, 2)
                putExtra(MainActivity.EXTRA_START_VOICE, false)
            }
            val aiNotePending = PendingIntent.getActivity(
                context,
                101,
                aiNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_ai_note, aiNotePending)

            val addTaskIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TAB, 0)
                putExtra(MainActivity.EXTRA_OPEN_ADD_DIALOG, true)
            }
            val addTaskPending = PendingIntent.getActivity(
                context,
                102,
                addTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add_task, addTaskPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
