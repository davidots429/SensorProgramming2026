package com.davidots.planmind.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AlarmHelper {
    fun scheduleAlarm(
        context: Context,
        id: Int,
        title: String,
        date: String,
        startTime: String,
        alarmTimeStr: String,
        isAllDay: Boolean,
        latitude: Double?,
        longitude: Double?,
        locationName: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("date", date)
            putExtra("startTime", startTime)
            putExtra("alarmTimeStr", alarmTimeStr)
            // 위경도가 null일 경우 구분을 위해 -1000.0 이라는 불가능한 좌표값 사용
            putExtra("latitude", latitude ?: -1000.0)
            putExtra("longitude", longitude ?: -1000.0)
            putExtra("locationName", locationName.ifBlank { "지정된 장소" })
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 하루 종일 일정이거나 알림이 '없음'이면 알람 취소 후 종료
        if (isAllDay || alarmTimeStr == "없음") {
            alarmManager.cancel(pendingIntent)
            return
        }

        val calendar = java.util.Calendar.getInstance()
        val dateParts = date.split("-")
        val timeParts = startTime.split(":")

        if (dateParts.size == 3 && timeParts.size == 2) {
            calendar.set(
                dateParts[0].toInt(),
                dateParts[1].toInt() - 1,
                dateParts[2].toInt(),
                timeParts[0].toInt(),
                timeParts[1].toInt(),
                0
            )

            when (alarmTimeStr) {
                "5분 전" -> calendar.add(java.util.Calendar.MINUTE, -5)
                "10분 전" -> calendar.add(java.util.Calendar.MINUTE, -10)
                "15분 전" -> calendar.add(java.util.Calendar.MINUTE, -15)
                "30분 전" -> calendar.add(java.util.Calendar.MINUTE, -30)
                "1시간 전" -> calendar.add(java.util.Calendar.HOUR_OF_DAY, -1)
            }

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    // 수면/기상 알림 관련
    fun scheduleDailyAlarm(context: Context, requestCode: Int, timeStr: String, type: String, title: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. 알람 리시버로 보낼 인텐트 구성
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            putExtra("ALARM_TYPE", type)
            putExtra("TITLE", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 2. 시간 계산 (timeStr은 "23:30" 형태)
        val calendar = java.util.Calendar.getInstance()
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            calendar.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(java.util.Calendar.MINUTE, parts[1].toInt())
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            // 계산된 시간이 현재 시간과 같거나 과거면 무조건 내일로 미룸
            if (calendar.timeInMillis <= System.currentTimeMillis() + 1000) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            // 3. 도즈 모드(배터리 절약)에서도 정확히 울리도록 설정
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }

    fun cancelAlarm(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }
}