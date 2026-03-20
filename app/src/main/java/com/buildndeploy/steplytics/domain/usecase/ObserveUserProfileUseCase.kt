package com.buildndeploy.steplytics.domain.usecase

import com.buildndeploy.steplytics.domain.repository.SteplyticsRepository

class ObserveUserProfileUseCase(
    private val repository: SteplyticsRepository
) {
    operator fun invoke() = repository.observeUserProfile()
}
