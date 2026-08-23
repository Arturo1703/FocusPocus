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
    private var selectedPackages: Set<String> = emptySet()

    private val _apps = MutableStateFlow<List<BlockableApp>>(emptyList())
    val apps: StateFlow<List<BlockableApp>> = _apps.asStateFlow()

    private val _schedule = MutableStateFlow(BlockingSchedule())
    val schedule: StateFlow<BlockingSchedule> = _schedule.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppBlockCategory.SOCIAL)
    val selectedCategory: StateFlow<AppBlockCategory> = _selectedCategory.asStateFlow()

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
        preferences.toggleBlockedApp(packageName)
    }

    fun setStartMinutes(minutes: Int) = preferences.setAppBlockingStartMinutes(minutes)

    fun setEndMinutes(minutes: Int) = preferences.setAppBlockingEndMinutes(minutes)

    fun toggleActiveDay(day: Int) = preferences.toggleAppBlockingActiveDay(day)

    private fun observeSelection() {
        viewModelScope.launch {
            preferences.selectedBlockedApps.collect { blockedPackages ->
                selectedPackages = blockedPackages
                _apps.update { apps ->
                    apps.map { app -> app.copy(isSelected = app.packageName in blockedPackages) }
                }
            }
        }
    }

    private fun observeSchedule() {
        viewModelScope.launch {
            preferences.appBlockingSchedule.collect { currentSchedule ->
                _schedule.value = currentSchedule
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
                        isSelected = installedApp.packageName in selectedPackages,
                    )
                }
            }
        }
    }
}
