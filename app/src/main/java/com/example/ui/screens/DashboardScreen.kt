package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sleepRecords by viewModel.allSleepRecords.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val assets by viewModel.allAssets.collectAsState()
    val liabilities by viewModel.allLiabilities.collectAsState()
    val trades by viewModel.allTrades.collectAsState()
    val screenTimeRecords by viewModel.allScreenTimeRecords.collectAsState()
    val activitySessions by viewModel.allActivitySessions.collectAsState()
    val plannedTasks by viewModel.allPlannedTasks.collectAsState()

    // Date display
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val displayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

    // Calculations for Today
    val todaySleep = sleepRecords.filter { it.date == todayDateStr }
    val latestSleep = todaySleep.lastOrNull()
    val totalSleepMinutes = todaySleep.sumOf { it.durationMinutes }

    val todayIncome = transactions.filter { it.date == todayDateStr && it.type == "INCOME" }.sumOf { it.amount }
    val todayExpense = transactions.filter { it.date == todayDateStr && it.type == "EXPENSE" }.sumOf { it.amount }

    val totalAssetsValue = assets.sumOf { it.currentTotalValue }
    val totalLiabilitiesValue = liabilities.sumOf { it.outstandingAmount }
    val netWorth = totalAssetsValue - totalLiabilitiesValue

    val todayTrades = trades.filter { it.date == todayDateStr }
    val todayTradingProfit = todayTrades.sumOf { it.netProfitLoss }
    val holdingsValue = assets.filter { it.category == "TRADING_HOLDINGS" }.sumOf { it.currentTotalValue }

    val todayScreenMinutes = screenTimeRecords.filter { it.date == todayDateStr }.sumOf { it.durationMinutes }

    // Activity sessions today
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    val todayActivitySeconds = activitySessions.filter { it.startTime >= startOfDay }.sumOf { it.durationSeconds }

    // Planned tasks today
    val todayTasks = plannedTasks.filter { it.date == todayDateStr }
    val completedTasks = todayTasks.count { it.isCompleted }
    val totalTasksCount = todayTasks.size
    val progressPct = if (totalTasksCount > 0) (completedTasks * 100) / totalTasksCount else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome to OmniLife",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = "OmniLife",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Net Worth Spotlight
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NET WORTH (ASSETS - LIABILITIES)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹%,.2f".format(netWorth),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (netWorth >= 0) ProfitGreen else LossRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Assets: ₹%,.2f".format(totalAssetsValue),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Liab: ₹%,.2f".format(totalLiabilitiesValue),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Today's Metrics Grid
        item {
            Text(
                text = "Today at a Glance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Sleep & Wake",
                    value = if (totalSleepMinutes > 0) "%dh %dm".format(totalSleepMinutes / 60, totalSleepMinutes % 60) else "Not logged",
                    subtitle = if (!latestSleep?.wakeTime.isNullOrBlank()) "Woke at ${latestSleep?.wakeTime}" else "Wake-up pending",
                    icon = Icons.Default.Bedtime,
                    iconTint = IndigoAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("sleep") }
                )
                MetricCard(
                    title = "Cash Flow Today",
                    value = "₹%,.0f".format(todayIncome - todayExpense),
                    subtitle = "In: ₹%,.0f | Out: ₹%,.0f".format(todayIncome, todayExpense),
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = ProfitGreen,
                    isProfit = (todayIncome - todayExpense) >= 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("finance") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Trading P&L",
                    value = "₹%,.2f".format(todayTradingProfit),
                    subtitle = "Holdings: ₹%,.0f".format(holdingsValue),
                    icon = Icons.Default.TrendingUp,
                    iconTint = AccentGold,
                    isProfit = todayTradingProfit >= 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("trading") }
                )
                MetricCard(
                    title = "Screen Time",
                    value = "%dh %dm".format(todayScreenMinutes / 60, todayScreenMinutes % 60),
                    subtitle = "Usage stats active",
                    icon = Icons.Default.PhoneAndroid,
                    iconTint = PurpleAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("screen_time") }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Activity Time",
                    value = "%dh %dm".format(todayActivitySeconds / 3600, (todayActivitySeconds % 3600) / 60),
                    subtitle = "Stopwatch sessions",
                    icon = Icons.Default.Timer,
                    iconTint = TealAccent,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("activity") }
                )
                MetricCard(
                    title = "Daily Plan",
                    value = "$completedTasks / $totalTasksCount",
                    subtitle = "$progressPct% tasks completed",
                    icon = Icons.Default.TaskAlt,
                    iconTint = PrimaryBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("planner") }
                )
            }
        }

        // Quick Navigation Grid
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Life Management Modules",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleButton("Sleep & Wake", Icons.Default.Bedtime, IndigoAccent, Modifier.weight(1f)) { onNavigateTo("sleep") }
                    ModuleButton("Finance & Ledger", Icons.Default.AccountBalance, ProfitGreen, Modifier.weight(1f)) { onNavigateTo("finance") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleButton("Trading Journal", Icons.Default.CandlestickChart, AccentGold, Modifier.weight(1f)) { onNavigateTo("trading") }
                    ModuleButton("Private Diary", Icons.Default.Lock, Color(0xFFE11D48), Modifier.weight(1f)) { onNavigateTo("diary") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleButton("Screen Time", Icons.Default.AppBlocking, PurpleAccent, Modifier.weight(1f)) { onNavigateTo("screen_time") }
                    ModuleButton("Activity Tracker", Icons.Default.Timer, TealAccent, Modifier.weight(1f)) { onNavigateTo("activity") }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleButton("Daily Planner", Icons.Default.EventNote, PrimaryBlue, Modifier.weight(1f)) { onNavigateTo("planner") }
                    ModuleButton("Reports & PDF", Icons.Default.PictureAsPdf, Color(0xFFDC2626), Modifier.weight(1f)) { onNavigateTo("reports") }
                }
            }
        }
    }
}

@Composable
private fun ModuleButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(58.dp)
            .clickable { onClick() }
            .testTag("nav_btn_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}
