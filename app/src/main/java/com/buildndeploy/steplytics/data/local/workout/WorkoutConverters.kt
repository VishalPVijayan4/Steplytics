package com.buildndeploy.steplytics.data.local.workout

import androidx.room.TypeConverter
import com.buildndeploy.steplytics.domain.model.RoutePoint
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class WorkoutConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRoute(value: List<RoutePoint>): String {
        return json.encodeToString(ListSerializer(RoutePoint.serializer()), value)
    }

    @TypeConverter
    fun toRoute(value: String): List<RoutePoint> {
        if (value.isBlank()) return emptyList()
        return json.decodeFromString(ListSerializer(RoutePoint.serializer()), value)
    }
}
