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

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "일정"
        val startTime = intent.getStringExtra("startTime") ?: ""
        val alarmTimeStr = intent.getStringExtra("alarmTimeStr") ?: ""
        val notificationId = intent.getIntExtra("id", 0)

        // 클릭 시 상세 화면으로 이동하기 위해 날짜 데이터 추가 수신
        val date = intent.getStringExtra("date") ?: ""

        // 1. "시작 시간" 텍스트 버그 해결 로직
        val timeDisplay = if (alarmTimeStr == "시작 시간" || alarmTimeStr.isBlank()) {
            startTime // "09:00에 '회의' 일정이 있습니다."
        } else {
            alarmTimeStr // "10분 전에 '회의' 일정이 있습니다." (사용자가 커스텀 시)
        }
        val message = "${timeDisplay}에 '$title' 일정이 있습니다."

        // 2. 알림 클릭 시 특정 화면으로 이동하도록 인텐트 구성
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("scheduleId", notificationId)
            putExtra("scheduleDate", date)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. 알림 팝업의 [일정 완료] 액션 버튼 인텐트 구성
        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE_SCHEDULE_$notificationId"
            putExtra("scheduleId", notificationId)
            putExtra("notificationId", notificationId)
        }

        val pendingActionIntent = PendingIntent.getBroadcast(
            context, notificationId + 100, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. 알림 발송
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "planmind_alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "PlanMind 일정 알림", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 클릭 시 상세 화면 이동
            .addAction(android.R.drawable.ic_menu_edit, "일정 완료 (알람 끄기)", pendingActionIntent) // 팝업 내 버튼
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}