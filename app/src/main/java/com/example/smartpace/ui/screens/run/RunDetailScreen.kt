package com.example.smartpace.ui.screens.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.model.Run
import com.example.smartpace.viewmodel.RunViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*

@Composable
fun RunDetailScreen(
    navController: NavController,
    runId: String,
    runViewModel: RunViewModel = viewModel()
) {
    val runs by runViewModel.runs.collectAsState()
    val run = runs.firstOrNull { it.id == runId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
            Column {
                Text("Detalhe da corrida", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (run != null) {
                    Text(run.date, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }

        when {
            run == null && runs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            }
            run == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Corrida não encontrada", color = Color(0xFF94A3B8))
                }
            }
            else -> RunDetailContent(run)
        }
    }
}

@Composable
private fun ColumnScope.RunDetailContent(run: Run) {
    val routePoints = remember(run.id) {
        run.routePoints.map { LatLng(it.lat, it.lng) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            routePoints.firstOrNull() ?: LatLng(-8.05, -34.9), 15f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        if (routePoints.size >= 2) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false),
                onMapLoaded = {
                    val bounds = LatLngBounds.builder().apply {
                        routePoints.forEach { include(it) }
                    }.build()
                    runCatching {
                        cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    }
                }
            ) {
                Polyline(
                    points = routePoints,
                    color = Color(0xFF3B82F6),
                    width = 14f,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                    jointType = JointType.ROUND
                )
                Marker(state = MarkerState(position = routePoints.first()), title = "Início")
                Marker(state = MarkerState(position = routePoints.last()), title = "Fim")
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗺️", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sem trajeto GPS para esta corrida",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "${run.distance} km",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailMetric(value = run.duration, label = "TEMPO")
                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE2E8F0))
                DetailMetric(value = "${run.pace}/km", label = "PACE")
                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE2E8F0))
                DetailMetric(value = "${run.calories}", label = "KCAL")
            }
        }
    }
}

@Composable
private fun DetailMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
    }
}
