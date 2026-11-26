package com.lichviec.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lichviec.app.data.AiParsedTaskDraft
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.formatMinute
import com.lichviec.app.ui.theme.BgBlack
import com.lichviec.app.ui.theme.BorderBright
import com.lichviec.app.ui.theme.BorderGray
import com.lichviec.app.ui.theme.PureWhite
import com.lichviec.app.ui.theme.SurfaceDark
import com.lichviec.app.ui.theme.SurfaceElevated
import com.lichviec.app.ui.theme.TextGray
import com.lichviec.app.ui.theme.TextWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiNoteScreen(
    selectedDate: LocalDate,
    inputText: String,
    drafts: List<AiParsedTaskDraft>,
    isLoading: Boolean,
    statusMessage: String,
    hasApiKey: Boolean,
    onInputChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onToggleScheduleType: (String) -> Unit,
    onToggleRepeatDaily: (String) -> Unit,
    onApplyDrafts: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Ghi chú → Tự chia lịch",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Text(
                text = if (hasApiKey) "Gemini AI" else "Offline",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasApiKey) PureWhite else TextGray,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderBright, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ngày mặc định: ${selectedDate.format(dateFmt)}. Gõ tự nhiên các việc cố định, linh hoạt, lặp mỗi ngày.",
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp),
            shape = RoundedCornerShape(14.dp),
            placeholder = {
                Text(
                    "VD: 8h-9h30 học tiếng Anh cố định; làm báo cáo 90 phút ưu tiên cao; chiều tập gym 1 tiếng; 20h họp nhóm 45 phút; đọc sách 30 phút mỗi ngày",
                    color = TextGray
                )
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAnalyze,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = BgBlack)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BgBlack,
                        strokeWidth = 2.dp
                    )
                    Text(" Đang chia lịch...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Phân tích & chia giờ", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    onInputChange(
                        "8h-9h30 học tiếng Anh cố định\n" +
                            "Làm báo cáo công việc 90 phút ưu tiên cao buổi sáng\n" +
                            "Chiều đi tập gym 1 tiếng mỗi ngày\n" +
                            "20h họp nhóm 45 phút cố định\n" +
                            "Đọc sách 30 phút buổi tối mỗi ngày"
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Mẫu thử", color = TextWhite)
            }
        }

        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (drafts.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(drafts, key = { it.id }) { draft ->
                    DraftTaskCard(
                        draft = draft,
                        onToggleSelect = { onToggleSelect(draft.id) },
                        onToggleScheduleType = { onToggleScheduleType(draft.id) },
                        onToggleRepeatDaily = { onToggleRepeatDaily(draft.id) }
                    )
                }
            }

            Button(
                onClick = onApplyDrafts,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = BgBlack)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Lưu ${drafts.count { it.selected }} việc vào lịch & tự xếp khe trống", fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftTaskCard(
    draft: AiParsedTaskDraft,
    onToggleSelect: () -> Unit,
    onToggleScheduleType: () -> Unit,
    onToggleRepeatDaily: () -> Unit
) {
    val timeLabel = if (draft.startMinute != null && draft.endMinute != null) {
        "${formatMinute(draft.startMinute)} - ${formatMinute(draft.endMinute)}"
    } else {
        "Linh hoạt (${draft.durationMinutes}p)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.selected, onCheckedChange = { onToggleSelect() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
                Text(
                    text = "$timeLabel • Ngày ${draft.date} • Ưu tiên: ${draft.priority.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = draft.scheduleType == ScheduleType.FIXED,
                onClick = onToggleScheduleType,
                label = { Text(draft.scheduleType.label) }
            )
            FilterChip(
                selected = draft.repeatType == RepeatType.DAILY,
                onClick = onToggleRepeatDaily,
                label = { Text(if (draft.repeatType == RepeatType.DAILY) "Mỗi ngày" else "1 lần") }
            )
        }
    }
}
