package com.davidots.planmind.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidots.planmind.data.local.SleepRecordDao
import com.davidots.planmind.data.local.SleepRecordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


//수면 기록 관리 및 센서(조도, 만보기) 목표 달성 여부를 판별하는 수면 전용 뷰모델
class SleepViewModel(
    private val sleepRecordDao: SleepRecordDao // Room DB 접근용 DAO 주입
) : ViewModel() {

    // --- [1] 화면 및 센서 상태 ---
    private val _alarmState = MutableStateFlow(SleepAlarmState.IDLE)
    val alarmState: StateFlow<SleepAlarmState> = _alarmState.asStateFlow()

    private val _currentLux = MutableStateFlow(0f)
    val currentLux: StateFlow<Float> = _currentLux.asStateFlow()

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    // --- [2] 기록 데이터 ---
    // 실제 취침 시작 시간 (방의 불을 끈 시간)을 임시 저장하는 변수
    private var actualSleepTime: LocalTime? = null

    // Room DB에서 최근 7일간의 수면 기록을 실시간(Flow)으로 가져와 캐싱
    val recentSleepRecords: StateFlow<List<SleepRecordEntity>> =
        sleepRecordDao.getRecentSleepRecords()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // 알람 리시버나 외부(MainActivity) 인텐트로부터 전달받은 명령에 따라 화면 상태를 강제 전환
    fun setAlarmState(state: SleepAlarmState) {
        _alarmState.value = state
        if (state == SleepAlarmState.WAKE_RINGING) {
            _stepCount.value = 0 // 기상 시 걸음 수 초기화
        }
    }

    // 조도 센서의 값을 업데이트하고, 취침 대기 상태일 때 불이 꺼졌는지(5 lux 이하) 판별
    fun updateLux(lux: Float) {
        _currentLux.value = lux
        if (_alarmState.value == SleepAlarmState.SLEEP_WAITING && lux <= 5f) {
            // [비즈니스 로직] 방이 충분히 어두워졌으므로 취침 시작으로 간주
            actualSleepTime = LocalTime.now()
            _alarmState.value = SleepAlarmState.IDLE
        }
    }

    // 걸음 수 센서의 값을 업데이트하고, 기상 상태일 때 10걸음을 달성했는지 판별
    fun updateStepCount() {
        if (_alarmState.value == SleepAlarmState.WAKE_RINGING) {
            _stepCount.value += 1
            if (_stepCount.value >= 10) {
                // [비즈니스 로직] 10걸음 달성 시 기상 완료 처리 및 수면 시간 계산
                handleWakeUpComplete()
            }
        }
    }

    // 10걸음 걷기 미션을 완수하여 알람이 꺼질 때, 최종 수면 기록을 DB에 저장
    private fun handleWakeUpComplete() {
        val wakeTime = LocalTime.now()
        val sleepTime = actualSleepTime ?: wakeTime.minusHours(7) // 취침 기록이 없다면 임의로 7시간 전으로 계산 (예외 처리)

        // 자정을 넘긴 수면을 고려하여 분 단위로 총 수면 시간을 안전하게 계산
        var durationMins = ChronoUnit.MINUTES.between(sleepTime, wakeTime).toInt()
        if (durationMins < 0) durationMins += 24 * 60

        val newRecord = SleepRecordEntity(
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            actualSleepTime = sleepTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            actualWakeTime = wakeTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            sleepDurationMins = durationMins
        )

        // Room DB에 기록 적재 후 상태를 대기 모드로 초기화
        viewModelScope.launch {
            sleepRecordDao.insertSleepRecord(newRecord)
            actualSleepTime = null
            _alarmState.value = SleepAlarmState.IDLE
        }
    }

    // 에뮬레이터 환경이나 센서 오류 시 알람을 강제로 종료할 수 있는 디버그용 탈출구
    fun forceStopAlarm() {
        _alarmState.value = SleepAlarmState.IDLE
    }
}