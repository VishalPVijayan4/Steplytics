package com.buildndeploy.steplytics.domain.usecase

import com.buildndeploy.steplytics.domain.repository.SteplyticsRepository

class CompleteOnboardingUseCase(
    private val repository: SteplyticsRepository
) {
    suspend operator fun invoke() {
        repository.setFirstLaunchCompleted()
    }
}
