package com.buildndeploy.steplytics.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

class AqiService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchEnvironmentSnapshot(latitude: Double, longitude: Double): EnvironmentSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = buildString {
                append("https://air-quality-api.open-meteo.com/v1/air-quality")
                append("?latitude=$latitude&longitude=$longitude")
                append("&current=us_aqi,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen")
            }
            val response = URL(endpoint).readText()
            val current = json.decodeFromString(AqiResponse.serializer(), response).current
            current?.let {
                EnvironmentSnapshot(
                    aqi = it.usAqi,
                    pollen = listOfNotNull(
                        it.alderPollen,
                        it.birchPollen,
                        it.grassPollen,
                        it.mugwortPollen,
                        it.olivePollen,
                        it.ragweedPollen
                    ).maxOrNull()
                )
            }
        }.getOrNull()
    }
}

data class EnvironmentSnapshot(
    val aqi: Int?,
    val pollen: Int?
)

@Serializable
private data class AqiResponse(
    val current: CurrentAqi? = null
)

@Serializable
private data class CurrentAqi(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("alder_pollen") val alderPollen: Int? = null,
    @SerialName("birch_pollen") val birchPollen: Int? = null,
    @SerialName("grass_pollen") val grassPollen: Int? = null,
    @SerialName("mugwort_pollen") val mugwortPollen: Int? = null,
    @SerialName("olive_pollen") val olivePollen: Int? = null,
    @SerialName("ragweed_pollen") val ragweedPollen: Int? = null
)
