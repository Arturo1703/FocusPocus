package com.arturolopez.focuspocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.arturolopez.focuspocus.blocking.ShortVideoAppsCatalog
import com.arturolopez.focuspocus.blocking.InstalledAppsCatalog
import com.arturolopez.focuspocus.data.preferences.BlockingPreferences
import com.arturolopez.focuspocus.ui.FocusPocusApp
import com.arturolopez.focuspocus.ui.theme.FocusPocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        removeBlocksForUninstalledApps()

        setContent {
            FocusPocusTheme {
                FocusPocusApp()
            }
        }
    }

    private fun removeBlocksForUninstalledApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val installedAppIds = ShortVideoAppsCatalog.installedAppIds(packageManager)
            val blockablePackages = InstalledAppsCatalog
                .loadBlockableApps(applicationContext)
                .mapTo(mutableSetOf()) { app -> app.packageName }
            BlockingPreferences(applicationContext).apply {
                retainInstalledShortVideoApps(installedAppIds)
                retainInstalledBlockedApps(blockablePackages)
            }
        }
    }
}
