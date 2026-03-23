package com.buildndeploy.steplytics.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.buildndeploy.steplytics.R
import com.buildndeploy.steplytics.data.local.SteplyticsPreferencesDataSource
import com.buildndeploy.steplytics.data.local.workout.WorkoutDatabase
import com.buildndeploy.steplytics.data.repository.WorkoutRepository
import com.buildndeploy.steplytics.domain.model.RoutePoint
import com.buildndeploy.steplytics.domain.model.UnitSystem
import com.buildndeploy.steplytics.domain.model.UserProfile
import com.buildndeploy.steplytics.domain.model.WorkoutRecord
import com.buildndeploy.steplytics.service.TrackingSessionStore
import com.buildndeploy.steplytics.service.WorkoutTrackingService
import com.buildndeploy.steplytics.ui.theme.AppBackground
import com.buildndeploy.steplytics.ui.theme.CardBackground
import com.buildndeploy.steplytics.ui.theme.CardBorder
import com.buildndeploy.steplytics.ui.theme.PrimaryBlue
import com.buildndeploy.steplytics.ui.theme.PrimaryGreen
import com.buildndeploy.steplytics.ui.theme.TextSecondary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DashboardTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Home("Home", Icons.Outlined.Home),
    Calendar("Calendar", Icons.Outlined.CalendarMonth),
    Reports("Reports", Icons.Outlined.QueryStats),
    Profile("Profile", Icons.Outlined.PersonOutline)
}

private data class StatCardUi(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val background: Color
)

private data class ActivityTypeUi(
    val id: String,
    val title: String,
    val description: String,
    val badge: String,
    val gradient: List<Color>,
    val caloriesPerMinute: Float
)

private enum class ReportRange(val label: String) {
    Daily("Daily"),
    Weekly("Weekly"),
    Monthly("Monthly")
}

private enum class ExportFormat(val label: String) {
    Pdf("PDF Workout Report")
}

private data class DashboardInsight(
    val metrics: List<StatCardUi>,
    val weeklyProgress: List<Float>,
    val hourlyProgress: List<Float>
)

private data class AppInfo(
    val versionName: String,
    val versionCode: Long
)

private sealed interface HomeFlowState {
    data object Overview : HomeFlowState
    data class ChooseActivity(val selectedId: String? = null) : HomeFlowState
    data class Countdown(val activity: ActivityTypeUi, val secondsRemaining: Int = 5) : HomeFlowState
    data class Tracking(val session: TrackingSession) : HomeFlowState
    data class Complete(
        val workout: WorkoutRecord,
        val showShareHint: Boolean = false
    ) : HomeFlowState
}

private data class TrackingSession(
    val activity: ActivityTypeUi,
    val startedAt: Long = System.currentTimeMillis(),
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val route: List<RoutePoint> = emptyList(),
    val distanceKm: Float = 0f,
    val caloriesKcal: Float = 0f,
    val pacePerKm: Float = 0f,
    val currentAqi: Int? = null,
    val currentPollen: Int? = null,
    val currentLocation: RoutePoint? = null,
    val isStationary: Boolean = false,
    val movingTimeSeconds: Long = 0,
    val stationaryTimeSeconds: Long = 0,
    val currentSpeedMps: Float = 0f,
    val averageSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f
)

private object CalendarUiState {
    sealed interface Date {
        val dayOfMonth: String
        val isSelected: Boolean

        data class Day(
            override val dayOfMonth: String,
            override val isSelected: Boolean = false,
            val hasWorkout: Boolean = false
        ) : Date

        data object Empty : Date {
            override val dayOfMonth: String = ""
            override val isSelected: Boolean = false
        }
    }
}

