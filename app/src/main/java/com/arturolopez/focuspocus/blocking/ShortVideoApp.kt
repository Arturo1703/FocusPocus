package com.arturolopez.focuspocus.blocking

import android.graphics.Bitmap

data class ShortVideoApp(
    val id: String,
    val name: String,
    val packageName: String?,
    val icon: Bitmap?,
    val isInstalled: Boolean,
    val isSelected: Boolean = false,
)
