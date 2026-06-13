package com.davidots.planmind.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) return

        val transitionType = geofencingEvent.geofenceTransition

        // 진입(ENTER)하거나 일정 시간 머무를 때(DWELL) 알림 발생
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER || transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val title = intent.getStringExtra("title") ?: "하루 종일 일정"
            val locationName = intent.getStringExtra("locationName") ?: "지정된 장소"
            val scheduleId = intent.getIntExtra("scheduleId", -1)

            // 기존 시간 알람과 ID가 겹치지 않게 오프셋 부여
            val notificationId = scheduleId + 10000
            val message = "'$locationName' 근처에 도착했습니다. 일정을 확인해 주세요."

            sendGeofenceNotification(context, notificationId, scheduleId, title, message)
        }
    }

    private fun sendGeofenceNotification(context: Context, notificationId: Int, scheduleId: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "planmind_geofence_channel"

        val channel = NotificationChannel(channelId, "PlanMind 장소 알림", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // [확인] 버튼을 눌렀을 때 호출될 인텐트 (3번에서 만든 리시버로 연결)
        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra("scheduleId", scheduleId)
            putExtra("notificationId", notificationId)
        }
        val pendingActionIntent = PendingIntent.getBroadcast(
            context, notificationId, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_edit, "확인(알림 끄기)", pendingActionIntent) // [핵심] 알림창 자체에 버튼 생성
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}