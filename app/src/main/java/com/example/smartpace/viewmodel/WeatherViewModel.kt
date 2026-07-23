package com.example.smartpace.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class WeatherInfo(
    val temperatureC: Int,
    val emoji: String,
    val description: String,
    val isBad: Boolean
)

sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Success(val info: WeatherInfo) : WeatherState()
    object Error : WeatherState()
}

class WeatherViewModel : ViewModel() {

    private val _state = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val state: StateFlow<WeatherState> = _state

    fun load(context: Context) {
        viewModelScope.launch {
            _state.value = WeatherState.Loading
            val (lat, lng) = resolveLocation(context)
            val info = withContext(Dispatchers.IO) { fetchWeather(lat, lng) }
            _state.value = if (info != null) WeatherState.Success(info) else WeatherState.Error
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun resolveLocation(context: Context): Pair<Double, Double> {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            return try {
                val loc = LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
                if (loc != null) loc.latitude to loc.longitude else DEFAULT_LOCATION
            } catch (e: Exception) {
                DEFAULT_LOCATION
            }
        }
        return DEFAULT_LOCATION
    }

    private fun fetchWeather(lat: Double, lng: Double): WeatherInfo? {
        return try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lng&current=temperature_2m,weather_code"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(text).getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val code = current.getInt("weather_code")
            val (emoji, desc, bad) = describeWeather(code)
            WeatherInfo(temp, emoji, desc, bad)
        } catch (e: Exception) {
            null
        }
    }

    private fun describeWeather(code: Int): Triple<String, String, Boolean> = when (code) {
        0 -> Triple("☀️", "Céu limpo", false)
        1, 2 -> Triple("⛅", "Parc. nublado", false)
        3 -> Triple("☁️", "Nublado", false)
        45, 48 -> Triple("🌫️", "Névoa", false)
        51, 53, 55 -> Triple("🌦️", "Garoa", true)
        56, 57 -> Triple("🌧️", "Garoa gelada", true)
        61, 63, 65 -> Triple("🌧️", "Chuva", true)
        66, 67 -> Triple("🌧️", "Chuva gelada", true)
        71, 73, 75, 77 -> Triple("❄️", "Neve", true)
        80, 81, 82 -> Triple("🌦️", "Pancadas", true)
        85, 86 -> Triple("❄️", "Neve", true)
        95 -> Triple("⛈️", "Tempestade", true)
        96, 99 -> Triple("⛈️", "Tempestade", true)
        else -> Triple("🌡️", "", false)
    }

    companion object {
        private val DEFAULT_LOCATION = -8.05 to -34.9
    }
}
