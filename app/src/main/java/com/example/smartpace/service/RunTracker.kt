package com.example.smartpace.service

import com.example.smartpace.model.RouteAlert
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Estado da corrida compartilhado entre o [RunService] (que roda em background)
 * e a tela de corrida. Como o serviço vive fora do ciclo de vida da UI, usamos
 * um holder singleton com StateFlows que a UI apenas observa.
 */
object RunTracker {
    val currentLocation = MutableStateFlow<LatLng?>(null)
    val routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val distanceMeters = MutableStateFlow(0f)
    val elapsedSeconds = MutableStateFlow(0)
    val isPaused = MutableStateFlow(false)
    val isTracking = MutableStateFlow(false)

    /** Último alerta em cujo raio o corredor entrou (para a UI mostrar um aviso). */
    val nearbyAlert = MutableStateFlow<RouteAlert?>(null)

    fun reset() {
        currentLocation.value = null
        routePoints.value = emptyList()
        distanceMeters.value = 0f
        elapsedSeconds.value = 0
        isPaused.value = false
        nearbyAlert.value = null
    }
}
