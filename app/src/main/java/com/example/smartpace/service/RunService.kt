package com.example.smartpace.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.smartpace.MainActivity
import com.example.smartpace.R
import com.example.smartpace.model.RouteAlert
import com.example.smartpace.repository.FirestoreRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Serviço em primeiro plano que mantém o rastreamento de GPS ativo mesmo com o
 * app em background: soma a distância, conta o tempo, checa a proximidade dos
 * alertas comunitários e vibra ao entrar no raio de um deles.
 */
class RunService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var fused: FusedLocationProviderClient
    private var callback: LocationCallback? = null
    private var lastLocation: Location? = null
    private var timerJob: Job? = null

    private val repository = FirestoreRepository()
    private var alerts: List<RouteAlert> = emptyList()
    private val alertedIds = mutableSetOf<String>()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRun()
            ACTION_PAUSE -> pauseRun()
            ACTION_RESUME -> resumeRun()
            ACTION_STOP -> stopRun()
        }
        return START_STICKY
    }

    private fun startRun() {
        RunTracker.reset()
        RunTracker.isTracking.value = true
        startForeground(NOTIF_ID, buildNotification("Iniciando…"))
        loadAlerts()
        startLocationUpdates()
        startTimer()
    }

    private fun loadAlerts() {
        scope.launch {
            alerts = try { repository.getAlerts() } catch (e: Exception) { emptyList() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (RunTracker.isPaused.value) return
                result.lastLocation?.let { handleLocation(it) }
            }
        }
        fused.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }

    private fun handleLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        RunTracker.currentLocation.value = latLng
        RunTracker.routePoints.value = RunTracker.routePoints.value + latLng
        lastLocation?.let { prev ->
            RunTracker.distanceMeters.value += prev.distanceTo(location)
        }
        lastLocation = location
        checkProximity(location)
        updateNotification()
    }

    /** Avisa (uma vez) ao entrar no raio de um alerta ainda não sinalizado. */
    private fun checkProximity(location: Location) {
        for (alert in alerts) {
            if (alert.id.isEmpty() || alert.id in alertedIds) continue
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                alert.lat, alert.lng, results
            )
            if (results[0] <= PROXIMITY_RADIUS_M) {
                alertedIds.add(alert.id)
                RunTracker.nearbyAlert.value = alert
                vibrate()
            }
        }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 400, 200, 400)
        val effect = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(effect)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                if (!RunTracker.isPaused.value) {
                    RunTracker.elapsedSeconds.value += 1
                    updateNotification()
                }
            }
        }
    }

    private fun pauseRun() {
        RunTracker.isPaused.value = true
        callback?.let { fused.removeLocationUpdates(it) }
        lastLocation = null
        updateNotification()
    }

    private fun resumeRun() {
        RunTracker.isPaused.value = false
        startLocationUpdates()
        updateNotification()
    }

    private fun stopRun() {
        RunTracker.isTracking.value = false
        callback?.let { fused.removeLocationUpdates(it) }
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ─── Notificação ─────────────────────────────────────────────

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Rastreamento de corrida",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartPace • Corrida em andamento")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun updateNotification() {
        val km = RunTracker.distanceMeters.value / 1000f
        val sec = RunTracker.elapsedSeconds.value
        val time = "%02d:%02d".format(sec / 60, sec % 60)
        val text = if (RunTracker.isPaused.value) "Pausado • %.2f km".format(km)
        else "%.2f km • %s".format(km, time)
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        callback?.let { fused.removeLocationUpdates(it) }
        timerJob?.cancel()
        scope.cancel()
    }

    companion object {
        const val ACTION_START = "com.example.smartpace.RUN_START"
        const val ACTION_PAUSE = "com.example.smartpace.RUN_PAUSE"
        const val ACTION_RESUME = "com.example.smartpace.RUN_RESUME"
        const val ACTION_STOP = "com.example.smartpace.RUN_STOP"

        private const val CHANNEL_ID = "run_tracking"
        private const val NOTIF_ID = 1001
        private const val PROXIMITY_RADIUS_M = 100f

        /** Dispara uma ação no serviço (o START sobe como foreground). */
        fun send(context: Context, action: String) {
            val intent = Intent(context, RunService::class.java).apply { this.action = action }
            if (action == ACTION_START) {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
