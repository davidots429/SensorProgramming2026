package com.davidots.planmind.ui.calendar


// 캘린더 화면의 현재 디스플레이 모드(월간, 주간, 일간)를 정의하는 Enum 클래스
// ViewModel의 상태(State) 자료형으로 사용되며, UI에서는 이 값에 따라 렌더링할 하위 컴포넌트 결정
enum class ViewType(val title: String) {
    MONTHLY("월간"),
    WEEKLY("주간"),
    DAILY("일간");

    // 상단 헤더의 '뷰 전환' 버튼 클릭 시 다음 화면 모드로 순환하기 위한 헬퍼 함수
    fun next(): ViewType = when (this) {
        MONTHLY -> WEEKLY
        WEEKLY -> DAILY
        DAILY -> MONTHLY
    }
}