private val activityTypes = listOf(
    ActivityTypeUi(
        id = "walking",
        title = "Walking",
        description = "Light exercise, perfect for recovery",
        badge = "W",
        gradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
        caloriesPerMinute = 4.8f
    ),
    ActivityTypeUi(
        id = "running",
        title = "Running",
        description = "High intensity cardio workout",
        badge = "R",
        gradient = listOf(Color(0xFFA855F7), Color(0xFFEC4899)),
        caloriesPerMinute = 8.6f
    ),
    ActivityTypeUi(
        id = "jogging",
        title = "Jogging",
        description = "Moderate pace for endurance",
        badge = "J",
        gradient = listOf(Color(0xFF22C55E), Color(0xFF10B981)),
        caloriesPerMinute = 6.2f
    )
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    profile: UserProfile?,
    modifier: Modifier = Modifier,
    initiallyOpenTracking: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val preferencesDataSource = remember { SteplyticsPreferencesDataSource(context) }
    val repository = remember {
        WorkoutRepository(WorkoutDatabase.getInstance(context).workoutDao())
    }
    val workouts by repository.observeWorkouts().collectAsState(initial = emptyList())
    val serviceSession by TrackingSessionStore.session.collectAsState()
    val unitSystem by preferencesDataSource.observeUnitSystem().collectAsState(initial = UnitSystem.Metric)
    val notificationsEnabled by preferencesDataSource.observeNotificationsEnabled().collectAsState(initial = true)

    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
    var homeFlow by remember { mutableStateOf<HomeFlowState>(HomeFlowState.Overview) }
    var selectedCalendarDate by remember { mutableStateOf(LocalDate.now()) }
    var showWeeklyBreakdown by remember { mutableStateOf(false) }
    var reportRange by remember { mutableStateOf(ReportRange.Weekly) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val appInfo = remember(context) { context.resolveAppInfo() }
    val dashboardInsight = remember(workouts, unitSystem) {
        buildDashboardInsight(workouts, unitSystem)
    }

    LaunchedEffect(initiallyOpenTracking, serviceSession) {
        if (initiallyOpenTracking && serviceSession != null) {
            selectedTab = DashboardTab.Home
        }
    }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    fun hasTrackingPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun startTrackingSession(activity: ActivityTypeUi) {
        val lifecycleReady = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (!lifecycleReady) {
            Toast.makeText(context, "Keep Steplytics open while tracking starts.", Toast.LENGTH_SHORT).show()
            homeFlow = HomeFlowState.ChooseActivity(activity.id)
            return
        }
        WorkoutTrackingService.start(
            context = context,
            activityId = activity.id,
            activityTitle = activity.title,
            caloriesPerMinute = activity.caloriesPerMinute,
            userWeight = profile?.weight ?: 70f
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val hasAllTrackingPermissions = permissions.all { permission -> result[permission] == true || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED }
        if (hasAllTrackingPermissions) {
            val selectedId = (homeFlow as? HomeFlowState.ChooseActivity)?.selectedId
            val activity = activityTypes.firstOrNull { it.id == selectedId }
            if (activity != null) {
                startTrackingSession(activity)
            }
        }
    }
    LaunchedEffect(serviceSession) {
        val active = serviceSession
        when {
            active != null -> {
                val activity = activityTypes.firstOrNull { it.id == active.activityId }
                    ?: ActivityTypeUi(
                        id = active.activityId,
                        title = active.activityTitle,
                        description = active.activityTitle,
                        badge = active.activityTitle.firstOrNull()?.uppercase() ?: "A",
                        gradient = listOf(PrimaryBlue, PrimaryGreen),
                        caloriesPerMinute = active.caloriesPerMinute
                    )
                homeFlow = HomeFlowState.Tracking(
                    TrackingSession(
                        activity = activity,
                        startedAt = active.startedAt,
                        elapsedSeconds = active.elapsedSeconds,
                        isPaused = active.isPaused,
                        route = active.route,
                        distanceKm = active.distanceKm,
                        caloriesKcal = active.caloriesKcal,
                        pacePerKm = active.pacePerKm,
                        currentAqi = active.currentAqi,
                        currentPollen = active.currentPollen,
                        currentLocation = active.currentLocation,
                        isStationary = active.isStationary,
                        movingTimeSeconds = active.movingTimeSeconds,
                        stationaryTimeSeconds = active.stationaryTimeSeconds,
                        currentSpeedMps = active.currentSpeedMps,
                        averageSpeedMps = active.averageSpeedMps,
                        maxSpeedMps = active.maxSpeedMps
                    )
                )
            }
            active == null && homeFlow is HomeFlowState.Tracking -> Unit
        }
    }

    val isFocusedWorkoutFlow = selectedTab == DashboardTab.Home && homeFlow != HomeFlowState.Overview

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppBackground,
        bottomBar = {
            if (!isFocusedWorkoutFlow) {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
        ) {
            when {
                selectedTab == DashboardTab.Home && homeFlow is HomeFlowState.ChooseActivity -> {
                    val selectedId = (homeFlow as HomeFlowState.ChooseActivity).selectedId
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ChooseActivityScreen(
                            selectedId = selectedId,
                            activities = activityTypes,
                            onBack = { homeFlow = HomeFlowState.Overview },
                            onActivitySelected = { activity ->
                                homeFlow = HomeFlowState.ChooseActivity(selectedId = activity.id)
                            },
                            onStartTracking = {
                                val selectedActivity = activityTypes.firstOrNull { it.id == selectedId }
                                if (selectedActivity != null) {
                                    homeFlow = HomeFlowState.Countdown(selectedActivity)
                                }
                            }
                        )
                    }
                }

                selectedTab == DashboardTab.Home && homeFlow is HomeFlowState.Countdown -> {
                    val countdown = homeFlow as HomeFlowState.Countdown
                    CountdownStartScreen(
                        activity = countdown.activity,
                        secondsRemaining = countdown.secondsRemaining,
                        onBack = { homeFlow = HomeFlowState.ChooseActivity(countdown.activity.id) },
                        onTick = { seconds -> homeFlow = countdown.copy(secondsRemaining = seconds) },
                        onFinished = {
                            if (hasTrackingPermissions()) {
                                startTrackingSession(countdown.activity)
                            } else {
                                homeFlow = HomeFlowState.ChooseActivity(countdown.activity.id)
                                permissionLauncher.launch(permissions)
                            }
                        }
                    )
                }

                selectedTab == DashboardTab.Home && homeFlow is HomeFlowState.Tracking -> {
                    val session = (homeFlow as HomeFlowState.Tracking).session
                    TrackingScreen(
                        activity = session.activity,
                        elapsedSeconds = session.elapsedSeconds,
                        routePoints = session.route.map { LatLng(it.latitude, it.longitude) },
                        isPaused = session.isPaused,
                        unitSystem = unitSystem,
                        distanceKm = session.distanceKm,
                        pacePerKm = session.pacePerKm,
                        caloriesKcal = session.caloriesKcal,
                        currentAqi = session.currentAqi,
                        currentPollen = session.currentPollen,
                        currentLocation = session.currentLocation?.let { LatLng(it.latitude, it.longitude) },
                        isStationary = session.isStationary,
                        stationaryTimeSeconds = session.stationaryTimeSeconds,
                        currentSpeedMps = session.currentSpeedMps,
                        movingTimeSeconds = session.movingTimeSeconds,
                        onPauseResume = {
                            val latest = (homeFlow as? HomeFlowState.Tracking)?.session
                            if (latest != null) {
                                if (latest.isPaused) {
                                    WorkoutTrackingService.resume(context)
                                } else {
                                    WorkoutTrackingService.pause(context)
                                }
                            }
                        },
                        onStop = {
                            scope.launch(Dispatchers.IO) {
                                val latest = serviceSession ?: return@launch
                                val record = WorkoutRecord(
                                    activityType = latest.activityTitle,
                                    startedAt = latest.startedAt,
                                    endedAt = System.currentTimeMillis(),
                                    durationSeconds = latest.elapsedSeconds,
                                    distanceKm = latest.distanceKm,
                                    caloriesKcal = latest.caloriesKcal,
                                    pacePerKm = latest.pacePerKm,
                                    avgAqi = latest.currentAqi,
                                    avgPollen = latest.currentPollen,
                                    movingTimeSeconds = latest.movingTimeSeconds,
                                    stationaryTimeSeconds = latest.stationaryTimeSeconds,
                                    averageSpeedMps = latest.averageSpeedMps,
                                    maxSpeedMps = latest.maxSpeedMps,
                                    route = latest.route
                                )
                                val id = repository.insertWorkout(record)
                                WorkoutTrackingService.stop(context)
                                val savedRecord = record.copy(id = id)
                                launch(Dispatchers.Main) {
                                    selectedCalendarDate = Instant.ofEpochMilli(savedRecord.startedAt)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    homeFlow = HomeFlowState.Complete(savedRecord)
                                }
                            }
                        }
                    )
                }

                selectedTab == DashboardTab.Home && homeFlow is HomeFlowState.Complete -> {
                    val workout = (homeFlow as HomeFlowState.Complete).workout
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WorkoutCompleteScreen(
                            workout = workout,
                            profile = profile,
                            unitSystem = unitSystem,
                            showShareHint = (homeFlow as HomeFlowState.Complete).showShareHint,
                            onSave = { homeFlow = HomeFlowState.Overview },
                            onShare = {
                                shareWorkoutReport(
                                    context = context,
                                    workout = workout,
                                    unitSystem = unitSystem,
                                    profile = profile
                                )
                                val latest = homeFlow as? HomeFlowState.Complete
                                if (latest != null) {
                                    homeFlow = latest.copy(showShareHint = true)
                                }
                            }
                        )
                    }
                }

                else -> {
                    when (selectedTab) {
                        DashboardTab.Home -> DashboardOverviewScreen(
                            profile = profile,
                            dashboardInsight = dashboardInsight,
                            unitSystem = unitSystem,
                            showWeeklyBreakdown = showWeeklyBreakdown,
                            onToggleWeeklyBreakdown = { showWeeklyBreakdown = !showWeeklyBreakdown },
                            onStartActivity = { homeFlow = HomeFlowState.ChooseActivity() }
                        )
                        DashboardTab.Calendar -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            CalendarScreen(
                                workouts = workouts,
                                selectedDate = selectedCalendarDate,
                                unitSystem = unitSystem,
                                onDateSelected = { selectedCalendarDate = it }
                            )
                        }

                        DashboardTab.Reports -> ReportsScreen(
                            workouts = workouts,
                            unitSystem = unitSystem,
                            reportRange = reportRange,
                            onReportRangeChange = { reportRange = it }
                        )
                        DashboardTab.Profile -> ProfileScreen(
                            profile = profile,
                            workouts = workouts,
                            unitSystem = unitSystem,
                            notificationsEnabled = notificationsEnabled,
                            onUnitsClick = { showUnitDialog = true },
                            onNotificationsClick = { showNotificationDialog = true },
                            onExportClick = { showExportDialog = true },
                            appInfo = appInfo,
                            onAboutClick = { showAboutDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showUnitDialog) {
        SelectionDialog(
            title = "Units",
            message = "Choose the measurement system that should be reflected across the application.",
            options = UnitSystem.entries.associateWith { if (it == UnitSystem.Metric) "Metric (km, kg)" else "Imperial (mi, lb)" },
            selected = unitSystem,
            onDismiss = { showUnitDialog = false },
            onConfirm = { selectedUnit ->
                scope.launch {
                    preferencesDataSource.saveUnitSystem(selectedUnit)
                    showUnitDialog = false
                }
            }
        )
    }

    if (showNotificationDialog) {
        BooleanPreferenceDialog(
            title = "Notifications",
            enabled = notificationsEnabled,
            enabledLabel = "Enabled",
            disabledLabel = "Disabled",
            onDismiss = { showNotificationDialog = false },
            onSave = { enabled ->
                scope.launch {
                    preferencesDataSource.saveNotificationsEnabled(enabled)
                    showNotificationDialog = false
                }
            }
        )
    }

    if (showExportDialog) {
        SelectionDialog(
            title = "Export Data",
            message = "Generate a polished PDF report for your saved workouts.",
            options = ExportFormat.entries.associateWith { it.label },
            selected = ExportFormat.Pdf,
            onDismiss = { showExportDialog = false },
            onConfirm = { _ ->
                showExportDialog = false
                exportWorkoutData(
                    context = context,
                    workouts = workouts,
                    unitSystem = unitSystem,
                    profile = profile
                )
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            appInfo = appInfo,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun DashboardOverviewScreen(
    profile: UserProfile?,
    dashboardInsight: DashboardInsight,
    unitSystem: UnitSystem,
    showWeeklyBreakdown: Boolean,
    onToggleWeeklyBreakdown: () -> Unit,
    onStartActivity: () -> Unit
) {
    val todayWorkoutsLabel = if (profile != null) "Let's crush your goals today" else "Set your pace and begin"
    ScreenEntrance {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = todayWorkoutsLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            StartActivityButton(onClick = onStartActivity)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                dashboardInsight.metrics.forEach { card ->
                    MetricCard(card = card, modifier = Modifier.weight(1f))
                }
            }

            WeeklyProgressCard(
                unitSystem = unitSystem,
                weeklyProgress = dashboardInsight.weeklyProgress,
                hourlyProgress = dashboardInsight.hourlyProgress,
                showHourlyBreakdown = showWeeklyBreakdown,
                onClick = onToggleWeeklyBreakdown
            )
        }
    }
}

@Composable
private fun WeeklyProgressCard(
    unitSystem: UnitSystem,
    weeklyProgress: List<Float>,
    hourlyProgress: List<Float>,
    showHourlyBreakdown: Boolean,
    onClick: () -> Unit
) {
    SurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Weekly Progress", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = if (showHourlyBreakdown) "Hourly view" else "Last 7 days", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            if (showHourlyBreakdown) {
                BarChartCard(
                    title = "24 Hour Activity",
                    labels = (0 until 24).map { formatHourLabel(it) },
                    values = hourlyProgress,
                    enableHorizontalScroll = true
                )
            } else {
                LineChartCard(
                    values = weeklyProgress,
                    labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                    unitLabel = if (unitSystem == UnitSystem.Metric) "Steps" else "Effort"
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ChooseActivityScreen(
    selectedId: String?,
    activities: List<ActivityTypeUi>,
    onBack: () -> Unit,
    onActivitySelected: (ActivityTypeUi) -> Unit,
    onStartTracking: () -> Unit
) {
    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBackground, Color(0xFF1A2137))))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BackHeader(onBack = onBack)
        Text(
            text = "Choose Activity",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select the type of workout you want to track",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            activities.forEach { activity ->
                ActivityOptionCard(
                    activity = activity,
                    selected = activity.id == selectedId,
                    onClick = { onActivitySelected(activity) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        GradientButton(
            label = "Start Tracking",
            enabled = selectedId != null,
            onClick = onStartTracking
        )
    }
    }
}

@Composable
private fun CountdownStartScreen(
    activity: ActivityTypeUi,
    secondsRemaining: Int,
    onBack: () -> Unit,
    onTick: (Int) -> Unit,
    onFinished: () -> Unit
) {
    val latestFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(activity.id) {
        object : CountDownTimer(5_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                onTick(kotlin.math.ceil(millisUntilFinished / 1000.0).toInt())
            }
            override fun onFinish() { latestFinished() }
        }.start()
    }
    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBackground, Color(0xFF1A2137))))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BackHeader(onBack = onBack)
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(Brush.radialGradient(activity.gradient), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = activity.badge, color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = secondsRemaining.toString(), color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Text(text = "Get ready for ${activity.title.lowercase(Locale.getDefault())}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = "Lock in your pace. Tracking starts automatically after the 5-second countdown.", color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.weight(1f))
    }
    }
}

@Composable
private fun TrackingScreen(
    activity: ActivityTypeUi,
    elapsedSeconds: Long,
    routePoints: List<LatLng>,
    isPaused: Boolean,
    unitSystem: UnitSystem,
    distanceKm: Float,
    pacePerKm: Float,
    caloriesKcal: Float,
    currentAqi: Int?,
    currentPollen: Int?,
    currentLocation: LatLng?,
    isStationary: Boolean,
    stationaryTimeSeconds: Long,
    currentSpeedMps: Float,
    movingTimeSeconds: Long,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    var showAqi by remember { mutableStateOf(true) }
    var showPollen by remember { mutableStateOf(true) }
    var showInactivityPrompt by remember { mutableStateOf(false) }
    var inactivityAlertShown by remember { mutableStateOf(false) }

    LaunchedEffect(isStationary, stationaryTimeSeconds) {
        if (!isStationary) {
            inactivityAlertShown = false
            showInactivityPrompt = false
            return@LaunchedEffect
        }
        if (stationaryTimeSeconds >= 10 && !inactivityAlertShown) {
            inactivityAlertShown = true
            showInactivityPrompt = true
        }
    }

    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBackground, Color(0xFF1B2238))))
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = activity.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = if (isPaused) "Paused • Tap resume when you're ready" else "Live tracking is locked in", color = TextSecondary)
            }
            Text(text = formatElapsedTime(elapsedSeconds), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        WorkoutMapCard(
            title = "Live Route",
            subtitle = currentLocation?.let { "Marker: ${String.format(Locale.US, "%.5f", it.latitude)}, ${String.format(Locale.US, "%.5f", it.longitude)}" } ?: "Waiting for your first location fix...",
            routePoints = routePoints,
            followLatestPoint = true,
            currentLocation = currentLocation,
            markerInfo = if (isStationary) "Stationary for ${formatElapsedTime(stationaryTimeSeconds)}" else "Moving • ${String.format(Locale.US, "%.1f", currentSpeedMps * 3.6f)} km/h",
            topOverlay = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showAqi) MiniStatusChip(title = "AQI", value = currentAqi?.toString() ?: "--")
                    if (showPollen) MiniStatusChip(title = "Pollen", value = currentPollen?.toString() ?: "--")
                    MiniStatusChip(title = "Moving", value = formatElapsedTime(movingTimeSeconds))
                    MiniStatusChip(title = "Speed", value = String.format(Locale.US, "%.1f km/h", currentSpeedMps * 3.6f))
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MetricOverlayCard(modifier = Modifier.weight(1f), value = formatDistance(distanceKm, unitSystem), label = "Distance")
            MetricOverlayCard(modifier = Modifier.weight(1f), value = formatPace(pacePerKm, unitSystem), label = "Pace")
            MetricOverlayCard(modifier = Modifier.weight(1f), value = caloriesKcal.toInt().toString(), label = "Calories")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ToggleChip(label = if (showAqi) "Hide AQI" else "Show AQI", selected = showAqi) { showAqi = !showAqi }
            ToggleChip(label = if (showPollen) "Hide Pollen" else "Show Pollen", selected = showPollen) { showPollen = !showPollen }
        }

        AnimatedVisibility(visible = showAqi || showPollen) {
            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (showAqi) Text(text = "AQI: ${currentAqi?.toString() ?: "--"}", color = Color.White)
                    if (showPollen) Text(text = "Pollen: ${currentPollen?.toString() ?: "--"}", color = Color.White)
                    Text(text = "Moving time ${formatElapsedTime(movingTimeSeconds)} • Idle ${formatElapsedTime(stationaryTimeSeconds)}", color = TextSecondary)
                }
            }
        }

        AnimatedVisibility(visible = showInactivityPrompt) {
            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Movement paused",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You have been stationary for 10 seconds. The route marker and pace are locked until movement resumes.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { showInactivityPrompt = false }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryActionButton(
                label = if (isPaused) "Resume" else "Pause",
                background = Color(0xFF111A31),
                contentColor = if (isPaused) PrimaryGreen else Color.White,
                modifier = Modifier.weight(1f),
                onClick = onPauseResume
            )
            SecondaryActionButton(
                label = "Stop",
                background = Color(0xFFFF1B2D),
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onStop
            )
        }
    }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WorkoutCompleteScreen(
    workout: WorkoutRecord,
    profile: UserProfile?,
    unitSystem: UnitSystem,
    showShareHint: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBackground, Color(0xFF202741))))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .background(PrimaryGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Workout Complete!",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Great job on your ${workout.activityType.lowercase(Locale.getDefault())}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        SurfaceCard {
            StatsGrid(
                items = listOf(
                    formatDistance(workout.distanceKm, unitSystem) to "Distance\n${distanceUnit(unitSystem)}",
                    formatElapsedTime(workout.durationSeconds) to "Duration\nmin",
                    formatPace(workout.pacePerKm, unitSystem) to "Avg Pace\n${paceUnit(unitSystem)}",
                    workout.caloriesKcal.toInt().toString() to "Calories\nkcal",
                    Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy")) to "Workout Date",
                    Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mm a")) to "Start Time"
                ),
                useFullWidth = true
            )
            profile?.let { user ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Profile • ${user.gender} • ${user.age} yrs • ${formatHeight(user.height, unitSystem)} • ${formatWeight(user.weight, unitSystem)}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        WorkoutMapCard(
            title = "Route Map",
            subtitle = "Saved route snapshot for this workout.",
            routePoints = workout.route.map { LatLng(it.latitude, it.longitude) },
            followLatestPoint = false
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            GradientButton(label = "Save", modifier = Modifier.weight(1f), onClick = onSave)
            SecondaryActionButton(
                label = if (showShareHint) "Shared" else "Share",
                background = CardBackground,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
                onClick = onShare
            )
        }

        if (showShareHint) {
            Text(
                text = "Your PDF workout report is ready to share with apps like WhatsApp, Instagram, Gmail, and more from the system share sheet.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarScreen(
    workouts: List<WorkoutRecord>,
    selectedDate: LocalDate,
    unitSystem: UnitSystem,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = YearMonth.from(selectedDate)
    val monthWorkouts = workouts.filter { workoutDate(it) in currentMonth.atDay(1)..currentMonth.atEndOfMonth() }
    val activeDays = monthWorkouts.map { workoutDate(it).dayOfMonth }.toSet()
    val firstDay = currentMonth.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value % 7
    val dates = remember(monthWorkouts, selectedDate) {
        buildList {
            repeat(leadingBlanks) { add(CalendarUiState.Date.Empty) }
            repeat(currentMonth.lengthOfMonth()) { index ->
                val day = index + 1
                add(
                    CalendarUiState.Date.Day(
                        dayOfMonth = day.toString(),
                        isSelected = day == selectedDate.dayOfMonth,
                        hasWorkout = day in activeDays
                    )
                )
            }
        }
    }
    val dayWorkouts = workouts.filter { workoutDate(it) == selectedDate }
    val totalDistance = dayWorkouts.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalCalories = dayWorkouts.sumOf { it.caloriesKcal.toDouble() }.toFloat()
    val totalDuration = dayWorkouts.sumOf { it.durationSeconds }
    val avgAqi = dayWorkouts.mapNotNull { it.avgAqi }.takeIf { it.isNotEmpty() }?.average()?.toInt()

    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Activity Calendar",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarArrow(symbol = "‹")
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                CalendarArrow(symbol = "›")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Content(
                dates = dates,
                onDateClickListener = { date ->
                    if (date is CalendarUiState.Date.Day) {
                        onDateSelected(currentMonth.atDay(date.dayOfMonth.toInt()))
                    }
                }
            )
        }

        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (dayWorkouts.isEmpty()) "No Activity" else "${dayWorkouts.size} Workout(s)",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            StatsGrid(
                items = listOf(
                    formatDistance(totalDistance, unitSystem) to "Distance\n${distanceUnit(unitSystem)}",
                    formatElapsedTime(totalDuration) to "Duration\nmin",
                    totalCalories.toInt().toString() to "Calories\nkcal",
                    (avgAqi?.toString() ?: "--") to "AQI\navg"
                ),
                useFullWidth = true
            )
        }

        if (dayWorkouts.isNotEmpty()) {
            Text(
                text = "Workout Details",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(dayWorkouts, key = { it.id }) { workout ->
                    WorkoutSummaryCard(workout = workout, unitSystem = unitSystem)
                }
            }
        }
    }
    }
}

