package com.buildndeploy.steplytics.ui.setup

sealed interface UserSetupIntent {
    data class OnValueChanged(val field: UserProfileField, val value: String) : UserSetupIntent
    data class OnGenderSelected(val value: String) : UserSetupIntent
    data object OnContinueClicked : UserSetupIntent
}

enum class UserProfileField {
    AGE,
    WEIGHT,
    HEIGHT
}

data class UserSetupState(
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val gender: String = "Male",
    val ageError: String? = null,
    val weightError: String? = null,
    val heightError: String? = null,
    val genderError: String? = null,
    val isSaving: Boolean = false
)

sealed interface UserSetupSideEffect {
    data object NavigateToHome : UserSetupSideEffect
}
