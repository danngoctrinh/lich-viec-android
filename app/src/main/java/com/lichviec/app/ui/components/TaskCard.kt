package com.lichviec.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskPriority
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.ui.theme.BgBlack
import com.lichviec.app.ui.theme.BorderBright
import com.lichviec.app.ui.theme.BorderGray
import com.lichviec.app.ui.theme.PureWhite
import com.lichviec.app.ui.theme.SurfaceDark
import com.lichviec.app.ui.theme.SurfaceElevated
import com.lichviec.app.ui.theme.TagGrayBg
import com.lichviec.app.ui.theme.TextGray
import com.lichviec.app.ui.theme.TextMuted
import com.lichviec.app.ui.theme.TextWhite
import java.time.LocalDate

@Composable
fun TaskCard(
    task: TaskItem,
    date: LocalDate,
    onCycleStatus: () -> Unit,
    onEdit: () -> Unit,
    onStartPomodoro: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = task.statusOn(date)
    val isDone = status == TaskStatus.DONE

    val statusIconTint = when (status) {
        TaskStatus.TODO -> TextMuted
        TaskStatus.UPCOMING -> PureWhite
        TaskStatus.DONE -> TextGray
    }

    val priorityDotColor = when (task.priority) {
        TaskPriority.HIGH -> PureWhite
        TaskPriority.MEDIUM -> TextGray
        TaskPriority.LOW -> TextMuted
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) BgBlack else SurfaceDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (status == TaskStatus.UPCOMING) 1.5.dp else 1.dp,
                    color = when {
                        status == TaskStatus.UPCOMING -> PureWhite.copy(alpha = 0.7f)
                        isDone -> BorderGray.copy(alpha = 0.5f)
                        else -> BorderGray
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onCycleStatus,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        TaskStatus.DONE -> Icons.Rounded.CheckCircle
                        TaskStatus.UPCOMING -> Icons.Rounded.Schedule
                        TaskStatus.TODO -> Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = status.label,
                    tint = statusIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (task.priority == TaskPriority.HIGH) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(priorityDotColor)
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isDone) TextMuted else TextWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (task.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(9.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MonoTag(
                        text = task.formattedTimeRange(),
                        bg = if (task.scheduleType == ScheduleType.FIXED) PureWhite else SurfaceElevated,
                        fg = if (task.scheduleType == ScheduleType.FIXED) BgBlack else TextWhite,
                        borderColor = if (task.scheduleType == ScheduleType.FIXED) PureWhite else BorderBright,
                        icon = if (task.scheduleType == ScheduleType.FIXED) Icons.Rounded.Lock else Icons.Rounded.SwapHoriz
                    )

                    MonoTag(
                        text = status.label,
                        bg = if (status == TaskStatus.UPCOMING) SurfaceElevated else TagGrayBg,
                        fg = if (status == TaskStatus.UPCOMING) PureWhite else TextGray,
                        borderColor = if (status == TaskStatus.UPCOMING) BorderBright else BorderGray
                    )

                    if (task.repeatType != RepeatType.NONE) {
                        MonoTag(
                            text = task.repeatType.label,
                            bg = TagGrayBg,
                            fg = TextWhite,
                            borderColor = BorderGray,
                            icon = Icons.Rounded.Repeat
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (!isDone) {
                    IconButton(
                        onClick = onStartPomodoro,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Tập trung",
                            tint = TextWhite,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Xóa",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonoTag(
    text: String,
    bg: Color,
    fg: Color,
    borderColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(0.8.dp, borderColor, RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
