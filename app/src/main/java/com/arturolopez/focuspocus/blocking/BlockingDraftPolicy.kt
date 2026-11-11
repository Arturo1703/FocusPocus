package com.arturolopez.focuspocus.blocking

import com.arturolopez.focuspocus.data.preferences.BlockingSchedule

internal fun requiresBlockingWait(
    appliedApps: Set<String>,
    appliedSchedule: BlockingSchedule,
    draftApps: Set<String>,
    draftSchedule: BlockingSchedule,
): Boolean = appliedApps.isNotEmpty() &&
    (!draftApps.containsAll(appliedApps) || !draftSchedule.covers(appliedSchedule))
