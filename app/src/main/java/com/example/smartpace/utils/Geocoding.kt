package com.example.smartpace.utils

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Converte coordenadas no nome da região (bairro, cidade) usando o Geocoder
 * nativo do Android. Retorna "" se não conseguir resolver.
 */
@Suppress("DEPRECATION")
suspend fun reverseGeocodeRegion(context: Context, lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
        try {
            val addresses = Geocoder(context, Locale("pt", "BR")).getFromLocation(lat, lng, 1)
            val address = addresses?.firstOrNull() ?: return@withContext ""
            val bairro = address.subLocality ?: address.subAdminArea
            val cidade = address.locality ?: address.adminArea
            listOfNotNull(bairro, cidade).distinct().joinToString(", ")
        } catch (e: Exception) {
            ""
        }
    }
