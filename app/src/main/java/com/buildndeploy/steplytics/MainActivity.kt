package com.buildndeploy.steplytics

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.buildndeploy.steplytics.ui.auth.LoginScreen
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

    @RequiresApi(Build.VERSION_CODES.O)
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SteplyticsApp(
    appViewModelFactory: AppViewModel.Factory,
    onboardingFactory: OnboardingViewModel.Factory,
    userSetupFactory: UserSetupViewModel.Factory,
    initiallyOpenTracking: Boolean
) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                if (authResult.isSuccessful) {
                    isAuthenticated = true
                    auth.currentUser?.let { user ->
                        val payload = hashMapOf(
                            "uid" to user.uid,
                            "name" to (user.displayName ?: ""),
                            "email" to (user.email ?: ""),
                            "photoUrl" to (user.photoUrl?.toString() ?: "")
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .set(payload)
                    }
                }
            }
        }
    }

    if (!isAuthenticated) {
        val context = androidx.compose.ui.platform.LocalContext.current
        LoginScreen(onGoogleSignIn = {
            val options = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, options)
            signInLauncher.launch(client.signInIntent)
        }, modifier = Modifier.fillMaxSize())
        return
    }

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
