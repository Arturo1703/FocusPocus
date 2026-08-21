package com.arturolopez.focuspocus.blocking.detector

import android.view.accessibility.AccessibilityNodeInfo

object YouTubeShortsDetector {

    data class Detection(
        val reason: String,
    )

    fun detect(root: AccessibilityNodeInfo?): Detection? {
        if (root == null) return null

        val signals = collectSignals(root)

        val strongViewId = signals.viewIds.firstOrNull { viewId ->
            STRONG_VIEW_ID_MARKERS.any(viewId::contains)
        }
        if (strongViewId != null) {
            return Detection(reason = "viewId:$strongViewId")
        }

        val hasShortsMarker = signals.labels.any { label ->
            SHORTS_LABEL_MARKERS.any(label::contains)
        }
        val actionScore = ACTION_MARKER_GROUPS.count { markerGroup ->
            signals.labels.any { label -> markerGroup.any(label::contains) }
        }

        return if (hasShortsMarker && actionScore >= 3) {
            Detection(reason = "shorts-label+actions:$actionScore")
        } else {
            null
        }
    }

    private fun collectSignals(root: AccessibilityNodeInfo): Signals {
        val labels = mutableSetOf<String>()
        val viewIds = mutableSetOf<String>()
        val pendingNodes = ArrayDeque<AccessibilityNodeInfo>()
        pendingNodes.add(root)

        var visitedNodes = 0
        while (pendingNodes.isNotEmpty() && visitedNodes < MAX_NODES) {
            val node = pendingNodes.removeFirst()
            visitedNodes++

            node.text?.toString()?.normalize()?.takeIf(String::isNotBlank)?.let(labels::add)
            node.contentDescription?.toString()?.normalize()?.takeIf(String::isNotBlank)?.let(labels::add)
            node.viewIdResourceName?.normalize()?.takeIf(String::isNotBlank)?.let(viewIds::add)

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pendingNodes::add)
            }
        }

        return Signals(labels = labels, viewIds = viewIds)
    }

    private fun String.normalize(): String = lowercase().trim()

    private data class Signals(
        val labels: Set<String>,
        val viewIds: Set<String>,
    )

    private const val MAX_NODES = 500

    private val STRONG_VIEW_ID_MARKERS = listOf(
        "reel_watch",
        "reel_player",
        "reel_recycler",
        "shorts_player",
        "shorts_video",
    )

    private val SHORTS_LABEL_MARKERS = listOf(
        "shorts",
        "vídeos cortos",
    )

    private val ACTION_MARKER_GROUPS = listOf(
        listOf("like", "me gusta"),
        listOf("dislike", "no me gusta"),
        listOf("comment", "comentario"),
        listOf("share", "compartir"),
        listOf("remix", "remezclar"),
        listOf("sound", "sonido"),
    )
}
