package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                var currentScreen by remember { mutableStateOf("dashboard") }
                val screenBackStack = remember { mutableStateListOf("dashboard") }

                fun navigateTo(screen: String) {
                    if (currentScreen != screen) {
                        screenBackStack.add(screen)
                        currentScreen = screen
                    }
                }

                fun navigateBack() {
                    if (screenBackStack.size > 1) {
                        screenBackStack.removeAt(screenBackStack.size - 1)
                        currentScreen = screenBackStack.last()
                    } else if (currentScreen != "dashboard") {
                        currentScreen = "dashboard"
                        screenBackStack.clear()
                        screenBackStack.add("dashboard")
                    }
                }

                BackHandler(enabled = currentScreen != "dashboard") {
                    navigateBack()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentScreen == "dashboard") {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.SelfImprovement,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "OmniLife",
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { navigateTo("diary") },
                                        modifier = Modifier.testTag("topbar_btn_diary")
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = "Private Diary")
                                    }
                                    IconButton(
                                        onClick = { navigateTo("reports") },
                                        modifier = Modifier.testTag("topbar_btn_reports")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Reports")
                                    }
                                    IconButton(
                                        onClick = { navigateTo("settings") },
                                        modifier = Modifier.testTag("topbar_btn_settings")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            val items = listOf(
                                NavigationItem("dashboard", "Home", Icons.Default.Dashboard),
                                NavigationItem("finance", "Finance", Icons.Default.AccountBalance),
                                NavigationItem("trading", "Trading", Icons.Default.CandlestickChart),
                                NavigationItem("activity", "Activity", Icons.Default.Timer),
                                NavigationItem("planner", "Planner", Icons.Default.EventNote)
                            )

                            items.forEach { item ->
                                val isSelected = currentScreen == item.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { navigateTo(item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.testTag("nav_item_${item.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateTo = { screen -> navigateTo(screen) }
                            )
                            "sleep" -> SleepScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "finance" -> FinanceScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "trading" -> TradingScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "diary" -> DiaryScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "screen_time" -> ScreenTimeScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "activity" -> ActivityScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "planner" -> PlannerScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "reports" -> ReportsScreen(
                                viewModel = viewModel,
                                onBack = { navigateBack() }
                            )
                            "settings" -> SettingsScreen(
                                viewModel = viewModel,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = { isDarkTheme = it },
                                onBack = { navigateBack() }
                            )
                            else -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateTo = { screen -> navigateTo(screen) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
