package com.example.smartpace.repository

import com.example.smartpace.model.Achievement
import com.example.smartpace.model.AlertType
import com.example.smartpace.model.RouteAlert
import com.example.smartpace.model.Run
import com.example.smartpace.model.UserProfile

object MockData {

    val currentUser = UserProfile(
        id = "1",
        name = "João Silva",
        email = "joao@email.com",
        memberSince = "Jan 2024",
        weeklyGoalKm = 50.0,
        totalRuns = 24,
        totalKm = 187.0,
        avgPace = "5:24"
    )

    val recentRuns = listOf(
        Run(id = "1", distance = 5.2, duration = "28:14", pace = "5:26", date = "Hoje, 06:30", calories = 312),
        Run(id = "2", distance = 8.1, duration = "44:32", pace = "5:30", date = "Ontem, 07:15", calories = 487),
        Run(id = "3", distance = 3.8, duration = "20:10", pace = "5:18", date = "Seg, 06:45", calories = 228),
        Run(id = "4", distance = 10.0, duration = "55:20", pace = "5:32", date = "Sem. passada, 06:30", calories = 602),
        Run(id = "5", distance = 6.4, duration = "35:12", pace = "5:30", date = "Sem. passada, 07:00", calories = 384)
    )

    val weeklyKmByDay = listOf(
        Pair("Seg", 5.2),
        Pair("Ter", 8.1),
        Pair("Qua", 3.8),
        Pair("Qui", 10.0),
        Pair("Sex", 0.0),
        Pair("Sáb", 6.4),
        Pair("Dom", 3.2)
    )

    val achievements = listOf(
        Achievement(
            id = "1", title = "Primeira Corrida",
            description = "Completou sua primeira corrida registrada no app",
            emoji = "👟", unlocked = true
        ),
        Achievement(
            id = "2", title = "5km em 25min",
            description = "Manteve pace abaixo de 5:00/km por 5km seguidos",
            emoji = "⚡", unlocked = true
        ),
        Achievement(
            id = "3", title = "7 Dias Seguidos",
            description = "Registrou ao menos uma corrida por 7 dias consecutivos",
            emoji = "🔥", unlocked = false
        ),
        Achievement(
            id = "4", title = "100km Totais",
            description = "Acumulou 100km em corridas registradas no SmartPace",
            emoji = "🏆", unlocked = false
        )
    )

    val routeAlerts = listOf(
        RouteAlert(id = "1", type = AlertType.CONSTRUCTION, lat = -8.063, lng = -34.871, sightings = 23),
        RouteAlert(id = "2", type = AlertType.OBSTACLE, lat = -8.058, lng = -34.875, sightings = 47),
        RouteAlert(id = "3", type = AlertType.WET_FLOOR, lat = -8.061, lng = -34.868, sightings = 18)
    )

    val weeklyKmDone = 36.0
    val weeklyKmGoal = 50.0
    val weeklyProgress = weeklyKmDone / weeklyKmGoal

    val weeklyRuns = 5
    val weeklyTime = "3h12m"
    val bestPace = "5:14"
}
