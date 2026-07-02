package com.example.smartpace.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val memberSince: String = "",
    val weeklyGoalKm: Double = 50.0,
    val totalRuns: Int = 0,
    val totalKm: Double = 0.0,
    val avgPace: String = "",
    val weightKg: Double = 70.0
)
