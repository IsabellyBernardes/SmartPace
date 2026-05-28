package com.example.smartpace.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.smartpace.repository.MockData

@Composable
fun DashboardScreen(navController: NavController) {
    var selectedPeriod by remember { mutableStateOf("semana") }
    val periods = listOf("semana", "mês", "ano")

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
            Text("Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                periods.forEach { period ->
                    val isSelected = period == selectedPeriod
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF0F172A) else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Card Distância Total
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
                    Text("DISTÂNCIA TOTAL", fontSize = 11.sp, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("↑ +12%", fontSize = 12.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("37.7 km", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(modifier = Modifier
    .fillMaxWidth()
    .height(60.dp)) {
    val w = size.width
    val h = size.height
    val points = listOf(0.3f, 0.5f, 0.4f, 0.6f, 0.55f, 0.7f, 0.65f, 0.8f)
    val path = Path()

    points.forEachIndexed { i, v ->
        val x = (i / (points.size - 1).toFloat()) * w
        val y = h - (v * h)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            // Bezier cúbico: ponto de controle entre o ponto anterior e o atual
            val prevX = ((i - 1) / (points.size - 1).toFloat()) * w
            val prevY = h - (points[i - 1] * h)
            val cpX = (prevX + x) / 2f
            path.cubicTo(cpX, prevY, cpX, y, x, y)
        }
    }

    // Linha da curva
    drawPath(
        path,
        color = Color.White.copy(alpha = 0.9f),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Pontos nos vértices
    points.forEachIndexed { i, v ->
        val x = (i / (points.size - 1).toFloat()) * w
        val y = h - (v * h)
        drawCircle(
            color = Color.White.copy(alpha = 0.6f),
            radius = 4f,
            center = Offset(x, y)
        )
    }
}
            }
        }

        // Card Km por Dia
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Km por dia esta semana", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    MockData.weeklyKmByDay.forEachIndexed { index, (day, km) ->
                        val maxKm = MockData.weeklyKmByDay.maxOf { it.second }
                        val barHeight = if (maxKm > 0) ((km / maxKm) * 80).dp else 4.dp
                        val isToday = index == 3
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (km > 0) {
                                Text("$km", fontSize = 9.sp, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(if (km > 0) barHeight else 4.dp)
                                    .background(
                                        if (isToday) Color(0xFF3B82F6) else Color(0xFFBFDBFE),
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                day,
                                fontSize = 11.sp,
                                color = if (isToday) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Grid de Métricas 2x2
        val metrics = listOf(
            Triple("⚡", "5:14/km", "Melhor pace"),
            Triple("📍", "10.0 km", "Mais longa"),
            Triple("🏃", "24", "Corridas totais"),
            Triple("🔥", "12.4k", "Calorias")
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (emoji, value, label) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
