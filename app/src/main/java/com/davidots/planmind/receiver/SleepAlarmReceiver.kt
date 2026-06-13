package com.davidots.planmind.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.davidots.planmind.MainActivity

class SleepAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("ALARM_TYPE") ?: return
        val title = intent.getStringExtra("TITLE") ?: "알람"
        val message = if (type == "SLEEP") "숙면을 취할 시간입니다. 방을 어둡게 해주세요." else "기상 시간입니다! 일어나서 걸어주세요."

        // 알림이 지워지더라도 앱이 미션을 기억하도록 로컬 저장소에 미션 보류(Pending) 상태를 각인
        val sharedPref = context.getSharedPreferences("PlanMindPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("PENDING_MISSION", type).apply()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "planmind_sleep_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "수면/기상 알림", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 사용자가 알림을 눌렀을 때 앱의 MainActivity를 깨우며, 어떤 알람인지(SLEEP/WAKE) 정보를 전달
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TRIGGER_SLEEP_ALARM", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, type.hashCode(), launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true) // 잠금화면에서도 깨우기 시도
            .setAutoCancel(true)
            .build()

        notificationManager.notify(type.hashCode(), notification)

        // 매일 반복을 위해 내일 알람을 다시 예약
        val timeStr = if (type == "SLEEP") sharedPref.getString("sleepTime", "23:30") else sharedPref.getString("wakeTime", "07:00")
        if (timeStr != null) {
            AlarmHelper.scheduleDailyAlarm(context, type.hashCode(), timeStr, type, title)
        }
    }
}