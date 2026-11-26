package com.lichviec.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class TaskStatus(val label: String) {
    TODO("Chưa làm"),
    UPCOMING("Sắp làm"),
    DONE("Đã làm");

    fun next(): TaskStatus = when (this) {
        TODO -> UPCOMING
        UPCOMING -> DONE
        DONE -> TODO
    }
}

enum class ScheduleType(val label: String) {
    FIXED("Giờ cố định"),
    FLEXIBLE("Giờ linh hoạt")
}

enum class RepeatType(val label: String) {
    NONE("1 lần"),
    DAILY("Mỗi ngày"),
    WEEKLY_DAYS("Theo thứ")
}

enum class TimePreference(val label: String, val startMinute: Int, val endMinute: Int) {
    ANY("Bất kỳ", 6 * 60, 23 * 60),
    MORNING("Sáng (6h-12h)", 6 * 60, 12 * 60),
    AFTERNOON("Chiều (13h-18h)", 13 * 60, 18 * 60),
    EVENING("Tối (18h-23h)", 18 * 60, 23 * 60)
}

enum class TaskPriority(val label: String, val weight: Int) {
    HIGH("Cao", 3),
    MEDIUM("Trung bình", 2),
    LOW("Thấp", 1)
}

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val date: String = LocalDate.now().toString(),
    val scheduleType: ScheduleType = ScheduleType.FLEXIBLE,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val durationMinutes: Int = 45,
    val timePreference: TimePreference = TimePreference.ANY,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatDays: Set<Int> = emptySet(),
    val statusByDate: Map<String, TaskStatus> = emptyMap(),
    val reminderEnabled: Boolean = true,
    val category: String = "Chung"
) {
    fun appliesToDate(targetDate: LocalDate): Boolean {
        val startDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        return when (repeatType) {
            RepeatType.NONE -> date == targetDate.toString()
            RepeatType.DAILY -> !targetDate.isBefore(startDate)
            RepeatType.WEEKLY_DAYS -> {
                !targetDate.isBefore(startDate) && repeatDays.contains(targetDate.dayOfWeek.value)
            }
        }
    }

    fun statusOn(targetDate: LocalDate): TaskStatus {
        return statusByDate[targetDate.toString()] ?: TaskStatus.TODO
    }

    fun withStatusOn(targetDate: LocalDate, newStatus: TaskStatus): TaskItem {
        val updated = statusByDate.toMutableMap()
        updated[targetDate.toString()] = newStatus
        return copy(statusByDate = updated)
    }

    fun effectiveDurationMinutes(): Int {
        return if (startMinute != null && endMinute != null && endMinute > startMinute) {
            endMinute - startMinute
        } else {
            durationMinutes.coerceAtLeast(10)
        }
    }

    fun formattedTimeRange(): String {
        return if (startMinute != null && endMinute != null) {
            "${formatMinute(startMinute)} - ${formatMinute(endMinute)}"
        } else {
            "Linh hoạt (${durationMinutes}p)"
        }
    }
}

data class AppSettings(
    val geminiApiKey: String = "",
    val workStartMinute: Int = 7 * 60,
    val workEndMinute: Int = 22 * 60 + 30,
    val breakMinutes: Int = 10,
    val reminderBeforeMinutes: Int = 10,
    val lockScreenSummaryEnabled: Boolean = true
)

data class AiParsedTaskDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val date: String = LocalDate.now().toString(),
    val scheduleType: ScheduleType = ScheduleType.FLEXIBLE,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val durationMinutes: Int = 45,
    val timePreference: TimePreference = TimePreference.ANY,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatDays: Set<Int> = emptySet(),
    val category: String = "Chung",
    val selected: Boolean = true
) {
    fun toTaskItem(): TaskItem = TaskItem(
        id = id,
        title = title.trim(),
        note = note.trim(),
        date = date,
        scheduleType = scheduleType,
        startMinute = startMinute,
        endMinute = endMinute,
        durationMinutes = durationMinutes.coerceAtLeast(10),
        timePreference = timePreference,
        priority = priority,
        repeatType = repeatType,
        repeatDays = repeatDays,
        statusByDate = mapOf(date to TaskStatus.TODO),
        reminderEnabled = true,
        category = category.ifBlank { "Chung" }
    )
}

fun formatMinute(totalMinutes: Int): String {
    val safe = totalMinutes.coerceIn(0, 23 * 60 + 59)
    val h = safe / 60
    val m = safe % 60
    return "%02d:%02d".format(h, m)
}

fun shortDayName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "T2"
    DayOfWeek.TUESDAY -> "T3"
    DayOfWeek.WEDNESDAY -> "T4"
    DayOfWeek.THURSDAY -> "T5"
    DayOfWeek.FRIDAY -> "T6"
    DayOfWeek.SATURDAY -> "T7"
    DayOfWeek.SUNDAY -> "CN"
}
