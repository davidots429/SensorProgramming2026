package com.davidots.planmind.ui.sleep

// 수면 시간 계산 및 적정성 판별 로직을 처리하는 유틸리티 객체
object SleepTimeUtil {

    // "HH:mm" 문자열을 00:00 기준 누적 '분' 정수로 변환[cite: 5]
    fun timeToMins(t: String): Int {
        val parts = t.split(":")
        return if (parts.size == 2) parts[0].toInt() * 60 + parts[1].toInt() else 0
    }

    // 취침 시간과 기상 시간을 비교하여 총 수면 시간(분)을 계산 (자정 넘김 처리 포함)[cite: 5]
    fun calculateDurationMins(sleepTime: String, wakeTime: String): Int {
        val sMins = timeToMins(sleepTime)
        val wMins = timeToMins(wakeTime)
        var duration = wMins - sMins
        if (duration < 0) duration += 24 * 60
        return duration
    }

    // 최소 수면 시간인 6시간(360분)을 충족하는지 판별[cite: 5]
    fun isEnoughSleep(durationMins: Int): Boolean {
        return durationMins >= 6 * 60
    }

    // Deep Sleep이 가능한 권장 취침 시간(저녁 6시 ~ 새벽 2시 사이)인지 판별[cite: 5]
    fun isGoodSleepTime(sleepTime: String): Boolean {
        val sMins = timeToMins(sleepTime)
        return sMins >= 18 * 60 || sMins <= 2 * 60
    }
}