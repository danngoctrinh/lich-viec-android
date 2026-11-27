package com.lichviec.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleForDate(
        context: Context,
        tasks: List<TaskItem>,
        date: LocalDate,
        minutesBefore: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nowMillis = System.currentTimeMillis()
        val allowExact = canScheduleExact(context)

        tasks.filter { it.appliesToDate(date) }.forEach { task ->
            val requestCode = (task.id + "_" + date.toString()).hashCode()
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra(TaskReminderReceiver.EXTRA_TITLE, task.title)
                putExtra(TaskReminderReceiver.EXTRA_TIME, task.formattedTimeRange())
                putExtra(TaskReminderReceiver.EXTRA_NOTIF_ID, requestCode)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val startMin = task.startMinute
            if (!task.reminderEnabled || startMin == null || task.statusOn(date) == TaskStatus.DONE) {
                alarmManager.cancel(pendingIntent)
                return@forEach
            }

            val triggerMinute = (startMin - minutesBefore).coerceAtLeast(0)
            val triggerTime = LocalDateTime.of(
                date,
                LocalTime.of(triggerMinute / 60, triggerMinute % 60)
            )
            val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerMillis > nowMillis) {
                runCatching {
                    if (allowExact) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerMillis,
                            pendingIntent
                        )
                    }
                }
            }
        }
    }
}
