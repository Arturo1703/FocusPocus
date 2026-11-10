package com.arturolopez.focuspocus.blocking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arturolopez.focuspocus.data.preferences.BlockingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlockingActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BlockingPreferences(application)
    private val _pauseUntilEpochMillis = MutableStateFlow(0L)
    val pauseUntilEpochMillis: StateFlow<Long> = _pauseUntilEpochMillis.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.restrictionsPausedUntilEpochMillis.collect { pausedUntil ->
                _pauseUntilEpochMillis.value = pausedUntil
            }
        }
    }

    fun pauseRestrictions(durationMinutes: Int) {
        preferences.pauseRestrictions(durationMinutes)
    }
}
