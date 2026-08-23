package com.arturolopez.focuspocus.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.arturolopez.focuspocus.blocking.detector.FacebookReelsDetector
import com.arturolopez.focuspocus.blocking.detector.InstagramReelsDetector
import com.arturolopez.focuspocus.blocking.detector.TikTokDetector
import com.arturolopez.focuspocus.blocking.detector.XVideoFeedDetector
import com.arturolopez.focuspocus.blocking.detector.YouTubeShortsDetector
import com.arturolopez.focuspocus.data.preferences.BlockingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FocusPocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var selectedShortVideoApps: Set<String> = emptySet()
    private var lastBlockingActionEpochMillis = 0L
    private var instagramEntryArmed = true
    private var xVideoFeedActionLatched = false
    private var facebookReelActionLatched = false
    private var facebookImmersiveWindowActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateServiceEventFilter(emptySet())

        serviceScope.launch {
            BlockingPreferences(applicationContext).selectedShortVideoApps.collectLatest { selectedApps ->
                selectedShortVideoApps = selectedApps
                resetDetectorState()
                updateServiceEventFilter(selectedApps)
            }
        }
    }

    private fun updateServiceEventFilter(selectedApps: Set<String>) {
        val monitoredPackages = selectedApps
            .flatMapTo(mutableSetOf()) { appId -> MONITORED_PACKAGES_BY_APP_ID[appId].orEmpty() }

        serviceInfo = serviceInfo.apply {
            eventTypes = if (monitoredPackages.isEmpty()) 0 else MONITORED_EVENT_TYPES
            packageNames = monitoredPackages.toTypedArray()
            notificationTimeout = EVENT_NOTIFICATION_TIMEOUT_MILLIS
        }
    }

    private fun resetDetectorState() {
        instagramEntryArmed = true
        xVideoFeedActionLatched = false
        facebookReelActionLatched = false
        facebookImmersiveWindowActive = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (packageName != INSTAGRAM_PACKAGE) {
            instagramEntryArmed = true
        }
        if (packageName != X_PACKAGE) {
            xVideoFeedActionLatched = false
        }
        if (packageName != FACEBOOK_PACKAGE) {
            facebookImmersiveWindowActive = false
            facebookReelActionLatched = false
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val windowClass = event.className?.toString().orEmpty()
            val wasImmersive = facebookImmersiveWindowActive
            facebookImmersiveWindowActive = windowClass.contains(
                FACEBOOK_IMMERSIVE_ACTIVITY,
                ignoreCase = true,
            )
            if (facebookImmersiveWindowActive != wasImmersive) {
                facebookReelActionLatched = false
            }
        }

        if (TIKTOK_APP_ID in selectedShortVideoApps && TikTokDetector.isTikTok(packageName)) {
            exitTikTok()
            return
        }

        if (packageName == INSTAGRAM_PACKAGE && INSTAGRAM_APP_ID in selectedShortVideoApps) {
            blockInstagramReel(event)
            return
        }

        if (packageName == X_PACKAGE && X_APP_ID in selectedShortVideoApps) {
            inspectXVideoFeed(event.source)
            return
        }

        if (packageName == FACEBOOK_PACKAGE && FACEBOOK_APP_ID in selectedShortVideoApps) {
            inspectFacebookReel(event.source)
            return
        }

        if (packageName != YOUTUBE_PACKAGE) return
        if (YOUTUBE_APP_ID !in selectedShortVideoApps) return

        val detection = YouTubeShortsDetector.detect(rootInActiveWindow) ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockingActionEpochMillis < ACTION_COOLDOWN_MILLIS) return

        lastBlockingActionEpochMillis = now
        Log.i(TAG, "YouTube Short detectado: ${detection.reason}")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun blockInstagramReel(event: AccessibilityEvent) {
        val detection = InstagramReelsDetector.detectFromEventSource(event.source)
        if (detection == null &&
            (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        ) {
            instagramEntryArmed = true
            return
        }
        if (detection == null || !instagramEntryArmed) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockingActionEpochMillis < ACTION_COOLDOWN_MILLIS) return

        instagramEntryArmed = false
        lastBlockingActionEpochMillis = now
        Log.i(TAG, "Instagram Reel detectado: ${detection.reason}")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun exitTikTok() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockingActionEpochMillis < ACTION_COOLDOWN_MILLIS) return

        lastBlockingActionEpochMillis = now
        Log.i(TAG, "TikTok detectado: saliendo a Inicio")
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun inspectXVideoFeed(eventSource: android.view.accessibility.AccessibilityNodeInfo?) {
        val sourceInspection = XVideoFeedDetector.inspect(eventSource)
        val inspection = if (sourceInspection.detection != null) {
            sourceInspection
        } else {
            val rootInspection = XVideoFeedDetector.inspect(rootInActiveWindow)
            XVideoFeedDetector.Inspection(
                detection = rootInspection.detection,
                candidateViewIds = sourceInspection.candidateViewIds + rootInspection.candidateViewIds,
            )
        }
        val detection = inspection.detection
        if (detection != null) {
            if (xVideoFeedActionLatched) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastBlockingActionEpochMillis < ACTION_COOLDOWN_MILLIS) return

            xVideoFeedActionLatched = true
            lastBlockingActionEpochMillis = now
            Log.i(TAG, "Feed vertical de X detectado: ${detection.reason}")
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        xVideoFeedActionLatched = false

    }

    private fun inspectFacebookReel(eventSource: android.view.accessibility.AccessibilityNodeInfo?) {
        val sourceInspection = FacebookReelsDetector.inspect(eventSource)
        val rootInspection = FacebookReelsDetector.inspect(rootInActiveWindow)
        val detection = sourceInspection.detection ?: rootInspection.detection

        val isConfirmedReelsSurface = facebookImmersiveWindowActive ||
            rootInspection.isReelsTabSelected ||
            sourceInspection.hasFullscreenReelDetails
        if (!isConfirmedReelsSurface) {
            facebookReelActionLatched = false
            return
        }
        if (detection == null || facebookReelActionLatched) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockingActionEpochMillis < ACTION_COOLDOWN_MILLIS) return

        facebookReelActionLatched = true
        lastBlockingActionEpochMillis = now
        Log.i(TAG, "Facebook Reel detectado: ${detection.reason}")
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
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        const val INSTAGRAM_APP_ID = "instagram"
        const val TIKTOK_APP_ID = "tiktok"
        const val X_PACKAGE = "com.twitter.android"
        const val X_APP_ID = "x"
        const val FACEBOOK_PACKAGE = "com.facebook.katana"
        const val FACEBOOK_APP_ID = "facebook"
        const val FACEBOOK_IMMERSIVE_ACTIVITY = "immersiveactivity"
        const val ACTION_COOLDOWN_MILLIS = 2_000L
        const val EVENT_NOTIFICATION_TIMEOUT_MILLIS = 50L
        const val MONITORED_EVENT_TYPES =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_CLICKED

        val MONITORED_PACKAGES_BY_APP_ID = mapOf(
            YOUTUBE_APP_ID to setOf(YOUTUBE_PACKAGE),
            INSTAGRAM_APP_ID to setOf(INSTAGRAM_PACKAGE),
            TIKTOK_APP_ID to setOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
            ),
            FACEBOOK_APP_ID to setOf(FACEBOOK_PACKAGE),
            X_APP_ID to setOf(X_PACKAGE),
        )
    }
}
