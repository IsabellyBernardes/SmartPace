package com.example.smartpace.ui.screens.run

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.navigation.Screen
import com.example.smartpace.model.AlertType
import com.example.smartpace.model.LatLngPoint
import com.example.smartpace.viewmodel.AlertViewModel
import com.example.smartpace.viewmodel.LocationViewModel
import com.example.smartpace.viewmodel.RunViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RunScreen(
    navController: NavController,
    runViewModel: RunViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    alertViewModel: AlertViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) locationViewModel.startTracking(context)
    }

    var isRunning by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showStopDialog by remember { mutableStateOf(false) }

    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val routePoints by locationViewModel.routePoints.collectAsState()
    val distanceMeters by locationViewModel.distanceMeters.collectAsState()
    val distanceKm = distanceMeters / 1000.0

    val alerts by alertViewModel.alerts.collectAsState()
    val alertSaved by alertViewModel.saved.collectAsState()
    var showAlertDialog by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(-8.05, -34.9), 17f
        )
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            locationViewModel.startTracking(context)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { loc ->
            coroutineScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(loc, 17f),
                    durationMs = 1000
                )
            }
        }
    }

    LaunchedEffect(alertSaved) {
        if (alertSaved) {
            delay(2000L)
            alertViewModel.resetSaved()
        }
    }

    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // Parar rastreamento ao sair da tela
    DisposableEffect(Unit) {
        onDispose { locationViewModel.stopTracking() }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)
    val distFormatted = "%.2f".format(distanceKm)
    val paceFormatted = if (distanceKm > 0.01) {
        val paceSeconds = (elapsedSeconds / distanceKm).toInt()
        "%d:%02d".format(paceSeconds / 60, paceSeconds % 60)
    } else "--:--"

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Encerrar corrida?", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja salvar e encerrar a corrida atual?") },
            confirmButton = {
                Button(
                    onClick = {
                        showStopDialog = false
                        isRunning = false
                        locationViewModel.stopTracking()
                        val savedRoute = routePoints.map { LatLngPoint(it.latitude, it.longitude) }
                        runViewModel.saveRun(distanceKm, elapsedSeconds, savedRoute)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Run.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) { Text("Salvar e sair") }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Continuar", color = Color(0xFF3B82F6))
                }
            }
        )
    }

    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = { showAlertDialog = false },
            title = { Text("Sinalizar problema", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Qual problema você encontrou?", fontSize = 14.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(4.dp))
                    AlertType.entries.forEach { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    alertViewModel.saveAlert(type, currentLocation)
                                    showAlertDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(type.emoji, fontSize = 20.sp)
                                Text(type.label, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAlertDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) Color(0xFFFBBF24) else Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaused) "PAUSADO" else "EM ANDAMENTO",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF97316))
                        .clickable { showAlertDialog = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            modifier = Modifier.size(14.dp), tint = Color(0xFF7C2D12))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sinalizar problema", fontSize = 12.sp,
                            color = Color(0xFF7C2D12), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RunMetricItem(value = timeFormatted, label = "TEMPO", light = false)
            Box(
                modifier = Modifier
                    .background(Color(0xFF3B82F6), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(distFormatted, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("KM", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.sp)
                }
            }
            RunMetricItem(value = paceFormatted, label = "MIN/KM", light = false)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = false,
                    compassEnabled = false
                )
            ) {
                if (routePoints.size >= 2) {
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF3B82F6),
                        width = 12f,
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        jointType = JointType.ROUND
                    )
                }
                alerts.forEach { alert ->
                    Marker(
                        state = MarkerState(position = LatLng(alert.lat, alert.lng)),
                        title = alert.type.label,
                        snippet = "${alert.type.emoji} ${alert.sightings} avistamentos"
                    )
                }
                currentLocation?.let { loc ->
                    Circle(
                        center = loc,
                        radius = 8.0,
                        fillColor = Color(0xFF3B82F6),
                        strokeColor = Color.White,
                        strokeWidth = 3f
                    )
                }
            }
            if (alertSaved) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Alerta registrado!", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (alertSaved) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF22C55E))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Alerta registrado com sucesso!", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFF1E293B),
            thickness = 1.dp
        )

        Row(
    modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF0F172A))
        .padding(vertical = 20.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = {
                        isPaused = !isPaused
                        if (isPaused) locationViewModel.pauseTracking()
                        else locationViewModel.resumeTracking(context)
                    }) {
            Icon(
                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Retomar" else "Pausar",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
        }
    }
    Spacer(modifier = Modifier.width(24.dp))
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = { showStopDialog = true }) {
            Icon(
                Icons.Default.Stop,
                contentDescription = "Parar",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(28.dp)
            )
        }
    }
    Spacer(modifier = Modifier.width(24.dp))
    Spacer(modifier = Modifier.size(56.dp))
}
    }
}

@Composable
fun RunMetricItem(value: String, label: String, light: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (light) Color(0xFF0F172A) else Color.White
        )
        Text(
            label,
            fontSize = 11.sp,
            color = if (light) Color(0xFF94A3B8) else Color(0xFF64748B),
            letterSpacing = 1.sp
        )
    }
}