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

class AppBlockingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BlockingPreferences(application)
    private var appliedSelectedPackages: Set<String> = emptySet()
    private var appliedSchedule = BlockingSchedule()

    private val _apps = MutableStateFlow<List<BlockableApp>>(emptyList())
    val apps: StateFlow<List<BlockableApp>> = _apps.asStateFlow()

    private val _schedule = MutableStateFlow(BlockingSchedule())
    val schedule: StateFlow<BlockingSchedule> = _schedule.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppBlockCategory.SOCIAL)
    val selectedCategory: StateFlow<AppBlockCategory> = _selectedCategory.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    init {
        observeSelection()
        observeSchedule()
        loadApps()
    }

    fun selectCategory(category: AppBlockCategory) {
        _selectedCategory.value = category
    }

    fun toggleSelection(packageName: String) {
        if (_apps.value.none { app -> app.packageName == packageName }) return
        _apps.update { apps ->
            apps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isSelected = !app.isSelected)
                } else {
                    app
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
        val selectedPackages = selectedDraftPackages()
        val currentSchedule = _schedule.value
        appliedSelectedPackages = selectedPackages
        appliedSchedule = currentSchedule
        _hasChanges.value = false
        preferences.applyAppBlockingConfiguration(selectedPackages, currentSchedule)
    }

    fun discardDraft() {
        _schedule.value = appliedSchedule
        _apps.update { currentApps ->
            currentApps.map { app ->
                app.copy(isSelected = app.packageName in appliedSelectedPackages)
            }
        }
        _hasChanges.value = false
    }

    private fun observeSelection() {
        viewModelScope.launch {
            preferences.selectedBlockedApps.collect { blockedPackages ->
                appliedSelectedPackages = blockedPackages
                if (!_hasChanges.value) {
                    _apps.update { apps ->
                        apps.map { app ->
                            app.copy(isSelected = app.packageName in blockedPackages)
                        }
                    }
                }
            }
        }
    }

    private fun observeSchedule() {
        viewModelScope.launch {
            preferences.appBlockingSchedule.collect { currentSchedule ->
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
                val application = getApplication<Application>()
                val packageManager = application.packageManager

                InstalledAppsCatalog.loadBlockableApps(application).map { installedApp ->
                    val icon = runCatching {
                        packageManager.getApplicationIcon(installedApp.applicationInfo)
                            .toBitmap(144, 144)
                    }.getOrNull()

                    BlockableApp(
                        packageName = installedApp.packageName,
                        name = installedApp.name,
                        icon = icon,
                        category = installedApp.category,
                        isSelected = installedApp.packageName in appliedSelectedPackages,
                    )
                }
            }
        }
    }

    private fun selectedDraftPackages(): Set<String> = _apps.value
        .asSequence()
        .filter(BlockableApp::isSelected)
        .mapTo(mutableSetOf(), BlockableApp::packageName)

    private fun updateHasChanges() {
        _hasChanges.value = selectedDraftPackages() != appliedSelectedPackages ||
            _schedule.value != appliedSchedule
    }
}
