package com.arturolopez.focuspocus.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BlockingPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    val selectedShortVideoApps: Flow<Set<String>> = callbackFlow {
        fun emitCurrentSelection() {
            trySend(preferences.getStringSet(SELECTED_SHORT_VIDEO_APPS, emptySet()).orEmpty().toSet())
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SELECTED_SHORT_VIDEO_APPS) {
                emitCurrentSelection()
            }
        }

        emitCurrentSelection()
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val shortVideoSchedule: Flow<BlockingSchedule> = callbackFlow {
        fun emitCurrentSchedule() {
            trySend(readShortVideoSchedule())
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in SCHEDULE_KEYS) emitCurrentSchedule()
        }

        emitCurrentSchedule()
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val selectedBlockedApps: Flow<Set<String>> = callbackFlow {
        fun emitCurrentSelection() {
            trySend(preferences.getStringSet(SELECTED_BLOCKED_APPS, emptySet()).orEmpty().toSet())
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SELECTED_BLOCKED_APPS) emitCurrentSelection()
        }

        emitCurrentSelection()
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val appBlockingSchedule: Flow<BlockingSchedule> = callbackFlow {
        fun emitCurrentSchedule() {
            trySend(readAppBlockingSchedule())
        }

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in APP_BLOCKING_SCHEDULE_KEYS) emitCurrentSchedule()
        }

        emitCurrentSchedule()
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    suspend fun toggleShortVideoApp(appId: String) {
        val selectedApps = preferences
            .getStringSet(SELECTED_SHORT_VIDEO_APPS, emptySet())
            .orEmpty()
            .toMutableSet()

        if (!selectedApps.add(appId)) {
            selectedApps.remove(appId)
        }

        preferences.edit()
            .putStringSet(SELECTED_SHORT_VIDEO_APPS, selectedApps)
            .apply()
    }

    fun retainInstalledShortVideoApps(installedAppIds: Set<String>) {
        val selectedApps = preferences
            .getStringSet(SELECTED_SHORT_VIDEO_APPS, emptySet())
            .orEmpty()
        val validSelection = selectedApps.intersect(installedAppIds)

        if (validSelection != selectedApps) {
            preferences.edit()
                .putStringSet(SELECTED_SHORT_VIDEO_APPS, validSelection)
                .apply()
        }
    }

    fun setShortVideoStartMinutes(minutes: Int) {
        preferences.edit()
            .putInt(SHORT_VIDEO_START_MINUTES, minutes.coerceIn(0, 1439))
            .apply()
    }

    fun setShortVideoEndMinutes(minutes: Int) {
        preferences.edit()
            .putInt(SHORT_VIDEO_END_MINUTES, minutes.coerceIn(0, 1439))
            .apply()
    }

    fun toggleShortVideoActiveDay(day: Int) {
        if (day !in 1..7) return

        val schedule = readShortVideoSchedule()
        val activeDays = schedule.activeDays.toMutableSet().apply {
            if (!add(day)) remove(day)
        }
        preferences.edit()
            .putStringSet(SHORT_VIDEO_ACTIVE_DAYS, activeDays.map(Int::toString).toSet())
            .apply()
    }

    fun toggleBlockedApp(packageName: String) {
        val blockedApps = preferences
            .getStringSet(SELECTED_BLOCKED_APPS, emptySet())
            .orEmpty()
            .toMutableSet()
        if (!blockedApps.add(packageName)) blockedApps.remove(packageName)

        preferences.edit()
            .putStringSet(SELECTED_BLOCKED_APPS, blockedApps)
            .apply()
    }

    fun retainInstalledBlockedApps(installedPackages: Set<String>) {
        val blockedApps = preferences
            .getStringSet(SELECTED_BLOCKED_APPS, emptySet())
            .orEmpty()
        val validSelection = blockedApps.intersect(installedPackages)

        if (validSelection != blockedApps) {
            preferences.edit()
                .putStringSet(SELECTED_BLOCKED_APPS, validSelection)
                .apply()
        }
    }

    fun setAppBlockingStartMinutes(minutes: Int) {
        preferences.edit()
            .putInt(APP_BLOCKING_START_MINUTES, minutes.coerceIn(0, 1439))
            .apply()
    }

    fun setAppBlockingEndMinutes(minutes: Int) {
        preferences.edit()
            .putInt(APP_BLOCKING_END_MINUTES, minutes.coerceIn(0, 1439))
            .apply()
    }

    fun toggleAppBlockingActiveDay(day: Int) {
        if (day !in 1..7) return

        val schedule = readAppBlockingSchedule()
        val activeDays = schedule.activeDays.toMutableSet().apply {
            if (!add(day)) remove(day)
        }
        preferences.edit()
            .putStringSet(APP_BLOCKING_ACTIVE_DAYS, activeDays.map(Int::toString).toSet())
            .apply()
    }

    private fun readShortVideoSchedule(): BlockingSchedule {
        val storedDays = preferences.getStringSet(SHORT_VIDEO_ACTIVE_DAYS, null)
        val activeDays = storedDays
            ?.mapNotNull(String::toIntOrNull)
            ?.filterTo(mutableSetOf()) { day -> day in 1..7 }
            ?: BlockingSchedule.ALL_DAYS

        return BlockingSchedule(
            startMinutes = preferences.getInt(SHORT_VIDEO_START_MINUTES, 0),
            endMinutes = preferences.getInt(SHORT_VIDEO_END_MINUTES, 1439),
            activeDays = activeDays,
        )
    }

    private fun readAppBlockingSchedule(): BlockingSchedule {
        val storedDays = preferences.getStringSet(APP_BLOCKING_ACTIVE_DAYS, null)
        val activeDays = storedDays
            ?.mapNotNull(String::toIntOrNull)
            ?.filterTo(mutableSetOf()) { day -> day in 1..7 }
            ?: BlockingSchedule.ALL_DAYS

        return BlockingSchedule(
            startMinutes = preferences.getInt(APP_BLOCKING_START_MINUTES, 0),
            endMinutes = preferences.getInt(APP_BLOCKING_END_MINUTES, 1439),
            activeDays = activeDays,
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "blocking_preferences"
        const val SELECTED_SHORT_VIDEO_APPS = "selected_short_video_apps"
        const val SHORT_VIDEO_START_MINUTES = "short_video_start_minutes"
        const val SHORT_VIDEO_END_MINUTES = "short_video_end_minutes"
        const val SHORT_VIDEO_ACTIVE_DAYS = "short_video_active_days"
        const val SELECTED_BLOCKED_APPS = "selected_blocked_apps"
        const val APP_BLOCKING_START_MINUTES = "app_blocking_start_minutes"
        const val APP_BLOCKING_END_MINUTES = "app_blocking_end_minutes"
        const val APP_BLOCKING_ACTIVE_DAYS = "app_blocking_active_days"
        val SCHEDULE_KEYS = setOf(
            SHORT_VIDEO_START_MINUTES,
            SHORT_VIDEO_END_MINUTES,
            SHORT_VIDEO_ACTIVE_DAYS,
        )
        val APP_BLOCKING_SCHEDULE_KEYS = setOf(
            APP_BLOCKING_START_MINUTES,
            APP_BLOCKING_END_MINUTES,
            APP_BLOCKING_ACTIVE_DAYS,
        )
    }
}
