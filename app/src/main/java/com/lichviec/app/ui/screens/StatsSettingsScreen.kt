package com.lichviec.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lichviec.app.data.AppSettings
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskStatus
import com.lichviec.app.data.formatMinute
import com.lichviec.app.ui.theme.BgBlack
import com.lichviec.app.ui.theme.BorderGray
import com.lichviec.app.ui.theme.PureWhite
import com.lichviec.app.ui.theme.SurfaceDark
import com.lichviec.app.ui.theme.TextGray
import com.lichviec.app.ui.theme.TextWhite
import java.time.LocalDate

@Composable
fun StatsSettingsScreen(
    tasksProvider: (LocalDate) -> List<TaskItem>,
    dailyStreak: Int,
    settings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current

    var apiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var workStartText by remember(settings.workStartMinute) { mutableStateOf(formatMinute(settings.workStartMinute)) }
    var workEndText by remember(settings.workEndMinute) { mutableStateOf(formatMinute(settings.workEndMinute)) }
    var breakText by remember(settings.breakMinutes) { mutableStateOf(settings.breakMinutes.toString()) }
    var reminderText by remember(settings.reminderBeforeMinutes) { mutableStateOf(settings.reminderBeforeMinutes.toString()) }
    var lockScreenEnabled by remember(settings.lockScreenSummaryEnabled) { mutableStateOf(settings.lockScreenSummaryEnabled) }
    var importText by remember { mutableStateOf("") }

    val today = LocalDate.now()
    var last7Total = 0
    var last7Done = 0
    var last7FixedMin = 0
    var last7FlexMin = 0
    for (i in 0..6) {
        val d = today.minusDays(i.toLong())
        val dayTasks = tasksProvider(d)
        last7Total += dayTasks.size
        last7Done += dayTasks.count { it.statusOn(d) == TaskStatus.DONE }
        last7FixedMin += dayTasks.filter { it.scheduleType == ScheduleType.FIXED }.sumOf { it.effectiveDurationMinutes() }
        last7FlexMin += dayTasks.filter { it.scheduleType == ScheduleType.FLEXIBLE }.sumOf { it.effectiveDurationMinutes() }
    }
    val rate = if (last7Total == 0) 0 else (last7Done * 100 / last7Total)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Thống kê 7 ngày gần nhất",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KpiBox("Tỷ lệ xong", "$rate%", PureWhite)
            KpiBox("Đã làm", "$last7Done/$last7Total", TextWhite)
            KpiBox("Chuỗi mỗi ngày", "${dailyStreak}d", PureWhite)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KpiBox("Tổng giờ cố định", "${last7FixedMin / 60}h ${last7FixedMin % 60}p", PureWhite)
            KpiBox("Tổng giờ linh hoạt", "${last7FlexMin / 60}h ${last7FlexMin % 60}p", TextGray)
        }

        HorizontalDivider(color = BorderGray)

        Text(
            text = "Cài đặt AI, Khung giờ & Màn hình khóa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hiện lịch trên màn hình khóa", color = PureWhite, fontWeight = FontWeight.Medium)
                    Text(
                        "Ghim danh sách việc hôm nay kèm nút + Note AI ngay trên màn hình khóa",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                Switch(
                    checked = lockScreenEnabled,
                    onCheckedChange = { lockScreenEnabled = it }
                )
            }

            HorizontalDivider(color = BorderGray)

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key (tùy chọn)") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    OutlinedButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Ẩn" else "Hiện", color = TextWhite)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Lấy API Key miễn phí tại aistudio.google.com. Nếu để trống, app vẫn tự tách ghi chú ở chế độ Offline.",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = workStartText,
                    onValueChange = { workStartText = it },
                    label = { Text("Bắt đầu ngày") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = workEndText,
                    onValueChange = { workEndText = it },
                    label = { Text("Kết thúc ngày") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { breakText = it.filter { c -> c.isDigit() } },
                    label = { Text("Nghỉ đệm (phút)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reminderText,
                    onValueChange = { reminderText = it.filter { c -> c.isDigit() } },
                    label = { Text("Nhắc trước (phút)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val startMin = parseHm(workStartText) ?: settings.workStartMinute
                    val endMin = parseHm(workEndText) ?: settings.workEndMinute
                    onSaveSettings(
                        AppSettings(
                            geminiApiKey = apiKey.trim(),
                            workStartMinute = startMin,
                            workEndMinute = endMin.coerceAtLeast(startMin + 60),
                            breakMinutes = breakText.toIntOrNull()?.coerceIn(0, 60) ?: 10,
                            reminderBeforeMinutes = reminderText.toIntOrNull()?.coerceIn(0, 60) ?: 10,
                            lockScreenSummaryEnabled = lockScreenEnabled
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = BgBlack)
            ) {
                Text("Lưu cài đặt", fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = BorderGray)

        Text(
            text = "Sao lưu & Khôi phục dữ liệu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val json = onExportJson()
                    clipboard.setText(AnnotatedString(json))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sao chép dữ liệu sao lưu (JSON)", color = TextWhite)
            }

            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("Dán JSON sao lưu vào đây để khôi phục") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = importText.isNotBlank(),
                onClick = {
                    onImportJson(importText)
                    importText = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = BgBlack)
            ) {
                Text("Khôi phục dữ liệu", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun KpiBox(label: String, value: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextGray)
    }
}

private fun parseHm(text: String): Int? {
    val parts = text.trim().replace('h', ':').split(':')
    if (parts.isEmpty()) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}
