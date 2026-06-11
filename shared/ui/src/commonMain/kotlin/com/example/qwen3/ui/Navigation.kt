package com.example.qwen3.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val title: String) {
    data object ChatList : Screen("chat_list", "Chats")
    data object ChatDetail : Screen("chat_detail/{chatId}", "Chat") {
        fun createRoute(chatId: String) = "chat_detail/$chatId"
    }
    data object ModelSelection : Screen("model_selection", "Models")
    data object Settings : Screen("settings", "Settings")
    data object Diagnostics : Screen("diagnostics", "Diagnostics")
    data object Notes : Screen("notes", "Notes")
}

val bottomNavItems = listOf(
    Screen.ChatList,
    Screen.ModelSelection,
    Screen.Notes,
    Screen.Settings,
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.ChatList.route,
            modifier = androidx.compose.foundation.layout.Modifier.padding(paddingValues),
        ) {
            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onChatSelected = { chatId ->
                        navController.navigate(Screen.ChatDetail.createRoute(chatId))
                    },
                    onNavigateToDiagnostics = {
                        navController.navigate(Screen.Diagnostics.route)
                    },
                )
            }
            composable(Screen.ChatDetail.route) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                ChatDetailScreen(
                    chatId = chatId,
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }
            composable(Screen.ModelSelection.route) {
                ModelSelectionScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    },
                )
            }
            composable(Screen.Notes.route) {
                NotesScreen()
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.ChatList -> Icons.Default.Chat
                            Screen.ModelSelection -> Icons.Default.Download
                            Screen.Notes -> Icons.Default.Edit
                            Screen.Settings -> Icons.Default.Settings
                            else -> Icons.Default.Info
                        },
                        contentDescription = screen.title,
                    )
                },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
