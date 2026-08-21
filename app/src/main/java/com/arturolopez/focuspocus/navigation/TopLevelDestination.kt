package com.arturolopez.focuspocus.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
) {
    BLOCKING(AppRoute.Blocking, "Blocking", Icons.Default.VisibilityOff),
    FOCUS(AppRoute.Focus, "Focus", Icons.Default.Timer),
    PROFILES(AppRoute.Profiles, "Perfiles", Icons.Default.AccountCircle),
    STATISTICS(AppRoute.Statistics, "Estadísticas", Icons.Default.BarChart),
    SETTINGS(AppRoute.Settings, "Ajustes", Icons.Default.Settings),
}