@Composable
private fun ReportsScreen(
    workouts: List<WorkoutRecord>,
    unitSystem: UnitSystem,
    reportRange: ReportRange,
    onReportRangeChange: (ReportRange) -> Unit
) {
    val reportSummary = remember(workouts, reportRange) { buildReportSummary(workouts, reportRange) }

    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Performance Reports",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        ReportRangeTabs(selectedRange = reportRange, onSelected = onReportRangeChange)

        StatsGrid(
            items = listOf(
                "${reportSummary.averageSteps}" to "Avg Steps\n${reportSummary.stepDelta}",
                "${reportSummary.averageCalories}" to "Avg Calories\n${reportSummary.calorieDelta}",
                "${formatDistance(reportSummary.totalDistanceKm, unitSystem)} ${distanceUnit(unitSystem)}" to "Total Distance\n${reportSummary.distanceDelta}",
                "${reportSummary.activeDays}/${reportSummary.periodLength}" to "Active Days\n${reportSummary.activeDayDelta}"
            ),
            useFullWidth = true
        )

        BarChartCard(
            title = "Steps Overview",
            labels = reportSummary.labels,
            values = reportSummary.values,
            enableHorizontalScroll = reportRange == ReportRange.Daily
        )
    }
    }
}

@Composable
private fun ProfileScreen(
    profile: UserProfile?,
    workouts: List<WorkoutRecord>,
    unitSystem: UnitSystem,
    notificationsEnabled: Boolean,
    onUnitsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onExportClick: () -> Unit,
    appInfo: AppInfo,
    onAboutClick: () -> Unit
) {
    val totalDistance = workouts.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalCalories = workouts.sumOf { it.caloriesKcal.toDouble() }.toFloat()
    val activeDays = workouts.map { workoutDate(it) }.distinct().size
    ScreenEntrance {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        SurfaceCard {
            ProfileHeader(profile = profile, unitSystem = unitSystem)
            Spacer(modifier = Modifier.height(20.dp))
            StatsGrid(
                items = listOf(
                    workouts.size.toString() to "Total Workouts",
                    formatDistance(totalDistance, unitSystem) to "Total Distance\n${distanceUnit(unitSystem)}",
                    formatCompactNumber(totalCalories.toInt()) to "Total Calories\nkcal",
                    activeDays.toString() to "Active Days"
                ),
                useFullWidth = true
            )
        }

        Text(text = "Preferences", color = TextSecondary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SurfaceCard {
            PreferenceRow(
                icon = Icons.Outlined.Settings,
                title = "Units",
                subtitle = if (unitSystem == UnitSystem.Metric) "Metric (km, kg)" else "Imperial (mi, lb)",
                onClick = onUnitsClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceRow(
                icon = Icons.Outlined.NotificationsNone,
                title = "Notifications",
                subtitle = if (notificationsEnabled) "Enabled" else "Disabled",
                onClick = onNotificationsClick
            )
        }

        Text(text = "Data", color = TextSecondary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SurfaceCard {
            PreferenceRow(
                icon = Icons.Outlined.Download,
                title = "Export Data",
                subtitle = "Create a branded PDF workout report from your saved workouts",
                onClick = onExportClick
            )
        }

        Text(text = "About", color = TextSecondary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SurfaceCard {
            PreferenceRow(
                icon = Icons.Outlined.Info,
                title = "About Steplytics",
                subtitle = "Version ${appInfo.versionName} (${appInfo.versionCode})",
                onClick = onAboutClick
            )
        }
    }
    }
}

@Composable
private fun StartActivityButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Start Activity",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActivityOptionCard(
    activity: ActivityTypeUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardAlpha by animateFloatAsState(targetValue = if (selected) 1f else 0.9f, label = "activity-card-alpha")
    val previewScale by animateFloatAsState(targetValue = if (selected) 1.08f else 1f, label = "activity-card-scale")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFF2B2140) else CardBackground, RoundedCornerShape(24.dp))
            .border(1.dp, if (selected) PrimaryBlue else CardBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size((52 * previewScale).dp)
                .background(Brush.horizontalGradient(activity.gradient.map { it.copy(alpha = cardAlpha) }), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = activity.badge, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = activity.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = activity.description, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MetricOverlayCard(modifier: Modifier = Modifier, value: String, label: String) {
    Box(
        modifier = modifier
            .background(Color(0x88212B46), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0x3322C55E) else CardBackground, RoundedCornerShape(999.dp))
            .border(1.dp, if (selected) PrimaryGreen else CardBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = Color.White)
    }
}

@Composable
private fun BackHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onBack)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "←", color = Color.White, fontSize = 20.sp)
        Text(text = "Back", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WorkoutMapCard(
    title: String,
    subtitle: String,
    routePoints: List<LatLng>,
    followLatestPoint: Boolean,
    currentLocation: LatLng? = routePoints.lastOrNull(),
    markerInfo: String? = null,
    topOverlay: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }

    SurfaceCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D1326), RoundedCornerShape(20.dp))
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    runCatching { MapsInitializer.initialize(context) }
                        .onFailure { mapErrorMessage = "Map preview is unavailable on this device." }
                        .getOrNull() ?: return@AndroidView

                    view.getMapAsync { googleMap ->
                        runCatching {
                            mapErrorMessage = null
                            googleMap.uiSettings.apply {
                                isCompassEnabled = false
                                isMapToolbarEnabled = false
                                isZoomControlsEnabled = false
                                isMyLocationButtonEnabled = false
                            }
                            googleMap.clear()
                            if (routePoints.isNotEmpty()) {
                                currentLocation?.let { location ->
                                    googleMap.addMarker(
                                        MarkerOptions()
                                            .position(location)
                                            .title(title)
                                            .snippet(markerInfo ?: subtitle)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                    )?.showInfoWindow()
                                }
                                googleMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(routePoints)
                                        .color(PrimaryBlue.toArgb())
                                        .width(12f)
                                )
                                if (followLatestPoint) {
                                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routePoints.last(), 17f))
                                } else if (routePoints.size == 1) {
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 16f))
                                } else {
                                    val bounds = LatLngBounds.builder().apply { routePoints.forEach { include(it) } }.build()
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
                                }
                            }
                        }.onFailure {
                            mapErrorMessage = "Map preview is unavailable on this device."
                        }
                    }
                }
            )

            mapErrorMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp)
                        .background(Color(0xDD161F39), RoundedCornerShape(16.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xAA161F39), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                    topOverlay?.invoke()
                }
                Text(
                    text = subtitle,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xAA161F39), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ScreenEntrance(content: @Composable () -> Unit) {
    val offsetY = remember { Animatable(48f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { offsetY.animateTo(0f, animationSpec = tween(durationMillis = 450)) }
            launch { alpha.animateTo(1f, animationSpec = tween(durationMillis = 350)) }
        }
    }

    Box(
        modifier = Modifier
            .offset(y = offsetY.value.dp)
            .scale(0.98f + (alpha.value * 0.02f))
    ) {
        AnimatedVisibility(
            visible = alpha.value > 0f,
            enter = fadeIn(animationSpec = tween(350)) + slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(450))
        ) {
            content()
        }
    }
}

