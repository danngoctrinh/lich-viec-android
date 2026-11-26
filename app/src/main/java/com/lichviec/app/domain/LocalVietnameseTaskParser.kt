package com.lichviec.app.domain

import com.lichviec.app.data.AiParsedTaskDraft
import com.lichviec.app.data.RepeatType
import com.lichviec.app.data.ScheduleType
import com.lichviec.app.data.TaskPriority
import com.lichviec.app.data.TimePreference
import java.time.LocalDate

/**
 * Bộ phân tích ghi chú tiếng Việt chạy offline khi chưa nhập Gemini API Key hoặc không có mạng.
 * Nhận diện:
 * - Khung giờ cố định: "8h-9h30", "08:00 - 09:30", "từ 14h đến 15h30"
 * - Giờ bắt đầu + thời lượng: "20h họp nhóm 45 phút", "18h30 tập gym 1 tiếng"
 * - Thời lượng linh hoạt: "90 phút", "1.5 tiếng", "2h"
 * - Lặp mỗi ngày: "mỗi ngày", "hàng ngày", "daily"
 * - Buổi ưu tiên: "sáng", "chiều", "tối"
 * - Độ ưu tiên: "gấp", "ưu tiên cao", "quan trọng", "ưu tiên thấp"
 */
object LocalVietnameseTaskParser {

    private val rangeRegex = Regex(
        """(?i)(?:từ\s*)?(\d{1,2})\s*(?:h|:)\s*(\d{0,2})\s*(?:-|–|đến|tới)\s*(\d{1,2})\s*(?:h|:)\s*(\d{0,2})"""
    )

    private val singleTimeRegex = Regex(
        """(?i)(?:lúc\s*)?(\d{1,2})\s*(?:h|:)\s*(\d{2})?(?:\s*(sáng|chiều|tối))?(?!\s*(?:-|–|đến|tới))"""
    )

    private val durationMinRegex = Regex("""(?i)(\d{1,3})\s*(?:phút|phut|p\b|min)""")
    private val durationHourRegex = Regex("""(?i)(\d+(?:[.,]\d+)?)\s*(?:tiếng|tieng|giờ|gio)""")

    fun parseNote(rawNote: String, defaultDate: LocalDate): List<AiParsedTaskDraft> {
        val segments = rawNote
            .split(Regex("""[\n;]+|(?<=\.)\s+"""))
            .flatMap { line ->
                // Chỉ tách theo dấu phẩy nếu dòng dài và chứa nhiều ý
                if (line.length > 45 && line.count { it == ',' } >= 1) {
                    line.split(",")
                } else {
                    listOf(line)
                }
            }
            .map { it.trim().trimStart('-', '*', '•', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '.', ' ') }
            .filter { it.length >= 2 }

        return segments.map { segment ->
            parseSegment(segment, defaultDate)
        }
    }

