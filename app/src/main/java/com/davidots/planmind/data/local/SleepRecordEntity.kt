package com.davidots.planmind.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,             // 기록 날짜 (예: "2023-10-27" - 일어난 날 기준)
    val actualSleepTime: String,  // 실제 취침 시간 (조도 센서가 꺼진 시간)
    val actualWakeTime: String,   // 실제 기상 시간 (10걸음을 달성한 시간)
    val sleepDurationMins: Int    // 총 수면 시간 (분 단위)
)