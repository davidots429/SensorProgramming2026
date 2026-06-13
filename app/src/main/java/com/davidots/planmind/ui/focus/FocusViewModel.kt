package com.davidots.planmind.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// 집중 모드의 모든 타이머 연산, 포모도로 세션 관리, 환경(소음/조도) 판별을 담당하는 ViewModel.
class FocusViewModel : ViewModel() {

    // --- [1] 기본 설정 상태 ---
    private val _focusMode = MutableStateFlow(FocusMode.POMODORO)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    val pomoDurationMins = MutableStateFlow(25f)
    val pomoTotalSessions = MutableStateFlow(1)

    // 일반 타이머용 시, 분, 초
    val normalHour = MutableStateFlow(0)
    val normalMinute = MutableStateFlow(10)
    val normalSecond = MutableStateFlow(0)

    // --- [2] 진행 상태 ---
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _currentSession = MutableStateFlow(1)
    val currentSession: StateFlow<Int> = _currentSession.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(25 * 60)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    // --- [3] 센서 및 환경 변수 ---
    private val _currentLux = MutableStateFlow(500f)
    val currentLux: StateFlow<Float> = _currentLux.asStateFlow()

    private val _currentDb = MutableStateFlow(0f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    // 소음 80dB 이상이거나 조도 300lx 이하일 때 나쁜 환경으로 판별하는 비즈니스 로직
    val isBadEnvironment: Boolean
        get() = _currentDb.value >= 80f || _currentLux.value <= 300f

    // --- [4] 일회성 이벤트 (알람 울림 등) ---
    private val _alarmEvent = MutableSharedFlow<Unit>()
    val alarmEvent: SharedFlow<Unit> = _alarmEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var isFaceDown = false

    // 상단 버튼 클릭 시 타이머 모드를 변경
    fun toggleMode() {
        _focusMode.value = _focusMode.value.next()
        resetTimer()
    }

    // 센서 컴포넌트(View)에서 읽어들인 센서 값을 뷰모델로 밀어넣어 상태를 업데이트
    fun updateEnvironment(lux: Float, db: Float) {
        _currentLux.value = lux
        _currentDb.value = db
    }

    // 기기가 뒤집혔는지 여부를 판단하여 타이머를 자동 일시정지/재개
    fun updateFaceDownState(isDown: Boolean) {
        if (isFaceDown == isDown) return
        isFaceDown = isDown

        // 휴식 중이거나 완료/대기 상태일 때는 뒤집기 동작을 무시
        if (_timerState.value != TimerState.FOCUSING && _timerState.value != TimerState.PAUSED_FACE_UP) return

        if (isFaceDown && _timerState.value == TimerState.PAUSED_FACE_UP) {
            // 기기를 엎으면 타이머 재개
            _timerState.value = TimerState.FOCUSING
            startTimerLoop()
        } else if (!isFaceDown && _timerState.value == TimerState.FOCUSING) {
            // 똑바로 들면 타이머 일시정지
            _timerState.value = TimerState.PAUSED_FACE_UP
            timerJob?.cancel()
        }
    }

    // 타이머를 최초 시작. 사용자가 설정한 시간에 맞춰 초를 계산
    fun startTimer() {
        _currentSession.value = 1
        _timeLeftSeconds.value = if (_focusMode.value == FocusMode.POMODORO) {
            pomoDurationMins.value.toInt() * 60
        } else {
            (normalHour.value * 3600) + (normalMinute.value * 60) + normalSecond.value
        }

        // 시작하자마자 엎어놓기를 유도하기 위해 일시정지 상태로 시작
        _timerState.value = TimerState.PAUSED_FACE_UP
    }

    // 1초마다 시간이 줄어드는 실제 코루틴 타이머 루프
    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeftSeconds.value > 0) {
                delay(1000L)
                _timeLeftSeconds.value -= 1
            }
            handleTimerComplete()
        }
    }

    // 시간이 0초가 되었을 때의 State Machine Transition을 제어
    private suspend fun handleTimerComplete() {
        // UI 쪽에 진동/소리를 울리라고 이벤트를 방출합니다.
        _alarmEvent.emit(Unit)

        when (_timerState.value) {
            TimerState.FOCUSING -> {
                if (_focusMode.value == FocusMode.POMODORO) {
                    if (_currentSession.value < pomoTotalSessions.value) {
                        // 목표 세션이 남았다면 5분 휴식 모드로 진입
                        _timerState.value = TimerState.BREAK
                        _timeLeftSeconds.value = 5 * 60
                        startTimerLoop() // 휴식 타이머 즉시 가동
                    } else {
                        // 모든 세션 완료
                        _timerState.value = TimerState.COMPLETED
                    }
                } else {
                    // 일반 타이머는 즉시 완료
                    _timerState.value = TimerState.COMPLETED
                }
            }
            TimerState.BREAK -> {
                // 휴식이 끝나면 다음 세션으로 넘어가고 엎기를 기다림
                _currentSession.value += 1
                _timeLeftSeconds.value = pomoDurationMins.value.toInt() * 60
                _timerState.value = TimerState.PAUSED_FACE_UP
            }
            else -> {}
        }
    }

    fun forceStopTimer() {
        resetTimer()
    }

    private fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        _currentSession.value = 1
    }
}