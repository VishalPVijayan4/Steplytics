package com.buildndeploy.steplytics.data.repository

import com.buildndeploy.steplytics.data.local.workout.WorkoutDao
import com.buildndeploy.steplytics.data.local.workout.WorkoutEntity
import com.buildndeploy.steplytics.domain.model.WorkoutRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val workoutDao: WorkoutDao
) {
    fun observeWorkouts(): Flow<List<WorkoutRecord>> = workoutDao.observeAll().map { workouts ->
        workouts.map { it.toDomain() }
    }

    suspend fun insertWorkout(workout: WorkoutRecord): Long {
        return workoutDao.insert(workout.toEntity())
    }
}

private fun WorkoutEntity.toDomain() = WorkoutRecord(
    id = id,
    activityType = activityType,
    startedAt = startedAt,
    endedAt = endedAt,
    durationSeconds = durationSeconds,
    distanceKm = distanceKm,
    caloriesKcal = caloriesKcal,
    pacePerKm = pacePerKm,
    avgAqi = avgAqi,
    avgPollen = avgPollen,
    movingTimeSeconds = movingTimeSeconds,
    stationaryTimeSeconds = stationaryTimeSeconds,
    averageSpeedMps = averageSpeedMps,
    maxSpeedMps = maxSpeedMps,
    route = route
)

private fun WorkoutRecord.toEntity() = WorkoutEntity(
    id = id,
    activityType = activityType,
    startedAt = startedAt,
    endedAt = endedAt,
    durationSeconds = durationSeconds,
    distanceKm = distanceKm,
    caloriesKcal = caloriesKcal,
    pacePerKm = pacePerKm,
    avgAqi = avgAqi,
    avgPollen = avgPollen,
    movingTimeSeconds = movingTimeSeconds,
    stationaryTimeSeconds = stationaryTimeSeconds,
    averageSpeedMps = averageSpeedMps,
    maxSpeedMps = maxSpeedMps,
    route = route
)
