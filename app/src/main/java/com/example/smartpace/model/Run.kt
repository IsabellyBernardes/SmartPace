package com.example.smartpace.model

data class Run(
    val id: String = "",
    val distance: Double = 0.0,
    val duration: String = "",
    val pace: String = "",
    val date: String = "",
    val calories: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val region: String = "",
    val routePoints: List<LatLngPoint> = emptyList()
)

data class LatLngPoint(val lat: Double = 0.0, val lng: Double = 0.0)
