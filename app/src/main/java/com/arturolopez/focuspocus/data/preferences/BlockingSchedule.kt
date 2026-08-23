package com.arturolopez.focuspocus.data.preferences

import java.time.LocalDateTime

data class BlockingSchedule(
    val startMinutes: Int = 0,
    val endMinutes: Int = MINUTES_PER_DAY - 1,
    val activeDays: Set<Int> = ALL_DAYS,
) {
    fun isActiveAt(dateTime: LocalDateTime): Boolean {
        if (activeDays.isEmpty()) return false

        val currentDay = dateTime.dayOfWeek.value
        val currentMinutes = dateTime.hour * 60 + dateTime.minute

        return when {
            startMinutes <= endMinutes ->
                currentDay in activeDays && currentMinutes in startMinutes..endMinutes

            currentMinutes >= startMinutes -> currentDay in activeDays
            else -> previousDay(currentDay) in activeDays && currentMinutes <= endMinutes
        }
    }

    private fun previousDay(day: Int): Int = if (day == 1) 7 else day - 1

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        val ALL_DAYS: Set<Int> = (1..7).toSet()
    }
}
