package com.davidots.planmind.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.davidots.planmind.data.local.ScheduleDao
import com.davidots.planmind.data.local.ScheduleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


//일정 추가, 수정, 삭제(ScheduleDetailScreen) 작업만을 전담하는 단일 책임 뷰모델
class ScheduleViewModel(
    private val scheduleDao: ScheduleDao
) : ViewModel() {

    // 현재 편집 또는 조회 중인 단일 일정 데이터를 보관하는 상태
    private val _currentSchedule = MutableStateFlow<ScheduleEntity?>(null)
    val currentSchedule: StateFlow<ScheduleEntity?> = _currentSchedule.asStateFlow()

    // 특정 일정 ID를 받아 DB에서 데이터를 불러와 상태에 저장 (수정 모드 진입 시 호출)
    fun loadSchedule(id: Int) {
        viewModelScope.launch {
            val schedule = scheduleDao.getScheduleById(id)
            _currentSchedule.value = schedule
        }
    }

    // 일정을 추가하거나 수정하여 DB에 저장
    fun saveSchedule(schedule: ScheduleEntity, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val generatedId = scheduleDao.insertSchedule(schedule)
            // 새로 생성된 경우 generatedId를, 기존 수정인 경우 원래 id를 반환
            val finalId = if (schedule.id == 0) generatedId.toInt() else schedule.id
            onComplete(finalId)
        }
    }

    // 현재 일정을 DB에서 삭제
    fun deleteSchedule(schedule: ScheduleEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            scheduleDao.deleteSchedule(schedule)
            onComplete() // 삭제가 완료되면 화면을 닫을 수 있도록 콜백 실행
        }
    }

    // 새로운 일정 추가 모드일 때 기존에 남아있던 상태를 초기화
    fun clearCurrentSchedule() {
        _currentSchedule.value = null
    }
}


class ScheduleViewModelFactory(
    private val scheduleDao: ScheduleDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(scheduleDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}