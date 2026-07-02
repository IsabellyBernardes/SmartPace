package com.example.smartpace.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.example.smartpace.ui.screens.auth.LoginScreen
import com.example.smartpace.ui.screens.auth.RegisterScreen
import com.example.smartpace.ui.screens.dashboard.DashboardScreen
import com.example.smartpace.ui.screens.friends.FriendProfileScreen
import com.example.smartpace.ui.screens.friends.FriendsScreen
import com.example.smartpace.ui.screens.history.HistoryScreen
import com.example.smartpace.ui.screens.home.HomeScreen
import com.example.smartpace.ui.screens.profile.ProfileScreen
import com.example.smartpace.ui.screens.run.RunScreen
import com.example.smartpace.ui.screens.run.RunDetailScreen
import com.example.smartpace.viewmodel.ProfileViewModel
import com.example.smartpace.viewmodel.RunViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object History : Screen("history")
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
    object Run : Screen("run")
    object Friends : Screen("friends")
    object RunDetail : Screen("run_detail/{runId}") {
        fun createRoute(runId: String) = "run_detail/$runId"
    }
    object FriendProfile : Screen("friend_profile/{uid}") {
        fun createRoute(uid: String) = "friend_profile/$uid"
    }
}

private val mainRoutes = setOf(
    Screen.Home.route,
    Screen.History.route,
    Screen.Dashboard.route,
    Screen.Profile.route,
)

@Composable
fun SmartPaceApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) Screen.Home.route else Screen.Login.route

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainRoutes

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController = navController)
            }
        },
    ) { innerPadding ->
        SmartPaceNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
fun BottomBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                icon = Icons.Default.Home,
                label = "Início",
                selected = currentRoute == Screen.Home.route,
                onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } }
            )
            BottomBarItem(
                icon = Icons.Default.Schedule,
                label = "Histórico",
                selected = currentRoute == Screen.History.route,
                onClick = { navController.navigate(Screen.History.route) { launchSingleTop = true } }
            )
            Box(
                modifier = Modifier.offset(y = (-10).dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .clickable { navController.navigate(Screen.Run.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
                }
            }
            BottomBarItem(
                icon = Icons.Default.BarChart,
                label = "Dashboard",
                selected = currentRoute == Screen.Dashboard.route,
                onClick = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } }
            )
            BottomBarItem(
                icon = Icons.Default.Person,
                label = "Perfil",
                selected = currentRoute == Screen.Profile.route,
                onClick = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
            )
        }
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) Color(0xFF3B82F6) else Color(0xFF94A3B8)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SmartPaceNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier,
) {
    // ViewModels compartilhados no escopo da Activity: garantem que a lista de
    // corridas e o perfil sejam os mesmos em todas as telas, e que a gravação de
    // uma corrida sobreviva à navegação (não é cancelada ao sair da tela de corrida).
    val runViewModel: RunViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController, runViewModel, profileViewModel) }
        composable(Screen.History.route) { HistoryScreen(navController, runViewModel) }
        composable(Screen.Dashboard.route) { DashboardScreen(navController, runViewModel) }
        composable(Screen.Profile.route) {
            ProfileScreen(navController, profileViewModel = profileViewModel, runViewModel = runViewModel)
        }
        composable(Screen.Run.route) { RunScreen(navController, runViewModel) }
        composable(Screen.Friends.route) { FriendsScreen(navController) }
        composable(
            Screen.RunDetail.route,
            arguments = listOf(navArgument("runId") { type = NavType.StringType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getString("runId") ?: ""
            RunDetailScreen(navController, runId, runViewModel)
        }
        composable(
            Screen.FriendProfile.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            FriendProfileScreen(navController, uid)
        }
    }
}
