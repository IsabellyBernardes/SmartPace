package com.example.smartpace.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.model.Run
import com.example.smartpace.navigation.Screen
import com.example.smartpace.ui.components.RouteThumbnail
import com.example.smartpace.utils.isLastWeek
import com.example.smartpace.utils.isThisWeek
import com.example.smartpace.utils.isToday
import com.example.smartpace.viewmodel.RunViewModel

enum class HistoryFilter(val label: String, val maxAgeDays: Long?) {
    ALL("Todas", null),
    WEEK("Últimos 7 dias", 7),
    MONTH("Últimos 30 dias", 30),
    YEAR("Último ano", 365);

    fun matches(timestamp: Long): Boolean {
        val days = maxAgeDays ?: return true
        val ageMs = System.currentTimeMillis() - timestamp
        return ageMs <= days * 24L * 60L * 60L * 1000L
    }
}

@Composable
fun HistoryScreen(navController: NavController, runViewModel: RunViewModel = viewModel()) {
    val allRuns by runViewModel.runs.collectAsState()

    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var filterMenuOpen by remember { mutableStateOf(false) }

    val runs = allRuns.filter { selectedFilter.matches(it.timestamp) }

    val todayRuns = runs.filter { isToday(it.timestamp) }
    val thisWeekRuns = runs.filter { isThisWeek(it.timestamp) && !isToday(it.timestamp) }
    val lastWeekRuns = runs.filter { isLastWeek(it.timestamp) }
    val olderRuns = runs.filter { !isThisWeek(it.timestamp) && !isLastWeek(it.timestamp) }

    val totalKm = runs.sumOf { it.distance }
    val totalCalories = runs.sumOf { it.calories }

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
            Text("Histórico", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Box {
                OutlinedButton(
                    onClick = { filterMenuOpen = true },
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedFilter == HistoryFilter.ALL) "Filtrar" else selectedFilter.label,
                        fontSize = 13.sp, color = Color(0xFF64748B)
                    )
                }
                DropdownMenu(
                    expanded = filterMenuOpen,
                    onDismissRequest = { filterMenuOpen = false }
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = {
                                selectedFilter = filter
                                filterMenuOpen = false
                            },
                            trailingIcon = if (filter == selectedFilter) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3B82F6)) }
                            } else null
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistorySummaryItem(runs.size.toString(), "CORRIDAS")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                HistorySummaryItem("%.1f km".format(totalKm), "TOTAL")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                HistorySummaryItem(totalCalories.toString(), "CALORIAS")
            }
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
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterActive = selectedFilter != HistoryFilter.ALL
                    Text(if (filterActive) "🔍" else "📋", fontSize = 40.sp)
                    Text(
                        if (filterActive) "Nenhuma corrida no período" else "Sem corridas registradas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        if (filterActive) "Tente selecionar outro período no filtro"
                        else "Suas corridas aparecerão aqui após a primeira atividade",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            if (todayRuns.isNotEmpty()) RunGroup(label = "HOJE", runs = todayRuns, navController = navController)
            if (thisWeekRuns.isNotEmpty()) RunGroup(label = "ESTA SEMANA", runs = thisWeekRuns, navController = navController)
            if (lastWeekRuns.isNotEmpty()) RunGroup(label = "SEMANA PASSADA", runs = lastWeekRuns, navController = navController)
            if (olderRuns.isNotEmpty()) RunGroup(label = "ANTERIORES", runs = olderRuns, navController = navController)
        }

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
fun RunGroup(label: String, runs: List<Run>, navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        runs.forEach { run ->
            HistoryRunCard(
                run = run,
                onClick = {
                    if (run.id.isNotEmpty()) {
                        navController.navigate(Screen.RunDetail.createRoute(run.id))
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryRunCard(run: Run, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            RouteThumbnail(
                points = run.routePoints,
                modifier = Modifier.size(48.dp)
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
