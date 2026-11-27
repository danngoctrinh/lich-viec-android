package com.lichviec.app.domain

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Đọc các khung giờ bận từ lịch hệ thống / Google Calendar trên máy (chỉ đọc)
 * để SmartScheduler tự động né giờ họp khi xếp lịch linh hoạt.
 */
object DeviceCalendarHelper {

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readBusySlotsForDate(context: Context, date: LocalDate): List<TimeSlot> {
        if (!hasCalendarPermission(context)) return emptyList()

        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, startMillis)
        ContentUris.appendId(uriBuilder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.AVAILABILITY
        )

        val slots = mutableListOf<TimeSlot>()
        runCatching {
            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val allDay = cursor.getInt(2) == 1
                    val availability = cursor.getInt(3)
                    if (allDay || availability == CalendarContract.Events.AVAILABILITY_FREE) {
                        continue
                    }

                    val beginMs = cursor.getLong(0).coerceAtLeast(startMillis)
                    val endMs = cursor.getLong(1).coerceAtMost(endMillis)
                    if (endMs <= beginMs) continue

                    val beginTime = Instant.ofEpochMilli(beginMs).atZone(zone).toLocalTime()
                    val endTime = Instant.ofEpochMilli(endMs).atZone(zone).toLocalTime()
                    val startMin = beginTime.hour * 60 + beginTime.minute
                    val endMin = (endTime.hour * 60 + endTime.minute).coerceAtLeast(startMin + 15)
                    if (endMin > startMin) {
                        slots.add(TimeSlot(startMin, endMin.coerceAtMost(23 * 60 + 59)))
                    }
                }
            }
        }
        return slots
    }
}
