package com.example.smartpace.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartpace.model.Run
import com.example.smartpace.repository.MockData

@Composable
fun HistoryScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Histórico", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            OutlinedButton(
                onClick = {},
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Filtrar", fontSize = 13.sp, color = Color(0xFF64748B))
            }
        }

        // Card Resumo Total
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistorySummaryItem("6", "CORRIDAS")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                HistorySummaryItem("37.7 km", "TOTAL")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                HistorySummaryItem("2.267", "CALORIAS")
            }
        }

        // Grupos de corridas
        RunGroup(label = "HOJE", runs = listOf(MockData.recentRuns[0]))
        RunGroup(label = "ESTA SEMANA", runs = listOf(MockData.recentRuns[1], MockData.recentRuns[2]))
        RunGroup(label = "SEMANA PASSADA", runs = listOf(MockData.recentRuns[3], MockData.recentRuns[4]))

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HistorySummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
    }
}

@Composable
fun RunGroup(label: String, runs: List<Run>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        runs.forEach { run -> HistoryRunCard(run = run) }
    }
}

@Composable
fun HistoryRunCard(run: Run) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${run.distance} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("${run.date} · ${run.duration}", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${run.pace}/km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("${run.calories} kcal", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
