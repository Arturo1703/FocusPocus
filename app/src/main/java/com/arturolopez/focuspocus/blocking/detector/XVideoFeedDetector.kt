package com.arturolopez.focuspocus.blocking.detector

import android.view.accessibility.AccessibilityNodeInfo

object XVideoFeedDetector {

    data class Inspection(
        val detection: Detection?,
        val candidateViewIds: Set<String>,
    )

    data class Detection(
        val reason: String,
    )

    fun inspect(root: AccessibilityNodeInfo?): Inspection {
        if (root == null) return Inspection(null, emptySet())

        val candidateViewIds = mutableSetOf<String>()
        val pendingNodes = ArrayDeque<AccessibilityNodeInfo>()
        pendingNodes.add(root)

        var visitedNodes = 0
        while (pendingNodes.isNotEmpty() && visitedNodes < MAX_NODES) {
            val node = pendingNodes.removeFirst()
            visitedNodes++

            node.viewIdResourceName?.let { viewId ->
                val normalizedViewId = viewId.lowercase()
                if (CANDIDATE_MARKERS.any(normalizedViewId::contains)) {
                    candidateViewIds.add(viewId)
                }
                if (normalizedViewId in CONFIRMED_VIDEO_FEED_IDS) {
                    return Inspection(
                        detection = Detection(reason = "viewId:$viewId"),
                        candidateViewIds = candidateViewIds,
                    )
                }
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pendingNodes::add)
            }
        }

        return Inspection(
            detection = null,
            candidateViewIds = candidateViewIds,
        )
    }

    private const val MAX_NODES = 600

    private val CANDIDATE_MARKERS = listOf(
        "video",
        "player",
        "immersive",
        "fullscreen",
        "media",
        "recycler",
        "pager",
    )

    private val CONFIRMED_VIDEO_FEED_IDS = setOf(
        "videotab",
    )
}
