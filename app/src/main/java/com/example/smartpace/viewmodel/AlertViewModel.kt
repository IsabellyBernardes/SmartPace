package com.example.smartpace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpace.model.AlertType
import com.example.smartpace.model.RouteAlert
import com.example.smartpace.repository.FirestoreRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertViewModel : ViewModel() {

    private val repository = FirestoreRepository()

    private val _alerts = MutableStateFlow<List<RouteAlert>>(emptyList())
    val alerts: StateFlow<List<RouteAlert>> = _alerts

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init { loadAlerts() }

    fun loadAlerts() {
        viewModelScope.launch {
            try {
                _alerts.value = repository.getAlerts()
            } catch (e: Exception) { }
        }
    }

    fun saveAlert(type: AlertType, location: LatLng?) {
        viewModelScope.launch {
            try {
                val alert = RouteAlert(
                    type = type,
                    lat = location?.latitude ?: 0.0,
                    lng = location?.longitude ?: 0.0,
                    sightings = 1
                )
                repository.saveAlert(alert)
                _alerts.value = _alerts.value + alert
                _saved.value = true
            } catch (e: Exception) { }
        }
    }

    fun resetSaved() { _saved.value = false }
}
