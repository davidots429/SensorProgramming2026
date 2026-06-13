package com.davidots.planmind.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.davidots.planmind.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            val sharedPref = context.getSharedPreferences("PlanMindPrefs", Context.MODE_PRIVATE)

            // 1. 수면/기상 알람
            val sleepTime = sharedPref.getString("sleepTime", null)
            if (sleepTime != null) {
                AlarmHelper.scheduleDailyAlarm(context, "SLEEP".hashCode(), sleepTime, "SLEEP", "취침 알람")
            }

            val wakeTime = sharedPref.getString("wakeTime", null)
            if (wakeTime != null) {
                AlarmHelper.scheduleDailyAlarm(context, "WAKE".hashCode(), wakeTime, "WAKE", "기상 알람")
            }

            // 2. 캘린더 일정 알람
            // BroadcastReceiver는 메인 스레드에서 돌기 때문에, DB 작업은 별도의 IO 코루틴에서 실행해야함
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getDatabase(context)
                val scheduleDao = database.scheduleDao()

                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val futureSchedules = scheduleDao.getFutureSchedules(todayStr)

                futureSchedules.forEach { schedule ->
                    // 리마인더이거나, 종일 일정이 아닌 경우에만 알람 등록 시도
                    AlarmHelper.scheduleAlarm(
                        context = context,
                        id = schedule.id,
                        title = schedule.title,
                        date = schedule.date,
                        startTime = schedule.startTime,
                        alarmTimeStr = schedule.alarmTime,
                        isAllDay = schedule.isAllDay,
                        latitude = schedule.latitude,
                        longitude = schedule.longitude,
                        locationName = schedule.location
                    )

                    // 지오펜스(위치 기반 알람)가 설정되어 있다면 함께 복구
                    if (schedule.latitude != null && schedule.longitude != null) {
                        GeofenceHelper.addGeofence(context, schedule)
                    }
                }
            }
        }
    }
}