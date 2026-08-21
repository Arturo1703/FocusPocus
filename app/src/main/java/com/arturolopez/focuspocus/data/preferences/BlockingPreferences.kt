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

    private companion object {
        const val PREFERENCES_NAME = "blocking_preferences"
        const val SELECTED_SHORT_VIDEO_APPS = "selected_short_video_apps"
    }
}
