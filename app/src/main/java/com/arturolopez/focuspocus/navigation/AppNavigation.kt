package com.arturolopez.focuspocus.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.arturolopez.focuspocus.blocking.BlockingScreen
import com.arturolopez.focuspocus.focus.FocusScreen
import com.arturolopez.focuspocus.profiles.ProfilesScreen
import com.arturolopez.focuspocus.settings.SettingsScreen
import com.arturolopez.focuspocus.statistics.StatisticsScreen

@Composable
fun AppNavigation(backStack: NavBackStack<NavKey>) {
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<AppRoute.Blocking> { BlockingScreen() }
            entry<AppRoute.Focus> { FocusScreen() }
            entry<AppRoute.Profiles> { ProfilesScreen() }
            entry<AppRoute.Statistics> { StatisticsScreen() }
            entry<AppRoute.Settings> { SettingsScreen() }
        },
    )
}
