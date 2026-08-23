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
    private var selectedIds: Set<String> = emptySet()
    private val _apps = MutableStateFlow<List<ShortVideoApp>>(emptyList())
    val apps: StateFlow<List<ShortVideoApp>> = _apps.asStateFlow()
    private val _schedule = MutableStateFlow(BlockingSchedule())
    val schedule: StateFlow<BlockingSchedule> = _schedule.asStateFlow()

    init {
        observeSelection()
        observeSchedule()
        loadApps()
    }

    fun toggleSelection(appId: String) {
        val app = _apps.value.firstOrNull { it.id == appId }
        if (app?.isInstalled != true) return

        viewModelScope.launch {
            preferences.toggleShortVideoApp(appId)
        }
    }

    fun setStartMinutes(minutes: Int) = preferences.setShortVideoStartMinutes(minutes)

    fun setEndMinutes(minutes: Int) = preferences.setShortVideoEndMinutes(minutes)

    fun toggleActiveDay(day: Int) = preferences.toggleShortVideoActiveDay(day)

    private fun observeSelection() {
        viewModelScope.launch {
            preferences.selectedShortVideoApps.collect { selectedApps ->
                selectedIds = selectedApps
                _apps.update { currentApps ->
                    currentApps.map { app ->
                        app.copy(isSelected = app.id in selectedApps)
                    }
                }
            }
        }
    }

    private fun observeSchedule() {
        viewModelScope.launch {
            preferences.shortVideoSchedule.collect { currentSchedule ->
                _schedule.value = currentSchedule
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
                        isSelected = supportedApp.id in selectedIds,
                    )
                }
            }
        }
    }

}
