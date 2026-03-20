package com.buildndeploy.steplytics.domain.repository

import com.buildndeploy.steplytics.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface SteplyticsRepository {
    fun observeIsFirstLaunch(): Flow<Boolean>
    suspend fun setFirstLaunchCompleted()
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
}
