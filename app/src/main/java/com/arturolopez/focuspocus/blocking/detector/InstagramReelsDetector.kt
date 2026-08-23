package com.arturolopez.focuspocus.blocking.detector

import android.view.accessibility.AccessibilityNodeInfo

object InstagramReelsDetector {

    data class Detection(
        val reason: String,
    )

    fun detectFromEventSource(source: AccessibilityNodeInfo?): Detection? {
        if (source == null) return null

        var ancestor: AccessibilityNodeInfo? = source
        var visitedAncestors = 0
        while (ancestor != null && visitedAncestors < MAX_ANCESTORS) {
            if (ancestor.isActiveReelsPager()) {
                return Detection(reason = "activeAncestorPager")
            }
            ancestor = ancestor.parent
            visitedAncestors++
        }

        val pendingNodes = ArrayDeque<AccessibilityNodeInfo>()
        pendingNodes.add(source)

        var visitedNodes = 0
        while (pendingNodes.isNotEmpty() && visitedNodes < MAX_NODES) {
            val node = pendingNodes.removeFirst()
            visitedNodes++

            if (node.isActiveReelsPager()) {
                return Detection(reason = "activePager")
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pendingNodes::add)
            }
        }

        return null
    }

    private fun AccessibilityNodeInfo.isActiveReelsPager(): Boolean {
        if (viewIdResourceName != REELS_VIEW_PAGER_ID || !isVisibleToUser) return false
        return refresh() && isVisibleToUser
    }

    private const val MAX_NODES = 500
    private const val MAX_ANCESTORS = 30
    private const val REELS_VIEW_PAGER_ID =
        "com.instagram.android:id/clips_viewer_view_pager"
}
