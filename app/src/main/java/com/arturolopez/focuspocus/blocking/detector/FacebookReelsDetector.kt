package com.arturolopez.focuspocus.blocking.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object FacebookReelsDetector {

    data class Inspection(
        val detection: Detection?,
        val isReelsTabSelected: Boolean,
        val hasFullscreenReelDetails: Boolean,
    )

    data class Detection(
        val reason: String,
    )

    fun inspect(root: AccessibilityNodeInfo?): Inspection {
        if (root == null) return Inspection(null, false, false)

        val rootBounds = Rect().also(root::getBoundsInScreen)

        val pendingNodes = ArrayDeque<AccessibilityNodeInfo>()
        pendingNodes.add(root)
        var hasReelDetails = false
        var hasVideoControls = false
        var hasScrollableVideoPager = false
        var isReelsTabSelected = false
        var hasFullscreenReelDetails = false

        var visitedNodes = 0
        while (pendingNodes.isNotEmpty() && visitedNodes < MAX_NODES) {
            val node = pendingNodes.removeFirst()
            visitedNodes++

            val description = node.contentDescription?.toString()?.lowercase().orEmpty()
            val text = node.text?.toString()?.lowercase().orEmpty()
            if (description == "detalles del reel" || description == "reel details") {
                val bounds = Rect().also(node::getBoundsInScreen)
                val coversMostOfScreen = rootBounds.width() > 0 && rootBounds.height() > 0 &&
                    bounds.width() >= rootBounds.width() * 9 / 10 &&
                    bounds.height() >= rootBounds.height() * 4 / 5 &&
                    bounds.top <= rootBounds.top + rootBounds.height() / 10
                if (coversMostOfScreen) hasFullscreenReelDetails = true
            }
            if (node.isSelected &&
                (description == "reels" ||
                    text == "reels" ||
                    description.startsWith("reels, pesta\u00f1a") ||
                    description.startsWith("reels, tab"))
            ) {
                isReelsTabSelected = true
            }
            if (description == "detalles de la pesta\u00f1a reels" ||
                description == "reels tab details" ||
                description == "detalles del reel" ||
                description == "reel details"
            ) {
                hasReelDetails = true
            }
            if (description == "toca para mostrar los controles de v\u00eddeo" ||
                description == "tap to show video controls"
            ) {
                hasVideoControls = true
            }
            val className = node.className?.toString().orEmpty()
            if (node.isScrollable &&
                (className.endsWith("ViewPager") || className.endsWith("RecyclerView"))
            ) {
                hasScrollableVideoPager = true
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pendingNodes::add)
            }
        }

        if (hasReelDetails &&
            hasVideoControls &&
            hasScrollableVideoPager
        ) {
            return Inspection(
                detection = Detection(
                    reason = "reel-details+video-controls+scrollable-pager",
                ),
                isReelsTabSelected = isReelsTabSelected,
                hasFullscreenReelDetails = hasFullscreenReelDetails,
            )
        }

        return Inspection(
            detection = null,
            isReelsTabSelected = isReelsTabSelected,
            hasFullscreenReelDetails = hasFullscreenReelDetails,
        )
    }

    private const val MAX_NODES = 600
}
