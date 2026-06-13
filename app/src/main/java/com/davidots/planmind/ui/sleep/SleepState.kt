package com.davidots.planmind.ui.sleep

// 수면/기상 모듈의 현재 작동 상태를 정의하는 Enum 클래스입니다.
enum class SleepAlarmState {
    IDLE,           // 대기 상태 (수면/기상 시간 설정 및 최근 기록 확인 화면)
    SLEEP_WAITING,  // 취침 알람이 울린 후, 사용자가 방의 불을 끄기(조도 낮추기)를 기다리는 상태
    WAKE_RINGING    // 기상 알람이 울리고 있으며, 사용자가 일어나서 10걸음을 걷기를 기다리는 상태
}
