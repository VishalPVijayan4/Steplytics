package com.buildndeploy.steplytics.domain.usecase

import com.buildndeploy.steplytics.domain.model.UserProfile
import com.buildndeploy.steplytics.domain.repository.SteplyticsRepository

class SaveUserProfileUseCase(
    private val repository: SteplyticsRepository
) {
    suspend operator fun invoke(profile: UserProfile) {
        repository.saveUserProfile(profile)
    }
}
