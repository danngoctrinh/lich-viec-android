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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.lichviec.app.ui.theme.BgBlack
import com.lichviec.app.ui.theme.BorderGray
import com.lichviec.app.ui.theme.PureWhite
import com.lichviec.app.ui.theme.SurfaceDark
import com.lichviec.app.ui.theme.TextGray
import com.lichviec.app.ui.theme.TextMuted
import com.lichviec.app.ui.theme.TextWhite
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
            .background(BgBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onSelectDate(selectedDate.minusWeeks(1)) }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Tuần trước", tint = TextWhite)
            }
            Text(
                text = "Tuần ${weekStart.format(shortFmt)} – ${weekEnd.format(shortFmt)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            IconButton(onClick = { onSelectDate(selectedDate.plusWeeks(1)) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Tuần sau", tint = TextWhite)
            }
        }

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
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (isSelected) PureWhite else SurfaceDark)
                        .border(
                            width = if (isToday && !isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> PureWhite
                                isToday -> PureWhite.copy(alpha = 0.7f)
                                else -> BorderGray
                            },
                            shape = RoundedCornerShape(13.dp)
                        )
                        .clickable { onSelectDate(d) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = shortDayName(d.dayOfWeek),
                        fontSize = 11.sp,
                        color = if (isSelected) BgBlack.copy(alpha = 0.7f) else TextGray
                    )
                    Text(
                        text = "${d.dayOfMonth}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BgBlack else PureWhite
                    )
                    Text(
                        text = if (dayTasks.isEmpty()) "-" else "$done/${dayTasks.size}",
                        fontSize = 10.sp,
                        color = when {
                            isSelected -> BgBlack
                            dayTasks.isNotEmpty() && done == dayTasks.size -> PureWhite
                            else -> TextMuted
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                .padding(13.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WeekMetric("Hoàn thành", "$weekDoneTasks/$weekTotalTasks")
            WeekMetric("Giờ cố định", "${weekFixedMinutes / 60}h${weekFixedMinutes % 60}p")
            WeekMetric("Giờ linh hoạt", "${weekFlexMinutes / 60}h${weekFlexMinutes % 60}p")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Lịch ${shortDayName(selectedDate.dayOfWeek)}, ${selectedDate.format(shortFmt)} (${selectedTasks.size} việc)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Ngày này đang trống lịch.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                items(selectedTasks, key = { "${selectedDate}_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        date = selectedDate,
                        onCycleStatus = { onCycleStatus(task, selectedDate) },
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
private fun WeekMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = PureWhite, style = MaterialTheme.typography.bodyMedium)
        Text(label, color = TextGray, style = MaterialTheme.typography.bodySmall)
    }
}
