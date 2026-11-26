package com.lichviec.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.shortDayName
import com.lichviec.app.ui.components.TaskCard
import com.lichviec.app.ui.theme.CanvasBlack
import com.lichviec.app.ui.theme.InkMuted
import com.lichviec.app.ui.theme.InkSecondary
import com.lichviec.app.ui.theme.InkSoftWhite
import com.lichviec.app.ui.theme.InkWhite
import com.lichviec.app.ui.theme.RuleGray
import com.lichviec.app.ui.theme.RuleStrong
import com.lichviec.app.ui.theme.SurfaceMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeekScreen(
    selectedDate: LocalDate,
    weekDates: List<LocalDate>,
    tasksProvider: (LocalDate) -> List<TaskItem>,
    onSelectDate: (LocalDate) -> Unit,
    onCycleStatus: (TaskItem, LocalDate) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onStartPomodoro: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val shortFmt = DateTimeFormatter.ofPattern("dd/MM")
    val weekStart = weekDates.firstOrNull() ?: selectedDate
    val weekEnd = weekDates.lastOrNull() ?: selectedDate

    var weekTotalTasks = 0
    var weekDoneTasks = 0
    var weekFixedMinutes = 0
    var weekFlexMinutes = 0

    weekDates.forEach { d ->
        val dayList = tasksProvider(d)
        weekTotalTasks += dayList.size
        weekDoneTasks += dayList.count { it.statusOn(d) == TaskStatus.DONE }
        weekFixedMinutes += dayList.filter { it.scheduleType == ScheduleType.FIXED }.sumOf { it.effectiveDurationMinutes() }
        weekFlexMinutes += dayList.filter { it.scheduleType == ScheduleType.FLEXIBLE }.sumOf { it.effectiveDurationMinutes() }
    }

    val selectedTasks = tasksProvider(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Lịch tuần",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkWhite
                )
                Text(
                    text = "${weekStart.format(shortFmt)} – ${weekEnd.format(shortFmt)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = InkSecondary
                )
            }
            Row {
                IconButton(onClick = { onSelectDate(selectedDate.minusWeeks(1)) }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Tuần trước", tint = InkSecondary)
                }
                IconButton(onClick = { onSelectDate(selectedDate.plusWeeks(1)) }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Tuần sau", tint = InkSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Thanh 7 ngày trong tuần gọn gàng với chấm mật độ việc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            weekDates.forEach { d ->
                val dayTasks = tasksProvider(d)
                val done = dayTasks.count { it.statusOn(d) == TaskStatus.DONE }
                val isSelected = d == selectedDate
                val isToday = d == LocalDate.now()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) InkWhite else CanvasBlack)
                        .border(
                            width = if (isToday && !isSelected) 1.2.dp else 0.8.dp,
                            color = when {
                                isSelected -> InkWhite
                                isToday -> RuleStrong
                                else -> RuleGray
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectDate(d) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = shortDayName(d.dayOfWeek),
                        fontSize = 11.sp,
                        color = if (isSelected) CanvasBlack.copy(alpha = 0.65f) else InkMuted
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${d.dayOfMonth}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (isSelected) CanvasBlack else InkWhite
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    dayTasks.isEmpty() -> CanvasBlack.copy(alpha = 0f)
                                    isSelected -> CanvasBlack
                                    done == dayTasks.size -> InkSecondary
                                    else -> InkWhite
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hàng chỉ số tuần phân cách bằng đường kẻ mảnh (không hộp nặng)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceMuted)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WeekStat("Hoàn thành", "$weekDoneTasks/$weekTotalTasks")
            WeekStat("Cố định", "${weekFixedMinutes / 60}g ${weekFixedMinutes % 60}p")
            WeekStat("Linh hoạt", "${weekFlexMinutes / 60}g ${weekFlexMinutes % 60}p")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${shortDayName(selectedDate.dayOfWeek)}, ${selectedDate.format(shortFmt)}",
                style = MaterialTheme.typography.titleMedium,
                color = InkWhite
            )
            Text(
                text = "${selectedTasks.size} việc",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = RuleGray)

        if (selectedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Ngày này chưa có việc nào.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp)
            ) {
                itemsIndexed(selectedTasks, key = { _, task -> "${selectedDate}_${task.id}" }) { index, task ->
                    TaskCard(
                        task = task,
                        date = selectedDate,
                        onCycleStatus = { onCycleStatus(task, selectedDate) },
                        onEdit = { onEditTask(task) },
                        onStartPomodoro = { onStartPomodoro(task) },
                        onDelete = { onDeleteTask(task) },
                        isLast = index == selectedTasks.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = InkWhite
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = InkSecondary
        )
    }
}
