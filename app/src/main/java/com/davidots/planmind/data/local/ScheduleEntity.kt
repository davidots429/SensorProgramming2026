package com.davidots.planmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,                  // 제목
    val date: String,                   // 날짜
    val startTime: String,              // 시작 시간
    val endTime: String,                // 종료 시간
    val isAllDay: Boolean = true,       // 하루 종일 일정인지
    val type: String = "일정",           // "일정" 또는 "리마인더"
    val location: String = "",          // 사용자가 지정할 장소 별명
    val address: String = "",           // 구글 지도에서 받아온 주소
    val latitude: Double? = null,       // 지도에서 얻은 위도
    val longitude: Double? = null,      // 지도에서 얻은 경도
    val alarmTime: String = "시작 시간",  // 알림 시간
    val repeatMode: String = "안함",     // TODO: 이 일정을 반복할 것인지
    val memo: String = "",              // 일정 메모
    val isCompleted: Boolean = false,   // TODO: 일정을 완료했는지
    val isAcknowledged: Boolean = false // 알림을 확인했는지 여부
)
