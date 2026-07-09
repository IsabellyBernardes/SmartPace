package com.example.smartpace.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.model.Run
import com.example.smartpace.navigation.Screen
import com.example.smartpace.utils.formatTotalTime
import com.example.smartpace.utils.isThisWeek
import com.example.smartpace.utils.paceToSeconds
import com.example.smartpace.utils.parseDurationToSeconds
import com.example.smartpace.viewmodel.ProfileViewModel
import com.example.smartpace.viewmodel.RunViewModel
import com.example.smartpace.viewmodel.WeatherState
import com.example.smartpace.viewmodel.WeatherViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    runViewModel: RunViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val runs by runViewModel.runs.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    val weatherState by weatherViewModel.state.collectAsState()
    val context = LocalContext.current

    // Recarrega ao abrir a Home: cobre o caso de o usuário logar depois que os
    // ViewModels compartilhados já foram criados (com dados vazios).
    LaunchedEffect(Unit) {
        runViewModel.loadRuns()
        profileViewModel.loadProfile()
        weatherViewModel.load(context)
    }

    val weeklyRuns = runs.filter { isThisWeek(it.timestamp) }
    val weeklyKmDone = weeklyRuns.sumOf { it.distance }
    val weeklyKmGoal = profile.weeklyGoalKm
    val weeklyProgress = if (weeklyKmGoal > 0) (weeklyKmDone / weeklyKmGoal).toFloat().coerceAtMost(1f) else 0f
    val weeklyRunCount = weeklyRuns.size
    val weeklyTimeSec = weeklyRuns.sumOf { parseDurationToSeconds(it.duration) }
    val weeklyTimeStr = if (weeklyTimeSec > 0) formatTotalTime(weeklyTimeSec) else "--"
    val bestPaceStr = runs.minByOrNull { paceToSeconds(it.pace) }?.pace ?: "--:--"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeader(name = profile.name, weatherState = weatherState)
        WeeklyGoalCard(weeklyProgress, weeklyKmDone, weeklyKmGoal)
        WeeklyStatsCard(weeklyRunCount, weeklyTimeStr, bestPaceStr)
        RecentRunsSection(runs = runs.take(3), navController = navController)
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HomeHeader(name: String, weatherState: WeatherState) {
    val firstName = name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Corredor"
    val initials = name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Olá, $firstName 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Pronto para correr hoje?",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherChip(weatherState)
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF1E40AF), Color(0xFF0F172A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WeatherChip(state: WeatherState) {
    if (state !is WeatherState.Success) return
    val info = state.info
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (info.isBad) Color(0xFFFEF3C7) else Color(0xFFEFF6FF))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(info.emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "${info.temperatureC}°",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
        // Só mostra a descrição quando o tempo pede atenção (chuva, tempestade…)
        if (info.isBad && info.description.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(info.description, fontSize = 12.sp, color = Color(0xFF92400E))
        }
    }
}

@Composable
fun WeeklyGoalCard(weeklyProgress: Float, weeklyKmDone: Double, weeklyKmGoal: Double) {
    val percent = (weeklyProgress * 100).toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Meta semanal", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text(
                    text = "$percent%",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { weeklyProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF3B82F6),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("%.1f".format(weeklyKmDone), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" km percorridos", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${weeklyKmGoal.toInt()}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" km meta", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun WeeklyStatsCard(weeklyRuns: Int, weeklyTime: String, bestPace: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(value = weeklyRuns.toString(), unit = "corridas", label = "Esta semana")
            VerticalDivider(modifier = Modifier.height(48.dp), color = Color(0xFFE2E8F0))
            StatItem(value = weeklyTime, unit = "semana", label = "Tempo total")
            VerticalDivider(modifier = Modifier.height(48.dp), color = Color(0xFFE2E8F0))
            StatItem(value = bestPace, unit = "/km", label = "Melhor pace")
        }
    }
}

@Composable
fun StatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            if (unit == "/km") Text(unit, fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 3.dp))
        }
        if (unit != "/km") Text(unit, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFFCBD5E1))
    }
}

@Composable
fun RecentRunsSection(runs: List<Run>, navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Corridas recentes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(
                "Ver todas",
                fontSize = 13.sp,
                color = Color(0xFF3B82F6),
                modifier = Modifier.clickable {
                    navController.navigate(Screen.History.route) { launchSingleTop = true }
                }
            )
        }
        if (runs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏃", fontSize = 40.sp)
                    Text(
                        "Nenhuma corrida ainda",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        "Toque em + para registrar sua primeira corrida",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            runs.forEach { run ->
                com.example.smartpace.ui.components.RunCard(run = run)
            }
        }
    }
}
