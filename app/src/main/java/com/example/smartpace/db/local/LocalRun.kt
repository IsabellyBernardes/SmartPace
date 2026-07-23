package com.example.smartpace.db.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.smartpace.model.LatLngPoint
import com.example.smartpace.model.Run

/**
 * Entidade da tabela local de corridas. Seguindo a recomendação do Google,
 * é uma classe separada do modelo de UI ([Run]), com conversões entre si.
 * Os pontos da rota são guardados como texto serializado ("lat,lng;lat,lng").
 */
@Entity(tableName = "runs")
data class LocalRun(
    @PrimaryKey val id: String,
    val distance: Double,
    val duration: String,
    val pace: String,
    val date: String,
    val calories: Int,
    val timestamp: Long,
    val region: String,
    val routePoints: String
)

fun LocalRun.toRun() = Run(
    id = id,
    distance = distance,
    duration = duration,
    pace = pace,
    date = date,
    calories = calories,
    timestamp = timestamp,
    region = region,
    routePoints = if (routePoints.isBlank()) emptyList()
    else routePoints.split(";").mapNotNull { pair ->
        val parts = pair.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()
        if (lat != null && lng != null) LatLngPoint(lat, lng) else null
    }
)

fun Run.toLocalRun() = LocalRun(
    id = id,
    distance = distance,
    duration = duration,
    pace = pace,
    date = date,
    calories = calories,
    timestamp = timestamp,
    region = region,
    routePoints = routePoints.joinToString(";") { "${it.lat},${it.lng}" }
)
