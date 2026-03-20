package com.buildndeploy.steplytics.data.repository

import com.buildndeploy.steplytics.data.local.SteplyticsPreferencesDataSource
import com.buildndeploy.steplytics.domain.model.UserProfile
import com.buildndeploy.steplytics.domain.repository.SteplyticsRepository
import kotlinx.coroutines.flow.Flow

class SteplyticsRepositoryImpl(
    private val dataSource: SteplyticsPreferencesDataSource
) : SteplyticsRepository {
    override fun observeIsFirstLaunch(): Flow<Boolean> = dataSource.observeIsFirstLaunch()

    override suspend fun setFirstLaunchCompleted() {
        dataSource.setFirstLaunchCompleted()
    }

    override fun observeUserProfile(): Flow<UserProfile?> = dataSource.observeUserProfile()

    override suspend fun saveUserProfile(profile: UserProfile) {
        dataSource.saveUserProfile(profile)
    }
}
