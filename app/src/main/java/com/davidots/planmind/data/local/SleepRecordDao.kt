package com.davidots.planmind.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepRecordDao {
    // 새로운 수면 기록 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecordEntity)

    // 대시보드용: 최근 7개의 수면 기록을 날짜 내림차순(최신순)으로 가져오기
    @Query("SELECT * FROM sleep_records ORDER BY date DESC LIMIT 7")
    fun getRecentSleepRecords(): Flow<List<SleepRecordEntity>>

    // 모든 수면 기록을 한 번에 삭제하는 기능 (데이터 초기화용)
    @Query("DELETE FROM sleep_records")
    suspend fun deleteAllSleepRecords()
}