    private fun parseSegment(text: String, defaultDate: LocalDate): AiParsedTaskDraft {
        val lower = text.lowercase()
        var targetDate = defaultDate
        if (lower.contains("ngày mai") || lower.contains("mai ")) {
            targetDate = defaultDate.plusDays(1)
        } else if (lower.contains("ngày kia")) {
            targetDate = defaultDate.plusDays(2)
        }

        val repeatType = when {
            lower.contains("mỗi ngày") || lower.contains("hàng ngày") || lower.contains("moi ngay") || lower.contains("daily") ->
                RepeatType.DAILY
            else -> RepeatType.NONE
        }

        val priority = when {
            lower.contains("gấp") || lower.contains("quan trọng") || lower.contains("ưu tiên cao") || lower.contains("khẩn") ->
                TaskPriority.HIGH
            lower.contains("ưu tiên thấp") || lower.contains("rảnh thì") || lower.contains("không gấp") ->
                TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }

        val timePref = when {
            lower.contains("buổi sáng") || lower.contains("sáng") -> TimePreference.MORNING
            lower.contains("buổi chiều") || lower.contains("chiều") -> TimePreference.AFTERNOON
            lower.contains("buổi tối") || lower.contains("tối") || lower.contains("đêm") -> TimePreference.EVENING
            else -> TimePreference.ANY
        }

        var duration = 45
        durationHourRegex.find(text)?.let { match ->
            val hours = match.groupValues[1].replace(',', '.').toDoubleOrNull()
            if (hours != null && hours in 0.25..8.0) {
                duration = (hours * 60).toInt()
            }
        }
        durationMinRegex.find(text)?.let { match ->
            val mins = match.groupValues[1].toIntOrNull()
            if (mins != null && mins in 5..480) {
                duration = mins
            }
        }

        var startMinute: Int? = null
        var endMinute: Int? = null
        var scheduleType = if (lower.contains("cố định") || lower.contains("co dinh")) {
            ScheduleType.FIXED
        } else {
            ScheduleType.FLEXIBLE
        }

        var cleanedTitle = text

        val rangeMatch = rangeRegex.find(text)
        if (rangeMatch != null) {
            var sh = rangeMatch.groupValues[1].toIntOrNull() ?: 8
            val sm = rangeMatch.groupValues[2].toIntOrNull() ?: 0
            var eh = rangeMatch.groupValues[3].toIntOrNull() ?: (sh + 1)
            val em = rangeMatch.groupValues[4].toIntOrNull() ?: 0

            if (timePref == TimePreference.AFTERNOON && sh in 1..6) sh += 12
            if (timePref == TimePreference.EVENING && sh in 1..11) sh += 12
            if (eh < sh && eh in 1..11) eh += 12

            val sTotal = (sh * 60 + sm).coerceIn(0, 23 * 60 + 55)
            val eTotal = (eh * 60 + em).coerceIn(sTotal + 15, 23 * 60 + 59)
            startMinute = sTotal
            endMinute = eTotal
            duration = eTotal - sTotal
            scheduleType = ScheduleType.FIXED
            cleanedTitle = cleanedTitle.replace(rangeMatch.value, " ")
        } else {
            val singleMatch = singleTimeRegex.find(text)
            if (singleMatch != null) {
                var h = singleMatch.groupValues[1].toIntOrNull() ?: 9
                val m = singleMatch.groupValues[2].toIntOrNull() ?: 0
                val period = singleMatch.groupValues[3].lowercase()
                if ((period == "chiều" || period == "tối" || timePref == TimePreference.AFTERNOON || timePref == TimePreference.EVENING) && h in 1..11) {
                    h += 12
                }
                val sTotal = (h * 60 + m).coerceIn(0, 23 * 60 + 45)
                val eTotal = (sTotal + duration).coerceAtMost(23 * 60 + 59)
                startMinute = sTotal
                endMinute = eTotal
                scheduleType = ScheduleType.FIXED
                cleanedTitle = cleanedTitle.replace(singleMatch.value, " ")
            }
        }

        // Dọn dẹp từ khóa phụ khỏi tiêu đề cho gọn
        val noisePatterns = listOf(
            Regex("""(?i)\b(hôm nay|ngày mai|ngày kia|cố định|co dinh|linh hoạt|linh hoat|mỗi ngày|hàng ngày|ưu tiên cao|ưu tiên thấp|buổi sáng|buổi chiều|buổi tối)\b"""),
            durationMinRegex,
            durationHourRegex
        )
        noisePatterns.forEach { regex ->
            cleanedTitle = cleanedTitle.replace(regex, " ")
        }
        cleanedTitle = cleanedTitle.replace(Regex("""\s+"""), " ").trim(' ', ',', '-', ':')
        if (cleanedTitle.length < 2) {
            cleanedTitle = text.trim()
        } else {
            cleanedTitle = cleanedTitle.replaceFirstChar { it.uppercaseChar() }
        }

        val category = inferCategory(lower)

        return AiParsedTaskDraft(
            title = cleanedTitle,
            note = text.trim(),
            date = targetDate.toString(),
            scheduleType = scheduleType,
            startMinute = startMinute,
            endMinute = endMinute,
            durationMinutes = duration,
            timePreference = timePref,
            priority = priority,
            repeatType = repeatType,
            category = category,
            selected = true
        )
    }

    private fun inferCategory(lower: String): String = when {
        lower.contains("gym") || lower.contains("tập") || lower.contains("chạy bộ") || lower.contains("thể dục") -> "Sức khỏe"
        lower.contains("học") || lower.contains("tiếng anh") || lower.contains("toeic") || lower.contains("đọc sách") -> "Học tập"
        lower.contains("họp") || lower.contains("báo cáo") || lower.contains("code") || lower.contains("video") || lower.contains("live") -> "Công việc"
        else -> "Cá nhân"
    }
}
