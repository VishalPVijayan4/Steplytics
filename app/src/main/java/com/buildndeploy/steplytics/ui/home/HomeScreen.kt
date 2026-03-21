package com.buildndeploy.steplytics.ui.home

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Whatshot
import android.os.Bundle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.buildndeploy.steplytics.domain.model.UserProfile
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.buildndeploy.steplytics.ui.theme.AppBackground
import com.buildndeploy.steplytics.ui.theme.CardBackground
import com.buildndeploy.steplytics.ui.theme.CardBorder
import com.buildndeploy.steplytics.ui.theme.PrimaryBlue
import com.buildndeploy.steplytics.ui.theme.PrimaryGreen
import com.buildndeploy.steplytics.ui.theme.TextSecondary
import kotlinx.coroutines.delay
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
    val speedFactor: Float,
    val caloriesPerMinute: Float
)

private sealed interface HomeFlowState {
    data object Overview : HomeFlowState
    data class ChooseActivity(val selectedId: String? = null) : HomeFlowState
    data class Tracking(
        val activity: ActivityTypeUi,
        val elapsedSeconds: Int = 0,
        val isPaused: Boolean = false
    ) : HomeFlowState
    data class Complete(
        val activity: ActivityTypeUi,
        val elapsedSeconds: Int,
        val showShareHint: Boolean = false
    ) : HomeFlowState
}

private data class TrackingMetrics(
    val distanceKm: String,
    val avgPacePerKm: String,
    val caloriesKcal: String
)

private object CalendarUiState {
    sealed interface Date {
        val dayOfMonth: String
        val isSelected: Boolean

        data class Day(
            override val dayOfMonth: String,
            override val isSelected: Boolean = false,
            val hasActivity: Boolean = false
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
        speedFactor = 0.78f,
        caloriesPerMinute = 4.8f
    ),
    ActivityTypeUi(
        id = "running",
        title = "Running",
        description = "High intensity cardio workout",
        badge = "R",
        gradient = listOf(Color(0xFFA855F7), Color(0xFFEC4899)),
        speedFactor = 1.35f,
        caloriesPerMinute = 8.6f
    ),
    ActivityTypeUi(
        id = "jogging",
        title = "Jogging",
        description = "Moderate pace for endurance",
        badge = "J",
        gradient = listOf(Color(0xFF22C55E), Color(0xFF10B981)),
        speedFactor = 1.02f,
        caloriesPerMinute = 6.2f
    )
)

@Composable
fun HomeScreen(
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }
    var homeFlow by remember { mutableStateOf<HomeFlowState>(HomeFlowState.Overview) }
    val isFocusedWorkoutFlow = selectedTab == DashboardTab.Home && homeFlow != HomeFlowState.Overview

