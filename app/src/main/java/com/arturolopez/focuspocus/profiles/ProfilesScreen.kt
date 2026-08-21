package com.arturolopez.focuspocus.profiles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import com.arturolopez.focuspocus.ui.components.PlaceholderScreen

@Composable
fun ProfilesScreen() {
    PlaceholderScreen(
        title = "Perfiles",
        description = "Crea perfiles de bloqueo y alterna fácilmente entre ellos.",
        icon = Icons.Default.AccountCircle,
    )
}
