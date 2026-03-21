package com.buildndeploy.steplytics.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

class AqiService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCurrentUsAqi(latitude: Double, longitude: Double): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$latitude&longitude=$longitude&current=us_aqi"
            val response = URL(endpoint).readText()
            json.decodeFromString(AqiResponse.serializer(), response).current?.usAqi
        }.getOrNull()
    }
}

@Serializable
private data class AqiResponse(
    val current: CurrentAqi? = null
)

@Serializable
private data class CurrentAqi(
    @SerialName("us_aqi") val usAqi: Int? = null
)
