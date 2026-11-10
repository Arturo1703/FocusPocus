package com.arturolopez.focuspocus.blocking

import android.app.Application
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arturolopez.focuspocus.data.preferences.BlockingPreferences
import com.arturolopez.focuspocus.data.preferences.BlockingSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortVideoAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BlockingPreferences(application)
    private var appliedSelectedIds: Set<String> = emptySet()
    private var appliedSchedule = BlockingSchedule()
    private val _apps = MutableStateFlow<List<ShortVideoApp>>(emptyList())
    val apps: StateFlow<List<ShortVideoApp>> = _apps.asStateFlow()
    private val _schedule = MutableStateFlow(BlockingSchedule())
    val schedule: StateFlow<BlockingSchedule> = _schedule.asStateFlow()
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    init {
        observeSelection()
        observeSchedule()
        loadApps()
    }

    fun toggleSelection(appId: String) {
        val app = _apps.value.firstOrNull { it.id == appId }
        if (app?.isInstalled != true) return

        _apps.update { currentApps ->
            currentApps.map { currentApp ->
                if (currentApp.id == appId) {
                    currentApp.copy(isSelected = !currentApp.isSelected)
                } else {
                    currentApp
                }
            }
        }
        updateHasChanges()
    }

    fun setStartMinutes(minutes: Int) {
        _schedule.update { it.copy(startMinutes = minutes.coerceIn(0, 1439)) }
        updateHasChanges()
    }

    fun setEndMinutes(minutes: Int) {
        _schedule.update { it.copy(endMinutes = minutes.coerceIn(0, 1439)) }
        updateHasChanges()
    }

    fun toggleActiveDay(day: Int) {
        if (day !in 1..7) return
        _schedule.update { current ->
            current.copy(
                activeDays = current.activeDays.toMutableSet().apply {
                    if (!add(day)) remove(day)
                },
            )
        }
        updateHasChanges()
    }

    fun applyDraft() {
        val selectedIds = selectedDraftIds()
        val currentSchedule = _schedule.value
        appliedSelectedIds = selectedIds
        appliedSchedule = currentSchedule
        _hasChanges.value = false
        preferences.applyShortVideoConfiguration(selectedIds, currentSchedule)
    }

    fun discardDraft() {
        _schedule.value = appliedSchedule
        _apps.update { currentApps ->
            currentApps.map { app -> app.copy(isSelected = app.id in appliedSelectedIds) }
        }
        _hasChanges.value = false
    }

    private fun observeSelection() {
        viewModelScope.launch {
            preferences.selectedShortVideoApps.collect { selectedApps ->
                appliedSelectedIds = selectedApps
                if (!_hasChanges.value) {
                    _apps.update { currentApps ->
                        currentApps.map { app ->
                            app.copy(isSelected = app.id in selectedApps)
                        }
                    }
                }
            }
        }
    }

    private fun observeSchedule() {
        viewModelScope.launch {
            preferences.shortVideoSchedule.collect { currentSchedule ->
                appliedSchedule = currentSchedule
                if (!_hasChanges.value) {
                    _schedule.value = currentSchedule
                }
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) {
                val packageManager = getApplication<Application>().packageManager

                ShortVideoAppsCatalog.apps.mapNotNull { supportedApp ->
                    val installedPackage = ShortVideoAppsCatalog
                        .installedPackageName(packageManager, supportedApp)
                        ?: return@mapNotNull null

                    val icon = runCatching {
                        packageManager.getApplicationIcon(installedPackage).toBitmap(144, 144)
                    }.getOrNull()

                    ShortVideoApp(
                        id = supportedApp.id,
                        name = supportedApp.name,
                        packageName = installedPackage,
                        icon = icon,
                        isInstalled = true,
                        isSelected = supportedApp.id in appliedSelectedIds,
                    )
                }
            }
        }
    }

    private fun selectedDraftIds(): Set<String> = _apps.value
        .asSequence()
        .filter(ShortVideoApp::isSelected)
        .mapTo(mutableSetOf(), ShortVideoApp::id)

    private fun updateHasChanges() {
        _hasChanges.value = selectedDraftIds() != appliedSelectedIds ||
            _schedule.value != appliedSchedule
    }

}
