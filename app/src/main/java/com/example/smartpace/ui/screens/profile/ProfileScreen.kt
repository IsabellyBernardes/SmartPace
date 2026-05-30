package com.example.smartpace.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
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
import com.example.smartpace.model.Achievement
import com.example.smartpace.navigation.Screen
import com.example.smartpace.utils.hasSevenConsecutiveDays
import com.example.smartpace.utils.paceToSeconds
import com.example.smartpace.viewmodel.AuthViewModel
import com.example.smartpace.viewmodel.ProfileViewModel
import com.example.smartpace.viewmodel.RunViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    runViewModel: RunViewModel = viewModel()
) {
    val user by profileViewModel.profile.collectAsState()
    val runs by runViewModel.runs.collectAsState()
    val initials = user.name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    val totalKm = runs.sumOf { it.distance }
    val achievements = listOf(
        Achievement(
            id = "1", title = "Primeira Corrida",
            description = "Completou sua primeira corrida registrada no app",
            emoji = "👟", unlocked = runs.isNotEmpty()
        ),
        Achievement(
            id = "2", title = "5km em 25min",
            description = "Manteve pace abaixo de 5:00/km por 5km seguidos",
            emoji = "⚡", unlocked = runs.any { it.distance >= 5.0 && paceToSeconds(it.pace) <= 300 }
        ),
        Achievement(
            id = "3", title = "7 Dias Seguidos",
            description = "Registrou ao menos uma corrida por 7 dias consecutivos",
            emoji = "🔥", unlocked = hasSevenConsecutiveDays(runs)
        ),
        Achievement(
            id = "4", title = "100km Totais",
            description = "Acumulou 100km em corridas registradas no SmartPace",
            emoji = "🏆", unlocked = totalKm >= 100.0
        )
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF1E40AF), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Corredor desde ${user.memberSince}", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            }
            IconButton(
                onClick = {
                    viewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Sair",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp)
                )
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
                ProfileStatItem(value = user.totalRuns.toString(), label = "CORRIDAS")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                ProfileStatItem(value = "${user.totalKm.toInt()} km", label = "TOTAL")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                ProfileStatItem(value = user.avgPace.ifEmpty { "--:--" }, label = "PACE MÉDIO")
            }
        }

        Text(
            "CONQUISTAS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp, color = Color(0xFF94A3B8)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            achievements.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { achievement ->
                        AchievementCard(achievement = achievement, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Text(
            "CONFIGURAÇÕES", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp, color = Color(0xFF94A3B8)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                SettingsItem(title = "Metas semanais", subtitle = "${user.weeklyGoalKm.toInt()} km / semana")
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItem(title = "Notificações", subtitle = "Ativadas")
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItem(title = "Dispositivo conectado", subtitle = "Nenhum")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ProfileStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
    }
}

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) Color.White else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (achievement.unlocked) 1.dp else 0.dp),
        border = if (!achievement.unlocked) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = achievement.emoji,
                fontSize = 28.sp,
                color = if (achievement.unlocked) Color.Unspecified else Color(0xFFCBD5E1)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (achievement.unlocked) Color(0xFF0F172A) else Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (achievement.unlocked) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (achievement.unlocked) "✓ CONQUISTA" else "BLOQUEADA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.unlocked) Color(0xFF16A34A) else Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        Icon(
            Icons.Default.ChevronRight, contentDescription = null,
            tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp)
        )
    }
}
