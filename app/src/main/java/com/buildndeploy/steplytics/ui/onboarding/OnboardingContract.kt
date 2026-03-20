package com.buildndeploy.steplytics.ui.onboarding

sealed interface OnboardingIntent {
    data class OnPageSwiped(val pageIndex: Int) : OnboardingIntent
    data object OnGetStartedClicked : OnboardingIntent
}

data class OnboardingState(
    val currentPageIndex: Int = 0,
    val pages: List<OnboardingPage> = OnboardingPage.defaults
)

sealed interface OnboardingSideEffect {
    data object NavigateToSetup : OnboardingSideEffect
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val iconLabel: String
) {
    companion object {
        val defaults = listOf(
            OnboardingPage(
                title = "Track Your Activities",
                description = "Monitor your steps, distance, and calories with precision tracking.",
                iconLabel = "S"
            ),
            OnboardingPage(
                title = "Understand Your Progress",
                description = "Turn movement into trends you can review day after day.",
                iconLabel = "P"
            ),
            OnboardingPage(
                title = "Personalized Insights",
                description = "Use your profile data to power better calorie estimates.",
                iconLabel = "I"
            )
        )
    }
}
