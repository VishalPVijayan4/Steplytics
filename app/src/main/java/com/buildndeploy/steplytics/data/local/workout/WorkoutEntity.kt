package com.buildndeploy.steplytics.data.local.workout

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.buildndeploy.steplytics.domain.model.RoutePoint

@Entity(tableName = "workouts")
@TypeConverters(WorkoutConverters::class)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
