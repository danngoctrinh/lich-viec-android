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
import com.lichviec.app.domain.GeminiAiService
import com.lichviec.app.domain.SmartScheduler
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
    val toastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            tasks = repository.loadTasks(),
            settings = repository.loadSettings()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.tasks.isEmpty()) {
            seedInitialSamples()
        } else {
            refreshUpcomingStatuses(LocalDate.now())
            syncExternalSurfaces(_uiState.value.tasks, _uiState.value.settings)
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
        _uiState.update { it.copy(toastMessage = if (idx >= 0) "Đã cập nhật công việc" else "Đã thêm công việc mới") }
    }

    fun deleteTask(taskId: String) {
        val updated = _uiState.value.tasks.filterNot { it.id == taskId }
        persistTasks(updated)
        _uiState.update { it.copy(toastMessage = "Đã xóa công việc") }
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

    fun autoScheduleSelectedDay() {
        val date = _uiState.value.selectedDate
        val scheduled = SmartScheduler.autoScheduleDay(
            allTasks = _uiState.value.tasks,
            targetDate = date,
            settings = _uiState.value.settings
        )
        persistTasks(scheduled)
        _uiState.update { it.copy(toastMessage = "Đã tự động xếp giờ các việc linh hoạt") }
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
            _uiState.update { it.copy(toastMessage = "Không có việc chưa xong từ hôm qua") }
        }
    }

    fun updateAiInput(text: String) {
        _uiState.update { it.copy(aiInputText = text) }
    }

    fun analyzeNoteWithAi(targetDate: LocalDate = _uiState.value.selectedDate) {
        val text = _uiState.value.aiInputText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Hãy nhập ghi chú công việc trước") }
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
            _uiState.update { it.copy(toastMessage = "Chưa chọn công việc nào để lưu") }
            return
        }
        val newTasks = selected.map { it.toTaskItem() }
        var combined = _uiState.value.tasks + newTasks

        if (autoScheduleFlexible) {
            val affectedDates = newTasks.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.distinct()
            for (d in affectedDates) {
                combined = SmartScheduler.autoScheduleDay(combined, d, _uiState.value.settings)
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
            _uiState.update { it.copy(toastMessage = "JSON sao lưu không hợp lệ") }
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
            settings.lockScreenSummaryEnabled
        )
        LichViecWidgetProvider.updateAllWidgets(app)
    }
}
