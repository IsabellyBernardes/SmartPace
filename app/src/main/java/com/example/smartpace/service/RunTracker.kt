package com.example.smartpace.service

import com.example.smartpace.model.RouteAlert
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow

object RunTracker {
    val currentLocation = MutableStateFlow<LatLng?>(null)
    val routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val distanceMeters = MutableStateFlow(0f)
    val elapsedSeconds = MutableStateFlow(0)
    val isPaused = MutableStateFlow(false)
    val isTracking = MutableStateFlow(false)

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
