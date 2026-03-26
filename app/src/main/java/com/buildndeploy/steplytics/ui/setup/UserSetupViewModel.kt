package com.buildndeploy.steplytics.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buildndeploy.steplytics.domain.model.UserProfile
import com.buildndeploy.steplytics.domain.usecase.SaveUserProfileUseCase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserSetupViewModel(
    private val saveUserProfileUseCase: SaveUserProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UserSetupState())
    val state: StateFlow<UserSetupState> = _state.asStateFlow()

    private val _sideEffects = MutableSharedFlow<UserSetupSideEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sideEffects: SharedFlow<UserSetupSideEffect> = _sideEffects.asSharedFlow()

    fun process(intent: UserSetupIntent) {
        when (intent) {
            is UserSetupIntent.OnValueChanged -> updateField(intent.field, intent.value)
            is UserSetupIntent.OnGenderSelected -> _state.update {
                it.copy(gender = intent.value, genderError = null)
            }
            UserSetupIntent.OnContinueClicked -> persistProfile()
        }
    }

    private fun updateField(field: UserProfileField, value: String) {
        _state.update {
            when (field) {
                UserProfileField.NAME -> it.copy(name = value, nameError = null)
                UserProfileField.EMAIL -> it.copy(email = value.trim(), emailError = null)
                UserProfileField.AGE -> it.copy(age = value.filter(Char::isDigit), ageError = null)
                UserProfileField.WEIGHT -> it.copy(weight = value.filterAllowedDecimal(), weightError = null)
                UserProfileField.HEIGHT -> it.copy(height = value.filterAllowedDecimal(), heightError = null)
            }
        }
    }

    private fun persistProfile() {
        val currentState = _state.value
        val name = currentState.name.trim()
        val email = currentState.email.trim()
        val age = currentState.age.toIntOrNull()
        val weight = currentState.weight.toFloatOrNull()
        val height = currentState.height.toFloatOrNull()
        val nameError = if (name.isBlank()) "Enter your name" else null
        val emailError = when {
            email.isBlank() -> "Enter your email"
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) -> "Enter a valid email"
            else -> null
        }
        val ageError = when {
            age == null -> "Enter a valid age"
            age !in 10..120 -> "Age must be between 10 and 120"
            else -> null
        }
        val weightError = when {
            weight == null -> "Enter a valid weight"
            weight <= 0f -> "Weight must be greater than 0"
            else -> null
        }
        val heightError = when {
            height == null -> "Enter a valid height"
            height <= 0f -> "Height must be greater than 0"
            else -> null
        }
        val genderError = if (currentState.gender.isBlank()) "Select a gender" else null

        if (nameError != null || emailError != null || ageError != null || weightError != null || heightError != null || genderError != null) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    ageError = ageError,
                    weightError = weightError,
                    heightError = heightError,
                    genderError = genderError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            saveUserProfileUseCase(
                UserProfile(
                    name = name,
                    email = email,
                    age = requireNotNull(age),
                    weight = requireNotNull(weight),
                    height = requireNotNull(height),
                    gender = currentState.gender
                )
            )
            _state.update { it.copy(isSaving = false) }
            _sideEffects.emit(UserSetupSideEffect.NavigateToHome)
        }
    }

    private fun String.filterAllowedDecimal(): String {
        val filtered = filter { it.isDigit() || it == '.' }
        val firstDecimalIndex = filtered.indexOf('.')
        if (firstDecimalIndex == -1) return filtered
        return buildString {
            filtered.forEachIndexed { index, char ->
                if (char != '.' || index == firstDecimalIndex) {
                    append(char)
                }
            }
        }
    }

    class Factory(
        private val saveUserProfileUseCase: SaveUserProfileUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserSetupViewModel(saveUserProfileUseCase) as T
        }
    }
}
