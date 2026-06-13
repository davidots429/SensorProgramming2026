package com.davidots.planmind.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.davidots.planmind.data.local.ScheduleEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceHelper {
    fun addGeofence(context: Context, schedule: ScheduleEntity) {
        // 위치가 없거나, 이미 확인한 일정이거나, 하루 종일 일정이 아니면 Geofence 등록 제외
        if (schedule.latitude == null || schedule.longitude == null || schedule.isAcknowledged || !schedule.isAllDay) return

        val geofencingClient = LocationServices.getGeofencingClient(context)

        val geofence = Geofence.Builder()
            .setRequestId(schedule.id.toString())
            .setCircularRegion(schedule.latitude, schedule.longitude, 500f) // 500m 반경
            .setExpirationDuration(Geofence.NEVER_EXPIRE) // 사용자가 확인할 때까지 무한 유지
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(60000) // 500m 반경 내에 1분 이상 머물면 DWELL 재알림 발동
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            putExtra("title", schedule.title)
            putExtra("locationName", schedule.location)
            putExtra("scheduleId", schedule.id)
        }

        // 안드로이드 12 이상에서 Geofence는 반드시 FLAG_MUTABLE을 사용해야 합니다.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, schedule.id, intent, flags)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
        }
    }

    fun removeGeofence(context: Context, scheduleId: Int) {
        val geofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.removeGeofences(listOf(scheduleId.toString()))
    }
}
