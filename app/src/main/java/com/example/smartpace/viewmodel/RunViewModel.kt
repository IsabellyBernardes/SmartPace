package com.example.smartpace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpace.model.Run
import com.example.smartpace.repository.FirestoreRepository
import com.example.smartpace.repository.MockData
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

    var usingMockData: Boolean = false
        private set

    init {
        loadRuns()
    }

    fun loadRuns() {
        viewModelScope.launch {
            _runsState.value = RunsState.Loading
            try {
                val firestoreRuns = repository.getRuns()
                if (firestoreRuns.isEmpty()) {
                    usingMockData = true
                    _runs.value = MockData.recentRuns
                    _runsState.value = RunsState.Success(MockData.recentRuns)
                } else {
                    usingMockData = false
                    _runs.value = firestoreRuns
                    _runsState.value = RunsState.Success(firestoreRuns)
                }
            } catch (e: Exception) {
                usingMockData = true
                _runs.value = MockData.recentRuns
                _runsState.value = RunsState.Success(MockData.recentRuns)
            }
        }
    }

    fun saveRun(
        distanceKm: Double,
        elapsedSeconds: Int,
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
                val estimatedCalories = if (calories == 0) (distanceKm * 60).toInt() else calories

                val run = Run(
                    distance = String.format("%.2f", distanceKm).toDouble(),
                    duration = duration,
                    pace = pace,
                    date = date,
                    calories = estimatedCalories,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveRun(run)
                loadRuns()
            } catch (e: Exception) {
                // corrida não salva — continua silenciosamente
            }
        }
    }

    val totalDistance get() = _runs.value.sumOf { it.distance }
    val totalRuns get() = _runs.value.size
    val bestPace get() = _runs.value.minByOrNull { paceToSeconds(it.pace) }?.pace ?: "--:--"
    val weeklyRuns get() = _runs.value.take(7)

    private fun paceToSeconds(pace: String): Int {
        return try {
            val parts = pace.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            Int.MAX_VALUE
        }
    }
}
