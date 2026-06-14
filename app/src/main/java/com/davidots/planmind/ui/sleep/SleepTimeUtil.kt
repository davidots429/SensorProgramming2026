package com.davidots.planmind.ui.sleep

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// 수면 시간 계산 및 적정성 판별 로직을 처리하는 유틸리티 객체
object SleepTimeUtil {

    // "HH:mm" 문자열을 00:00 기준 누적 '분' 정수로 변환
    fun timeToMins(t: String): Int {
        val parts = t.split(":")
        return if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
    }

    // 취침 시간과 기상 시간을 비교하여 총 수면 시간(분)을 계산 (자정 넘김 처리 포함)
    fun calculateDurationMins(sleepTime: String, wakeTime: String): Int {
        val sMins = timeToMins(sleepTime)
        val wMins = timeToMins(wakeTime)
        var duration = wMins - sMins
        if (duration < 0) duration += 24 * 60
        return duration
    }

    // 최소 수면 시간인 6시간(360분)을 충족하는지 판별
    fun isEnoughSleep(durationMins: Int): Boolean {
        return durationMins >= 6 * 60
    }

    // Deep Sleep이 가능한 권장 취침 시간(저녁 6시 ~ 새벽 2시 사이)인지 판별
    fun isGoodSleepTime(sleepTime: String): Boolean {
        val sMins = timeToMins(sleepTime)
        return sMins >= 18 * 60 || sMins <= 2 * 60
    }

    // 실제 흐른 시간이 자야 할 시간을 넘겼는지 계산
    fun isWakeTimePassed(actualSleepStr: String, wakeTimeStr: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val sleepTime = LocalTime.parse(actualSleepStr, formatter)
        val wakeTime = LocalTime.parse(wakeTimeStr, formatter)
        val nowTime = LocalTime.now()

        var sleepToWakeMins = ChronoUnit.MINUTES.between(sleepTime, wakeTime).toInt()
        if (sleepToWakeMins < 0) sleepToWakeMins += 24 * 60

        var sleepToNowMins = ChronoUnit.MINUTES.between(sleepTime, nowTime).toInt()
        if (sleepToNowMins < 0) sleepToNowMins += 24 * 60

        return sleepToNowMins >= sleepToWakeMins
    }
}