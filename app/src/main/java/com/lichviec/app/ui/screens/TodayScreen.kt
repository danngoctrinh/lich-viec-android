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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.shortDayName
import com.lichviec.app.domain.VietnameseLunarCalendar
import com.lichviec.app.ui.components.FocusHeroCard
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
    onMarkDone: (TaskItem) -> Unit,
    onToggleSubTask: (String, String) -> Unit,
    onRescheduleOverdue: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onStartPomodoro: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val lunarLabel = VietnameseLunarCalendar.fromSolarDate(selectedDate).shortLabel()

    val todoCount = allDayTasks.count { it.statusOn(selectedDate) == TaskStatus.TODO }
    val upcomingCount = allDayTasks.count { it.statusOn(selectedDate) == TaskStatus.UPCOMING }
    val doneCount = allDayTasks.count { it.statusOn(selectedDate) == TaskStatus.DONE }
    val totalCount = allDayTasks.size
    val progress = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount.toFloat()

    val totalMinutes = allDayTasks.sumOf { it.effectiveDurationMinutes() }
    val fixedCount = allDayTasks.count { it.scheduleType == ScheduleType.FIXED }
    val flexCount = allDayTasks.count { it.scheduleType == ScheduleType.FLEXIBLE }

    val heroTask = if (statusFilter == null) {
        allDayTasks.firstOrNull { it.statusOn(selectedDate) == TaskStatus.UPCOMING }
            ?: allDayTasks.firstOrNull { it.statusOn(selectedDate) == TaskStatus.TODO }
    } else {
        null
    }

    val filteredTasks = if (statusFilter == null) {
        allDayTasks
    } else {
        allDayTasks.filter { it.statusOn(selectedDate) == statusFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 92.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "%02d".format(selectedDate.dayOfMonth),
                        style = MaterialTheme.typography.displaySmall,
                        color = InkWhite
                    )
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = if (isToday) "Hôm nay" else "${shortDayName(selectedDate.dayOfWeek)}, Tháng ${selectedDate.monthValue}",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkWhite
                        )
                        Text(
                            text = "${shortDayName(selectedDate.dayOfWeek)}, Th${selectedDate.monthValue}  |  $lunarLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isToday) {
                        Text(
                            text = "Hôm nay",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkWhite,
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, RuleStrong, CircleShape)
                                .clickable { onSelectDate(LocalDate.now()) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(onClick = { onSelectDate(selectedDate.minusDays(1)) }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Ngày trước", tint = InkSecondary)
                    }
                    IconButton(onClick = { onSelectDate(selectedDate.plusDays(1)) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Ngày sau", tint = InkSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đã xong $doneCount / $totalCount việc",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkSoftWhite
                )
                Text(
                    text = "${totalMinutes / 60}g ${totalMinutes % 60}p ($fixedCount cố định, $flexCount linh hoạt)",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = InkWhite,
                trackColor = SurfaceMuted
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (heroTask != null) {
            item {
                FocusHeroCard(
                    task = heroTask,
                    date = selectedDate,
                    onStartPomodoro = { onStartPomodoro(heroTask) },
                    onMarkDone = { onMarkDone(heroTask) },
                    onReschedule = { onRescheduleOverdue(heroTask) },
                    onEdit = { onEditTask(heroTask) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceMuted)
                        .border(1.dp, RuleGray, RoundedCornerShape(12.dp))
                        .clickable { onAutoSchedule() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = InkWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "  Xếp giờ tự động",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkWhite
                    )
                }

                if (isToday) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, RuleGray, RoundedCornerShape(12.dp))
                            .clickable { onCarryOverYesterday() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = null,
                            tint = InkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " Việc hôm qua",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    PillFilter(
                        label = "Tất cả $totalCount",
                        selected = statusFilter == null,
                        onClick = { onSelectFilter(null) }
                    )
                }
                items(TaskStatus.entries) { status ->
                    val count = when (status) {
                        TaskStatus.TODO -> todoCount
                        TaskStatus.UPCOMING -> upcomingCount
                        TaskStatus.DONE -> doneCount
                    }
                    val selected = statusFilter == status
                    PillFilter(
                        label = "${status.label} $count",
                        selected = selected,
                        onClick = { onSelectFilter(if (selected) null else status) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = RuleGray)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (filteredTasks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lịch đang trống",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkSoftWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Chạm nút + để thêm việc hoặc mở Ghi chú để nói/nhập chia giờ tự động.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted
                    )
                }
            }
        } else {
            itemsIndexed(filteredTasks, key = { _, item -> item.id }) { index, task ->
                TaskCard(
                    task = task,
                    date = selectedDate,
                    onCycleStatus = { onCycleStatus(task) },
                    onEdit = { onEditTask(task) },
                    onStartPomodoro = { onStartPomodoro(task) },
                    onDelete = { onDeleteTask(task) },
                    onToggleSubTask = { subId -> onToggleSubTask(task.id, subId) },
                    onRescheduleOverdue = { onRescheduleOverdue(task) },
                    isLast = index == filteredTasks.lastIndex
                )
            }
        }
    }
}

@Composable
private fun PillFilter(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) InkWhite else CanvasBlack)
            .border(
                width = 1.dp,
                color = if (selected) InkWhite else RuleGray,
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) CanvasBlack else InkSecondary
        )
    }
}
