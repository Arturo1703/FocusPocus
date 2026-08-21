package com.arturolopez.focuspocus.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.arturolopez.focuspocus.blocking.detector.YouTubeShortsDetector
import com.arturolopez.focuspocus.data.preferences.BlockingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FocusPocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var selectedShortVideoApps: Set<String> = emptySet()
    private var lastBlockingActionEpochMillis = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceScope.launch {
            BlockingPreferences(applicationContext).selectedShortVideoApps.collectLatest { selectedApps ->
                selectedShortVideoApps = selectedApps
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != YOUTUBE_PACKAGE) return
        if (YOUTUBE_APP_ID !in selectedShortVideoApps) return

        val detection = YouTubeShortsDetector.detect(rootInActiveWindow) ?: return
        val now = System.currentTimeMillis()
        if (now - lastBlockingActionEpochMillis < ACTION_COOLDOWN_MILLIS) return

        lastBlockingActionEpochMillis = now
        Log.i(TAG, "YouTube Short detectado: ${detection.reason}")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "FocusPocusAccessibility"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val YOUTUBE_APP_ID = "youtube"
        const val ACTION_COOLDOWN_MILLIS = 2_000L
    }
}
