package com.lichviec.app.domain

import com.lichviec.app.data.AppSettings
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import java.time.LocalDate
import java.time.LocalTime

data class TimeSlot(val start: Int, val end: Int) {
    val duration: Int get() = end - start
}

object SmartScheduler {

    /**
     * Tự động sắp xếp lại khung giờ cho các công việc FLEXIBLE (chưa hoàn thành) trong ngày [targetDate],
     * giữ nguyên hoàn toàn các công việc FIXED (giờ cố định).
     */
    fun autoScheduleDay(
        allTasks: List<TaskItem>,
        targetDate: LocalDate,
        settings: AppSettings
    ): List<TaskItem> {
        val dayTasks = allTasks.filter { it.appliesToDate(targetDate) }
        if (dayTasks.isEmpty()) return allTasks

        val dayStart = settings.workStartMinute.coerceIn(0, 23 * 60)
        val dayEnd = settings.workEndMinute.coerceIn(dayStart + 60, 23 * 60 + 55)
        val gap = settings.breakMinutes.coerceIn(0, 60)

        // Nếu là hôm nay, ưu tiên xếp từ thời điểm hiện tại trở đi (làm tròn lên bội số 5 phút)
        val nowMinute = if (targetDate == LocalDate.now()) {
            val now = LocalTime.now()
            val raw = now.hour * 60 + now.minute
            ((raw + 4) / 5) * 5
        } else {
            dayStart
        }

        // Các khối thời gian bị chiếm cứng: việc FIXED + việc đã DONE có giờ
        val occupied = mutableListOf<TimeSlot>()
        dayTasks.forEach { task ->
            val st = task.statusOn(targetDate)
            if ((task.scheduleType == ScheduleType.FIXED || st == TaskStatus.DONE) &&
                task.startMinute != null && task.endMinute != null && task.endMinute > task.startMinute
            ) {
                occupied.add(TimeSlot(task.startMinute, task.endMinute))
            }
        }

        // Các việc linh hoạt chưa hoàn thành cần xếp giờ
        val flexibleToSchedule = dayTasks
            .filter { it.scheduleType == ScheduleType.FLEXIBLE && it.statusOn(targetDate) != TaskStatus.DONE }
            .sortedWith(
                compareByDescending<TaskItem> { it.priority.weight }
                    .thenBy { it.timePreference.startMinute }
                    .thenByDescending { it.durationMinutes }
            )

        val updatedMap = mutableMapOf<String, TaskItem>()

        for (task in flexibleToSchedule) {
            val duration = task.durationMinutes.coerceAtLeast(15)
            val prefStart = maxOf(dayStart, task.timePreference.startMinute, nowMinute)
            val prefEnd = minOf(dayEnd, task.timePreference.endMinute)

            // Tìm khe trống trong buổi ưu tiên trước
            var slot = findFirstAvailableSlot(occupied, prefStart, prefEnd, duration, gap)

            // Nếu buổi ưu tiên đã kín, tìm trong toàn bộ khung giờ làm việc từ nowMinute trở đi
            if (slot == null) {
                slot = findFirstAvailableSlot(occupied, maxOf(dayStart, nowMinute), dayEnd, duration, gap)
            }

            // Nếu hôm nay đã muộn, thử lại từ đầu ngày (dayStart..dayEnd)
            if (slot == null && nowMinute > dayStart) {
                slot = findFirstAvailableSlot(occupied, dayStart, dayEnd, duration, gap)
            }

            if (slot != null) {
                occupied.add(slot)
                updatedMap[task.id] = task.copy(
                    startMinute = slot.start,
                    endMinute = slot.end
                )
            }
        }

        return allTasks.map { updatedMap[it.id] ?: it }
    }

    private fun findFirstAvailableSlot(
        occupied: List<TimeSlot>,
        windowStart: Int,
        windowEnd: Int,
        duration: Int,
        gap: Int
    ): TimeSlot? {
        if (windowEnd - windowStart < duration) return null
        val sorted = occupied.sortedBy { it.start }
        var cursor = windowStart

        for (block in sorted) {
            if (block.end <= cursor) continue
            if (block.start >= windowEnd) break

            val freeEnd = minOf(block.start - gap, windowEnd)
            if (freeEnd - cursor >= duration) {
                return TimeSlot(cursor, cursor + duration)
            }
            cursor = maxOf(cursor, block.end + gap)
            if (cursor + duration > windowEnd) return null
        }

        return if (cursor + duration <= windowEnd) {
            TimeSlot(cursor, cursor + duration)
        } else {
            null
        }
    }
}
