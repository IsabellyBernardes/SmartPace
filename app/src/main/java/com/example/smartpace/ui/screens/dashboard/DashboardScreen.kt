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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.viewmodel.RunViewModel
import java.util.Calendar

@Composable
fun DashboardScreen(navController: NavController, runViewModel: RunViewModel = viewModel()) {
    var selectedPeriod by remember { mutableStateOf("dia") }
    val periods = listOf("dia", "mês")

    val runs by runViewModel.runs.collectAsState()

    val filteredRuns = remember(runs, selectedPeriod) {
        val now = Calendar.getInstance()
        runs.filter { run ->
            val cal = Calendar.getInstance().also { it.timeInMillis = run.timestamp }
            when (selectedPeriod) {
                "dia" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                "mês" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                else -> true
            }
        }
    }

    val totalDistance = filteredRuns.sumOf { it.distance }
    val bestPace = filteredRuns.minByOrNull { run ->
        try {
            val parts = run.pace.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) { Int.MAX_VALUE }
    }?.pace ?: "--:--"
    val totalRuns = filteredRuns.size
    val totalCalories = filteredRuns.sumOf { it.calories }
    val longestRun = filteredRuns.maxOfOrNull { it.distance }

    val dayNames = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val todayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
    val weeklyKmByDay = when (selectedPeriod) {
        "dia" -> dayNames.mapIndexed { index, day ->
            val km = filteredRuns.filter { run ->
                val cal = Calendar.getInstance().also { it.timeInMillis = run.timestamp }
                (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 == index
            }.sumOf { it.distance }
            Pair(day, km)
        }
        "mês" -> {
            val months = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
                "Jul", "Ago", "Set", "Out", "Nov", "Dez")
            val now = Calendar.getInstance()
            months.mapIndexed { index, month ->
                val km = runs.filter { run ->
                    val cal = Calendar.getInstance().also { it.timeInMillis = run.timestamp }
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == index
                }.sumOf { it.distance }
                Pair(month, km)
            }
        }
        else -> dayNames.map { Pair(it, 0.0) }
    }

    val metrics = listOf(
        Triple("⚡", if (bestPace != "--:--") "$bestPace/km" else "--", "Melhor pace"),
        Triple("📍", longestRun?.let { "%.1f km".format(it) } ?: "--", "Mais longa"),
        Triple("🏃", totalRuns.toString(), "Corridas totais"),
        Triple("🔥", if (totalCalories >= 1000) "%.1fk".format(totalCalories / 1000.0) else totalCalories.toString(), "Calorias")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                Text("%.1f km".format(totalDistance), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val points = listOf(0.3f, 0.5f, 0.4f, 0.6f, 0.55f, 0.7f, 0.65f, 0.8f)

                    val linePath = Path()
                    points.forEachIndexed { i, v ->
                        val x = (i / (points.size - 1).toFloat()) * w
                        val y = h - (v * h)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                        } else {
                            val prevX = ((i - 1) / (points.size - 1).toFloat()) * w
                            val prevY = h - (points[i - 1] * h)
                            val cpX = (prevX + x) / 2f
                            linePath.cubicTo(cpX, prevY, cpX, y, x, y)
                        }
                    }

                    val fillPath = Path()
                    fillPath.addPath(linePath)
                    fillPath.lineTo(w, h)
                    fillPath.lineTo(0f, h)
                    fillPath.close()

                    drawPath(
                        fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = size.height
                        )
                    )

                    drawPath(
                        linePath,
                        color = Color.White.copy(alpha = 0.9f),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    points.forEachIndexed { i, v ->
                        val x = (i / (points.size - 1).toFloat()) * w
                        val y = h - (v * h)
                        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 3f, center = Offset(x, y))
                    }

                    val lastX = w
                    val lastY = h - (points.last() * h)
                    drawCircle(color = Color.White, radius = 5f, center = Offset(lastX, lastY))
                    drawCircle(color = Color(0xFF3B82F6), radius = 3f, center = Offset(lastX, lastY))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    when (selectedPeriod) {
                        "dia" -> "Km por dia esta semana"
                        "mês" -> "Km por mês este ano"
                        else -> "Km por período"
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyKmByDay.forEachIndexed { index, (day, km) ->
                        val maxKm = weeklyKmByDay.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
                        val barHeight = ((km / maxKm) * 80).dp
                        val isToday = selectedPeriod == "dia" && index == todayIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (km > 0) {
                                Text("%.1f".format(km), fontSize = 9.sp, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(if (km > 0) barHeight.coerceAtLeast(8.dp) else 4.dp)
                                    .background(
                                        if (isToday) Color(0xFF3B82F6)
                                        else if (km > 0) Color(0xFFBFDBFE)
                                        else Color(0xFFE2E8F0),
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
                                Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
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
