package com.arturolopez.focuspocus.data.preferences

import java.time.LocalDateTime

data class BlockingSchedule(
    val startMinutes: Int = 0,
    val endMinutes: Int = MINUTES_PER_DAY - 1,
    val activeDays: Set<Int> = ALL_DAYS,
) {
    fun isActiveAt(dateTime: LocalDateTime): Boolean =
        isActiveAt(dateTime.dayOfWeek.value, dateTime.hour * 60 + dateTime.minute)

    internal fun covers(other: BlockingSchedule): Boolean {
        if (this == other || other.activeDays.isEmpty()) return true

        // Between these boundaries, neither schedule can change its state.
        val boundaries = setOf(0, startMinutes, endMinutes + 1, other.startMinutes, other.endMinutes + 1)
            .filter { it in 0 until MINUTES_PER_DAY }
        return ALL_DAYS.all { day ->
            boundaries.none { minute -> other.isActiveAt(day, minute) && !isActiveAt(day, minute) }
        }
    }

    private fun isActiveAt(currentDay: Int, currentMinutes: Int): Boolean {
        if (activeDays.isEmpty()) return false

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
