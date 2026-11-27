package com.lichviec.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LoadTasksResult(
    val tasks: List<TaskItem>,
    val fileExisted: Boolean,
    val wasCorrupted: Boolean,
    val corruptBackupPath: String? = null
)

class TaskRepository(private val context: Context) {

    private val tasksFile: File
        get() = File(context.filesDir, "tasks_db.json")

    private val prefs = context.getSharedPreferences("lichviec_settings", Context.MODE_PRIVATE)

    fun loadTasksSafe(): LoadTasksResult {
        if (!tasksFile.exists()) {
            return LoadTasksResult(tasks = emptyList(), fileExisted = false, wasCorrupted = false)
        }
        return try {
            val raw = tasksFile.readText(Charsets.UTF_8)
            val parsed = parseTasksJson(raw)
            LoadTasksResult(tasks = parsed, fileExisted = true, wasCorrupted = false)
        } catch (_: Exception) {
            val backupName = "tasks_db.corrupt.${System.currentTimeMillis()}.json"
            val backupFile = File(context.filesDir, backupName)
            runCatching { tasksFile.copyTo(backupFile, overwrite = true) }
            LoadTasksResult(
                tasks = emptyList(),
                fileExisted = true,
                wasCorrupted = true,
                corruptBackupPath = backupFile.name
            )
        }
    }

    fun loadTasks(): List<TaskItem> = loadTasksSafe().tasks

    fun saveTasks(tasks: List<TaskItem>) {
        val json = serializeTasksJson(tasks)
        val tmp = File(context.filesDir, "tasks_db.tmp")
        tmp.writeText(json, Charsets.UTF_8)
        if (!tmp.renameTo(tasksFile)) {
            tasksFile.writeText(json, Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun exportBackupJson(tasks: List<TaskItem>): String {
        return serializeTasksJson(tasks)
    }

    fun importBackupJson(rawJson: String): Result<List<TaskItem>> {
        return runCatching {
            val parsed = parseTasksJson(rawJson)
            saveTasks(parsed)
            parsed
        }
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
            geminiModel = prefs.getString("gemini_model", "gemini-3.8-flash") ?: "gemini-3.8-flash",
            workStartMinute = prefs.getInt("work_start_minute", 7 * 60),
            workEndMinute = prefs.getInt("work_end_minute", 22 * 60 + 30),
            breakMinutes = prefs.getInt("break_minutes", 10),
            reminderBeforeMinutes = prefs.getInt("reminder_before_minutes", 10),
            lockScreenSummaryEnabled = prefs.getBoolean("lock_screen_summary_enabled", true),
            hideLockScreenDetails = prefs.getBoolean("hide_lock_screen_details", false),
            avoidDeviceCalendarEvents = prefs.getBoolean("avoid_device_calendar_events", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("gemini_api_key", settings.geminiApiKey.trim())
            .putString("gemini_model", settings.geminiModel.trim().ifBlank { "gemini-3.8-flash" })
            .putInt("work_start_minute", settings.workStartMinute)
            .putInt("work_end_minute", settings.workEndMinute)
            .putInt("break_minutes", settings.breakMinutes)
            .putInt("reminder_before_minutes", settings.reminderBeforeMinutes)
            .putBoolean("lock_screen_summary_enabled", settings.lockScreenSummaryEnabled)
            .putBoolean("hide_lock_screen_details", settings.hideLockScreenDetails)
            .putBoolean("avoid_device_calendar_events", settings.avoidDeviceCalendarEvents)
            .apply()
    }

    private fun serializeTasksJson(tasks: List<TaskItem>): String {
        val arr = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("note", task.note)
            obj.put("date", task.date)
            obj.put("scheduleType", task.scheduleType.name)
            if (task.startMinute != null) obj.put("startMinute", task.startMinute) else obj.put("startMinute", JSONObject.NULL)
            if (task.endMinute != null) obj.put("endMinute", task.endMinute) else obj.put("endMinute", JSONObject.NULL)
            obj.put("durationMinutes", task.durationMinutes)
            obj.put("timePreference", task.timePreference.name)
            obj.put("priority", task.priority.name)
            obj.put("repeatType", task.repeatType.name)

            val daysArr = JSONArray()
            task.repeatDays.sorted().forEach { daysArr.put(it) }
            obj.put("repeatDays", daysArr)

            val statusObj = JSONObject()
            task.statusByDate.forEach { (dateKey, st) ->
                statusObj.put(dateKey, st.name)
            }
            obj.put("statusByDate", statusObj)
            obj.put("reminderEnabled", task.reminderEnabled)
            obj.put("category", task.category)

            val subArr = JSONArray()
            task.subtasks.forEach { st ->
                subArr.put(
                    JSONObject().apply {
                        put("id", st.id)
                        put("title", st.title)
                        put("done", st.done)
                    }
                )
            }
            obj.put("subtasks", subArr)
            arr.put(obj)
        }
        return arr.toString(2)
    }

    private fun parseTasksJson(raw: String): List<TaskItem> {
        val arr = JSONArray(raw)
        val result = mutableListOf<TaskItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val days = mutableSetOf<Int>()
            val daysArr = obj.optJSONArray("repeatDays")
            if (daysArr != null) {
                for (j in 0 until daysArr.length()) {
                    val d = daysArr.optInt(j, -1)
                    if (d in 1..7) days.add(d)
                }
            }
            val statusMap = mutableMapOf<String, TaskStatus>()
            val statusObj = obj.optJSONObject("statusByDate")
            if (statusObj != null) {
                val keys = statusObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val stName = statusObj.optString(key, TaskStatus.TODO.name)
                    statusMap[key] = runCatching { TaskStatus.valueOf(stName) }.getOrDefault(TaskStatus.TODO)
                }
            }
            val subtasks = mutableListOf<SubTask>()
            val subArr = obj.optJSONArray("subtasks")
            if (subArr != null) {
                for (j in 0 until subArr.length()) {
                    val sObj = subArr.optJSONObject(j) ?: continue
                    val sTitle = sObj.optString("title").trim()
                    if (sTitle.isNotEmpty()) {
                        subtasks.add(
                            SubTask(
                                id = sObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                title = sTitle,
                                done = sObj.optBoolean("done", false)
                            )
                        )
                    }
                }
            }
            val startMin = if (obj.isNull("startMinute")) null else obj.optInt("startMinute")
            val endMin = if (obj.isNull("endMinute")) null else obj.optInt("endMinute")

            result.add(
                TaskItem(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    title = obj.optString("title"),
                    note = obj.optString("note", ""),
                    date = obj.optString("date"),
                    scheduleType = runCatching { ScheduleType.valueOf(obj.optString("scheduleType")) }
                        .getOrDefault(ScheduleType.FLEXIBLE),
                    startMinute = startMin,
                    endMinute = endMin,
                    durationMinutes = obj.optInt("durationMinutes", 45),
                    timePreference = runCatching { TimePreference.valueOf(obj.optString("timePreference")) }
                        .getOrDefault(TimePreference.ANY),
                    priority = runCatching { TaskPriority.valueOf(obj.optString("priority")) }
                        .getOrDefault(TaskPriority.MEDIUM),
                    repeatType = runCatching { RepeatType.valueOf(obj.optString("repeatType")) }
                        .getOrDefault(RepeatType.NONE),
                    repeatDays = days,
                    statusByDate = statusMap,
                    reminderEnabled = obj.optBoolean("reminderEnabled", true),
                    category = obj.optString("category", "Chung"),
                    subtasks = subtasks
                )
            )
        }
        return result
    }
}
