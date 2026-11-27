package com.lichviec.app.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
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
import com.lichviec.app.domain.DeviceCalendarHelper
import com.lichviec.app.notification.ReminderScheduler
import com.lichviec.app.ui.theme.CanvasBlack
import com.lichviec.app.ui.theme.InkSecondary
import com.lichviec.app.ui.theme.InkSoftWhite
import com.lichviec.app.ui.theme.InkWhite
import com.lichviec.app.ui.theme.RailDark
import com.lichviec.app.ui.theme.RuleGray
import com.lichviec.app.ui.theme.SurfaceMuted
import java.time.LocalDate

@Composable
fun StatsSettingsScreen(
    tasksProvider: (LocalDate) -> List<TaskItem>,
    dailyStreak: Int,
    settings: AppSettings,
    weeklyReviewText: String,
    isWeeklyReviewLoading: Boolean,
    onGenerateWeeklyReview: () -> Unit,
    onSaveSettings: (AppSettings) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current

    var apiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var geminiModel by remember(settings.geminiModel) { mutableStateOf(settings.geminiModel) }
    var showKey by remember { mutableStateOf(false) }
    var workStartText by remember(settings.workStartMinute) { mutableStateOf(formatMinute(settings.workStartMinute)) }
    var workEndText by remember(settings.workEndMinute) { mutableStateOf(formatMinute(settings.workEndMinute)) }
    var breakText by remember(settings.breakMinutes) { mutableStateOf(settings.breakMinutes.toString()) }
    var reminderText by remember(settings.reminderBeforeMinutes) { mutableStateOf(settings.reminderBeforeMinutes.toString()) }
    var lockScreenEnabled by remember(settings.lockScreenSummaryEnabled) { mutableStateOf(settings.lockScreenSummaryEnabled) }
    var hideLockScreenDetails by remember(settings.hideLockScreenDetails) { mutableStateOf(settings.hideLockScreenDetails) }
    var avoidCalendarEvents by remember(settings.avoidDeviceCalendarEvents) { mutableStateOf(settings.avoidDeviceCalendarEvents) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            avoidCalendarEvents = true
            onShowMessage("Đã cấp quyền đọc lịch máy để tự né giờ họp")
        } else {
            avoidCalendarEvents = false
            onShowMessage("Chưa được cấp quyền đọc lịch trên máy")
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = onExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onShowMessage("Đã xuất tệp sao lưu JSON thành công")
            }.onFailure {
                onShowMessage("Không thể ghi tệp sao lưu")
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: ""
            }.onSuccess { raw ->
                if (raw.isNotBlank()) onImportJson(raw)
                else onShowMessage("Tệp sao lưu đang trống")
            }.onFailure {
                onShowMessage("Không thể đọc tệp sao lưu")
            }
        }
    }

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
    val canExactAlarm = ReminderScheduler.canScheduleExact(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Thống kê 7 ngày",
            style = MaterialTheme.typography.headlineSmall,
            color = InkWhite
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RailDark)
                .border(1.dp, RuleGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KpiBox("Tỷ lệ xong", "$rate%", InkWhite)
            KpiBox("Đã làm", "$last7Done/$last7Total", InkSoftWhite)
            KpiBox("Chuỗi ngày", "${dailyStreak}d", InkWhite)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RailDark)
                .border(1.dp, RuleGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KpiBox("Giờ cố định", "${last7FixedMin / 60}g ${last7FixedMin % 60}p", InkWhite)
            KpiBox("Giờ linh hoạt", "${last7FlexMin / 60}g ${last7FlexMin % 60}p", InkSecondary)
        }

        // Tổng kết tuần bằng AI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RailDark)
                .border(1.dp, RuleGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tổng kết tuần bằng AI", style = MaterialTheme.typography.titleMedium, color = InkWhite)
                OutlinedButton(
                    enabled = !isWeeklyReviewLoading,
                    onClick = onGenerateWeeklyReview,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isWeeklyReviewLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = InkWhite)
                        Text("  Đang phân tích", color = InkWhite)
                    } else {
                        Text("Phân tích", color = InkWhite)
                    }
                }
            }
            if (weeklyReviewText.isNotBlank()) {
                Text(
                    text = weeklyReviewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoftWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceMuted)
                        .padding(10.dp)
                )
            } else {
                Text(
                    text = "Nhấn Phân tích để xem khung giờ làm hiệu quả nhất và các việc hay bị tồn lại trong 7 ngày qua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
        }

        HorizontalDivider(color = RuleGray)

        Text(
            text = "Cài đặt hệ thống & AI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkWhite
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RailDark)
                .border(1.dp, RuleGray, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingToggleRow(
                title = "Hiện lịch trên màn hình khóa",
                subtitle = "Ghim nhanh danh sách việc hôm nay kèm nút + Note AI",
                checked = lockScreenEnabled,
                onCheckedChange = { lockScreenEnabled = it }
            )

            SettingToggleRow(
                title = "Ẩn chi tiết trên màn hình khóa & Widget",
                subtitle = "Chỉ hiện số lượng việc còn lại để bảo vệ quyền riêng tư",
                checked = hideLockScreenDetails,
                onCheckedChange = { hideLockScreenDetails = it }
            )

            SettingToggleRow(
                title = "Né lịch họp trên máy (Google Calendar)",
                subtitle = "Chỉ đọc giờ bận từ lịch điện thoại khi xếp giờ tự động",
                checked = avoidCalendarEvents,
                onCheckedChange = { wantEnable ->
                    if (wantEnable) {
                        if (DeviceCalendarHelper.hasCalendarPermission(context)) {
                            avoidCalendarEvents = true
                        } else {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    } else {
                        avoidCalendarEvents = false
                    }
                }
            )

            if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cấp quyền báo thức đúng giờ (Android 12+)", color = InkWhite)
                }
            }

            HorizontalDivider(color = RuleGray)

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key (tùy chọn)") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    OutlinedButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Ẩn" else "Hiện", color = InkWhite)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = geminiModel,
                onValueChange = { geminiModel = it },
                label = { Text("Tên model Gemini") },
                supportingText = { Text("Mặc định: gemini-3.8-flash (dự phòng: gemini-3.5-flash-lite)", color = InkSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
                            geminiModel = geminiModel.trim().ifBlank { "gemini-3.8-flash" },
                            workStartMinute = startMin,
                            workEndMinute = endMin.coerceAtLeast(startMin + 60),
                            breakMinutes = breakText.toIntOrNull()?.coerceIn(0, 60) ?: 10,
                            reminderBeforeMinutes = reminderText.toIntOrNull()?.coerceIn(0, 60) ?: 10,
                            lockScreenSummaryEnabled = lockScreenEnabled,
                            hideLockScreenDetails = hideLockScreenDetails,
                            avoidDeviceCalendarEvents = avoidCalendarEvents
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InkWhite, contentColor = CanvasBlack)
            ) {
                Text("Lưu cài đặt", fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = RuleGray)

        Text(
            text = "Sao lưu & Khôi phục tệp JSON",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkWhite
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { exportFileLauncher.launch("lichviec_backup_${LocalDate.now()}.json") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Xuất tệp JSON", color = InkWhite)
            }
            Button(
                onClick = { importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InkWhite, contentColor = CanvasBlack)
            ) {
                Text("Nhập tệp JSON", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = InkWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun KpiBox(label: String, value: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
    }
}

private fun parseHm(text: String): Int? {
    val parts = text.trim().replace('h', ':').split(':')
    if (parts.isEmpty()) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}
