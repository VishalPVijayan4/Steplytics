package com.buildndeploy.steplytics.ui.root

sealed interface AppScreen {
    data object Loading : AppScreen
    data object Onboarding : AppScreen
    data object Setup : AppScreen
    data object Home : AppScreen
}
