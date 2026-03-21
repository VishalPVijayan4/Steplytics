package com.buildndeploy.steplytics.domain.model

data class ActiveTrackingSession(
    val activityId: String,
    val activityTitle: String,
    val caloriesPerMinute: Float,
    val userWeight: Float,
    val startedAt: Long,
    val elapsedSeconds: Long = 0,
    val isPaused: Boolean = false,
    val route: List<RoutePoint> = emptyList(),
    val distanceKm: Float = 0f,
    val caloriesKcal: Float = 0f,
    val pacePerKm: Float = 0f,
    val currentAqi: Int? = null,
    val currentPollen: Int? = null,
    val currentLocation: RoutePoint? = null,
    val gpsEnabledMessage: String = "GPS enabled",
    val isStationary: Boolean = false,
    val movingTimeSeconds: Long = 0,
    val stationaryTimeSeconds: Long = 0,
    val currentSpeedMps: Float = 0f,
    val averageSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f
)
