package com.davidots.planmind.ui.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.davidots.planmind.data.local.ScheduleEntity
import java.time.LocalDate


// 달력 화면 전역에서 컴포저블의 렌더링 색상이나 날짜 문자열 변환을 위해 공통으로 사용하는 유틸리티 함수 모음
object CalendarUiUtil {

    //일정의 속성(타입)에 맞는 테마 포인트 색상을 반환
    @Composable
    fun getScheduleColor(type: String): Color {
        return if (type == "리마인더") Color(0xFF14B8A6) else MaterialTheme.colorScheme.primary
    }

    // DayOfWeek 정수 값(1~7)을 한글 요일 문자열(월~일)로 가공하여 반환
    fun getDayOfWeekString(dayOfWeekValue: Int): String {
        val days = listOf("월", "화", "수", "목", "금", "토", "일")
        return days[(dayOfWeekValue - 1) % 7]
    }

    // 요일에 알맞은 캘린더 전용 색상을 매핑. (토요일: 파란색, 일요일: 빨간색, 평일: 테마 기본 글자색)
    @Composable
    fun getDayOfWeekColor(dayOfWeekValue: Int): Color {
        return when (dayOfWeekValue) {
            6 -> Color.Blue // 토요일 강조색
            7 -> Color.Red  // 일요일 강조색
            else -> MaterialTheme.colorScheme.onSurface // 평일 기본색
        }
    }

    // "HH:mm" 포맷의 시간 문자열을 00:00시 기준 누적 '분(Minutes)' 정수로 변환하여 타임라인 상의 Y축 그리드 높이를 산출할 때 사용
    fun timeToMins(timeStr: String): Int {
        val parts = timeStr.split(":")
        return if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
    }

    // 하루 누적 '분(Minutes)' 정수를 "HH:mm" 시간 포맷 문자열로 역변환하여 저장 인터페이스로 전달
    fun minsToTime(mins: Int): String {
        val h = (mins / 60).coerceIn(0, 23)
        val m = (mins % 60).coerceIn(0, 59)
        return String.format("%02d:%02d", h, m)
    }

    // 특정 날짜가 해당 월의 달력 기준으로 몇 번째 주차(1~6주차)에 속하는지 오프셋을 역산하여 정확하게 산출
    fun getWeekOfMonth(date: LocalDate): Int {
        val firstDay = date.withDayOfMonth(1)
        val offset = firstDay.dayOfWeek.value % 7
        return (date.dayOfMonth + offset - 1) / 7 + 1
    }

    // 일간 타임테이블 뷰에서 일정 블록들이 동일 시간대에 서로 겹칠 때 가로 공간을 기둥(Column)으로 균등 분할 배정해주는 함수
    fun calculateScheduleColumns(schedules: List<ScheduleEntity>): Pair<Int, Map<Int, Int>> {
        val sortedSchedules = schedules.filter { !it.isAllDay }.sortedBy { timeToMins(it.startTime) }
        val columns = mutableListOf<MutableList<ScheduleEntity>>()
        val scheduleColumnMap = mutableMapOf<Int, Int>() // 일정 ID -> 배치될 Column Index 매핑 지도

        sortedSchedules.forEach { schedule ->
            val startMins = timeToMins(schedule.startTime)
            var placed = false
            for (i in columns.indices) {
                val lastEndMins = timeToMins(columns[i].last().endTime)
                if (lastEndMins <= startMins) {
                    columns[i].add(schedule)
                    scheduleColumnMap[schedule.id] = i
                    placed = true
                    break
                }
            }
            if (!placed) {
                columns.add(mutableListOf(schedule))
                scheduleColumnMap[schedule.id] = columns.size - 1
            }
        }
        return Pair(columns.size.coerceAtLeast(1), scheduleColumnMap)
    }
}