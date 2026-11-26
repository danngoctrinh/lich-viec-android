package com.lichviec.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.ui.components.AddEditTaskDialog
import com.lichviec.app.ui.components.PomodoroDialog
import com.lichviec.app.ui.screens.AiNoteScreen
import com.lichviec.app.ui.screens.StatsSettingsScreen
import com.lichviec.app.ui.screens.TodayScreen
import com.lichviec.app.ui.screens.WeekScreen
import com.lichviec.app.ui.theme.CanvasBlack
import com.lichviec.app.ui.theme.InkMuted
import com.lichviec.app.ui.theme.InkWhite
import com.lichviec.app.ui.theme.RailDark
import com.lichviec.app.ui.theme.RuleGray
import com.lichviec.app.ui.theme.SurfaceMuted
import com.lichviec.app.viewmodel.MainViewModel

@Composable
fun MainApp(
    launchTab: Int = 0,
    launchOpenAddDialog: Boolean = false,
    launchEventCounter: Int = 0,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(launchTab.coerceIn(0, 3)) }
    var showAddDialog by remember { mutableStateOf(launchOpenAddDialog) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var pomodoroTask by remember { mutableStateOf<TaskItem?>(null) }

    LaunchedEffect(launchEventCounter) {
        if (launchEventCounter > 0) {
            selectedTab = launchTab.coerceIn(0, 3)
            if (launchOpenAddDialog) {
                editingTask = null
                showAddDialog = true
            }
        }
    }

    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = InkWhite,
        selectedTextColor = InkWhite,
        indicatorColor = SurfaceMuted,
        unselectedIconColor = InkMuted,
        unselectedTextColor = InkMuted
    )

    Scaffold(
        containerColor = CanvasBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingTask = null
                        showAddDialog = true
                    },
                    containerColor = InkWhite,
                    contentColor = CanvasBlack
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Thêm công việc")
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 0.5.dp, color = RuleGray)
                NavigationBar(containerColor = RailDark, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Rounded.Today, contentDescription = null) },
                        label = { Text("Ngày") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
                        label = { Text("Tuần") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                        label = { Text("Ghi chú") },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                        label = { Text("Thống kê") },
                        colors = navItemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TodayScreen(
                    selectedDate = state.selectedDate,
                    allDayTasks = viewModel.tasksForDate(state.selectedDate),
                    statusFilter = state.statusFilter,
                    onSelectDate = { viewModel.selectDate(it) },
                    onSelectFilter = { viewModel.setStatusFilter(it) },
                    onAutoSchedule = { viewModel.autoScheduleSelectedDay() },
                    onCarryOverYesterday = { viewModel.carryOverUnfinishedFromYesterday() },
                    onCycleStatus = { viewModel.cycleTaskStatus(it.id, state.selectedDate) },
                    onMarkDone = { viewModel.setTaskStatus(it.id, state.selectedDate, TaskStatus.DONE) },
                    onEditTask = {
                        editingTask = it
                        showAddDialog = true
                    },
                    onStartPomodoro = { pomodoroTask = it },
                    onDeleteTask = { viewModel.deleteTask(it.id) }
                )

                1 -> WeekScreen(
                    selectedDate = state.selectedDate,
                    weekDates = viewModel.weekDatesAround(state.selectedDate),
                    tasksProvider = { viewModel.tasksForDate(it) },
                    onSelectDate = { viewModel.selectDate(it) },
                    onCycleStatus = { task, date -> viewModel.cycleTaskStatus(task.id, date) },
                    onEditTask = {
                        editingTask = it
                        showAddDialog = true
                    },
                    onStartPomodoro = { pomodoroTask = it },
                    onDeleteTask = { viewModel.deleteTask(it.id) }
                )

                2 -> AiNoteScreen(
                    selectedDate = state.selectedDate,
                    inputText = state.aiInputText,
                    drafts = state.aiDrafts,
                    isLoading = state.isAiLoading,
                    statusMessage = state.aiStatusMessage,
                    hasApiKey = state.settings.geminiApiKey.isNotBlank(),
                    onInputChange = { viewModel.updateAiInput(it) },
                    onAnalyze = { viewModel.analyzeNoteWithAi(state.selectedDate) },
                    onToggleSelect = { viewModel.toggleDraftSelection(it) },
                    onToggleScheduleType = { viewModel.toggleDraftScheduleType(it) },
                    onToggleRepeatDaily = { viewModel.toggleDraftRepeatDaily(it) },
                    onApplyDrafts = {
                        viewModel.applySelectedAiDrafts(autoScheduleFlexible = true)
                        selectedTab = 0
                    }
                )

                3 -> StatsSettingsScreen(
                    tasksProvider = { viewModel.tasksForDate(it) },
                    dailyStreak = viewModel.computeDailyHabitStreak(),
                    settings = state.settings,
                    onSaveSettings = { viewModel.saveSettings(it) },
                    onExportJson = { viewModel.exportBackupJson() },
                    onImportJson = { viewModel.importBackupJson(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddEditTaskDialog(
            initialTask = editingTask,
            defaultDate = state.selectedDate,
            onDismiss = { showAddDialog = false },
            onSave = { task ->
                viewModel.addOrUpdateTask(task)
                showAddDialog = false
            }
        )
    }

    pomodoroTask?.let { task ->
        PomodoroDialog(
            task = task,
            onDismiss = { pomodoroTask = null },
            onMarkDone = {
                viewModel.setTaskStatus(task.id, state.selectedDate, TaskStatus.DONE)
            }
        )
    }
}