@Composable
private fun MiniStatusChip(title: String, value: String) {
    Column(
        modifier = Modifier
            .background(Color(0xCC141C31), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = title, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(text = value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context).apply { onCreate(Bundle()) } }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return mapView
}

@Composable
private fun GradientButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = if (enabled) Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)) else Brush.horizontalGradient(listOf(Color(0xFF1B243D), Color(0xFF1B243D))),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (enabled) Color.White else TextSecondary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryActionButton(
    label: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(18.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(card: StatCardUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(card.background, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(imageVector = card.icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        Text(text = card.value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = card.title, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            Text(text = card.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LineChartCard(values: List<Float>, labels: List<String>, unitLabel: String) {
    val safeValues = values.ifEmpty { List(labels.size) { 0f } }
    val maxValue = safeValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF11182B), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartHeight = size.height - 24.dp.toPx()
                val chartWidth = size.width
                val stepX = if (safeValues.size > 1) chartWidth / (safeValues.size - 1) else chartWidth
                val path = Path()
                val fillPath = Path()
                safeValues.forEachIndexed { index, value ->
                    val x = stepX * index
                    val y = chartHeight - (value / maxValue) * chartHeight
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    drawCircle(color = PrimaryBlue, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                fillPath.lineTo(chartWidth, chartHeight)
                fillPath.close()
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(listOf(PrimaryBlue.copy(alpha = 0.35f), Color.Transparent))
                )
                drawPath(path = path, color = PrimaryBlue, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Text(text = unitLabel, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BarChartCard(
    title: String,
    labels: List<String>,
    values: List<Float>,
    enableHorizontalScroll: Boolean = false
) {
    val safeValues = values.ifEmpty { List(labels.size.coerceAtLeast(1)) { 0f } }
    val maxValue = safeValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val barWidth = if (enableHorizontalScroll) 44.dp else 0.dp
    val scrollState = rememberScrollState()

    SurfaceCard {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enableHorizontalScroll) Modifier.horizontalScroll(scrollState) else Modifier)
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            safeValues.forEachIndexed { index, value ->
                val animatedProgress by animateFloatAsState(
                    targetValue = value / maxValue,
                    animationSpec = tween(durationMillis = 650, delayMillis = index * 25),
                    label = "bar-animation-$index"
                )
                val barHeight = (animatedProgress * 150f).dp
                Column(
                    modifier = if (enableHorizontalScroll) Modifier.width(barWidth) else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(text = value.toInt().toString(), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight.coerceAtLeast(8.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF4B8CFF), PrimaryBlue)), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = labels.getOrElse(index) { "" },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}


private fun formatHourLabel(hour: Int): String {
    val normalized = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val meridiem = if (hour < 12) "AM" else "PM"
    return "$normalized $meridiem"
}

@Composable
private fun WorkoutSummaryCard(workout: WorkoutRecord, unitSystem: UnitSystem) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .background(Color(0xFF141C31), RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = workout.activityType, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mm a")),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(text = "${formatDistance(workout.distanceKm, unitSystem)} ${distanceUnit(unitSystem)} • ${workout.caloriesKcal.toInt()} kcal", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        Text(text = "Pace ${formatPace(workout.pacePerKm, unitSystem)} ${paceUnit(unitSystem)}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReportRangeTabs(selectedRange: ReportRange, onSelected: (ReportRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF11182B), RoundedCornerShape(18.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportRange.entries.forEach { range ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedRange == range) Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelected(range) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = range.label, color = if (selectedRange == range) Color.White else TextSecondary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile?, unitSystem: UnitSystem) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Brush.linearGradient(listOf(PrimaryBlue, PrimaryGreen)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Outlined.PersonOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Steplytics User", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "member@steplytics.app", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = profile?.let { "${it.age} yrs • ${formatHeight(it.height, unitSystem)} • ${formatWeight(it.weight, unitSystem)}" }
                    ?: "Complete your profile to personalize your metrics.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFF0C5B4A), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Premium Member", color = PrimaryGreen, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF162344), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(text = "›", color = TextSecondary, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    message: String,
    options: Map<T, String>,
    selected: T?,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = message)
                options.forEach { (value, label) ->
                    val isSelected = selected == value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { onConfirm(value) }
                            .padding(12.dp)
                    ) {
                        Text(text = label, color = if (isSelected) PrimaryBlue else Color.White)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun BooleanPreferenceDialog(
    title: String,
    enabled: Boolean,
    enabledLabel: String,
    disabledLabel: String,
    onDismiss: () -> Unit,
    onSave: (Boolean) -> Unit
) {
    SelectionDialog(
        title = title,
        message = "Update your preference.",
        options = linkedMapOf(true to enabledLabel, false to disabledLabel),
        selected = enabled,
        onDismiss = onDismiss,
        onConfirm = onSave
    )
}

@Composable
private fun AboutDialog(
    appInfo: AppInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Steplytics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Steplytics helps you track movement, environmental conditions, and workout performance in one place.")
                Text("Version: ${appInfo.versionName}")
                Text("Build: ${appInfo.versionCode}")
                Text("Features include live route tracking, workout history, calendar insights, and polished PDF workout reports.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun TrackingMetricColumn(value: String, label: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = value, color = PrimaryBlue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(text = unit, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BottomNavigationBar(selectedTab: DashboardTab, onTabSelected: (DashboardTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0E1426)).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier.clickable { onTabSelected(tab) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier.background(if (selected) PrimaryBlue.copy(alpha = 0.2f) else Color.Transparent, CircleShape).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = tab.icon, contentDescription = tab.label, tint = if (selected) PrimaryBlue else TextSecondary, modifier = Modifier.size(22.dp))
                }
                Text(text = tab.label, color = if (selected) PrimaryBlue else TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatsGrid(items: List<Pair<String, String>>, useFullWidth: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { (value, label) ->
                    val parts = label.split("\n")
                    Column(
                        modifier = Modifier.weight(1f).background(Color(0xFF141C31), RoundedCornerShape(18.dp)).border(1.dp, CardBorder, RoundedCornerShape(18.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = value, color = PrimaryBlue, style = if (useFullWidth) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = parts.firstOrNull().orEmpty(), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        if (parts.size > 1) {
                            Text(text = parts.drop(1).joinToString(" "), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SurfaceCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(CardBackground, RoundedCornerShape(24.dp)).border(1.dp, CardBorder, RoundedCornerShape(24.dp)).padding(20.dp)
    ) { content() }
}

@Composable
private fun CalendarArrow(symbol: String) {
    Box(modifier = Modifier.size(36.dp).background(Color(0xFF1A2137), CircleShape), contentAlignment = Alignment.Center) {
        Text(text = symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun Content(dates: List<CalendarUiState.Date>, onDateClickListener: (CalendarUiState.Date) -> Unit) {
    Column {
        var index = 0
        repeat(6) {
            if (index >= dates.size) return@repeat
            Row {
                repeat(7) {
                    val item = if (index < dates.size) dates[index] else CalendarUiState.Date.Empty
                    ContentItem(date = item, onClickListener = onDateClickListener, modifier = Modifier.weight(1f))
                    index++
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContentItem(date: CalendarUiState.Date, onClickListener: (CalendarUiState.Date) -> Unit, modifier: Modifier = Modifier) {
    val hasWorkout = (date as? CalendarUiState.Date.Day)?.hasWorkout == true
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(
                color = when {
                    date.isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    hasWorkout -> PrimaryBlue.copy(alpha = 0.25f)
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .clickable(enabled = date !is CalendarUiState.Date.Empty) { onClickListener(date) }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = date.dayOfMonth, style = MaterialTheme.typography.bodyMedium, color = if (date.isSelected || hasWorkout) Color.White else TextSecondary, modifier = Modifier.padding(10.dp))
    }
}

private fun workoutDate(workout: WorkoutRecord): LocalDate {
    return Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatElapsedTime(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun distanceUnit(unitSystem: UnitSystem): String = if (unitSystem == UnitSystem.Metric) "km" else "mi"

private fun paceUnit(unitSystem: UnitSystem): String = if (unitSystem == UnitSystem.Metric) "/km" else "/mi"

private fun formatDistance(distanceKm: Float, unitSystem: UnitSystem): String {
    val converted = if (unitSystem == UnitSystem.Metric) distanceKm else distanceKm * 0.621371f
    return String.format(Locale.US, "%.1f", converted)
}

private fun formatPace(pacePerKm: Float, unitSystem: UnitSystem): String {
    if (pacePerKm <= 0f || pacePerKm.isInfinite() || pacePerKm.isNaN()) return "0:00"
    val pace = if (unitSystem == UnitSystem.Metric) pacePerKm else pacePerKm / 0.621371f
    val totalSeconds = (pace * 60).toInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun formatDecimal(value: Float): String = String.format(Locale.US, "%.2f", value)

private fun formatWeight(weightKg: Float, unitSystem: UnitSystem): String {
    return if (unitSystem == UnitSystem.Metric) {
        "${formatDecimal(weightKg)} kg"
    } else {
        "${formatDecimal(weightKg * 2.20462f)} lb"
    }
}

private fun formatHeight(heightCm: Float, unitSystem: UnitSystem): String {
    return if (unitSystem == UnitSystem.Metric) {
        "${formatDecimal(heightCm)} cm"
    } else {
        val totalInches = heightCm / 2.54f
        val feet = totalInches.toInt() / 12
        val inches = totalInches.toInt() % 12
        "$feet'${inches}\""
    }
}

private fun formatCompactNumber(value: Int): String {
    return when {
        value >= 10000 -> String.format(Locale.US, "%.1fk", value / 1000f)
        else -> value.toString()
    }
}

private fun estimateSteps(distanceKm: Float): Int = (distanceKm * 1312f).toInt()

private fun buildDashboardInsight(workouts: List<WorkoutRecord>, unitSystem: UnitSystem): DashboardInsight {
    val endDate = LocalDate.now()
    val weekDays = (6 downTo 0).map { endDate.minusDays(it.toLong()) }
    val workoutByDay = workouts.groupBy { workoutDate(it) }
    val weeklyProgress = weekDays.map { day ->
        workoutByDay[day].orEmpty().sumOf { estimateSteps(it.distanceKm).toDouble() }.toFloat()
    }
    val hourlyProgress = MutableList(24) { 0f }
    workouts.filter { workoutDate(it) in weekDays.first()..weekDays.last() }.forEach { workout ->
        val hour = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault()).hour
        hourlyProgress[hour] += estimateSteps(workout.distanceKm).toFloat()
    }
    val recentWeek = workouts.filter { workoutDate(it) in weekDays.first()..weekDays.last() }
    val totalDistance = recentWeek.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalCalories = recentWeek.sumOf { it.caloriesKcal.toDouble() }.toFloat()
    val totalSteps = recentWeek.sumOf { estimateSteps(it.distanceKm).toLong() }.toInt()
    return DashboardInsight(
        metrics = listOf(
            StatCardUi("Steps", formatCompactNumber(totalSteps), "steps", Icons.Outlined.Straighten, Color(0xFF172446)),
            StatCardUi("Calories", totalCalories.toInt().toString(), "kcal", Icons.Outlined.Whatshot, Color(0xFF112D32)),
            StatCardUi("Distance", formatDistance(totalDistance, unitSystem), distanceUnit(unitSystem), Icons.Outlined.Timelapse, Color(0xFF2A1A3A))
        ),
        weeklyProgress = weeklyProgress,
        hourlyProgress = hourlyProgress
    )
}

private data class ReportSummary(
    val averageSteps: Int,
    val averageCalories: Int,
    val totalDistanceKm: Float,
    val activeDays: Int,
    val periodLength: Int,
    val stepDelta: String,
    val calorieDelta: String,
    val distanceDelta: String,
    val activeDayDelta: String,
    val labels: List<String>,
    val values: List<Float>
)

private fun buildReportSummary(workouts: List<WorkoutRecord>, reportRange: ReportRange): ReportSummary {
    val today = LocalDate.now()
    val (currentDates, previousDates, labels) = when (reportRange) {
        ReportRange.Daily -> {
            val current = (0 until 24).map { hour -> LocalDateTime.of(today, java.time.LocalTime.of(hour, 0)) }
            Triple(current, current.map { it.minusDays(1) }, (0 until 24).map(::formatHourLabel))
        }
        ReportRange.Weekly -> {
            val current = (6 downTo 0).map { today.minusDays(it.toLong()).atStartOfDay() }
            Triple(current, current.map { it.minusWeeks(1) }, listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
        }
        ReportRange.Monthly -> {
            val current = (3 downTo 0).map { today.minusWeeks(it.toLong()).atStartOfDay() }
            Triple(current, current.map { it.minusMonths(1) }, listOf("W1", "W2", "W3", "W4"))
        }
    }

    fun workoutsForSlot(slot: LocalDateTime, range: ReportRange, source: List<WorkoutRecord>): List<WorkoutRecord> {
        return when (range) {
            ReportRange.Daily -> source.filter {
                Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDateTime().hour == slot.hour &&
                    workoutDate(it) == slot.toLocalDate()
            }
            ReportRange.Weekly -> source.filter { workoutDate(it) == slot.toLocalDate() }
            ReportRange.Monthly -> source.filter {
                val date = workoutDate(it)
                val start = slot.toLocalDate()
                val end = start.plusDays(6)
                date in start..end
            }
        }
    }

    val currentValues = currentDates.map { slot ->
        workoutsForSlot(slot, reportRange, workouts).sumOf { estimateSteps(it.distanceKm).toDouble() }.toFloat()
    }
    val previousValues = previousDates.map { slot ->
        workoutsForSlot(slot, reportRange, workouts).sumOf { estimateSteps(it.distanceKm).toDouble() }.toFloat()
    }
    val selectedWorkouts = when (reportRange) {
        ReportRange.Daily -> workouts.filter { workoutDate(it) == today }
        ReportRange.Weekly -> workouts.filter { workoutDate(it) in today.minusDays(6)..today }
        ReportRange.Monthly -> workouts.filter { workoutDate(it) in today.minusDays(27)..today }
    }
    val previousWorkouts = when (reportRange) {
        ReportRange.Daily -> workouts.filter { workoutDate(it) == today.minusDays(1) }
        ReportRange.Weekly -> workouts.filter { workoutDate(it) in today.minusDays(13)..today.minusDays(7) }
        ReportRange.Monthly -> workouts.filter { workoutDate(it) in today.minusDays(55)..today.minusDays(28) }
    }
    return ReportSummary(
        averageSteps = selectedWorkouts.map { estimateSteps(it.distanceKm) }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
        averageCalories = selectedWorkouts.map { it.caloriesKcal.toDouble() }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
        totalDistanceKm = selectedWorkouts.sumOf { it.distanceKm.toDouble() }.toFloat(),
        activeDays = selectedWorkouts.map { workoutDate(it) }.distinct().size,
        periodLength = when (reportRange) {
            ReportRange.Daily -> 24
            ReportRange.Weekly -> 7
            ReportRange.Monthly -> 4
        },
        stepDelta = formatChange(currentValues.sum(), previousValues.sum()),
        calorieDelta = formatChange(
            selectedWorkouts.sumOf { it.caloriesKcal.toDouble() }.toFloat(),
            previousWorkouts.sumOf { it.caloriesKcal.toDouble() }.toFloat()
        ),
        distanceDelta = formatChange(
            selectedWorkouts.sumOf { it.distanceKm.toDouble() }.toFloat(),
            previousWorkouts.sumOf { it.distanceKm.toDouble() }.toFloat()
        ),
        activeDayDelta = formatChange(
            selectedWorkouts.map { workoutDate(it) }.distinct().size.toFloat(),
            previousWorkouts.map { workoutDate(it) }.distinct().size.toFloat()
        ),
        labels = labels,
        values = currentValues
    )
}

private fun formatChange(current: Float, previous: Float): String {
    if (current == 0f && previous == 0f) return "↗ +0%"
    if (previous == 0f) return "↗ +100%"
    val percentage = ((current - previous) / previous) * 100f
    val arrow = if (percentage >= 0) "↗" else "↘"
    return "$arrow ${if (percentage >= 0) "+" else ""}${percentage.toInt()}%"
}

private fun android.content.Context.resolveAppInfo(): AppInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return AppInfo(
        versionName = packageInfo.versionName ?: "1.0",
        versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    )
}

private fun exportWorkoutData(
    context: android.content.Context,
    workouts: List<WorkoutRecord>,
    unitSystem: UnitSystem,
    profile: UserProfile?
) {
    if (workouts.isEmpty()) {
        Toast.makeText(context, "No workout data available to export.", Toast.LENGTH_SHORT).show()
        return
    }

    runCatching {
        val exportDir = resolveExportDirectory(context)
        val file = createWorkoutPdf(
            context = context,
            directory = exportDir,
            workouts = workouts.sortedByDescending { it.startedAt },
            unitSystem = unitSystem,
            profile = profile,
            reportTitle = "Steplytics Workout Report",
            filePrefix = "steplytics-report"
        )
        openGeneratedPdf(context, file)
        file
    }.onSuccess { file ->
        Toast.makeText(context, "PDF report saved as ${file.name}", Toast.LENGTH_LONG).show()
    }.onFailure { throwable ->
        Toast.makeText(
            context,
            throwable.message ?: "Unable to generate the workout report right now.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun shareWorkoutReport(
    context: android.content.Context,
    workout: WorkoutRecord,
    unitSystem: UnitSystem,
    profile: UserProfile?
) {
    runCatching {
        val exportDir = resolveExportDirectory(context)
        val file = createWorkoutPdf(
            context = context,
            directory = exportDir,
            workouts = listOf(workout),
            unitSystem = unitSystem,
            profile = profile,
            reportTitle = "${workout.activityType} Workout Report",
            filePrefix = "steplytics-share"
        )
        sharePdfReport(
            context = context,
            file = file,
            shareText = buildWorkoutShareText(workout, unitSystem)
        )
    }.onFailure { throwable ->
        Toast.makeText(
            context,
            throwable.message ?: "Unable to share this workout right now.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun resolveExportDirectory(context: android.content.Context): File {
    val preferredRoot = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    val exportDir = File(preferredRoot, "exports")
    if (!exportDir.exists() && !exportDir.mkdirs()) {
        throw IllegalStateException("Unable to prepare the export folder.")
    }
    return exportDir
}

private fun openGeneratedPdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val openTargets = context.packageManager.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY)
    if (openTargets.isEmpty()) {
        throw ActivityNotFoundException("Report created, but no PDF app is available to open it.")
    }
    context.startActivity(openIntent)
}

private fun sharePdfReport(
    context: android.content.Context,
    file: File,
    shareText: String
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val targets = context.packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
    if (targets.isEmpty()) {
        throw ActivityNotFoundException("No app is available to share this workout report.")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share workout report").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun buildWorkoutShareText(workout: WorkoutRecord, unitSystem: UnitSystem): String {
    val startedAt = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault())
    return buildString {
        append("Steplytics workout summary")
        append("\n${workout.activityType} on ${startedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a"))}")
        append("\nDistance: ${formatDistance(workout.distanceKm, unitSystem)} ${distanceUnit(unitSystem)}")
        append("\nDuration: ${formatElapsedTime(workout.durationSeconds)}")
        append("\nCalories: ${workout.caloriesKcal.toInt()} kcal")
        append("\nPace: ${formatPace(workout.pacePerKm, unitSystem)} ${paceUnit(unitSystem)}")
    }
}

private fun createWorkoutPdf(
    context: android.content.Context,
    directory: File,
    workouts: List<WorkoutRecord>,
    unitSystem: UnitSystem,
    profile: UserProfile?,
    reportTitle: String,
    filePrefix: String
): File {
    val file = File(directory, "$filePrefix-${System.currentTimeMillis()}.pdf")
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36f
    val pageBackground = android.graphics.Color.parseColor("#10182B")
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 24f; isFakeBoldText = true }
    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 18f; isFakeBoldText = true }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D1D9E6"); textSize = 12f }
    val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#8EA0C2"); textSize = 11f }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#2E3A59"); strokeWidth = 1f }
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#151F36") }
    val logo = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

    fun createPage(pageNumber: Int): PdfDocument.Page {
        return document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    }

    fun PdfDocument.Page.preparePage() {
        canvas.drawColor(pageBackground)
    }

    fun PdfDocument.Page.drawWrappedText(text: String, x: Float, startY: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
        var y = startY
        wrapText(text, maxWidth, paint).forEach { line ->
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }

    fun PdfDocument.Page.drawMetricCard(left: Float, top: Float, width: Float, height: Float, label: String, value: String) {
        val rect = RectF(left, top, left + width, top + height)
        canvas.drawRoundRect(rect, 16f, 16f, cardPaint)
        canvas.drawText(label, left + 14f, top + 22f, captionPaint)
        canvas.drawText(value, left + 14f, top + 48f, headingPaint)
    }

    var pageNumber = 1
    var page = createPage(pageNumber).also { it.preparePage() }
    var y = margin + 8f

    fun nextPage() {
        document.finishPage(page)
        pageNumber += 1
        page = createPage(pageNumber).also { it.preparePage() }
        y = margin + 8f
    }

    fun ensureSpace(required: Float) {
        if (y + required > pageHeight - margin) nextPage()
    }

    logo?.let { bitmap ->
        page.canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, 52, 52, true), margin, y, null)
    }
    page.canvas.drawText(reportTitle, margin + 68f, y + 24f, titlePaint)
    page.canvas.drawText("Generated ${DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a").format(LocalDateTime.now())}", margin + 68f, y + 44f, captionPaint)
    y += 76f

    val totalDistance = workouts.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalDuration = workouts.sumOf { it.durationSeconds }
    val totalCalories = workouts.sumOf { it.caloriesKcal.toDouble() }.toFloat()
    val averagePace = workouts.map { it.pacePerKm }.filter { it > 0f }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f

    val metricWidth = (pageWidth - (margin * 2) - 12f) / 2f
    page.drawMetricCard(margin, y, metricWidth, 58f, "Distance", "${formatDistance(totalDistance, unitSystem)} ${distanceUnit(unitSystem)}")
    page.drawMetricCard(margin + metricWidth + 12f, y, metricWidth, 58f, "Duration", formatElapsedTime(totalDuration))
    y += 70f
    page.drawMetricCard(margin, y, metricWidth, 58f, "Calories", "${totalCalories.toInt()} kcal")
    page.drawMetricCard(margin + metricWidth + 12f, y, metricWidth, 58f, "Avg pace", "${formatPace(averagePace, unitSystem)} ${paceUnit(unitSystem)}")
    y += 78f

    profile?.let { user ->
        ensureSpace(56f)
        page.canvas.drawText("User info", margin, y, headingPaint)
        y += 20f
        y = page.drawWrappedText(
            "${user.gender} • ${user.age} yrs • ${formatHeight(user.height, unitSystem)} • ${formatWeight(user.weight, unitSystem)}",
            margin,
            y,
            pageWidth - (margin * 2),
            bodyPaint,
            16f
        )
        y += 10f
    }

    workouts.forEachIndexed { index, workout ->
        if (index == 0) {
            ensureSpace(370f)
        } else {
            nextPage()
        }

        val startedAt = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault())
        page.canvas.drawText(workout.activityType, margin, y, titlePaint)
        y += 26f
        page.canvas.drawText(startedAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a")), margin, y, captionPaint)
        y += 24f

        val singleMetricWidth = (pageWidth - (margin * 2) - 24f) / 3f
        page.drawMetricCard(margin, y, singleMetricWidth, 58f, "Distance", "${formatDistance(workout.distanceKm, unitSystem)} ${distanceUnit(unitSystem)}")
        page.drawMetricCard(margin + singleMetricWidth + 12f, y, singleMetricWidth, 58f, "Duration", formatElapsedTime(workout.durationSeconds))
        page.drawMetricCard(margin + ((singleMetricWidth + 12f) * 2f), y, singleMetricWidth, 58f, "Calories", "${workout.caloriesKcal.toInt()} kcal")
        y += 72f
        page.drawMetricCard(margin, y, singleMetricWidth, 58f, "Pace", "${formatPace(workout.pacePerKm, unitSystem)} ${paceUnit(unitSystem)}")
        page.drawMetricCard(margin + singleMetricWidth + 12f, y, singleMetricWidth, 58f, "Moving", formatElapsedTime(workout.movingTimeSeconds))
        page.drawMetricCard(margin + ((singleMetricWidth + 12f) * 2f), y, singleMetricWidth, 58f, "Idle", formatElapsedTime(workout.stationaryTimeSeconds))
        y += 84f

        page.canvas.drawText("Route snapshot", margin, y, headingPaint)
        y += 14f
        val mapBitmap = createRouteSnapshotBitmap(workout)
        val destination = RectF(margin, y, pageWidth - margin, y + 240f)
        page.canvas.drawBitmap(mapBitmap, null, destination, null)
        y += 258f

        page.canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 20f
        page.canvas.drawText("Split stats", margin, y, headingPaint)
        y += 18f
        y = page.drawWrappedText(
            "Average speed ${String.format(Locale.US, "%.1f", workout.averageSpeedMps * 3.6f)} km/h • Max speed ${String.format(Locale.US, "%.1f", workout.maxSpeedMps * 3.6f)} km/h • AQI ${workout.avgAqi ?: "--"} • Pollen ${workout.avgPollen ?: "--"}",
            margin,
            y,
            pageWidth - (margin * 2),
            bodyPaint,
            16f
        )
        y += 8f
    }

    document.finishPage(page)
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    return file
}

private fun createRouteSnapshotBitmap(workout: WorkoutRecord): Bitmap {
    val width = 1080
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.parseColor("#0F172A"))

    val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#17233D")
        style = Paint.Style.FILL
    }
    val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#4B8CFF")
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#D1D9E6")
        textSize = 30f
        isFakeBoldText = true
    }

    val rect = RectF(28f, 28f, width - 28f, height - 28f)
    canvas.drawRoundRect(rect, 28f, 28f, framePaint)

    if (workout.route.size < 2) {
        canvas.drawText("Route snapshot will appear after more GPS points are captured.", 72f, height / 2f, textPaint)
        return bitmap
    }

    val minLat = workout.route.minOf { it.latitude }
    val maxLat = workout.route.maxOf { it.latitude }
    val minLng = workout.route.minOf { it.longitude }
    val maxLng = workout.route.maxOf { it.longitude }
    val latRange = (maxLat - minLat).takeIf { it > 0 } ?: 0.001
    val lngRange = (maxLng - minLng).takeIf { it > 0 } ?: 0.001
    val padding = 96f

    fun project(point: RoutePoint): android.graphics.PointF {
        val x = padding + (((point.longitude - minLng) / lngRange) * (width - (padding * 2))).toFloat()
        val y = padding + (((maxLat - point.latitude) / latRange) * (height - (padding * 2))).toFloat()
        return android.graphics.PointF(x, y)
    }

    val path = android.graphics.Path()
    workout.route.map(::project).forEachIndexed { index, point ->
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    canvas.drawPath(path, routePaint)

    val start = project(workout.route.first())
    val end = project(workout.route.last())
    canvas.drawCircle(start.x, start.y, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#7EF0C4") })
    canvas.drawCircle(end.x, end.y, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#FFFFFF") })
    canvas.drawCircle(end.x, end.y, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#0F172A") })
    canvas.drawText("Start", start.x + 20f, start.y - 12f, textPaint)
    canvas.drawText("Finish", end.x + 20f, end.y - 12f, textPaint)
    return bitmap
}

private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
    if (text.isBlank()) return listOf("")
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    val current = StringBuilder()
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "${current} $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current.clear()
            current.append(candidate)
        } else {
            if (current.isNotEmpty()) lines += current.toString()
            current.clear()
            current.append(word)
        }
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}
