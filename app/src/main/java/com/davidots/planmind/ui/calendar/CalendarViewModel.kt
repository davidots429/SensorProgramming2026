package com.davidots.planmind.ui.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidots.planmind.data.local.ScheduleDao
import com.davidots.planmind.data.local.ScheduleEntity
import com.davidots.planmind.receiver.AlarmHelper
import com.davidots.planmind.receiver.GeofenceHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 캘린더 도메인의 핵심 비즈니스 로직 및 컴포저블 화면 상태(UiState) 흐름을 일괄 총괄하는 전용 ViewModel
class CalendarViewModel(
    private val scheduleDao: ScheduleDao // 의존성 주입 원칙에 의거해 데이터 소스 접근 객체(DAO)를 주입받아 활용
) : ViewModel() {

    // 현재 달력 화면의 디스플레이 모드 상태 스트림 (월간, 주간, 일간)
    private val _viewType = MutableStateFlow(ViewType.MONTHLY)
    val viewType: StateFlow<ViewType> = _viewType.asStateFlow()

    // 달력의 타겟 날짜 기준점 상태 스트림
    private val _targetDate = MutableStateFlow(LocalDate.now())
    val targetDate: StateFlow<LocalDate> = _targetDate.asStateFlow()

    // 유저가 스와이프 도중 끊김 현상을 느끼지 않도록 현재 화면 전후 일정 범위를 미리 들고 있도록 제어하는 감시 날짜 영역 상태
    private val _dateRange = MutableStateFlow(Pair(LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1)))

    // 감시 범위 변동 신호가 감지될 때마다 최적화된 Room 비동기 Flow를 유기적으로 flatMapLatest하여 날짜별 그룹화 맵 구조로 실시간 캐싱해 UI로 밀어줌.
    @OptIn(ExperimentalCoroutinesApi::class)
    val schedulesByDate: StateFlow<Map<String, List<ScheduleEntity>>> = _dateRange
        .flatMapLatest { range ->
            scheduleDao.getSchedulesBetweenDates(
                startDate = range.first.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = range.second.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        }
        .map { list -> list.groupBy { it.date } }
        .stateIn(
            scope = viewModelScope,
            // UI 미노출 시 5초의 버퍼 기간을 두고 구독을 해제하여 리소스 누수를 방지
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // 사용자의 스와이프 모션 또는 일자 탭 행위로 화면 중심 대상 날짜가 변경되었을 때 뷰에서 수신하는 동적 데이터 프리로드 가동 함수
    fun updateDisplayedDate(date: LocalDate, currentViewType: ViewType) {
        // 각 뷰어의 특성에 최적화된 범위 패딩 가중치를 차등 계산하여 DB 쿼리 부하를 방지
        val range = when (currentViewType) {
            ViewType.MONTHLY -> Pair(date.minusMonths(2).withDayOfMonth(1), date.plusMonths(2).withDayOfMonth(1).plusMonths(1).minusDays(1))
            ViewType.WEEKLY -> Pair(date.minusWeeks(4), date.plusWeeks(4))
            ViewType.DAILY -> Pair(date.minusDays(10), date.plusDays(10))
        }
        _dateRange.value = range
    }

    // 상단 헤더의 뷰 전환 버튼을 터치할 때마다 캘린더 모드 상태를 순환 변경
    fun toggleViewType() {
        _viewType.value = _viewType.value.next()
    }

    // 하단 리프레시 버튼 클릭 시 오늘 날짜 좌표로 화면을 원격 강제 정렬 동기화
    fun moveToToday() {
        val today = LocalDate.now()
        _targetDate.value = today
        updateDisplayedDate(today, _viewType.value)
    }

    // 하단 퀵 추가 입력 바를 통해 사용자가 작성한 타이틀 명세 일정을 오늘 기준 기본 시간대로 즉시 DB 레이어에 적재
    fun addQuickSchedule(title: String, date: LocalDate) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val newSchedule = ScheduleEntity(
                title = title,
                date = dateStr,
                startTime = "00:00",
                endTime = "23:59",
                isAllDay = true
            )
            scheduleDao.insertSchedule(newSchedule)
        }
    }

    // 일간 타임테이블 상에서 롱 프레스로 일정을 움직여 시간이 세부 변경되었을 때 데이터를 REPLACE 안전 업데이트 해주는 처리 영역
    fun updateScheduleTime(id: Int, newStartTime: String, newEndTime: String) {
        viewModelScope.launch {
            val existingSchedule = scheduleDao.getScheduleById(id)
            if (existingSchedule != null) {
                val updatedSchedule = existingSchedule.copy(
                    startTime = newStartTime,
                    endTime = newEndTime
                )
                scheduleDao.insertSchedule(updatedSchedule)
            }
        }
    }

    // 달력의 개별 일정을 안전하게 완전 삭제 조치하고,
    // 연계 결합 등록되어 있던 알림 매니저와 위치 기반 지오펜싱까지 백그라운드에서 추적 해제 청소해주는 비즈니스 로직
    fun deleteSchedule(schedule: ScheduleEntity, context: Context) {
        viewModelScope.launch {
            scheduleDao.deleteSchedule(schedule)
            AlarmHelper.cancelAlarm(context, schedule.id)
            GeofenceHelper.removeGeofence(context, schedule.id)
        }
    }
}