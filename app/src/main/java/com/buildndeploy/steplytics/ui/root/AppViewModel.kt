package com.buildndeploy.steplytics.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.buildndeploy.steplytics.domain.model.UserProfile
import com.buildndeploy.steplytics.domain.usecase.ObserveIsFirstLaunchUseCase
import com.buildndeploy.steplytics.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    observeIsFirstLaunchUseCase: ObserveIsFirstLaunchUseCase,
    observeUserProfileUseCase: ObserveUserProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeIsFirstLaunchUseCase(),
                observeUserProfileUseCase()
            ) { isFirstLaunch, userProfile ->
                when {
                    isFirstLaunch -> AppScreen.Onboarding
                    userProfile == null -> AppScreen.Setup
                    else -> AppScreen.Home
                } to userProfile
            }.collect { (screen, userProfile) ->
                _state.update {
                    it.copy(
                        currentScreen = screen,
                        userProfile = userProfile,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun navigateToSetup() {
        _state.update { it.copy(currentScreen = AppScreen.Setup) }
    }

    fun navigateToHome() {
        _state.update { it.copy(currentScreen = AppScreen.Home) }
    }

    data class AppUiState(
        val isLoading: Boolean = true,
        val currentScreen: AppScreen = AppScreen.Loading,
        val userProfile: UserProfile? = null
    )

    class Factory(
        private val observeIsFirstLaunchUseCase: ObserveIsFirstLaunchUseCase,
        private val observeUserProfileUseCase: ObserveUserProfileUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(
                observeIsFirstLaunchUseCase = observeIsFirstLaunchUseCase,
                observeUserProfileUseCase = observeUserProfileUseCase
            ) as T
        }
    }
}