    val trackingState = homeFlow as? HomeFlowState.Tracking
    LaunchedEffect(trackingState?.activity?.id, trackingState?.elapsedSeconds, trackingState?.isPaused) {
        if (trackingState != null && !trackingState.isPaused) {
            delay(1_000)
            homeFlow = trackingState.copy(elapsedSeconds = trackingState.elapsedSeconds + 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppBackground,
        bottomBar = {
            if (!isFocusedWorkoutFlow) {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        selectedTab = it
                        if (it != DashboardTab.Home) {
                            homeFlow = HomeFlowState.Overview
                        }
                    }
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
                    ChooseActivityScreen(
                        selectedId = (homeFlow as HomeFlowState.ChooseActivity).selectedId,
                        activities = activityTypes,
                        onBack = { homeFlow = HomeFlowState.Overview },
                        onActivitySelected = { activity ->
                            homeFlow = HomeFlowState.ChooseActivity(selectedId = activity.id)
                        },
                        onStartTracking = {
                            val selected = activityTypes.firstOrNull { it.id == (homeFlow as HomeFlowState.ChooseActivity).selectedId }
                            if (selected != null) {
                                homeFlow = HomeFlowState.Tracking(activity = selected)
                            }
                        }
                    )
                }

                selectedTab == DashboardTab.Home && homeFlow is HomeFlowState.Tracking -> {
                    val session = homeFlow as HomeFlowState.Tracking
                    TrackingScreen(
                        activity = session.activity,
                        elapsedSeconds = session.elapsedSeconds,
                        isPaused = session.isPaused,
                        routePoints = buildRoutePoints(session.activity, session.elapsedSeconds),
                        metrics = calculateTrackingMetrics(session.activity, session.elapsedSeconds),
                        onPauseResume = {
                            homeFlow = session.copy(isPaused = !session.isPaused)
                        },
                        onStop = {
                            homeFlow = HomeFlowState.Complete(
                                activity = session.activity,
                                elapsedSeconds = session.elapsedSeconds
                            )
                        }
                    )
                }

                selectedTab == DashboardTab.Home && homeFlow is HomeFlowState.Complete -> {
                    val summary = homeFlow as HomeFlowState.Complete
                    WorkoutCompleteScreen(
                        activity = summary.activity,
                        elapsedSeconds = summary.elapsedSeconds,
                        routePoints = buildRoutePoints(summary.activity, summary.elapsedSeconds),
                        metrics = calculateTrackingMetrics(summary.activity, summary.elapsedSeconds),
                        showShareHint = summary.showShareHint,
                        onSave = { homeFlow = HomeFlowState.Overview },
                        onShare = { homeFlow = summary.copy(showShareHint = !summary.showShareHint) }
                    )
                }

                else -> {
                    when (selectedTab) {
                        DashboardTab.Home -> DashboardOverviewScreen(
                            profile = profile,
                            onStartActivity = { homeFlow = HomeFlowState.ChooseActivity() }
                        )
                        DashboardTab.Calendar -> CalendarScreen()
                        DashboardTab.Reports -> ReportsScreen()
                        DashboardTab.Profile -> ProfileScreen(profile = profile)
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
    val headerName = profile?.let { "Welcome back, ${it.age} yrs!" } ?: "Welcome Back!"
    val cards = listOf(
        StatCardUi("Steps", "0", "steps", Icons.Outlined.Straighten, Color(0xFF172446)),
        StatCardUi("Calories", "0", "kcal", Icons.Outlined.Whatshot, Color(0xFF112D32)),
        StatCardUi("Distance", "0.0", "km", Icons.Outlined.Timelapse, Color(0xFF2A1A3A))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = headerName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Let's crush your goals today",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        StartActivityButton(onClick = onStartActivity)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            cards.forEach { card ->
                MetricCard(
                    card = card,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ChartContainer(
            title = "Weekly Progress",
            trailingLabel = "Last 7 days"
        ) {
            LineChart(
                points = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
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
    elapsedSeconds: Int,
    isPaused: Boolean,
    routePoints: List<LatLng>,
    metrics: TrackingMetrics,
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
                text = activity.title,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        WorkoutMapCard(
            title = "Live Route Preview",
            subtitle = "Route updates as the workout timer advances.",
            routePoints = routePoints
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
                    TrackingMetricColumn(value = metrics.distanceKm, label = "Distance", unit = "km")
                    TrackingMetricColumn(value = metrics.avgPacePerKm, label = "Pace", unit = "/km")
                    TrackingMetricColumn(value = metrics.caloriesKcal, label = "Calories", unit = "kcal")
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
    activity: ActivityTypeUi,
    elapsedSeconds: Int,
    routePoints: List<LatLng>,
    metrics: TrackingMetrics,
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
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Workout Complete!",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Great job on your ${activity.title.lowercase(Locale.getDefault())}",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        SurfaceCard {
            StatsGrid(
                items = listOf(
                    metrics.distanceKm to "Distance\nkm",
                    formatElapsedTime(elapsedSeconds) to "Duration\nmin",
                    metrics.avgPacePerKm to "Avg Pace\n/km",
                    metrics.caloriesKcal to "Calories\nkcal"
                ),
                useFullWidth = true
            )
        }

        WorkoutMapCard(
            title = "Route Map",
            subtitle = "Saved route snapshot for this workout.",
            routePoints = routePoints
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            GradientButton(
                label = "Save",
                modifier = Modifier.weight(1f),
                onClick = onSave
            )
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
                text = "Share tapped. Next step: wire this button to Android Sharesheet with a screenshot or route summary.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
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
            .background(
                color = if (selected) Color(0xFF2B2140) else CardBackground,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) PrimaryBlue else CardBorder,
                shape = RoundedCornerShape(24.dp)
            )
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
            Text(
                text = activity.badge,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = activity.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = activity.description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(PrimaryBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
            }
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
    routePoints: List<LatLng>
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

                            if (routePoints.size == 1) {
                                googleMap.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(routePoints.first(), 15f)
                                )
                            } else {
                                val bounds = LatLngBounds.builder().apply {
                                    routePoints.forEach { include(it) }
                                }.build()
                                googleMap.moveCamera(
                                    CameraUpdateFactory.newLatLngBounds(bounds, 96)
                                )
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
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }

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
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}

@Composable
private fun TrackingMetricColumn(
    value: String,
    label: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            color = PrimaryBlue,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(text = unit, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CalendarScreen() {
    val activeDays = setOf(3, 5, 8, 10, 12, 15, 17, 20, 22, 24, 28)
    var selectedDay by remember { mutableStateOf(15) }
    val dates = remember(selectedDay) {
        buildList {
            repeat(31) { index ->
                val day = index + 1
                add(
                    CalendarUiState.Date.Day(
                        dayOfMonth = day.toString(),
                        isSelected = day == selectedDay,
                        hasActivity = day in activeDays
                    )
                )
            }
        }
    }

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
                    text = "March 2026",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                CalendarArrow(symbol = "›")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                        selectedDay = date.dayOfMonth.toInt()
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
                    text = "March ${selectedDay}, 2026",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selectedDay in activeDays) "Active Day" else "Rest Day",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            StatsGrid(
                items = listOf(
                    "12,405" to "Steps",
                    "8.2 km" to "Distance",
                    "542 kcal" to "Calories",
                    "48 min" to "Duration"
                ),
                useFullWidth = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ReportsScreen() {
    var selectedRange by remember { mutableStateOf("Weekly") }
    val ranges = listOf("Daily", "Weekly", "Monthly")

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(18.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ranges.forEach { range ->
                val selected = range == selectedRange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            brush = if (selected) Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedRange = range }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range,
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        StatsGrid(
            items = listOf(
                "0" to "Avg Steps",
                "0" to "Avg Calories",
                "0.0 km" to "Total Distance",
                "0/7" to "Active Days"
            ),
            useFullWidth = true
        )

        ChartContainer(title = "Steps Overview") {
            BarChart(
                values = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }

        ChartContainer(title = "Calories Burned") {
            LineChart(
                points = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileScreen(profile: UserProfile?) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Steplytics User",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile?.let { "${it.age} yrs • ${it.height} cm • ${it.weight} kg" }
                            ?: "Complete your first activity to unlock insights.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0D5C53), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "New Member",
                            color = PrimaryGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            StatsGrid(
                items = listOf(
                    "0" to "Total Workouts",
                    "0 km" to "Total Distance",
                    "0" to "Total Calories",
                    "0" to "Active Days"
                ),
                useFullWidth = true
            )
        }

        SectionTitle("Preferences")
        PreferenceList(
            rows = listOf(
                Triple(Icons.Outlined.Settings, "Units", "Metric (km, kg)"),
                Triple(Icons.Outlined.NotificationsNone, "Notifications", "Enabled")
            )
        )

        SectionTitle("Data")
        PreferenceList(
            rows = listOf(
                Triple(Icons.Outlined.Download, "Export Data", "No data available yet")
            )
        )

        SurfaceCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "App Version",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Steplytics v1.0.0",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Updated for the latest home experience",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun StartActivityButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                shape = RoundedCornerShape(24.dp)
            )
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
                brush = if (enabled) {
                    Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF1B243D), Color(0xFF1B243D)))
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else TextSecondary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
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
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricCard(
    card: StatCardUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(card.background, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = card.icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = card.value,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = card.title,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = card.subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ChartContainer(
    title: String,
    trailingLabel: String? = null,
    content: @Composable () -> Unit
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun LineChart(
    points: List<Float>,
    labels: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val maxValue = (points.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val stepX = if (points.size == 1) size.width else size.width / (points.size - 1)
            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, value ->
                val x = index * stepX
                val normalized = value / maxValue
                val y = size.height - (normalized * (size.height * 0.7f) + size.height * 0.15f)
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryBlue.copy(alpha = 0.32f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                color = PrimaryBlue,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BarChart(
    values: List<Float>,
    labels: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val barWidth = size.width / (values.size * 1.6f)
            val spacing = barWidth * 0.6f
            values.forEachIndexed { index, value ->
                val left = index * (barWidth + spacing) + spacing
                val barHeight = (value / maxValue) * (size.height * 0.8f)
                drawRoundRect(
                    color = PrimaryBlue,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1426))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) PrimaryBlue.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = tab.label,
                    color = if (selected) PrimaryBlue else TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    items: List<Pair<String, String>>,
    useFullWidth: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { (value, label) ->
                    val parts = label.split("\n")
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF141C31), RoundedCornerShape(18.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = value,
                            color = PrimaryBlue,
                            style = if (useFullWidth) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parts.firstOrNull().orEmpty(),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (parts.size > 1) {
                            Text(
                                text = parts.drop(1).joinToString(" "),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
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
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
    }
}

@Composable
private fun CalendarArrow(symbol: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFF1A2137), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun Content(
    dates: List<CalendarUiState.Date>,
    onDateClickListener: (CalendarUiState.Date) -> Unit,
) {
    Column {
        var index = 0
        repeat(6) {
            if (index >= dates.size) return@repeat
            Row {
                repeat(7) {
                    val item = if (index < dates.size) dates[index] else CalendarUiState.Date.Empty
                    ContentItem(
                        date = item,
                        onClickListener = onDateClickListener,
                        modifier = Modifier.weight(1f)
                    )
                    index++
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContentItem(
    date: CalendarUiState.Date,
    onClickListener: (CalendarUiState.Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActivity = (date as? CalendarUiState.Date.Day)?.hasActivity == true
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(
                color = when {
                    date.isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    hasActivity -> PrimaryBlue.copy(alpha = 0.25f)
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .clickable(enabled = date !is CalendarUiState.Date.Empty) {
                onClickListener(date)
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth,
            style = MaterialTheme.typography.bodyMedium,
            color = if (date.isSelected || hasActivity) Color.White else TextSecondary,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PreferenceList(
    rows: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
    ) {
        rows.forEachIndexed { index, (icon, title, subtitle) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF16254A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(text = "›", color = TextSecondary, fontSize = 24.sp)
            }
            if (index != rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder)
                )
            }
        }
    }
}

private fun buildRoutePoints(
    activity: ActivityTypeUi,
    elapsedSeconds: Int
): List<LatLng> {
    val pointCount = (elapsedSeconds / 3).coerceAtLeast(6)
    val baseLat = 37.7749
    val baseLng = -122.4194
    val latStep = 0.00008 * activity.speedFactor
    val lngStep = 0.00011 * activity.speedFactor

    return List(pointCount) { index ->
        val offset = index.toDouble()
        val wave = kotlin.math.sin(offset / 2.2) * lngStep
        LatLng(
            baseLat + (offset * latStep),
            baseLng + wave + (offset * lngStep * 0.22)
        )
    }
}

private fun calculateTrackingMetrics(
    activity: ActivityTypeUi,
    elapsedSeconds: Int
): TrackingMetrics {
    val minutes = elapsedSeconds / 60f
    val distance = (minutes / 12f) * activity.speedFactor
    val calories = minutes * activity.caloriesPerMinute
    val pace = if (distance > 0f) minutes / distance else 0f

    return TrackingMetrics(
        distanceKm = String.format(Locale.US, "%.2f", distance.coerceAtLeast(0f)),
        avgPacePerKm = if (pace > 0f) formatMinutesToPace(pace) else "0:00",
        caloriesKcal = String.format(Locale.US, "%d", calories.toInt())
    )
}

private fun formatElapsedTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatMinutesToPace(totalMinutes: Float): String {
    val totalSeconds = (totalMinutes * 60).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
