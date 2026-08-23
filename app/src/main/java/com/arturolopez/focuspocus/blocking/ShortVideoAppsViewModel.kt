package com.arturolopez.focuspocus.blocking

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arturolopez.focuspocus.data.preferences.BlockingPreferences
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

    init {
        observeSelection()
        loadApps()
    }

    fun toggleSelection(appId: String) {
        val app = _apps.value.firstOrNull { it.id == appId }
        if (app?.isInstalled != true) return

        viewModelScope.launch {
            preferences.toggleShortVideoApp(appId)
        }
    }

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

    private fun loadApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.IO) {
                val packageManager = getApplication<Application>().packageManager

                supportedApps.map { supportedApp ->
                    val installedPackage = supportedApp.packageNames.firstNotNullOfOrNull { packageName ->
                        packageName.takeIf { packageManager.isInstalled(it) }
                    }

                    val icon = installedPackage?.let { packageName ->
                        runCatching {
                            packageManager.getApplicationIcon(packageName).toBitmap(144, 144)
                        }.getOrNull()
                    }

                    ShortVideoApp(
                        id = supportedApp.id,
                        name = supportedApp.name,
                        packageName = installedPackage,
                        icon = icon,
                        isInstalled = installedPackage != null,
                        isSelected = supportedApp.id in selectedIds,
                    )
                }
            }
        }
    }

    private fun PackageManager.isInstalled(packageName: String): Boolean =
        try {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0)).enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private data class SupportedApp(
        val id: String,
        val name: String,
        val packageNames: List<String>,
    )

    private companion object {
        val supportedApps = listOf(
            SupportedApp("youtube", "YouTube", listOf("com.google.android.youtube")),
            SupportedApp("instagram", "Instagram", listOf("com.instagram.android")),
            SupportedApp(
                "tiktok",
                "TikTok",
                listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            ),
            SupportedApp(
                "facebook",
                "Facebook",
                listOf("com.facebook.katana"),
            ),
            SupportedApp("x", "X", listOf("com.twitter.android")),
        )
    }
}
