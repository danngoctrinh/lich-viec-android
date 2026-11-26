package com.lichviec.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lichviec.app.data.TaskItem
import com.lichviec.app.ui.theme.BgBlack
import com.lichviec.app.ui.theme.PureWhite
import com.lichviec.app.ui.theme.SurfaceDark
import com.lichviec.app.ui.theme.TextWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onMarkDone: () -> Unit
) {
    val taskMinutes = task.effectiveDurationMinutes().coerceIn(5, 120)
    var selectedPresetMinutes by remember { mutableIntStateOf(25) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        } else if (remainingSeconds == 0) {
            isRunning = false
        }
    }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formatted = "%02d:%02d".format(mins, secs)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Tập trung: ${task.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25 to "25p", 45 to "45p", taskMinutes to "${taskMinutes}p (việc)")
                        .distinctBy { it.first }
                        .forEach { (preset, label) ->
                            FilterChip(
                                selected = selectedPresetMinutes == preset,
                                onClick = {
                                    isRunning = false
                                    selectedPresetMinutes = preset
                                    remainingSeconds = preset * 60
                                },
                                label = { Text(label) }
                            )
                        }
                }

                Text(
                    text = formatted,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = BgBlack)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Text(if (isRunning) " Tạm dừng" else " Bắt đầu")
                    }
                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            remainingSeconds = selectedPresetMinutes * 60
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        onMarkDone()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                    Text(" Đánh dấu Đã làm")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = TextWhite)
            }
        }
    )
}
