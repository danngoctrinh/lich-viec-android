package com.lichviec.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.lichviec.app.data.formatMinute
import com.lichviec.app.ui.theme.CanvasBlack
import com.lichviec.app.ui.theme.InkMuted
import com.lichviec.app.ui.theme.InkSecondary
import com.lichviec.app.ui.theme.InkSoftWhite
import com.lichviec.app.ui.theme.InkWhite
import com.lichviec.app.ui.theme.RuleGray
import com.lichviec.app.ui.theme.RuleStrong
import com.lichviec.app.ui.theme.SurfaceMuted
import java.time.LocalDate

/**
 * Một điểm nhấn duy nhất (Hero Focus Card) nền trắng tinh khiết, chữ đen tuyền
 * cho công việc quan trọng / sắp làm ngay lúc này.
 */
@Composable
fun FocusHeroCard(
    task: TaskItem,
    date: LocalDate,
    onStartPomodoro: () -> Unit,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = task.statusOn(date)
    val timeDisplay = if (task.startMinute != null && task.endMinute != null) {
        "${formatMinute(task.startMinute)} – ${formatMinute(task.endMinute)}"
    } else {
        "Linh hoạt ${task.durationMinutes} phút"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(InkWhite)
            .clickable { onEdit() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (status == TaskStatus.UPCOMING) "Sắp làm ngay" else "Việc tiếp theo",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF52525B)
            )
            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = CanvasBlack
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineSmall,
            color = CanvasBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (task.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.note,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF52525B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CanvasBlack)
                    .clickable { onStartPomodoro() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tập trung ngay",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkWhite
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFD4D4D8), RoundedCornerShape(12.dp))
                    .clickable { onMarkDone() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Đã xong",
                    style = MaterialTheme.typography.labelLarge,
                    color = CanvasBlack
                )
            }
        }
    }
}

/**
 * Hàng công việc theo bố cục trục thời gian (Timeline) tối giản, bỏ khung thẻ lặp lại.
 * - Cột trái: Giờ bắt đầu / kết thúc (hoặc thời lượng) dùng số thẳng hàng.
 * - Trục giữa: Đường kẻ dọc liên tục, nút trạng thái (vuông đặc = cố định, tròn = linh hoạt).
 * - Cột phải: Nội dung sạch, thoáng, tương phản rõ theo trạng thái.
 */
@Composable
fun TaskCard(
    task: TaskItem,
    date: LocalDate,
    onCycleStatus: () -> Unit,
    onEdit: () -> Unit,
    onStartPomodoro: () -> Unit,
    onDelete: () -> Unit,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val status = task.statusOn(date)
    val isDone = status == TaskStatus.DONE
    val isUpcoming = status == TaskStatus.UPCOMING
    val isFixed = task.scheduleType == ScheduleType.FIXED

    val titleColor by animateColorAsState(
        targetValue = when {
            isDone -> InkMuted
            isUpcoming -> InkWhite
            else -> InkSoftWhite
        },
        label = "titleColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable { onEdit() }
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Cột thời gian bên trái
            Column(
                modifier = Modifier.width(54.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (task.startMinute != null) {
                    Text(
                        text = formatMinute(task.startMinute),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isFixed && !isDone) FontWeight.SemiBold else FontWeight.Normal,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (isDone) InkMuted else if (isFixed) InkWhite else InkSecondary
                    )
                    if (task.endMinute != null) {
                        Text(
                            text = formatMinute(task.endMinute),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = InkMuted
                        )
                    }
                } else {
                    Text(
                        text = "${task.durationMinutes}p",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = if (isDone) InkMuted else InkSecondary
                    )
                    Text(
                        text = "Linh hoạt",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                }
            }

            // Trục thời gian dọc + nút đổi trạng thái 1 chạm
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val nodeShape = if (isFixed) RoundedCornerShape(5.dp) else CircleShape
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(nodeShape)
                        .background(
                            when {
                                isDone -> SurfaceMuted
                                isUpcoming -> InkWhite
                                else -> CanvasBlack
                            }
                        )
                        .border(
                            width = if (isFixed && !isDone) 1.6.dp else 1.2.dp,
                            color = when {
                                isDone -> RuleGray
                                isUpcoming -> InkWhite
                                isFixed -> InkWhite
                                else -> RuleStrong
                            },
                            shape = nodeShape
                        )
                        .clickable { onCycleStatus() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Đã làm",
                            tint = InkSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (isUpcoming) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CanvasBlack)
                        )
                    }
                }

                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(1.dp)
                            .weight(1f)
                            .background(RuleGray)
                    )
                }
            }

            // Nội dung công việc bên phải
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isUpcoming) FontWeight.SemiBold else FontWeight.Medium,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (task.note.isNotBlank() && !isDone) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isFixed) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = if (isFixed) "Cố định" else "Linh hoạt",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted
                        )
                    }

                    if (status == TaskStatus.UPCOMING) {
                        Text(
                            text = "Sắp làm",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = InkWhite
                        )
                    }

                    if (task.priority == TaskPriority.HIGH && !isDone) {
                        Text(
                            text = "Ưu tiên cao",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSoftWhite
                        )
                    }

                    if (task.repeatType != RepeatType.NONE) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = InkMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = task.repeatType.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkMuted
                            )
                        }
                    }
                }
            }

            // Nút thao tác gọn bên phải
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isDone) {
                    IconButton(
                        onClick = onStartPomodoro,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Tập trung",
                            tint = InkSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Xóa",
                        tint = InkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                thickness = 0.5.dp,
                color = RuleGray.copy(alpha = 0.65f)
            )
        }
    }
}
