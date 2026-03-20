package com.buildndeploy.steplytics.domain.usecase

import com.buildndeploy.steplytics.domain.repository.SteplyticsRepository

class ObserveIsFirstLaunchUseCase(
    private val repository: SteplyticsRepository
) {
    operator fun invoke() = repository.observeIsFirstLaunch()
}
