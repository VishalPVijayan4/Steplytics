package com.buildndeploy.steplytics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildndeploy.steplytics.data.local.SteplyticsPreferencesDataSource
import com.buildndeploy.steplytics.data.repository.SteplyticsRepositoryImpl
import com.buildndeploy.steplytics.domain.usecase.CompleteOnboardingUseCase
import com.buildndeploy.steplytics.domain.usecase.ObserveIsFirstLaunchUseCase
import com.buildndeploy.steplytics.domain.usecase.ObserveUserProfileUseCase
import com.buildndeploy.steplytics.domain.usecase.SaveUserProfileUseCase
import com.buildndeploy.steplytics.service.WorkoutTrackingService
import com.buildndeploy.steplytics.ui.home.HomeScreen
import com.buildndeploy.steplytics.ui.onboarding.OnboardingScreen
import com.buildndeploy.steplytics.ui.onboarding.OnboardingViewModel
import com.buildndeploy.steplytics.ui.root.AppScreen
import com.buildndeploy.steplytics.ui.root.AppViewModel
import com.buildndeploy.steplytics.ui.setup.UserSetupScreen
import com.buildndeploy.steplytics.ui.setup.UserSetupViewModel
import com.buildndeploy.steplytics.ui.theme.SteplyticsTheme

class MainActivity : ComponentActivity() {
    private val openTrackingState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val repository = SteplyticsRepositoryImpl(
            SteplyticsPreferencesDataSource(applicationContext)
        )

        setContent {
            SteplyticsTheme {
                SteplyticsApp(
                    appViewModelFactory = AppViewModel.Factory(
                        observeIsFirstLaunchUseCase = ObserveIsFirstLaunchUseCase(repository),
                        observeUserProfileUseCase = ObserveUserProfileUseCase(repository)
                    ),
                    onboardingFactory = OnboardingViewModel.Factory(
                        completeOnboardingUseCase = CompleteOnboardingUseCase(repository)
                    ),
                    userSetupFactory = UserSetupViewModel.Factory(
                        saveUserProfileUseCase = SaveUserProfileUseCase(repository)
                    ),
                    initiallyOpenTracking = openTrackingState.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        openTrackingState.value = intent?.getBooleanExtra(WorkoutTrackingService.EXTRA_OPEN_TRACKING, false) == true
    }
}

@Composable
private fun SteplyticsApp(
    appViewModelFactory: AppViewModel.Factory,
    onboardingFactory: OnboardingViewModel.Factory,
    userSetupFactory: UserSetupViewModel.Factory,
    initiallyOpenTracking: Boolean
) {
    val appViewModel: AppViewModel = viewModel(factory = appViewModelFactory)
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = onboardingFactory)
    val userSetupViewModel: UserSetupViewModel = viewModel(factory = userSetupFactory)

    val appState by appViewModel.state.collectAsStateWithLifecycle()
    val onboardingState by onboardingViewModel.state.collectAsStateWithLifecycle()
    val userSetupState by userSetupViewModel.state.collectAsStateWithLifecycle()

    when (appState.currentScreen) {
        AppScreen.Loading -> androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
        AppScreen.Onboarding -> OnboardingScreen(
            state = onboardingState,
            sideEffects = onboardingViewModel.sideEffects,
            onIntent = onboardingViewModel::process,
            onNavigateToSetup = appViewModel::navigateToSetup,
            modifier = Modifier.fillMaxSize()
        )
        AppScreen.Setup -> UserSetupScreen(
            state = userSetupState,
            sideEffects = userSetupViewModel.sideEffects,
            onIntent = userSetupViewModel::process,
            onNavigateToHome = appViewModel::navigateToHome,
            modifier = Modifier.fillMaxSize()
        )
        AppScreen.Home -> HomeScreen(
            profile = appState.userProfile,
            modifier = Modifier.fillMaxSize(),
            initiallyOpenTracking = initiallyOpenTracking
        )
    }
}
