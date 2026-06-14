package com.davidots.planmind.ui.sleep

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// 수면 시간 계산 및 적정성 판별 로직을 처리하는 유틸리티 객체
object SleepTimeUtil {
    // 시간 변환을 위한 포매터
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // 문자열("HH:mm")을 LocalTime 객체로 변환해 주는 함수
    private fun parseTime(timeStr: String): LocalTime {
        return LocalTime.parse(timeStr, timeFormatter)
    }

    // 두 시간(시작점, 끝점) 사이의 차이를 '분' 단위로 계산해 주는 함수
    fun getDurationMinutes(start: LocalTime, end: LocalTime): Int {
        var durationMins = ChronoUnit.MINUTES.between(start, end).toInt()
        if (durationMins < 0) {
            durationMins += 24 * 60 // 자정을 넘긴 경우 24시간(1440분)을 더해줌
        }
        return durationMins
    }

    // 취침 시간과 기상 시간을 비교하여 총 수면 시간(분)을 계산
    fun calculateDurationMins(sleepTimeStr: String, wakeTimeStr: String): Int {
        return getDurationMinutes(parseTime(sleepTimeStr), parseTime(wakeTimeStr))
    }

    // 최소 수면 시간인 6시간(360분)을 충족하는지 판별
    fun isEnoughSleep(durationMins: Int): Boolean {
        return durationMins >= 6 * 60
    }

    // Deep Sleep이 가능한 권장 취침 시간(저녁 6시 ~ 새벽 2시 이전)인지 판별
    fun isGoodSleepTime(sleepTimeStr: String): Boolean {
        // 복잡한 분(Mins) 계산 대신 LocalTime의 '시간(hour)' 속성을 직접 사용하여 직관성 극대화
        val hour = parseTime(sleepTimeStr).hour
        return hour !in 2..<18
    }

    // 현재 시간이 기상 시간을 넘겼는지 확인
    fun isWakeTimePassed(actualSleepStr: String, wakeTimeStr: String): Boolean {
        val sleepTime = parseTime(actualSleepStr)
        val wakeTime = parseTime(wakeTimeStr)
        val nowTime = LocalTime.now()

        val sleepToWakeMins = getDurationMinutes(sleepTime, wakeTime)
        val sleepToNowMins = getDurationMinutes(sleepTime, nowTime)

        return sleepToNowMins >= sleepToWakeMins
    }

    // 현재 시간이 설정한 취침 시간과 기상 시간 사이에 있는지(수면 윈도우) 확인
    fun isTimeInSleepWindow(nowTimeStr: String, sleepTimeStr: String, wakeTimeStr: String): Boolean {
        val sleepTime = parseTime(sleepTimeStr)
        val wakeTime = parseTime(wakeTimeStr)
        val nowTime = parseTime(nowTimeStr)

        val totalSleepMins = getDurationMinutes(sleepTime, wakeTime)
        val sleepToNowMins = getDurationMinutes(sleepTime, nowTime)

        return sleepToNowMins in 0 until totalSleepMins
    }
}