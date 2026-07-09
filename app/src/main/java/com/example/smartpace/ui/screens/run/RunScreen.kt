package com.example.smartpace.ui.screens.run

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import com.example.smartpace.service.RunService
import com.example.smartpace.service.RunTracker
import com.example.smartpace.viewmodel.AlertViewModel
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

    // Estado da corrida vem do serviço (que continua em background).
    val currentLocation by RunTracker.currentLocation.collectAsState()
    val routePoints by RunTracker.routePoints.collectAsState()
    val distanceMeters by RunTracker.distanceMeters.collectAsState()
    val elapsedSeconds by RunTracker.elapsedSeconds.collectAsState()
    val isPaused by RunTracker.isPaused.collectAsState()
    val nearbyAlert by RunTracker.nearbyAlert.collectAsState()
    val distanceKm = distanceMeters / 1000.0

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = granted
        if (granted && !RunTracker.isTracking.value) {
            RunService.send(context, RunService.ACTION_START)
        }
    }

    var showStopDialog by remember { mutableStateOf(false) }
    val alerts by alertViewModel.alerts.collectAsState()
    val alertSaved by alertViewModel.saved.collectAsState()
    var showAlertDialog by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(-8.05, -34.9), 17f
        )
    }

    // Inicia o serviço ao entrar (se ainda não houver corrida em andamento).
    LaunchedEffect(Unit) {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        if (hasLocationPermission) {
            if (!RunTracker.isTracking.value) RunService.send(context, RunService.ACTION_START)
        } else {
            permissionLauncher.launch(permissions)
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

    LaunchedEffect(nearbyAlert) {
        if (nearbyAlert != null) {
            delay(6000L)
            RunTracker.nearbyAlert.value = null
        }
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
                        val savedRoute = routePoints.map { LatLngPoint(it.latitude, it.longitude) }
                        runViewModel.saveRun(distanceKm, elapsedSeconds, context.applicationContext, savedRoute)
                        RunService.send(context, RunService.ACTION_STOP)
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

        // Aviso de alerta próximo (também vibra pelo serviço)
        nearbyAlert?.let { alert ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF97316))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(alert.type.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Atenção: ${alert.type.label} por perto",
                            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Text("Fique atento à sua volta", color = Color(0xFFFFF7ED), fontSize = 12.sp)
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
                    val markerIcon = remember(alert.type) { emojiMarkerDescriptor(alert.type.emoji) }
                    Marker(
                        state = MarkerState(position = LatLng(alert.lat, alert.lng)),
                        title = alert.type.label,
                        snippet = "${alert.type.emoji} ${alert.sightings} avistamentos",
                        icon = markerIcon
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
                    if (isPaused) RunService.send(context, RunService.ACTION_RESUME)
                    else RunService.send(context, RunService.ACTION_PAUSE)
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
