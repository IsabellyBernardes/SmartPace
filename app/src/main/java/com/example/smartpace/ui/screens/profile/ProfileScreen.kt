package com.example.smartpace.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.model.Achievement
import com.example.smartpace.navigation.Screen
import com.example.smartpace.utils.hasSevenConsecutiveDays
import com.example.smartpace.utils.paceToSeconds
import com.example.smartpace.utils.parseDurationToSeconds
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
    val totalRuns = runs.size
    val avgPace = if (totalKm > 0) {
        val totalDurationSec = runs.sumOf { parseDurationToSeconds(it.duration).toLong() }
        val paceSec = (totalDurationSec / totalKm).toInt()
        "%d:%02d".format(paceSec / 60, paceSec % 60)
    } else "--:--"
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

    var showWeightDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    if (showWeightDialog) {
        WeightDialog(
            currentWeight = user.weightKg,
            onDismiss = { showWeightDialog = false },
            onConfirm = { newWeight ->
                profileViewModel.updateWeight(newWeight)
                showWeightDialog = false
            }
        )
    }

    if (showUsernameDialog) {
        UsernameDialog(
            currentUsername = user.username,
            onDismiss = { showUsernameDialog = false },
            onConfirm = { newUsername, onResult ->
                profileViewModel.updateUsername(newUsername, onResult)
            }
        )
    }

    if (showGoalDialog) {
        WeeklyGoalDialog(
            currentGoal = user.weeklyGoalKm,
            onDismiss = { showGoalDialog = false },
            onConfirm = { newGoal ->
                profileViewModel.updateWeeklyGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

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
                    Text(
                        if (user.username.isNotEmpty()) "@${user.username}"
                        else "Corredor desde ${user.memberSince}",
                        fontSize = 13.sp, color = Color(0xFF94A3B8)
                    )
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
                ProfileStatItem(value = totalRuns.toString(), label = "CORRIDAS")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                ProfileStatItem(value = "${totalKm.toInt()} km", label = "TOTAL")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                ProfileStatItem(value = avgPace, label = "PACE MÉDIO")
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
                SettingsItem(
                    title = "Amigos",
                    subtitle = "Buscar e gerenciar amigos",
                    onClick = { navController.navigate(Screen.Friends.route) }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItem(
                    title = "Nome de usuário",
                    subtitle = if (user.username.isNotEmpty()) "@${user.username}" else "Definir username",
                    onClick = { showUsernameDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItem(
                    title = "Peso",
                    subtitle = "%.1f kg".format(user.weightKg),
                    onClick = { showWeightDialog = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItem(
                    title = "Metas semanais",
                    subtitle = "${user.weeklyGoalKm.toInt()} km / semana",
                    onClick = { showGoalDialog = true }
                )
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
fun SettingsItem(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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

@Composable
fun WeightDialog(
    currentWeight: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(currentWeight.toString()) }
    val parsed = text.replace(",", ".").toDoubleOrNull()
    val isValid = parsed != null && parsed in 20.0..300.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seu peso", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Usado para estimar as calorias das suas corridas.",
                    fontSize = 13.sp, color = Color(0xFF64748B)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = text.isNotEmpty() && !isValid,
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun WeeklyGoalDialog(
    currentGoal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(currentGoal.toInt().toString()) }
    val parsed = text.replace(",", ".").toDoubleOrNull()
    val isValid = parsed != null && parsed in 1.0..500.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meta semanal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Quantos quilômetros você quer correr por semana?",
                    fontSize = 13.sp, color = Color(0xFF64748B)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = text.isNotEmpty() && !isValid,
                    suffix = { Text("km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun UsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onConfirm: (String, (Boolean) -> Unit) -> Unit
) {
    var text by remember { mutableStateOf(currentUsername) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // username válido: 3-20 chars, apenas letras, números, ponto e underscore
    val normalized = text.lowercase().trim()
    val isValid = normalized.matches(Regex("^[a-z0-9._]{3,20}$"))

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Nome de usuário", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Como seus amigos vão te encontrar. 3 a 20 caracteres: letras, números, ponto ou _.",
                    fontSize = 13.sp, color = Color(0xFF64748B)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = true,
                    enabled = !saving,
                    isError = error != null || (text.isNotEmpty() && !isValid),
                    prefix = { Text("@") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, fontSize = 12.sp, color = Color(0xFFEF4444)) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saving = true
                    error = null
                    onConfirm(normalized) { ok ->
                        saving = false
                        if (ok) onDismiss() else error = "Este username já está em uso."
                    }
                },
                enabled = isValid && !saving && normalized != currentUsername,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) { Text(if (saving) "Salvando..." else "Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}
