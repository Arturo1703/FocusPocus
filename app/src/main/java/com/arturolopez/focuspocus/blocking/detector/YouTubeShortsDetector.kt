package com.arturolopez.focuspocus.blocking.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object YouTubeShortsDetector {

    data class Inspection(val visiblePlayerIds: Set<String>) {
        val isPlayerOpen: Boolean
            get() = visiblePlayerIds.size >= 2 && visiblePlayerIds.any(PLAYER_CONTAINER_IDS::contains)
    }

    fun inspect(root: AccessibilityNodeInfo): Inspection? {
        val windowBounds = Rect().also(root::getBoundsInScreen)
        if (windowBounds.isEmpty) return null

        val minPlayerHeight = windowBounds.height() * MIN_PLAYER_HEIGHT_RATIO
        val visiblePlayerIds = mutableSetOf<String>()
        val nodeBounds = Rect()
        val pendingNodes = ArrayDeque<AccessibilityNodeInfo>()
        pendingNodes.add(root)

        var visitedNodes = 0
        while (pendingNodes.isNotEmpty() && visitedNodes < MAX_NODES) {
            val node = pendingNodes.removeFirst()
            visitedNodes++

            val viewId = node.viewIdResourceName
            if (viewId != null && viewId in PLAYER_VIEW_IDS && node.isVisibleToUser) {
                node.getBoundsInScreen(nodeBounds)
                if (nodeBounds.intersect(windowBounds) && nodeBounds.height() >= minPlayerHeight) {
                    visiblePlayerIds.add(viewId)
                }
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pendingNodes::add)
            }
        }

        return Inspection(visiblePlayerIds)
    }

    private const val MAX_NODES = 500
    private const val MIN_PLAYER_HEIGHT_RATIO = 0.6
    private const val VIEW_ID_PREFIX = "com.google.android.youtube:id/"

    private val PLAYER_CONTAINER_IDS = setOf(
        "${VIEW_ID_PREFIX}reel_recycler",
        "${VIEW_ID_PREFIX}reel_recycler_view",
        "${VIEW_ID_PREFIX}reel_watch_fragment_root",
        "${VIEW_ID_PREFIX}reel_player_page",
        "${VIEW_ID_PREFIX}reel_player_page_container",
    )

    private val PLAYER_VIEW_IDS = PLAYER_CONTAINER_IDS + setOf(
        "${VIEW_ID_PREFIX}reel_player_overlay",
        "${VIEW_ID_PREFIX}reel_scrim_shorts",
        "${VIEW_ID_PREFIX}shorts_player",
        "${VIEW_ID_PREFIX}shorts_video_container",
    )
}
