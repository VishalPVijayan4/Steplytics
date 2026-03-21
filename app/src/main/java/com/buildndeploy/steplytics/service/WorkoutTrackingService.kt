package com.buildndeploy.steplytics.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.buildndeploy.steplytics.MainActivity
import com.buildndeploy.steplytics.R
import com.buildndeploy.steplytics.data.remote.AqiService
import com.buildndeploy.steplytics.domain.model.ActiveTrackingSession
import com.buildndeploy.steplytics.domain.model.RoutePoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class WorkoutTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val aqiService = AqiService()
    private var timerJob: Job? = null
    private var activeSession: ActiveTrackingSession? = null
    private var lastSignificantPoint: RoutePoint? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking(intent)
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking(intent: Intent) {
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        val session = ActiveTrackingSession(
            activityId = intent.getStringExtra(EXTRA_ACTIVITY_ID).orEmpty(),
            activityTitle = intent.getStringExtra(EXTRA_ACTIVITY_TITLE).orEmpty(),
            caloriesPerMinute = intent.getFloatExtra(EXTRA_CALORIES_PER_MINUTE, 0f),
            userWeight = intent.getFloatExtra(EXTRA_USER_WEIGHT, 70f),
            startedAt = System.currentTimeMillis()
        )
        activeSession = session
        TrackingSessionStore.update(session)
        try {
            startForeground(NOTIFICATION_ID, buildNotification(session))
        } catch (securityException: SecurityException) {
            Log.e(TAG, "Unable to start location foreground service", securityException)
            activeSession = null
            TrackingSessionStore.update(null)
            stopSelf()
            return
        }
        startTimer()
        startLocationUpdates()
    }

    private fun pauseTracking() {
        activeSession = activeSession?.copy(isPaused = true)
        activeSession?.let {
            TrackingSessionStore.update(it)
            updateNotification(it)
        }
        timerJob?.cancel()
        locationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun resumeTracking() {
        val resumed = activeSession?.copy(isPaused = false) ?: return
        activeSession = resumed
        TrackingSessionStore.update(resumed)
        updateNotification(resumed)
        startTimer()
        startLocationUpdates()
    }

    private fun stopTracking() {
        timerJob?.cancel()
        locationClient.removeLocationUpdates(locationCallback)
        TrackingSessionStore.update(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1_000)
                val session = activeSession ?: break
                if (!session.isPaused) {
                    val updated = session.copy(
                        elapsedSeconds = session.elapsedSeconds + 1,
                        caloriesKcal = ((session.elapsedSeconds + 1) / 60f) * session.caloriesPerMinute * (session.userWeight / 70f),
                        pacePerKm = if (session.distanceKm > 0f) ((session.elapsedSeconds + 1) / 60f) / session.distanceKm else 0f
                    )
                    activeSession = updated
                    TrackingSessionStore.update(updated)
                    updateNotification(updated)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateDistanceMeters(3f)
            .build()
        serviceScope.launch {
            locationClient.lastLocation.await()?.let { handleLocation(it) }
        }
        locationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { handleLocation(it) }
        }
    }

    private fun handleLocation(location: Location) {
        val session = activeSession ?: return
        if (session.isPaused) return
        val point = RoutePoint(location.latitude, location.longitude)
        val updatedRoute = if (session.route.lastOrNull() == point) session.route else session.route + point
        val distanceKm = calculateDistanceKm(updatedRoute)
        val previous = lastSignificantPoint
        val stationary = previous != null && distanceBetween(previous, point) < 5f
        lastSignificantPoint = point
        val updated = session.copy(
            route = updatedRoute,
            currentLocation = point,
            distanceKm = distanceKm,
            pacePerKm = if (distanceKm > 0f) (session.elapsedSeconds / 60f) / distanceKm else 0f,
            gpsEnabledMessage = if (hasLocationPermission()) "GPS enabled" else "Enable GPS",
            isStationary = stationary
        )
        activeSession = updated
        TrackingSessionStore.update(updated)
        updateNotification(updated)
        if (updated.currentAqi == null) {
            serviceScope.launch(Dispatchers.IO) {
                val aqi = aqiService.fetchCurrentUsAqi(point.latitude, point.longitude)
                val latest = activeSession ?: return@launch
                val finalSession = latest.copy(currentAqi = aqi)
                activeSession = finalSession
                TrackingSessionStore.update(finalSession)
                updateNotification(finalSession)
            }
        }
    }

    private fun buildNotification(session: ActiveTrackingSession): Notification {
        createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TRACKING, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${session.activityTitle} • ${formatElapsedTime(session.elapsedSeconds)}")
            .setContentText(session.gpsEnabledMessage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(session: ActiveTrackingSession) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(session))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Workout Tracking", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "WorkoutTrackingService"
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 2201
        const val EXTRA_OPEN_TRACKING = "open_tracking"
        private const val EXTRA_ACTIVITY_ID = "activity_id"
        private const val EXTRA_ACTIVITY_TITLE = "activity_title"
        private const val EXTRA_CALORIES_PER_MINUTE = "calories_per_minute"
        private const val EXTRA_USER_WEIGHT = "user_weight"
        private const val ACTION_START = "tracking_start"
        private const val ACTION_PAUSE = "tracking_pause"
        private const val ACTION_RESUME = "tracking_resume"
        private const val ACTION_STOP = "tracking_stop"

        fun start(context: Context, activityId: String, activityTitle: String, caloriesPerMinute: Float, userWeight: Float) {
            val intent = Intent(context, WorkoutTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACTIVITY_ID, activityId)
                putExtra(EXTRA_ACTIVITY_TITLE, activityTitle)
                putExtra(EXTRA_CALORIES_PER_MINUTE, caloriesPerMinute)
                putExtra(EXTRA_USER_WEIGHT, userWeight)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, WorkoutTrackingService::class.java).apply { action = ACTION_PAUSE })
        }

        fun resume(context: Context) {
            context.startService(Intent(context, WorkoutTrackingService::class.java).apply { action = ACTION_RESUME })
        }

        fun stop(context: Context) {
            context.startService(Intent(context, WorkoutTrackingService::class.java).apply { action = ACTION_STOP })
        }

        private fun calculateDistanceKm(route: List<RoutePoint>): Float {
            if (route.size < 2) return 0f
            var meters = 0f
            for (index in 1 until route.size) {
                meters += distanceBetween(route[index - 1], route[index])
            }
            return meters / 1_000f
        }

        private fun distanceBetween(start: RoutePoint, end: RoutePoint): Float {
            val results = FloatArray(1)
            Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
            return results[0]
        }

        private fun formatElapsedTime(totalSeconds: Long): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
