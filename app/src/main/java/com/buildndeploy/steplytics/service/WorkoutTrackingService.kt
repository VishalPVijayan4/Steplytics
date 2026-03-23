package com.buildndeploy.steplytics.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.graphics.BitmapFactory
import android.widget.RemoteViews
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
import kotlin.math.max

class WorkoutTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val aqiService = AqiService()
    private var timerJob: Job? = null
    private var activeSession: ActiveTrackingSession? = null
    private var lastLocationSample: Location? = null
    private var lastEnvironmentRefreshAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            when (intent?.action) {
                ACTION_START -> startTracking(intent)
                ACTION_PAUSE -> pauseTracking()
                ACTION_RESUME -> resumeTracking()
                ACTION_STOP -> stopTracking()
            }
            START_STICKY
        } catch (securityException: SecurityException) {
            Log.e(TAG, "Foreground tracking start blocked", securityException)
            activeSession = null
            TrackingSessionStore.update(null)
            stopSelf()
            START_NOT_STICKY
        }
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
        lastLocationSample = null
        lastEnvironmentRefreshAt = 0L
        startForeground(NOTIFICATION_ID, buildNotification(session))
        activeSession = session
        lastLocationSample = null
        lastEnvironmentRefreshAt = 0L
        TrackingSessionStore.update(session)
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
        activeSession = null
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
                    val movingSeconds = if (session.isStationary) session.movingTimeSeconds else session.movingTimeSeconds + 1
                    val stationarySeconds = if (session.isStationary) session.stationaryTimeSeconds + 1 else 0
                    val speedFactor = max(session.currentSpeedMps / 1.4f, 0.35f)
                    val calories = if (session.isStationary) session.caloriesKcal else session.caloriesKcal + ((session.caloriesPerMinute / 60f) * speedFactor * (session.userWeight / 70f))
                    val pace = if (!session.isStationary && session.distanceKm > 0f && movingSeconds > 0) (movingSeconds / 60f) / session.distanceKm else session.pacePerKm
                    val updated = session.copy(
                        elapsedSeconds = session.elapsedSeconds + 1,
                        caloriesKcal = calories,
                        pacePerKm = pace,
                        movingTimeSeconds = movingSeconds,
                        stationaryTimeSeconds = stationarySeconds,
                        currentSpeedMps = if (session.isStationary) 0f else session.currentSpeedMps
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
            .setMinUpdateDistanceMeters(2f)
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
        val previousLocation = lastLocationSample
        val segmentMeters = if (previousLocation != null) previousLocation.distanceTo(location) else 0f
        val rawSpeed = when {
            location.hasSpeed() && location.speed >= 0f -> location.speed
            previousLocation != null -> {
                val seconds = ((location.time - previousLocation.time).coerceAtLeast(1L)) / 1000f
                if (seconds > 0f) segmentMeters / seconds else 0f
            }
            else -> 0f
        }
        val jitterFilteredStationary = previousLocation != null && segmentMeters < GPS_JITTER_DISTANCE_METERS && rawSpeed < STATIONARY_SPEED_MPS
        val isStationary = rawSpeed < STATIONARY_SPEED_MPS && segmentMeters < STATIONARY_DISTANCE_METERS || jitterFilteredStationary
        val displayPoint = if (isStationary) session.currentLocation ?: point else point
        val shouldAppendRoutePoint = session.route.isEmpty() || (!isStationary && segmentMeters >= ROUTE_APPEND_DISTANCE_METERS)
        val updatedRoute = if (shouldAppendRoutePoint) session.route + point else session.route
        val distanceKm = calculateDistanceKm(updatedRoute)
        val averageSpeed = if (session.movingTimeSeconds > 0 && distanceKm > 0f) (distanceKm * 1000f) / session.movingTimeSeconds else session.averageSpeedMps
        val updated = session.copy(
            route = updatedRoute,
            currentLocation = displayPoint,
            distanceKm = distanceKm,
            pacePerKm = if (!isStationary && distanceKm > 0f && session.movingTimeSeconds > 0) (session.movingTimeSeconds / 60f) / distanceKm else session.pacePerKm,
            gpsEnabledMessage = if (hasLocationPermission()) "GPS locked" else "Enable GPS",
            isStationary = isStationary,
            currentSpeedMps = if (isStationary) 0f else rawSpeed,
            averageSpeedMps = averageSpeed,
            maxSpeedMps = if (isStationary) session.maxSpeedMps else max(session.maxSpeedMps, rawSpeed)
        )
        lastLocationSample = if (isStationary) previousLocation ?: location else location
        activeSession = updated
        TrackingSessionStore.update(updated)
        updateNotification(updated)
        refreshEnvironmentIfNeeded(point)
    }

    private fun refreshEnvironmentIfNeeded(point: RoutePoint) {
        val now = System.currentTimeMillis()
        if (now - lastEnvironmentRefreshAt < ENVIRONMENT_REFRESH_MS) return
        lastEnvironmentRefreshAt = now
        serviceScope.launch(Dispatchers.IO) {
            val snapshot = aqiService.fetchEnvironmentSnapshot(point.latitude, point.longitude) ?: return@launch
            val latest = activeSession ?: return@launch
            val finalSession = latest.copy(
                currentAqi = snapshot.aqi,
                currentPollen = snapshot.pollen
            )
            activeSession = finalSession
            TrackingSessionStore.update(finalSession)
            updateNotification(finalSession)
        }
    }

    private fun buildNotification(session: ActiveTrackingSession): Notification {
        createChannel()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TRACKING, true)
            action = ACTION_OPEN_TRACKING
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationView = RemoteViews(packageName, R.layout.notification_tracking).apply {
            setImageViewBitmap(R.id.notification_icon, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            setTextViewText(R.id.notification_title, session.activityTitle)
            setTextViewText(R.id.notification_status, if (session.isStationary) "Marker locked while stationary" else "Live workout in progress")
            setTextViewText(R.id.notification_elapsed, formatElapsedTime(session.elapsedSeconds))
            setTextViewText(R.id.notification_distance, formatDistance(session.distanceKm))
            setTextViewText(R.id.notification_pace, "${formatPace(session.pacePerKm)}/km")
            setTextViewText(R.id.notification_calories, "${session.caloriesKcal.toInt()} kcal")
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationView)
            .setCustomBigContentView(notificationView)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
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
        private const val STATIONARY_SPEED_MPS = 0.6f
        private const val STATIONARY_DISTANCE_METERS = 2f
        private const val ROUTE_APPEND_DISTANCE_METERS = 2f
        private const val GPS_JITTER_DISTANCE_METERS = 8f
        private const val ENVIRONMENT_REFRESH_MS = 60_000L
        const val EXTRA_OPEN_TRACKING = "open_tracking"
        private const val EXTRA_ACTIVITY_ID = "activity_id"
        private const val EXTRA_ACTIVITY_TITLE = "activity_title"
        private const val EXTRA_CALORIES_PER_MINUTE = "calories_per_minute"
        private const val EXTRA_USER_WEIGHT = "user_weight"
        private const val ACTION_START = "tracking_start"
        private const val ACTION_PAUSE = "tracking_pause"
        private const val ACTION_RESUME = "tracking_resume"
        private const val ACTION_STOP = "tracking_stop"
        private const val ACTION_OPEN_TRACKING = "open_tracking_screen"

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

        private fun formatDistance(distanceKm: Float): String = String.format(Locale.US, "%.2f km", distanceKm)

        private fun formatPace(pacePerKm: Float): String {
            if (pacePerKm <= 0f || pacePerKm.isNaN() || pacePerKm.isInfinite()) return "00:00"
            val seconds = (pacePerKm * 60).toInt()
            return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
        }
    }
}
