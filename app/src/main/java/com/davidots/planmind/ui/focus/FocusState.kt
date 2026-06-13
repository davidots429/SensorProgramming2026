package com.davidots.planmind.ui.focus

//타이머의 생명주기 및 현재 상태를 명확하게 구분하는 Enum
enum class TimerState {
    IDLE,              // 대기 상태 (설정 화면)
    FOCUSING,          // 집중 타이머 진행 중
    PAUSED_FACE_UP,    // 기기가 하늘을 향해 있어 일시 정지된 상태
    BREAK,             // 포모도로 휴식 시간 진행 중
    COMPLETED          // 목표 시간 도달 및 사이클 완료
}


// 집중 모드의 종류를 구분
enum class FocusMode(val title: String) {
    POMODORO("포모도로"),
    NORMAL("일반 타이머");

    // 다음 모드로 순환 변경하기 위한 헬퍼 함수
    fun next(): FocusMode = when (this) {
        POMODORO -> NORMAL
        NORMAL -> POMODORO
    }
}