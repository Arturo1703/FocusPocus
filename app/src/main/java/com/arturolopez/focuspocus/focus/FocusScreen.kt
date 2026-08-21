package com.arturolopez.focuspocus.focus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import com.arturolopez.focuspocus.ui.components.PlaceholderScreen

@Composable
fun FocusScreen() {
    PlaceholderScreen(
        title = "Focus",
        description = "Inicia rápidamente una sesión temporal o un ciclo Pomodoro.",
        icon = Icons.Default.Timer,
    )
}
