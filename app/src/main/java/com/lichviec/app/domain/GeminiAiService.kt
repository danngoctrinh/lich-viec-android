package com.lichviec.app.domain

import com.lichviec.app.data.AiParsedTaskDraft
import com.lichviec.app.data.AppSettings
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.SubTask
import com.lichviec.app.data.TaskItem
import com.lichviec.app.data.TaskPriority
import com.lichviec.app.data.TaskStatus
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

    private val defaultFallbackModels = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite"
    )

    private fun buildModelList(configuredModel: String): List<String> {
        val clean = configuredModel.trim()
        return (listOfNotNull(clean.takeIf { it.isNotEmpty() }) + defaultFallbackModels).distinct()
    }

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
                message = "Đã tách ${offlineDrafts.size} việc bằng chế độ Offline (nhập Gemini API Key ở mục Thống kê để AI chia lịch sâu hơn)."
            )
        }

        val prompt = buildSchedulePrompt(noteText, targetDate, existingTasksOnDate, settings)
        val models = buildModelList(settings.geminiModel)

        var lastError = ""
        for (model in models) {
            val responseResult = callGeminiApi(trimmedKey, model, prompt, expectJson = true)
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

    suspend fun generateWeeklyReview(
        tasksProvider: (LocalDate) -> List<TaskItem>,
        settings: AppSettings
    ): String = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        var total = 0
        var done = 0
        var fixedMin = 0
        var flexMin = 0
        val unfinishedTitles = mutableListOf<String>()
        val doneByPeriod = mutableMapOf("Sáng" to 0, "Chiều" to 0, "Tối" to 0)

        for (i in 0..6) {
            val d = today.minusDays(i.toLong())
            val dayTasks = tasksProvider(d)
            total += dayTasks.size
            for (t in dayTasks) {
                val st = t.statusOn(d)
                if (t.scheduleType == ScheduleType.FIXED) fixedMin += t.effectiveDurationMinutes()
                else flexMin += t.effectiveDurationMinutes()

                if (st == TaskStatus.DONE) {
                    done++
                    val start = t.startMinute
                    if (start != null) {
                        when {
                            start < 12 * 60 -> doneByPeriod["Sáng"] = (doneByPeriod["Sáng"] ?: 0) + 1
                            start < 18 * 60 -> doneByPeriod["Chiều"] = (doneByPeriod["Chiều"] ?: 0) + 1
                            else -> doneByPeriod["Tối"] = (doneByPeriod["Tối"] ?: 0) + 1
                        }
                    }
                } else {
                    unfinishedTitles.add(t.title)
                }
            }
        }

        val rate = if (total == 0) 0 else (done * 100 / total)
        val bestPeriod = doneByPeriod.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: "Chưa đủ dữ liệu"
        val repeatedUnfinished = unfinishedTitles
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key} (${it.value} lần)" }
            .ifBlank { "Không có" }

        val offlineSummary = buildString {
            appendLine("Tỷ lệ hoàn thành 7 ngày: $done/$total việc ($rate%).")
            appendLine("Phân bổ giờ: Cố định ${fixedMin / 60}g${fixedMin % 60}p, Linh hoạt ${flexMin / 60}g${flexMin % 60}p.")
            appendLine("Khung giờ hiệu quả nhất: $bestPeriod.")
            append("Việc hay bị tồn lại: $repeatedUnfinished.")
        }

        val trimmedKey = settings.geminiApiKey.trim()
        if (trimmedKey.isEmpty() || total == 0) {
            return@withContext offlineSummary
        }

        val prompt = """
Bạn là trợ lý năng suất cá nhân. Dựa trên số liệu 7 ngày gần nhất của người dùng:
- Hoàn thành: $done / $total việc ($rate%)
- Tổng giờ cố định: ${fixedMin / 60}g ${fixedMin % 60}p
- Tổng giờ linh hoạt: ${flexMin / 60}g ${flexMin % 60}p
- Khung buổi hoàn thành nhiều nhất: $bestPeriod
- Việc hay bị bỏ dở / tồn lại: $repeatedUnfinished

Hãy viết tổng kết ngắn gọn bằng tiếng Việt (3–4 gạch đầu dòng, tối đa 100 từ, trực diện, không sáo rỗng):
1) Điểm làm tốt trong tuần
2) Điểm nghẽn / việc hay bị dời
3) 1 gợi ý điều chỉnh lịch cho tuần tới.
        """.trimIndent()

        for (model in buildModelList(settings.geminiModel)) {
            val res = callGeminiApi(trimmedKey, model, prompt, expectJson = false)
            if (res.isSuccess) {
                val text = res.getOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) return@withContext text
            }
        }
        offlineSummary
    }

    private fun buildSchedulePrompt(
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
Ngày mục tiêu mặc định: $targetDate (Thứ ${targetDate.dayOfWeek.value + 1}, quy ước 1=T2, 2=T3, 3=T4, 4=T5, 5=T6, 6=T7, 7=CN).
Khung giờ hoạt động trong ngày: ${formatMinute(settings.workStartMinute)} đến ${formatMinute(settings.workEndMinute)}, nghỉ giữa các việc ${settings.breakMinutes} phút.

Lịch đã có sẵn trong ngày $targetDate (KHÔNG được xếp đè lên các giờ này):
$fixedScheduleText

Ghi chú của người dùng:
"$noteText"

Hãy phân tích ghi chú thành danh sách các công việc cụ thể:
- scheduleType: "FIXED" nếu người dùng nêu giờ cụ thể/cố định; "FLEXIBLE" nếu không nêu giờ cố định.
- Với việc "FLEXIBLE", đề xuất startMinute và endMinute hợp lý (tính bằng phút từ 00:00, ví dụ 08:30 = 510) không trùng lịch đã có.
- repeatType: "NONE", "DAILY", "WEEKLY_DAYS", "LUNAR_MONTHLY" (rằm/mùng 1 hàng tháng âm), hoặc "LUNAR_YEARLY" (ngày giỗ/sinh nhật âm hàng năm).
- Nếu repeatType là "WEEKLY_DAYS", BẮT BUỘC điền mảng số nguyên "repeatDays" (1=T2..7=CN), ví dụ [1, 3, 5].
- Nếu 1 việc lớn có các bước nhỏ rõ ràng, thêm mảng "subtasks": ["Bước 1", "Bước 2"].

CHỈ TRẢ VỀ JSON ARRAY HỢP LỆ, KHÔNG kèm markdown ```json hay giải thích gì thêm.
Mỗi phần tử có cấu trúc:
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
  "repeatType": "NONE | DAILY | WEEKLY_DAYS | LUNAR_MONTHLY | LUNAR_YEARLY",
  "repeatDays": [1, 3, 5],
  "subtasks": [],
  "category": "Công việc | Học tập | Sức khỏe | Cá nhân"
}
        """.trimIndent()
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        prompt: String,
        expectJson: Boolean
    ): Result<String> {
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
                if (expectJson) {
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("responseMimeType", "application/json")
                        }
                    )
                }
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

                val parsedDate = obj.optString("date", defaultDate.toString()).let {
                    runCatching { LocalDate.parse(it) }.getOrDefault(defaultDate)
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

                val repeatDays = mutableSetOf<Int>()
                val daysArr = obj.optJSONArray("repeatDays")
                if (daysArr != null) {
                    for (j in 0 until daysArr.length()) {
                        val d = daysArr.optInt(j, -1)
                        if (d in 1..7) repeatDays.add(d)
                    }
                }
                if (repeatType == RepeatType.WEEKLY_DAYS && repeatDays.isEmpty()) {
                    repeatDays.add(parsedDate.dayOfWeek.value)
                }

                val subtasks = mutableListOf<SubTask>()
                val subArr = obj.optJSONArray("subtasks")
                if (subArr != null) {
                    for (j in 0 until subArr.length()) {
                        val stTitle = subArr.optString(j).trim()
                        if (stTitle.isNotEmpty()) {
                            subtasks.add(SubTask(title = stTitle))
                        }
                    }
                }

                list.add(
                    AiParsedTaskDraft(
                        title = title,
                        note = obj.optString("note", ""),
                        date = parsedDate.toString(),
                        scheduleType = scheduleType,
                        startMinute = startMin,
                        endMinute = finalEnd,
                        durationMinutes = duration,
                        timePreference = timePref,
                        priority = priority,
                        repeatType = repeatType,
                        repeatDays = repeatDays,
                        category = obj.optString("category", "Chung"),
                        subtasks = subtasks,
                        selected = true
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }
}
