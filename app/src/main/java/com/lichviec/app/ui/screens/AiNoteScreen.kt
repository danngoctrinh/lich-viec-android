package com.lichviec.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.lichviec.app.ui.theme.CanvasBlack
import com.lichviec.app.ui.theme.InkMuted
import com.lichviec.app.ui.theme.InkSecondary
import com.lichviec.app.ui.theme.InkWhite
import com.lichviec.app.ui.theme.RuleGray
import com.lichviec.app.ui.theme.RuleStrong
import com.lichviec.app.ui.theme.SurfaceMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AiNoteScreen(
    selectedDate: LocalDate,
    inputText: String,
    drafts: List<AiParsedTaskDraft>,
    isLoading: Boolean,
    statusMessage: String,
    hasApiKey: Boolean,
    onInputChange: (String) -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
    onAnalyze: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onToggleScheduleType: (String) -> Unit,
    onToggleRepeatDaily: (String) -> Unit,
    onApplyDrafts: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                onVoiceResult(spoken)
            }
        }
    }

    fun launchVietnameseVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói công việc và khung giờ (VD: 8h học tiếng Anh cố định)")
        }
        runCatching { voiceLauncher.launch(intent) }.onFailure { onVoiceUnavailable() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBlack)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ghi chú thông minh",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkWhite
                )
                Text(
                    text = "Gõ hoặc nói tiếng Việt cho ngày ${selectedDate.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            Text(
                text = if (hasApiKey) "Gemini" else "Offline",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasApiKey) InkWhite else InkSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, if (hasApiKey) RuleStrong else RuleGray, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp),
            shape = RoundedCornerShape(14.dp),
            placeholder = {
                Text(
                    "Gõ hoặc bấm Nói: 8h-9h30 học tiếng Anh cố định; làm báo cáo 90 phút ưu tiên cao; chiều tập gym 1 tiếng mỗi ngày; 20h họp nhóm 45 phút",
                    color = InkMuted
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
                colors = ButtonDefaults.buttonColors(containerColor = InkWhite, contentColor = CanvasBlack)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CanvasBlack,
                        strokeWidth = 2.dp
                    )
                    Text("  Đang tách...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text("  Tách lịch", fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = { launchVietnameseVoiceInput() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = "Nói tiếng Việt", tint = InkWhite, modifier = Modifier.size(17.dp))
                Text(" Nói", color = InkWhite)
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
                Text("Mẫu", color = InkSecondary)
            }
        }

        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = InkWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceMuted)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (drafts.isNotEmpty()) {
            HorizontalDivider(thickness = 0.5.dp, color = RuleGray)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                itemsIndexed(drafts, key = { _, item -> item.id }) { index, draft ->
                    DraftTaskRow(
                        draft = draft,
                        isLast = index == drafts.lastIndex,
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
                    .padding(bottom = 14.dp, top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InkWhite, contentColor = CanvasBlack)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    "  Lưu ${drafts.count { it.selected }} việc vào lịch",
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DraftTaskRow(
    draft: AiParsedTaskDraft,
    isLast: Boolean,
    onToggleSelect: () -> Unit,
    onToggleScheduleType: () -> Unit,
    onToggleRepeatDaily: () -> Unit
) {
    val timeLabel = if (draft.startMinute != null && draft.endMinute != null) {
        "${formatMinute(draft.startMinute)} – ${formatMinute(draft.endMinute)}"
    } else {
        "${draft.durationMinutes} phút (tự xếp)"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(checked = draft.selected, onCheckedChange = { onToggleSelect() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = InkWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$timeLabel  |  ${draft.date}  |  Ưu tiên ${draft.priority.label.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
                if (draft.subtasks.isNotEmpty()) {
                    Text(
                        text = "${draft.subtasks.size} việc con",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTogglePill(
                        text = draft.scheduleType.label,
                        active = draft.scheduleType == ScheduleType.FIXED,
                        onClick = onToggleScheduleType
                    )
                    SmallTogglePill(
                        text = draft.repeatType.label,
                        active = draft.repeatType != RepeatType.NONE,
                        onClick = onToggleRepeatDaily
                    )
                }
            }
        }
        if (!isLast) {
            HorizontalDivider(thickness = 0.5.dp, color = RuleGray)
        }
    }
}

@Composable
private fun SmallTogglePill(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) InkWhite else CanvasBlack)
            .border(1.dp, if (active) InkWhite else RuleGray, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (active) CanvasBlack else InkSecondary
        )
    }
}
