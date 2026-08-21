package com.arturolopez.focuspocus.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {

    @Serializable
    data object Blocking : AppRoute

    @Serializable
    data object Focus : AppRoute

    @Serializable
    data object Profiles : AppRoute

    @Serializable
    data object Statistics : AppRoute

    @Serializable
    data object Settings : AppRoute
}