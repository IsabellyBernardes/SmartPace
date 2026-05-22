package com.example.smartpace.model

data class Run(
    val id: String = "",
    val distance: Double = 0.0,
    val duration: String = "",
    val pace: String = "",
    val date: String = "",
    val calories: Int = 0,
    val routePoints: List<LatLngPoint> = emptyList()
)

data class LatLngPoint(val lat: Double, val lng: Double)
