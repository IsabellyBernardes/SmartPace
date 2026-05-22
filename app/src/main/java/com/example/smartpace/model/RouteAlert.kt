package com.example.smartpace.model

data class RouteAlert(
    val id: String = "",
    val type: AlertType = AlertType.OBSTACLE,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val sightings: Int = 0
)

enum class AlertType(val label: String, val emoji: String) {
    OBSTACLE("Obstáculo na via", "⚠️"),
    CONSTRUCTION("Via em obra", "🔧"),
    WET_FLOOR("Piso molhado", "💧"),
    TRAFFIC("Tráfego intenso", "🚗"),
    LOW_LIGHT("Pouca iluminação", "💡"),
    DANGER_ZONE("Área de risco", "🚨")
}
