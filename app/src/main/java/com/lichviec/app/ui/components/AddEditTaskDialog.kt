package com.lichviec.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskPriority
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.TimePreference
import com.lichviec.app.data.formatMinute
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskDialog(
    initialTask: TaskItem?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var note by remember { mutableStateOf(initialTask?.note ?: "") }
    var dateText by remember { mutableStateOf(initialTask?.date ?: defaultDate.toString()) }
    var scheduleType by remember { mutableStateOf(initialTask?.scheduleType ?: ScheduleType.FLEXIBLE) }
    var startText by remember { mutableStateOf(initialTask?.startMinute?.let { formatMinute(it) } ?: "09:00") }
    var endText by remember { mutableStateOf(initialTask?.endMinute?.let { formatMinute(it) } ?: "10:00") }
    var durationText by remember { mutableStateOf((initialTask?.durationMinutes ?: 45).toString()) }
    var timePreference by remember { mutableStateOf(initialTask?.timePreference ?: TimePreference.ANY) }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var repeatType by remember { mutableStateOf(initialTask?.repeatType ?: RepeatType.NONE) }
    var repeatDays by remember { mutableStateOf(initialTask?.repeatDays ?: setOf(1, 3, 5)) }
    var status by remember { mutableStateOf(initialTask?.statusOn(defaultDate) ?: TaskStatus.TODO) }
    var reminderEnabled by remember { mutableStateOf(initialTask?.reminderEnabled ?: true) }
    var category by remember { mutableStateOf(initialTask?.category ?: "Công việc") }

    val dayLabels = listOf(1 to "T2", 2 to "T3", 3 to "T4", 4 to "T5", 5 to "T6", 6 to "T7", 7 to "CN")
    val categories = listOf("Công việc", "Học tập", "Sức khỏe", "Cá nhân")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTask == null) "Thêm công việc" else "Sửa công việc",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên công việc *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Ngày bắt đầu (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Loại giờ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleType.entries.forEach { type ->
                        FilterChip(
                            selected = scheduleType == type,
                            onClick = { scheduleType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                if (scheduleType == ScheduleType.FIXED) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startText,
                            onValueChange = { startText = it },
                            label = { Text("Bắt đầu (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endText,
                            onValueChange = { endText = it },
                            label = { Text("Kết thúc (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                        label = { Text("Thời lượng dự kiến (phút)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Ưu tiên buổi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TimePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = timePreference == pref,
                                onClick = { timePreference = pref },
                                label = { Text(pref.label) }
                            )
                        }
                    }
                }

                Text("Trạng thái", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                Text("Lặp lại", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatType.entries.forEach { rp ->
                        FilterChip(
                            selected = repeatType == rp,
                            onClick = { repeatType = rp },
                            label = { Text(rp.label) }
                        )
                    }
                }

                if (repeatType == RepeatType.WEEKLY_DAYS) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dayLabels.forEach { (num, lbl) ->
                            val selected = repeatDays.contains(num)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    repeatDays = if (selected && repeatDays.size > 1) {
                                        repeatDays - num
                                    } else {
                                        repeatDays + num
                                    }
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }

                Text("Mức ưu tiên", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                Text("Nhóm việc", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Thông báo nhắc trước giờ", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty(),
                onClick = {
                    val safeDate = runCatching { LocalDate.parse(dateText.trim()).toString() }
                        .getOrDefault(defaultDate.toString())

                    val parsedStart = if (scheduleType == ScheduleType.FIXED) parseHmToMinutes(startText) else initialTask?.startMinute
                    val parsedEnd = if (scheduleType == ScheduleType.FIXED) parseHmToMinutes(endText) else initialTask?.endMinute
                    val validDuration = if (scheduleType == ScheduleType.FIXED && parsedStart != null && parsedEnd != null && parsedEnd > parsedStart) {
                        parsedEnd - parsedStart
                    } else {
                        durationText.toIntOrNull()?.coerceIn(10, 480) ?: 45
                    }

                    val finalEnd = if (scheduleType == ScheduleType.FIXED && parsedStart != null) {
                        if (parsedEnd != null && parsedEnd > parsedStart) parsedEnd else (parsedStart + validDuration).coerceAtMost(23 * 60 + 59)
                    } else {
                        parsedEnd
                    }

                    val base = initialTask ?: TaskItem(title = title.trim(), date = safeDate)
                    val updated = base.copy(
                        title = title.trim(),
                        note = note.trim(),
                        date = safeDate,
                        scheduleType = scheduleType,
                        startMinute = if (scheduleType == ScheduleType.FIXED) parsedStart else base.startMinute,
                        endMinute = if (scheduleType == ScheduleType.FIXED) finalEnd else base.endMinute,
                        durationMinutes = validDuration,
                        timePreference = timePreference,
                        priority = priority,
                        repeatType = repeatType,
                        repeatDays = if (repeatType == RepeatType.WEEKLY_DAYS) repeatDays else emptySet(),
                        reminderEnabled = reminderEnabled,
                        category = category
                    ).withStatusOn(defaultDate, status)

                    onSave(updated)
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

private fun parseHmToMinutes(input: String): Int? {
    val parts = input.trim().replace('h', ':').replace('H', ':').split(':')
    if (parts.isEmpty()) return null
    val h = parts[0].trim().toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
