package com.buildndeploy.steplytics.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.buildndeploy.steplytics.data.local.workout.WorkoutDatabase
import com.buildndeploy.steplytics.data.remote.AqiService
import com.buildndeploy.steplytics.data.repository.WorkoutRepository
import com.buildndeploy.steplytics.domain.model.ActiveTrackingSession
import com.buildndeploy.steplytics.domain.model.RoutePoint
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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
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

private sealed interface HomeFlowState {
    data object Overview : HomeFlowState
    data class ChooseActivity(val selectedId: String? = null) : HomeFlowState
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
    val currentLocation: RoutePoint? = null
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

@Composable
fun HomeScreen(
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember {
        WorkoutRepository(WorkoutDatabase.getInstance(context).workoutDao())
    }
    val aqiService = remember { AqiService() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val workouts by repository.observeWorkouts().collectAsState(initial = emptyList())
    val serviceSession by TrackingSessionStore.session.collectAsState()

    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
    var homeFlow by remember { mutableStateOf<HomeFlowState>(HomeFlowState.Overview) }
    var selectedCalendarDate by remember { mutableStateOf(LocalDate.now()) }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.toTypedArray()
    }

    fun hasTrackingPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            val selectedId = (homeFlow as? HomeFlowState.ChooseActivity)?.selectedId
            val activity = activityTypes.firstOrNull { it.id == selectedId }
            if (activity != null) {
                WorkoutTrackingService.start(
                    context = context,
                    activityId = activity.id,
                    activityTitle = activity.title,
                    caloriesPerMinute = activity.caloriesPerMinute,
                    userWeight = profile?.weight ?: 70f
                )
            }
        }
    }

    val trackingFlow = (homeFlow as? HomeFlowState.Tracking)?.session
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
                        currentLocation = active.currentLocation
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
                    ChooseActivityScreen(
                        selectedId = selectedId,
                        activities = activityTypes,
                        onBack = { homeFlow = HomeFlowState.Overview },
                        onActivitySelected = { activity ->
                            homeFlow = HomeFlowState.ChooseActivity(selectedId = activity.id)
                        },
                        onStartTracking = {
                            if (hasTrackingPermissions()) {
                                val selectedActivity = activityTypes.firstOrNull { it.id == selectedId }
                                if (selectedActivity != null) {
                                    WorkoutTrackingService.start(
                                        context = context,
                                        activityId = selectedActivity.id,
                                        activityTitle = selectedActivity.title,
                                        caloriesPerMinute = selectedActivity.caloriesPerMinute,
                                        userWeight = profile?.weight ?: 70f
                                    )
                                }
                            } else {
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
                        distanceKm = session.distanceKm,
                        pacePerKm = session.pacePerKm,
                        caloriesKcal = session.caloriesKcal,
                        currentAqi = session.currentAqi,
                        isStationary = serviceSession?.isStationary == true,
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
                    WorkoutCompleteScreen(
                        workout = workout,
                        showShareHint = (homeFlow as HomeFlowState.Complete).showShareHint,
                        onSave = { homeFlow = HomeFlowState.Overview },
                        onShare = {
                            val latest = homeFlow as? HomeFlowState.Complete
                            if (latest != null) {
                                homeFlow = latest.copy(showShareHint = !latest.showShareHint)
                            }
                        }
                    )
                }

                else -> {
                    when (selectedTab) {
                        DashboardTab.Home -> DashboardOverviewScreen(
                            profile = profile,
                            onStartActivity = { homeFlow = HomeFlowState.ChooseActivity() }
                        )
                        DashboardTab.Calendar -> CalendarScreen(
                            workouts = workouts,
                            selectedDate = selectedCalendarDate,
                            onDateSelected = { selectedCalendarDate = it }
                        )
                        DashboardTab.Reports -> ReportsScreen(workouts = workouts)
                        DashboardTab.Profile -> ProfileScreen(profile = profile, workouts = workouts)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardOverviewScreen(
    profile: UserProfile?,
    onStartActivity: () -> Unit
) {
    val todayWorkoutsLabel = if (profile != null) "Ready for today's session?" else "Set your pace and begin"
    val cards = listOf(
        StatCardUi("Steps", "Live", "sensor ready", Icons.Outlined.Straighten, Color(0xFF172446)),
        StatCardUi("Calories", "Track", "burn rate", Icons.Outlined.Whatshot, Color(0xFF112D32)),
        StatCardUi("Distance", "Route", "km live", Icons.Outlined.Timelapse, Color(0xFF2A1A3A))
    )

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
            cards.forEach { card ->
                MetricCard(card = card, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChooseActivityScreen(
    selectedId: String?,
    activities: List<ActivityTypeUi>,
    onBack: () -> Unit,
    onActivitySelected: (ActivityTypeUi) -> Unit,
    onStartTracking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBackground, Color(0xFF1A2137))))
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

@Composable
private fun TrackingScreen(
    activity: ActivityTypeUi,
    elapsedSeconds: Long,
    routePoints: List<LatLng>,
    isPaused: Boolean,
    distanceKm: Float,
    pacePerKm: Float,
    caloriesKcal: Float,
    currentAqi: Int?,
    isStationary: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBackground, Color(0xFF1B2238))))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isPaused) PrimaryBlue else PrimaryGreen, CircleShape)
                    )
                    Text(
                        text = if (isPaused) "Tracking Paused" else "Tracking Active",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = if (isStationary) "User is constant" else (currentAqi?.let { "AQI $it" } ?: "AQI loading..."),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = activity.title,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        WorkoutMapCard(
            title = "Live Route Preview",
            subtitle = if (routePoints.isEmpty()) "Waiting for current location..." else "Tracking your real route until pause or stop.",
            routePoints = routePoints,
            followLatestPoint = true
        )

        SurfaceCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = formatElapsedTime(elapsedSeconds),
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Duration",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrackingMetricColumn(value = formatDecimal(distanceKm), label = "Distance", unit = "km")
                    TrackingMetricColumn(value = formatPace(pacePerKm), label = "Pace", unit = "/km")
                    TrackingMetricColumn(value = caloriesKcal.toInt().toString(), label = "Calories", unit = "kcal")
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

@Composable
private fun WorkoutCompleteScreen(
    workout: WorkoutRecord,
    showShareHint: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
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
                    formatDecimal(workout.distanceKm) to "Distance\nkm",
                    formatElapsedTime(workout.durationSeconds) to "Duration\nmin",
                    formatPace(workout.pacePerKm) to "Avg Pace\n/km",
                    workout.caloriesKcal.toInt().toString() to "Calories\nkcal",
                    (workout.avgAqi?.toString() ?: "--") to "AQI\ncurrent"
                ),
                useFullWidth = true
            )
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
                text = "Saved workouts now appear in Calendar based on the workout date.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarScreen(
    workouts: List<WorkoutRecord>,
    selectedDate: LocalDate,
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
                    formatDecimal(totalDistance) to "Distance\nkm",
                    formatElapsedTime(totalDuration) to "Duration\nmin",
                    totalCalories.toInt().toString() to "Calories\nkcal",
                    (avgAqi?.toString() ?: "--") to "AQI\navg"
                ),
                useFullWidth = true
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ReportsScreen(workouts: List<WorkoutRecord>) {
    val totalDistance = workouts.sumOf { it.distanceKm.toDouble() }.toFloat()
    val totalCalories = workouts.sumOf { it.caloriesKcal.toDouble() }.toFloat()
    val activeDays = workouts.map { workoutDate(it) }.distinct().size
    val avgDistance = if (workouts.isEmpty()) 0f else totalDistance / workouts.size

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

        StatsGrid(
            items = listOf(
                formatDecimal(avgDistance) to "Avg Distance\nkm",
                totalCalories.toInt().toString() to "Total Calories\nkcal",
                formatDecimal(totalDistance) to "Total Distance\nkm",
                activeDays.toString() to "Active Days"
            ),
            useFullWidth = true
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ProfileScreen(profile: UserProfile?, workouts: List<WorkoutRecord>) {
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
            Text(
                text = profile?.let { "${it.age} yrs • ${it.height} cm • ${it.weight} kg" }
                    ?: "Complete your profile to personalize calories.",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
            StatsGrid(
                items = listOf(
                    workouts.size.toString() to "Total Workouts",
                    formatDecimal(workouts.sumOf { it.distanceKm.toDouble() }.toFloat()) to "Distance\nkm",
                    workouts.sumOf { it.caloriesKcal.toDouble() }.toInt().toString() to "Calories\nkcal",
                    workouts.map { workoutDate(it) }.distinct().size.toString() to "Active Days"
                ),
                useFullWidth = true
            )
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
                .size(52.dp)
                .background(Brush.horizontalGradient(activity.gradient), RoundedCornerShape(16.dp)),
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
    followLatestPoint: Boolean
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()

    SurfaceCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D1326), RoundedCornerShape(20.dp))
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.getMapAsync { googleMap ->
                        MapsInitializer.initialize(context)
                        googleMap.uiSettings.apply {
                            isCompassEnabled = false
                            isMapToolbarEnabled = false
                            isZoomControlsEnabled = false
                            isMyLocationButtonEnabled = false
                        }
                        googleMap.clear()
                        if (routePoints.isNotEmpty()) {
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
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xAA161F39), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
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

private fun formatPace(pacePerKm: Float): String {
    if (pacePerKm <= 0f || pacePerKm.isInfinite() || pacePerKm.isNaN()) return "0:00"
    val totalSeconds = (pacePerKm * 60).toInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun formatDecimal(value: Float): String = String.format(Locale.US, "%.2f", value)

private fun calculateDistanceKm(route: List<RoutePoint>): Float {
    if (route.size < 2) return 0f
    var meters = 0f
    for (i in 1 until route.size) {
        val results = FloatArray(1)
        Location.distanceBetween(
            route[i - 1].latitude,
            route[i - 1].longitude,
            route[i].latitude,
            route[i].longitude,
            results
        )
        meters += results[0]
    }
    return meters / 1_000f
}

private fun calculatePacePerKm(elapsedSeconds: Long, distanceKm: Float): Float {
    if (distanceKm <= 0f) return 0f
    return (elapsedSeconds / 60f) / distanceKm
}

private fun calculateCalories(activity: ActivityTypeUi, elapsedSeconds: Long, userWeight: Float?): Float {
    val weightFactor = (userWeight ?: 70f) / 70f
    return (elapsedSeconds / 60f) * activity.caloriesPerMinute * weightFactor
}

@SuppressLint("MissingPermission")
private fun observeLocationUpdates(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
) = callbackFlow<Location> {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
        .setMinUpdateDistanceMeters(3f)
        .build()

    val callback = object : com.google.android.gms.location.LocationCallback() {
        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
            result.lastLocation?.let { trySend(it) }
        }
    }

    fusedLocationClient.lastLocation.await()?.let { trySend(it) }
    fusedLocationClient.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
    awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
}.conflate()
