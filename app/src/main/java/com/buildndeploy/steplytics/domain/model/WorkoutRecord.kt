package com.buildndeploy.steplytics.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

data class WorkoutRecord(
    val id: Long = 0,
    val activityType: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val distanceKm: Float,
    val caloriesKcal: Float,
    val pacePerKm: Float,
    val avgAqi: Int?,
    val route: List<RoutePoint>
)
