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
     * giữ nguyên hoàn toàn các công việc FIXED và né các khe bận từ lịch điện thoại [externalBusySlots].
     */
    fun autoScheduleDay(
        allTasks: List<TaskItem>,
        targetDate: LocalDate,
        settings: AppSettings,
        externalBusySlots: List<TimeSlot> = emptyList()
    ): List<TaskItem> {
        val dayTasks = allTasks.filter { it.appliesToDate(targetDate) }
        if (dayTasks.isEmpty()) return allTasks

        val dayStart = settings.workStartMinute.coerceIn(0, 23 * 60)
        val dayEnd = settings.workEndMinute.coerceIn(dayStart + 60, 23 * 60 + 55)
        val gap = settings.breakMinutes.coerceIn(0, 60)

        val nowMinute = if (targetDate == LocalDate.now()) {
            val now = LocalTime.now()
            val raw = now.hour * 60 + now.minute
            ((raw + 4) / 5) * 5
        } else {
            dayStart
        }

        val occupied = mutableListOf<TimeSlot>()
        occupied.addAll(externalBusySlots)

        dayTasks.forEach { task ->
            val st = task.statusOn(targetDate)
            if ((task.scheduleType == ScheduleType.FIXED || st == TaskStatus.DONE) &&
                task.startMinute != null && task.endMinute != null && task.endMinute > task.startMinute
            ) {
                occupied.add(TimeSlot(task.startMinute, task.endMinute))
            }
        }

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

            var slot = findFirstAvailableSlot(occupied, prefStart, prefEnd, duration, gap)
            if (slot == null) {
                slot = findFirstAvailableSlot(occupied, maxOf(dayStart, nowMinute), dayEnd, duration, gap)
            }
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

    /**
     * Dời 1 công việc bị trễ sang khe trống gần nhất tính từ thời điểm hiện tại.
     * Trả về cặp (danh sách cập nhật, khe giờ mới hoặc null nếu hôm nay đã kín).
     */
    fun rescheduleSingleTaskToNextSlot(
        allTasks: List<TaskItem>,
        taskId: String,
        targetDate: LocalDate,
        settings: AppSettings,
        externalBusySlots: List<TimeSlot> = emptyList()
    ): Pair<List<TaskItem>, TimeSlot?> {
        val target = allTasks.firstOrNull { it.id == taskId } ?: return allTasks to null
        val dayStart = settings.workStartMinute.coerceIn(0, 23 * 60)
        val dayEnd = settings.workEndMinute.coerceIn(dayStart + 60, 23 * 60 + 55)
        val gap = settings.breakMinutes.coerceIn(0, 60)

        val nowMinute = if (targetDate == LocalDate.now()) {
            val now = LocalTime.now()
            val raw = now.hour * 60 + now.minute + 5
            ((raw + 4) / 5) * 5
        } else {
            dayStart
        }

        val occupied = mutableListOf<TimeSlot>()
        occupied.addAll(externalBusySlots)
        allTasks.filter { it.id != taskId && it.appliesToDate(targetDate) }.forEach { other ->
            if (other.startMinute != null && other.endMinute != null && other.endMinute > other.startMinute) {
                occupied.add(TimeSlot(other.startMinute, other.endMinute))
            }
        }

        val duration = target.effectiveDurationMinutes().coerceAtLeast(15)
        val slot = findFirstAvailableSlot(occupied, maxOf(dayStart, nowMinute), dayEnd, duration, gap)
            ?: return allTasks to null

        val updated = allTasks.map { item ->
            if (item.id == taskId) {
                item.copy(
                    startMinute = slot.start,
                    endMinute = slot.end
                ).withStatusOn(targetDate, TaskStatus.UPCOMING)
            } else {
                item
            }
        }
        return updated to slot
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
