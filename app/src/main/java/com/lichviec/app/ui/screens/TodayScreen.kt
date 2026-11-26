package com.lichviec.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.shortDayName
import com.lichviec.app.ui.components.TaskCard
import com.lichviec.app.ui.theme.BgBlack
import com.lichviec.app.ui.theme.BorderBright
import com.lichviec.app.ui.theme.BorderGray
import com.lichviec.app.ui.theme.PureWhite
import com.lichviec.app.ui.theme.SurfaceDark
import com.lichviec.app.ui.theme.SurfaceElevated
import com.lichviec.app.ui.theme.TextGray
import com.lichviec.app.ui.theme.TextMuted
import com.lichviec.app.ui.theme.TextWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    selectedDate: LocalDate,
    allDayTasks: List<TaskItem>,
    statusFilter: TaskStatus?,
    onSelectDate: (LocalDate) -> Unit,
    onSelectFilter: (TaskStatus?) -> Unit,
    onAutoSchedule: () -> Unit,
    onCarryOverYesterday: () -> Unit,
    onCycleStatus: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onStartPomodoro: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val isToday = selectedDate == LocalDate.now()

    val todoCount = allDayTasks.count { it.statusOn(selectedDate) == TaskStatus.TODO }
    val upcomingCount = allDayTasks.count { it.statusOn(selectedDate) == TaskStatus.UPCOMING }
    val doneCount = allDayTasks.count { it.statusOn(selectedDate) == TaskStatus.DONE }
    val totalCount = allDayTasks.size
    val progress = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount.toFloat()

    val totalMinutes = allDayTasks.sumOf { it.effectiveDurationMinutes() }
    val fixedCount = allDayTasks.count { it.scheduleType == ScheduleType.FIXED }
    val flexCount = allDayTasks.count { it.scheduleType == ScheduleType.FLEXIBLE }

    val filteredTasks = if (statusFilter == null) {
        allDayTasks
    } else {
        allDayTasks.filter { it.statusOn(selectedDate) == statusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onSelectDate(selectedDate.minusDays(1)) }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Ngày trước", tint = TextWhite)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isToday) "Hôm nay (${shortDayName(selectedDate.dayOfWeek)})" else "${shortDayName(selectedDate.dayOfWeek)}, ${selectedDate.format(formatter)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                if (isToday) {
                    Text(
                        text = selectedDate.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                } else {
                    TextButton(
                        onClick = { onSelectDate(LocalDate.now()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Về hôm nay", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                    }
                }
            }

            IconButton(onClick = { onSelectDate(selectedDate.plusDays(1)) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Ngày sau", tint = TextWhite)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tiến độ: $doneCount/$totalCount việc",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
                Text(
                    text = "${totalMinutes / 60}h${totalMinutes % 60}p • $fixedCount cố định / $flexCount linh hoạt",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PureWhite,
                trackColor = SurfaceElevated
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMini("Chưa làm", todoCount, TextMuted)
                StatMini("Sắp làm", upcomingCount, PureWhite)
                StatMini("Đã làm", doneCount, TextGray)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAutoSchedule,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = BgBlack
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Tự xếp giờ linh hoạt", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            if (isToday) {
                OutlinedButton(
                    onClick = onCarryOverYesterday,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextWhite)
                    Text(" Dồn việc cũ", style = MaterialTheme.typography.labelMedium, color = TextWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceDark,
            labelColor = TextGray,
            selectedContainerColor = PureWhite,
            selectedLabelColor = BgBlack
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("Tất cả ($totalCount)") },
                    colors = chipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = statusFilter == null,
                        borderColor = BorderGray,
                        selectedBorderColor = PureWhite
                    )
                )
            }
            items(TaskStatus.entries) { status ->
                val count = when (status) {
                    TaskStatus.TODO -> todoCount
                    TaskStatus.UPCOMING -> upcomingCount
                    TaskStatus.DONE -> doneCount
                }
                val isSel = statusFilter == status
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectFilter(if (isSel) null else status) },
                    label = { Text("${status.label} ($count)") },
                    colors = chipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSel,
                        borderColor = BorderGray,
                        selectedBorderColor = BorderBright
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có công việc nào.\nBấm + để thêm hoặc dùng tab AI Ghi chú.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        date = selectedDate,
                        onCycleStatus = { onCycleStatus(task) },
                        onEdit = { onEditTask(task) },
                        onStartPomodoro = { onStartPomodoro(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(text = "$count", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
        Text(text = label, color = TextGray, style = MaterialTheme.typography.bodySmall)
    }
}
