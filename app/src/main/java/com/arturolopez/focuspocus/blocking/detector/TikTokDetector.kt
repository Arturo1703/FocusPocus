package com.arturolopez.focuspocus.blocking.detector

object TikTokDetector {

    fun isTikTok(packageName: String): Boolean = packageName in TIKTOK_PACKAGES

    private val TIKTOK_PACKAGES = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
    )
}
