package com.buildndeploy.steplytics.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buildndeploy.steplytics.domain.usecase.CompleteOnboardingUseCase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _sideEffects = MutableSharedFlow<OnboardingSideEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val sideEffects: SharedFlow<OnboardingSideEffect> = _sideEffects.asSharedFlow()

    fun process(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.OnPageSwiped -> {
                _state.update { it.copy(currentPageIndex = intent.pageIndex) }
            }

            OnboardingIntent.OnGetStartedClicked -> {
                viewModelScope.launch {
                    completeOnboardingUseCase()
                    _sideEffects.emit(OnboardingSideEffect.NavigateToSetup)
                }
            }
        }
    }

    class Factory(
        private val completeOnboardingUseCase: CompleteOnboardingUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(completeOnboardingUseCase) as T
        }
    }
}
