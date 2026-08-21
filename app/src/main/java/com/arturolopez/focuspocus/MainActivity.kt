package com.arturolopez.focuspocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arturolopez.focuspocus.ui.FocusPocusApp
import com.arturolopez.focuspocus.ui.theme.FocusPocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FocusPocusTheme {
                FocusPocusApp()
            }
        }
    }
}
