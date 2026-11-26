package com.lichviec.app.domain

import com.lichviec.app.data.AiParsedTaskDraft
import com.lichviec.app.data.AppSettings
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskPriority
import com.lichviec.app.data.TimePreference
import com.lichviec.app.data.formatMinute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

data class AiParseResult(
    val drafts: List<AiParsedTaskDraft>,
    val usedGemini: Boolean,
    val message: String
)

object GeminiAiService {

    private val modelCandidates = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash"
    )

    suspend fun parseAndScheduleNote(
        noteText: String,
        targetDate: LocalDate,
        existingTasksOnDate: List<TaskItem>,
        settings: AppSettings
    ): AiParseResult = withContext(Dispatchers.IO) {
        val trimmedKey = settings.geminiApiKey.trim()
        if (trimmedKey.isEmpty()) {
            val offlineDrafts = LocalVietnameseTaskParser.parseNote(noteText, targetDate)
            return@withContext AiParseResult(
                drafts = offlineDrafts,
                usedGemini = false,
                message = "Đã tách ${offlineDrafts.size} việc bằng chế độ Offline (nhập Gemini API Key ở mục Cài đặt để AI chia lịch sâu hơn)."
            )
        }

        val prompt = buildPrompt(noteText, targetDate, existingTasksOnDate, settings)

        var lastError = ""
        for (model in modelCandidates) {
            val responseResult = callGeminiApi(trimmedKey, model, prompt)
            if (responseResult.isSuccess) {
                val rawText = responseResult.getOrNull().orEmpty()
                val parsed = parseGeminiJsonResponse(rawText, targetDate)
                if (parsed.isNotEmpty()) {
                    return@withContext AiParseResult(
                        drafts = parsed,
                        usedGemini = true,
                        message = "AI ($model) đã phân tích và chia ${parsed.size} công việc."
                    )
                }
            } else {
                lastError = responseResult.exceptionOrNull()?.message ?: "Lỗi kết nối"
            }
        }

        val fallback = LocalVietnameseTaskParser.parseNote(noteText, targetDate)
        AiParseResult(
            drafts = fallback,
            usedGemini = false,
            message = "Không gọi được Gemini ($lastError), đã tự động tách ${fallback.size} việc bằng chế độ Offline."
        )
    }

    private fun buildPrompt(
        noteText: String,
        targetDate: LocalDate,
        existingTasksOnDate: List<TaskItem>,
        settings: AppSettings
    ): String {
        val fixedScheduleText = existingTasksOnDate
            .filter { it.startMinute != null && it.endMinute != null }
            .sortedBy { it.startMinute }
            .joinToString("\n") { task ->
                "- ${formatMinute(task.startMinute!!)}-${formatMinute(task.endMinute!!)}: ${task.title} (${task.scheduleType.name})"
            }
            .ifBlank { "(Chưa có lịch cố định nào trong ngày)" }

        return """
Bạn là trợ lý chia lịch công việc cá nhân bằng tiếng Việt.
Ngày mục tiêu mặc định: $targetDate
Khung giờ hoạt động trong ngày: ${formatMinute(settings.workStartMinute)} đến ${formatMinute(settings.workEndMinute)}, nghỉ giữa các việc ${settings.breakMinutes} phút.

Lịch đã có sẵn trong ngày $targetDate (KHÔNG được xếp đè lên các giờ này):
$fixedScheduleText

Ghi chú của người dùng:
"$noteText"

Hãy phân tích ghi chú thành danh sách các công việc cụ thể, phân loại rõ:
- scheduleType: "FIXED" nếu người dùng nêu giờ cụ thể/cố định; "FLEXIBLE" nếu không nêu giờ cố định.
- Với việc "FLEXIBLE", hãy tự động đề xuất startMinute và endMinute hợp lý (tính bằng phút từ 00:00, ví dụ 08:30 = 510) không trùng với lịch đã có và các việc khác.
- repeatType: "DAILY" nếu lặp mỗi ngày, "WEEKLY_DAYS" nếu lặp theo thứ, ngược lại "NONE".
- priority: "HIGH", "MEDIUM", hoặc "LOW".
- timePreference: "MORNING", "AFTERNOON", "EVENING", hoặc "ANY".

CHỈ TRẢ VỀ JSON ARRAY HỢP LỆ, KHÔNG kèm markdown ```json hay giải thích gì thêm.
Mỗi phần tử có đúng các trường sau:
{
  "title": "Tên công việc ngắn gọn",
  "note": "Chi tiết ngắn nếu có",
  "date": "YYYY-MM-DD",
  "scheduleType": "FIXED hoặc FLEXIBLE",
  "startMinute": 540,
  "endMinute": 600,
  "durationMinutes": 60,
  "timePreference": "MORNING | AFTERNOON | EVENING | ANY",
  "priority": "HIGH | MEDIUM | LOW",
  "repeatType": "NONE | DAILY | WEEKLY_DAYS",
  "category": "Công việc | Học tập | Sức khỏe | Cá nhân"
}
        """.trimIndent()
    }

    private fun callGeminiApi(apiKey: String, model: String, prompt: String): Result<String> {
        return runCatching {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", prompt))
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseMimeType", "application/json")
                    }
                )
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()

            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }

            val root = JSONObject(responseText)
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        }
    }

    private fun parseGeminiJsonResponse(raw: String, defaultDate: LocalDate): List<AiParsedTaskDraft> {
        val cleaned = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val startIdx = cleaned.indexOf('[')
        val endIdx = cleaned.lastIndexOf(']')
        if (startIdx == -1 || endIdx <= startIdx) return emptyList()

        val arrayStr = cleaned.substring(startIdx, endIdx + 1)
        return runCatching {
            val arr = JSONArray(arrayStr)
            val list = mutableListOf<AiParsedTaskDraft>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.optString("title").trim()
                if (title.isEmpty()) continue

                val dateStr = obj.optString("date", defaultDate.toString()).let {
                    runCatching { LocalDate.parse(it).toString() }.getOrDefault(defaultDate.toString())
                }
                val scheduleType = runCatching {
                    ScheduleType.valueOf(obj.optString("scheduleType", "FLEXIBLE").uppercase())
                }.getOrDefault(ScheduleType.FLEXIBLE)

                val startMin = if (obj.isNull("startMinute")) null else obj.optInt("startMinute").takeIf { it in 0..1439 }
                val endMin = if (obj.isNull("endMinute")) null else obj.optInt("endMinute").takeIf { it in 1..1439 }
                val duration = obj.optInt("durationMinutes", 45).coerceIn(10, 480)

                val finalEnd = when {
                    startMin != null && endMin != null && endMin > startMin -> endMin
                    startMin != null -> (startMin + duration).coerceAtMost(23 * 60 + 59)
                    else -> null
                }

                val timePref = runCatching {
                    TimePreference.valueOf(obj.optString("timePreference", "ANY").uppercase())
                }.getOrDefault(TimePreference.ANY)

                val priority = runCatching {
                    TaskPriority.valueOf(obj.optString("priority", "MEDIUM").uppercase())
                }.getOrDefault(TaskPriority.MEDIUM)

                val repeatType = runCatching {
                    RepeatType.valueOf(obj.optString("repeatType", "NONE").uppercase())
                }.getOrDefault(RepeatType.NONE)

                list.add(
                    AiParsedTaskDraft(
                        title = title,
                        note = obj.optString("note", ""),
                        date = dateStr,
                        scheduleType = scheduleType,
                        startMinute = startMin,
                        endMinute = finalEnd,
                        durationMinutes = duration,
                        timePreference = timePref,
                        priority = priority,
                        repeatType = repeatType,
                        category = obj.optString("category", "Chung"),
                        selected = true
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }
}
