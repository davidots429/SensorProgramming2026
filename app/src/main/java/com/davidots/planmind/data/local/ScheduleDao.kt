package com.davidots.planmind.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    // 특정 날짜의 일정만 시간순으로 가져옵니다.
    // Flow를 사용하면 DB가 변경될 때 UI가 자동으로 새로고침됩니다.
    @Query("SELECT * FROM schedules WHERE date = :date ORDER BY startTime ASC")
    fun getSchedulesByDate(date: String): Flow<List<ScheduleEntity>>

    // 특정 기간(예: 1일~31일)의 모든 일정을 날짜/시간 순으로 가져오기
    @Query("SELECT * FROM schedules WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getSchedulesBetweenDates(startDate: String, endDate: String): Flow<List<ScheduleEntity>>

    // 특정 ID의 일정 데이터를 하나만 가져옵니다.
    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Int): ScheduleEntity?

    // 일정을 추가하거나 수정합니다 (id가 같으면 덮어씌움).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    // 일정을 삭제합니다.
    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}