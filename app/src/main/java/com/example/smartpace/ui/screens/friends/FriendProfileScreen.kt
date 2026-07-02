package com.example.smartpace.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.model.UserProfile
import com.example.smartpace.viewmodel.FriendViewModel

@Composable
fun FriendProfileScreen(
    navController: NavController,
    friendUid: String,
    friendViewModel: FriendViewModel = viewModel()
) {
    val profile by friendViewModel.viewedProfile.collectAsState()

    LaunchedEffect(friendUid) {
        friendViewModel.loadProfile(friendUid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text("Perfil", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        val p = profile
        if (p == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else {
            FriendProfileContent(p)
        }
    }
}

@Composable
private fun FriendProfileContent(profile: UserProfile) {
    val initials = profile.name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("").ifEmpty { "?" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF1E40AF), Color(0xFF0F172A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                profile.name.ifEmpty { "Corredor" },
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)
            )
            if (profile.username.isNotEmpty()) {
                Text("@${profile.username}", fontSize = 14.sp, color = Color(0xFF94A3B8))
            }
            if (profile.memberSince.isNotEmpty()) {
                Text("Corredor desde ${profile.memberSince}", fontSize = 13.sp, color = Color(0xFF94A3B8))
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
                FriendStat(profile.totalRuns.toString(), "CORRIDAS")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                FriendStat("${profile.totalKm.toInt()} km", "TOTAL")
                VerticalDivider(modifier = Modifier.height(36.dp), color = Color.White.copy(alpha = 0.3f))
                FriendStat(profile.avgPace.ifEmpty { "--:--" }, "PACE MÉDIO")
            }
        }

        Text(
            "As corridas de ${profile.name.split(" ").firstOrNull() ?: "seu amigo"} são privadas.",
            fontSize = 12.sp, color = Color(0xFF94A3B8),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FriendStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
    }
}
