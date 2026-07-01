package com.example.smartpace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpace.model.LatLngPoint
import com.example.smartpace.model.Run
import com.example.smartpace.repository.FirestoreRepository
import com.example.smartpace.utils.paceToSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RunsState {
    object Loading : RunsState()
    data class Success(val runs: List<Run>) : RunsState()
    data class Error(val message: String) : RunsState()
}

class RunViewModel : ViewModel() {

    private val repository = FirestoreRepository()

    private val _runsState = MutableStateFlow<RunsState>(RunsState.Loading)
    val runsState: StateFlow<RunsState> = _runsState

    private val _runs = MutableStateFlow<List<Run>>(emptyList())
    val runs: StateFlow<List<Run>> = _runs

    init {
        loadRuns()
    }

    fun loadRuns() {
        viewModelScope.launch {
            _runsState.value = RunsState.Loading
            try {
                val firestoreRuns = repository.getRuns()
                _runs.value = firestoreRuns
                _runsState.value = RunsState.Success(firestoreRuns)
            } catch (e: Exception) {
                _runs.value = emptyList()
                _runsState.value = RunsState.Success(emptyList())
            }
        }
    }

    fun saveRun(
        distanceKm: Double,
        elapsedSeconds: Int,
        routePoints: List<LatLngPoint> = emptyList(),
        calories: Int = 0
    ) {
        viewModelScope.launch {
            try {
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val duration = "%02d:%02d".format(minutes, seconds)
                val paceSeconds = if (distanceKm > 0) (elapsedSeconds / distanceKm).toInt() else 0
                val pace = "%d:%02d".format(paceSeconds / 60, paceSeconds % 60)
                val dateFormat = SimpleDateFormat("dd/MM, HH:mm", Locale("pt", "BR"))
                val date = dateFormat.format(Date())
                val weightKg = repository.getUserProfile()?.weightKg ?: DEFAULT_WEIGHT_KG
                val estimatedCalories = if (calories > 0) calories
                    else estimateCalories(distanceKm, elapsedSeconds, weightKg)

                val run = Run(
                    distance = String.format("%.2f", distanceKm).toDouble(),
                    duration = duration,
                    pace = pace,
                    date = date,
                    calories = estimatedCalories,
                    timestamp = System.currentTimeMillis(),
                    routePoints = routePoints
                )
                repository.saveRun(run)
                loadRuns()
            } catch (e: Exception) { }
        }
    }

    // Estimativa por MET: kcal = MET * peso(kg) * horas.
    // No intervalo de corrida, o MET aproxima-se da velocidade em km/h.
    private fun estimateCalories(
        distanceKm: Double,
        elapsedSeconds: Int,
        weightKg: Double = DEFAULT_WEIGHT_KG
    ): Int {
        if (distanceKm <= 0.0 || elapsedSeconds <= 0) return 0
        val hours = elapsedSeconds / 3600.0
        val speedKmh = distanceKm / hours
        val met = speedKmh.coerceIn(6.0, 20.0)
        return (met * weightKg * hours).toInt()
    }

    val totalDistance get() = _runs.value.sumOf { it.distance }
    val totalRuns get() = _runs.value.size
    val bestPace get() = _runs.value.minByOrNull { paceToSeconds(it.pace) }?.pace ?: "--:--"
    val weeklyRuns get() = _runs.value.take(7)

    companion object {
        private const val DEFAULT_WEIGHT_KG = 70.0
    }
}
