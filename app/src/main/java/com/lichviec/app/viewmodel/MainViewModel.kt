package com.lichviec.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lichviec.app.data.AiParsedTaskDraft
import com.lichviec.app.data.AppSettings
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskPriority
import com.lichviec.app.data.TaskRepository
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.TimePreference
import com.lichviec.app.data.formatMinute
import com.lichviec.app.domain.DeviceCalendarHelper
import com.lichviec.app.domain.GeminiAiService
import com.lichviec.app.domain.SmartScheduler
import com.lichviec.app.domain.TimeSlot
import com.lichviec.app.notification.LockScreenNotificationHelper
import com.lichviec.app.notification.ReminderScheduler
import com.lichviec.app.widget.LichViecWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

data class MainUiState(
    val tasks: List<TaskItem> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val statusFilter: TaskStatus? = null,
    val settings: AppSettings = AppSettings(),
    val aiInputText: String = "",
    val aiDrafts: List<AiParsedTaskDraft> = emptyList(),
    val isAiLoading: Boolean = false,
    val aiStatusMessage: String = "",
    val weeklyReviewText: String = "",
    val isWeeklyReviewLoading: Boolean = false,
    val toastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            settings = repository.loadSettings()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val loadResult = repository.loadTasksSafe()
        when {
            loadResult.wasCorrupted -> {
                _uiState.update {
                    it.copy(
                        tasks = emptyList(),
                        toastMessage = "File dữ liệu bị lỗi, đã sao lưu thành ${loadResult.corruptBackupPath ?: "bản dự phòng"} (không ghi đè)."
                    )
                }
            }
            !loadResult.fileExisted -> {
                seedInitialSamples()
            }
            else -> {
                _uiState.update { it.copy(tasks = loadResult.tasks) }
                refreshUpcomingStatuses(LocalDate.now())
                syncExternalSurfaces(loadResult.tasks, _uiState.value.settings)
            }
        }
    }

    private fun seedInitialSamples() {
        val today = LocalDate.now().toString()
        val samples = listOf(
            TaskItem(
                title = "Lập kế hoạch trong ngày",
                note = "Rà soát các việc ưu tiên cao và kiểm tra khung giờ trống",
                date = today,
                scheduleType = ScheduleType.FIXED,
                startMinute = 8 * 60,
                endMinute = 8 * 60 + 30,
                durationMinutes = 30,
                priority = TaskPriority.HIGH,
                repeatType = RepeatType.DAILY,
                statusByDate = mapOf(today to TaskStatus.UPCOMING),
                category = "Công việc"
            ),
            TaskItem(
                title = "Học tiếng Anh 30 phút",
                note = "Nghe và ôn từ vựng mỗi ngày",
                date = today,
                scheduleType = ScheduleType.FLEXIBLE,
                startMinute = 20 * 60,
                endMinute = 20 * 60 + 30,
                durationMinutes = 30,
                timePreference = TimePreference.EVENING,
                priority = TaskPriority.MEDIUM,
                repeatType = RepeatType.DAILY,
                statusByDate = mapOf(today to TaskStatus.TODO),
                category = "Học tập"
            )
        )
        persistTasks(samples)
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun setStatusFilter(status: TaskStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun tasksForDate(date: LocalDate): List<TaskItem> {
        return _uiState.value.tasks
            .filter { it.appliesToDate(date) }
            .sortedWith(
                compareBy<TaskItem> { it.statusOn(date) == TaskStatus.DONE }
                    .thenBy { it.startMinute ?: Int.MAX_VALUE }
                    .thenByDescending { it.priority.weight }
            )
    }

    fun weekDatesAround(date: LocalDate): List<LocalDate> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun addOrUpdateTask(task: TaskItem) {
        val current = _uiState.value.tasks.toMutableList()
        val idx = current.indexOfFirst { it.id == task.id }
        if (idx >= 0) {
            current[idx] = task
        } else {
            current.add(task)
        }
        persistTasks(current)
        showToast(if (idx >= 0) "Đã cập nhật công việc" else "Đã thêm công việc mới")
    }

    fun deleteTask(taskId: String) {
        val updated = _uiState.value.tasks.filterNot { it.id == taskId }
        persistTasks(updated)
        showToast("Đã xóa công việc")
    }

    fun setTaskStatus(taskId: String, date: LocalDate, status: TaskStatus) {
        val updated = _uiState.value.tasks.map { task ->
            if (task.id == taskId) task.withStatusOn(date, status) else task
        }
        persistTasks(updated)
    }

    fun cycleTaskStatus(taskId: String, date: LocalDate) {
        val updated = _uiState.value.tasks.map { task ->
            if (task.id == taskId) {
                task.withStatusOn(date, task.statusOn(date).next())
            } else {
                task
            }
        }
        persistTasks(updated)
    }

    fun toggleSubTask(taskId: String, subTaskId: String) {
        val updated = _uiState.value.tasks.map { task ->
            if (task.id == taskId) {
                task.copy(
                    subtasks = task.subtasks.map { st ->
                        if (st.id == subTaskId) st.copy(done = !st.done) else st
                    }
                )
            } else {
                task
            }
        }
        persistTasks(updated)
    }

    fun refreshUpcomingStatuses(date: LocalDate = LocalDate.now()) {
        if (date != LocalDate.now()) return
        val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        var changed = false
        val updated = _uiState.value.tasks.map { task ->
            val st = task.statusOn(date)
            val start = task.startMinute
            if (task.appliesToDate(date) && st == TaskStatus.TODO && start != null && start in nowMin..(nowMin + 60)) {
                changed = true
                task.withStatusOn(date, TaskStatus.UPCOMING)
            } else {
                task
            }
        }
        if (changed) {
            persistTasks(updated)
        }
    }

    private fun readExternalBusySlots(date: LocalDate): List<TimeSlot> {
        val settings = _uiState.value.settings
        if (!settings.avoidDeviceCalendarEvents) return emptyList()
        return DeviceCalendarHelper.readBusySlotsForDate(getApplication(), date)
    }

    fun autoScheduleSelectedDay() {
        val date = _uiState.value.selectedDate
        val busySlots = readExternalBusySlots(date)
        val scheduled = SmartScheduler.autoScheduleDay(
            allTasks = _uiState.value.tasks,
            targetDate = date,
            settings = _uiState.value.settings,
            externalBusySlots = busySlots
        )
        persistTasks(scheduled)
        val suffix = if (busySlots.isNotEmpty()) " (đã né ${busySlots.size} lịch trên máy)" else ""
        showToast("Đã tự động xếp giờ các việc linh hoạt$suffix")
    }

    fun rescheduleOverdueTask(taskId: String, date: LocalDate = _uiState.value.selectedDate) {
        val busySlots = readExternalBusySlots(date)
        val (updated, slot) = SmartScheduler.rescheduleSingleTaskToNextSlot(
            allTasks = _uiState.value.tasks,
            taskId = taskId,
            targetDate = date,
            settings = _uiState.value.settings,
            externalBusySlots = busySlots
        )
        if (slot != null) {
            persistTasks(updated)
            showToast("Đã dời sang ${formatMinute(slot.start)} – ${formatMinute(slot.end)}")
        } else {
            showToast("Hôm nay không còn khe trống phù hợp")
        }
    }

    fun carryOverUnfinishedFromYesterday() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val yesterdayStr = yesterday.toString()
        val todayStr = today.toString()

        var movedCount = 0
        val updated = _uiState.value.tasks.map { task ->
            if (task.repeatType == RepeatType.NONE &&
                task.date == yesterdayStr &&
                task.statusOn(yesterday) != TaskStatus.DONE
            ) {
                movedCount++
                val newMap = task.statusByDate.toMutableMap()
                newMap.remove(yesterdayStr)
                newMap[todayStr] = TaskStatus.TODO
                task.copy(
                    date = todayStr,
                    startMinute = if (task.scheduleType == ScheduleType.FIXED) task.startMinute else null,
                    endMinute = if (task.scheduleType == ScheduleType.FIXED) task.endMinute else null,
                    statusByDate = newMap
                )
            } else {
                task
            }
        }
        if (movedCount > 0) {
            persistTasks(updated)
            _uiState.update { it.copy(selectedDate = today, toastMessage = "Đã chuyển $movedCount việc chưa xong sang hôm nay") }
        } else {
            showToast("Không có việc chưa xong từ hôm qua")
        }
    }

    fun updateAiInput(text: String) {
        _uiState.update { it.copy(aiInputText = text) }
    }

    fun appendVoiceTranscript(transcript: String) {
        val clean = transcript.trim()
        if (clean.isEmpty()) return
        _uiState.update { state ->
            val updatedText = if (state.aiInputText.isBlank()) clean else "${state.aiInputText.trim()}\n$clean"
            state.copy(aiInputText = updatedText)
        }
    }

    fun analyzeNoteWithAi(targetDate: LocalDate = _uiState.value.selectedDate) {
        val text = _uiState.value.aiInputText.trim()
        if (text.isEmpty()) {
            showToast("Hãy nhập hoặc nói ghi chú công việc trước")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiStatusMessage = "Đang phân tích ghi chú...") }
            val existing = tasksForDate(targetDate)
            val result = GeminiAiService.parseAndScheduleNote(
                noteText = text,
                targetDate = targetDate,
                existingTasksOnDate = existing,
                settings = _uiState.value.settings
            )
            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiDrafts = result.drafts,
                    aiStatusMessage = result.message
                )
            }
        }
    }

    fun toggleDraftSelection(draftId: String) {
        _uiState.update { state ->
            state.copy(
                aiDrafts = state.aiDrafts.map {
                    if (it.id == draftId) it.copy(selected = !it.selected) else it
                }
            )
        }
    }

    fun toggleDraftScheduleType(draftId: String) {
        _uiState.update { state ->
            state.copy(
                aiDrafts = state.aiDrafts.map {
                    if (it.id == draftId) {
                        val nextType = if (it.scheduleType == ScheduleType.FIXED) ScheduleType.FLEXIBLE else ScheduleType.FIXED
                        it.copy(scheduleType = nextType)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun toggleDraftRepeatDaily(draftId: String) {
        _uiState.update { state ->
            state.copy(
                aiDrafts = state.aiDrafts.map {
                    if (it.id == draftId) {
                        val nextRepeat = if (it.repeatType == RepeatType.DAILY) RepeatType.NONE else RepeatType.DAILY
                        it.copy(repeatType = nextRepeat)
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun applySelectedAiDrafts(autoScheduleFlexible: Boolean = true) {
        val selected = _uiState.value.aiDrafts.filter { it.selected }
        if (selected.isEmpty()) {
            showToast("Chưa chọn công việc nào để lưu")
            return
        }
        val newTasks = selected.map { it.toTaskItem() }
        var combined = _uiState.value.tasks + newTasks

        if (autoScheduleFlexible) {
            val affectedDates = newTasks.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct()
            for (d in affectedDates) {
                val busySlots = readExternalBusySlots(d)
                combined = SmartScheduler.autoScheduleDay(combined, d, _uiState.value.settings, busySlots)
            }
        }

        persistTasks(combined)
        _uiState.update {
            it.copy(
                aiInputText = "",
                aiDrafts = emptyList(),
                aiStatusMessage = "",
                toastMessage = "Đã lưu ${newTasks.size} công việc vào lịch"
            )
        }
    }

    fun generateWeeklyReview() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWeeklyReviewLoading = true) }
            val summary = GeminiAiService.generateWeeklyReview(
                tasksProvider = { date -> tasksForDate(date) },
                settings = _uiState.value.settings
            )
            _uiState.update {
                it.copy(
                    isWeeklyReviewLoading = false,
                    weeklyReviewText = summary
                )
            }
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings, toastMessage = "Đã lưu cài đặt") }
        syncExternalSurfaces(_uiState.value.tasks, newSettings)
    }

    fun exportBackupJson(): String {
        return repository.exportBackupJson(_uiState.value.tasks)
    }

    fun importBackupJson(rawJson: String) {
        val result = repository.importBackupJson(rawJson)
        if (result.isSuccess) {
            val loaded = result.getOrDefault(emptyList())
            _uiState.update { it.copy(tasks = loaded, toastMessage = "Đã khôi phục ${loaded.size} công việc") }
            syncExternalSurfaces(loaded, _uiState.value.settings)
        } else {
            showToast("Tệp JSON sao lưu không hợp lệ")
        }
    }

    fun computeDailyHabitStreak(): Int {
        val dailyTasks = _uiState.value.tasks.filter { it.repeatType == RepeatType.DAILY }
        if (dailyTasks.isEmpty()) return 0
        var streak = 0
        var cursor = LocalDate.now()
        repeat(60) {
            val activeToday = dailyTasks.filter { it.appliesToDate(cursor) }
            if (activeToday.isEmpty()) return streak
            val allDone = activeToday.all { it.statusOn(cursor) == TaskStatus.DONE }
            if (allDone) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                if (cursor == LocalDate.now()) {
                    cursor = cursor.minusDays(1)
                } else {
                    return streak
                }
            }
        }
        return streak
    }

    private fun persistTasks(tasks: List<TaskItem>) {
        repository.saveTasks(tasks)
        _uiState.update { it.copy(tasks = tasks) }
        syncExternalSurfaces(tasks, _uiState.value.settings)
    }

    private fun syncExternalSurfaces(tasks: List<TaskItem>, settings: AppSettings) {
        val app = getApplication<Application>()
        ReminderScheduler.scheduleForDate(
            app,
            tasks,
            LocalDate.now(),
            settings.reminderBeforeMinutes
        )
        LockScreenNotificationHelper.updateLockScreenSummary(
            app,
            tasks,
            settings.lockScreenSummaryEnabled,
            settings.hideLockScreenDetails
        )
        LichViecWidgetProvider.updateAllWidgets(app)
    }
}
