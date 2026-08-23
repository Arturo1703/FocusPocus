package com.arturolopez.focuspocus.blocking

import android.graphics.Bitmap

data class BlockableApp(
    val packageName: String,
    val name: String,
    val icon: Bitmap?,
    val category: AppBlockCategory,
    val isSelected: Boolean = false,
)
