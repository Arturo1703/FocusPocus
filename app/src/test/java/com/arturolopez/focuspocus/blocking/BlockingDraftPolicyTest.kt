package com.arturolopez.focuspocus.blocking

import com.arturolopez.focuspocus.data.preferences.BlockingSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingDraftPolicyTest {
    private val appliedApps = setOf("app-a")
    private val expandedApps = setOf("app-a", "app-b")
    private val allWeek = BlockingSchedule()
    private val weekdayEvenings = BlockingSchedule(18 * 60, 22 * 60, (1..5).toSet())

    @Test
    fun addingFirstAppWithCustomScheduleAppliesImmediately() {
        assertFalse(requiresBlockingWait(emptySet(), allWeek, appliedApps, weekdayEvenings))
    }

    @Test
    fun addingAnAppAndExpandingCoverageAppliesImmediately() {
        assertFalse(requiresBlockingWait(appliedApps, weekdayEvenings, expandedApps, allWeek))
    }

    @Test
    fun removingAnAppStillRequiresWaiting() {
        assertTrue(requiresBlockingWait(expandedApps, weekdayEvenings, appliedApps, allWeek))
    }

    @Test
    fun addingAnAppWhileNarrowingAnExistingScheduleStillRequiresWaiting() {
        assertTrue(requiresBlockingWait(appliedApps, allWeek, expandedApps, weekdayEvenings))
    }

    @Test
    fun droppingAnActiveDayStillRequiresWaiting() {
        val fewerDays = weekdayEvenings.copy(activeDays = setOf(1, 2, 3, 4))
        assertTrue(requiresBlockingWait(appliedApps, weekdayEvenings, appliedApps, fewerDays))
    }

    @Test
    fun coverageCanComeFromThePreviousNightsScheduleAcrossTheWeekBoundary() {
        val mondayMorning = BlockingSchedule(0, 2 * 60, setOf(1))
        val sundayNight = BlockingSchedule(23 * 60, 2 * 60, setOf(7))
        assertFalse(requiresBlockingWait(appliedApps, mondayMorning, appliedApps, sundayNight))
        assertTrue(requiresBlockingWait(appliedApps, sundayNight, appliedApps, mondayMorning))
    }

    @Test
    fun losingOnlyTheLastMinuteOfAnOvernightScheduleStillRequiresWaiting() {
        val overnight = BlockingSchedule(23 * 60, 2 * 60, setOf(7))
        val shorter = overnight.copy(endMinutes = overnight.endMinutes - 1)
        assertTrue(requiresBlockingWait(appliedApps, overnight, appliedApps, shorter))
    }
}
