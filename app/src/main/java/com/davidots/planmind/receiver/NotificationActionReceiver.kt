package com.davidots.planmind.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.davidots.planmind.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getIntExtra("scheduleId", -1)
        val notificationId = intent.getIntExtra("notificationId", -1)

        if (scheduleId != -1) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(context).scheduleDao()
                    val schedule = dao.getScheduleById(scheduleId)

                    if (schedule != null) {
                        dao.insertSchedule(schedule.copy(isCompleted = true, isAcknowledged = true))
                    }

                    GeofenceHelper.removeGeofence(context, scheduleId)
                    AlarmHelper.cancelAlarm(context, scheduleId)

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                } finally {
                    pendingResult.finish()
                }
            }

            // 알림 버튼 클릭 시 완료되었다는 시각적 피드백 제공
            Toast.makeText(context, "일정이 완료 처리되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}