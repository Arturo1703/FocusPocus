package com.arturolopez.focuspocus.statistics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.Composable
import com.arturolopez.focuspocus.ui.components.PlaceholderScreen

@Composable
fun StatisticsScreen() {
    PlaceholderScreen(
        title = "Estadísticas",
        description = "Consulta tus bloqueos, sesiones completadas y tiempo de concentración.",
        icon = Icons.Default.BarChart,
    )
}
