package com.buildndeploy.steplytics.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.buildndeploy.steplytics.domain.model.UnitSystem
import com.buildndeploy.steplytics.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "steplytics_preferences")

class SteplyticsPreferencesDataSource(private val context: Context) {

    fun observeIsFirstLaunch(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> = context.dataStore.data.map { preferences ->
        val age = preferences[AGE_KEY]
        val weight = preferences[WEIGHT_KEY]
        val height = preferences[HEIGHT_KEY]
        val gender = preferences[GENDER_KEY]

        if (age == null || weight == null || height == null || gender.isNullOrBlank()) {
            null
        } else {
            UserProfile(
                age = age,
                weight = weight,
                height = height,
                gender = gender
            )
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[AGE_KEY] = profile.age
            preferences[WEIGHT_KEY] = profile.weight
            preferences[HEIGHT_KEY] = profile.height
            preferences[GENDER_KEY] = profile.gender
        }
    }

    fun observeUnitSystem(): Flow<UnitSystem> = context.dataStore.data.map { preferences ->
        preferences[UNIT_SYSTEM_KEY]
            ?.let { storedValue -> UnitSystem.entries.firstOrNull { it.name == storedValue } }
            ?: UnitSystem.Metric
    }

    suspend fun saveUnitSystem(unitSystem: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[UNIT_SYSTEM_KEY] = unitSystem.name
        }
    }

    fun observeNotificationsEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    private companion object {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val AGE_KEY = intPreferencesKey("user_age")
        val WEIGHT_KEY = floatPreferencesKey("user_weight")
        val HEIGHT_KEY = floatPreferencesKey("user_height")
        val GENDER_KEY = stringPreferencesKey("user_gender")
        val UNIT_SYSTEM_KEY = stringPreferencesKey("unit_system")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    }
}
