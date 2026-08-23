package com.arturolopez.focuspocus.blocking

import android.content.pm.PackageManager
import android.os.Build

data class SupportedShortVideoApp(
    val id: String,
    val name: String,
    val packageNames: List<String>,
)

object ShortVideoAppsCatalog {

    val apps = listOf(
        SupportedShortVideoApp("youtube", "YouTube", listOf("com.google.android.youtube")),
        SupportedShortVideoApp("instagram", "Instagram", listOf("com.instagram.android")),
        SupportedShortVideoApp(
            "tiktok",
            "TikTok",
            listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        ),
        SupportedShortVideoApp("facebook", "Facebook", listOf("com.facebook.katana")),
        SupportedShortVideoApp("x", "X", listOf("com.twitter.android")),
    )

    fun installedPackageName(
        packageManager: PackageManager,
        app: SupportedShortVideoApp,
    ): String? = app.packageNames.firstOrNull { packageName ->
        packageManager.isPackageEnabled(packageName)
    }

    fun installedAppIds(packageManager: PackageManager): Set<String> = apps
        .filter { app -> installedPackageName(packageManager, app) != null }
        .mapTo(mutableSetOf(), SupportedShortVideoApp::id)

    private fun PackageManager.isPackageEnabled(packageName: String): Boolean =
        try {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                getApplicationInfo(packageName, 0)
            }
            applicationInfo.enabled
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
