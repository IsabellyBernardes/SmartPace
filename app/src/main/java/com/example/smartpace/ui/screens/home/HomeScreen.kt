package com.example.smartpace.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.smartpace.repository.MockData
import com.example.smartpace.viewmodel.ProfileViewModel
import com.example.smartpace.viewmodel.RunViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    runViewModel: RunViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val runs by runViewModel.runs.collectAsState()
    val profile by profileViewModel.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeader(name = profile.name)
        WeeklyGoalCard()
        WeeklyStatsCard()
        RecentRunsSection(runs = runs.take(3))
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HomeHeader(name: String) {
    val firstName = name.split(" ").firstOrNull() ?: name
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
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WeeklyGoalCard() {
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
                    text = "72%",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { MockData.weeklyProgress.toFloat() },
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
                Text("${MockData.weeklyKmDone.toInt()} km percorridos", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("${MockData.weeklyKmGoal.toInt()} km meta", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WeeklyStatsCard() {
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
            StatItem(value = MockData.weeklyRuns.toString(), unit = "corridas", label = "Esta semana")
            VerticalDivider(modifier = Modifier.height(48.dp), color = Color(0xFFE2E8F0))
            StatItem(value = MockData.weeklyTime, unit = "semana", label = "Tempo total")
            VerticalDivider(modifier = Modifier.height(48.dp), color = Color(0xFFE2E8F0))
            StatItem(value = MockData.bestPace, unit = "/km", label = "Melhor pace")
        }
    }
}

@Composable
fun StatItem(value: String, unit: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            if (unit == "/km") Text(unit, fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 3.dp))
        }
        if (unit != "/km") Text(unit, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFFCBD5E1))
    }
}

@Composable
fun RecentRunsSection(runs: List<Run>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Corridas recentes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text("Ver todas", fontSize = 13.sp, color = Color(0xFF3B82F6))
        }
        runs.forEach { run ->
            com.example.smartpace.ui.components.RunCard(run = run)
        }
    }
}
