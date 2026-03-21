package com.buildndeploy.steplytics.service

import com.buildndeploy.steplytics.domain.model.ActiveTrackingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TrackingSessionStore {
    private val _session = MutableStateFlow<ActiveTrackingSession?>(null)
    val session: StateFlow<ActiveTrackingSession?> = _session.asStateFlow()

    fun update(session: ActiveTrackingSession?) {
        _session.value = session
    }
}
