package com.example.smartpace.ui.screens.run

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
    locationViewModel: LocationViewModel = viewModel()
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
    var elapsedSeconds by remember { mutableStateOf(0) }
    var distanceKm by remember { mutableStateOf(0.0) }
    var showStopDialog by remember { mutableStateOf(false) }

    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val routePoints by locationViewModel.routePoints.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: LatLng(-8.05, -34.9), 17f
        )
    }

    // Solicitar permissão / iniciar rastreamento ao entrar na tela
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            locationViewModel.startTracking(context)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Mover câmera ao receber nova localização
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

    // Timer e distância simulada
    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused) {
            delay(1000L)
            elapsedSeconds++
            distanceKm += 0.0025
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
                        runViewModel.saveRun(distanceKm, elapsedSeconds)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Status Bar
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
                OutlinedButton(
                    onClick = {},
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning, contentDescription = null,
                        modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sinalizar problema", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // Métricas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RunMetricItem(value = timeFormatted, label = "TEMPO")
                Box(
                    modifier = Modifier
                        .background(Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(distFormatted, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("KM", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.sp)
                    }
                }
                RunMetricItem(value = paceFormatted, label = "MIN/KM")
            }
        }

        // Google Maps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
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
        }

        // Controles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Retomar" else "Pausar",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showStopDialog = true }) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Parar",
                        tint = Color.White,
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
fun RunMetricItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
    }
}
