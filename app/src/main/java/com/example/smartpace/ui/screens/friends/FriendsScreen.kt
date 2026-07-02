package com.example.smartpace.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.smartpace.model.Friend
import com.example.smartpace.model.FriendRequest
import com.example.smartpace.model.UserProfile
import com.example.smartpace.navigation.Screen
import com.example.smartpace.viewmodel.FriendViewModel
import com.example.smartpace.viewmodel.SearchState

@Composable
fun FriendsScreen(
    navController: NavController,
    friendViewModel: FriendViewModel = viewModel()
) {
    val searchState by friendViewModel.searchState.collectAsState()
    val requests by friendViewModel.requests.collectAsState()
    val friends by friendViewModel.friends.collectAsState()
    val requestSent by friendViewModel.requestSent.collectAsState()

    var query by remember { mutableStateOf("") }

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
            Text("Amigos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Busca por username
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    friendViewModel.clearSearch()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por @username", color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                trailingIcon = {
                    TextButton(onClick = { friendViewModel.search(query) }) {
                        Text("Buscar", color = Color(0xFF3B82F6))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            when (val state = searchState) {
                is SearchState.Searching -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFF3B82F6))
                    }
                }
                is SearchState.NotFound -> {
                    Text("Nenhum usuário com esse username.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
                is SearchState.Found -> {
                    SearchResultCard(
                        profile = state.profile,
                        requestSent = requestSent,
                        onAdd = { friendViewModel.sendRequest(state.profile.id) }
                    )
                }
                is SearchState.Idle -> {}
            }

            // Solicitações recebidas
            if (requests.isNotEmpty()) {
                SectionTitle("SOLICITAÇÕES (${requests.size})")
                requests.forEach { req ->
                    RequestCard(
                        request = req,
                        onAccept = { friendViewModel.accept(req) },
                        onReject = { friendViewModel.reject(req) }
                    )
                }
            }

            // Lista de amigos
            SectionTitle("MEUS AMIGOS (${friends.size})")
            if (friends.isEmpty()) {
                Text(
                    "Você ainda não tem amigos. Busque por username para adicionar.",
                    fontSize = 13.sp, color = Color(0xFF94A3B8)
                )
            } else {
                friends.forEach { friend ->
                    FriendCard(
                        friend = friend,
                        onClick = {
                            navController.navigate(Screen.FriendProfile.createRoute(friend.uid))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Color(0xFF94A3B8))
}

@Composable
private fun Avatar(name: String, size: Int = 42) {
    val initials = name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("").ifEmpty { "?" }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF1E40AF), Color(0xFF0F172A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Color.White, fontSize = (size / 3).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchResultCard(profile: UserProfile, requestSent: Boolean, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(profile.name)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name.ifEmpty { "Corredor" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("@${profile.username}", fontSize = 13.sp, color = Color(0xFF94A3B8))
            }
            if (requestSent) {
                Text("Enviado ✓", fontSize = 13.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
            } else {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("Adicionar", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun RequestCard(request: FriendRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(request.fromName)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.fromName.ifEmpty { "Corredor" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("@${request.fromUsername}", fontSize = 13.sp, color = Color(0xFF94A3B8))
            }
            IconButton(onClick = onAccept) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Check, contentDescription = "Aceitar", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
            IconButton(onClick = onReject) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, contentDescription = "Recusar", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Friend, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(friend.name, size = 38)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(friend.name.ifEmpty { "Corredor" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                Text("@${friend.username}", fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
