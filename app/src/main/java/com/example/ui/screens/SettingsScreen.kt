package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recordLockDays by viewModel.recordLockDays.collectAsState()
    val selectedFY by viewModel.selectedFinancialYear.collectAsState()

    var showLockDialog by remember { mutableStateOf(false) }
    var showFyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings & Policies", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Theme Setting
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Dark Theme", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("Switch between light and dark modes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme(it) },
                            modifier = Modifier.testTag("toggle_dark_theme")
                        )
                    }
                }
            }

            // Record Locking Policy
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLockDialog = true }
                        .testTag("setting_record_lock"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockClock, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Record-Locking Policy", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                val lockLabel = when (recordLockDays) {
                                    0 -> "Disabled (Never Lock)"
                                    1 -> "Lock after 24 Hours"
                                    7 -> "Lock after 7 Days"
                                    30 -> "Lock after 30 Days"
                                    else -> "Lock after $recordLockDays Days"
                                }
                                Text("Current: $lockLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Financial Year Setting
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFyDialog = true }
                        .testTag("setting_financial_year"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Default Financial Year", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                Text("April to March cycle: $selectedFY", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // About Application Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("OMNILIFE | ARCHITECTURE & DATA GOVERNANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "• Double-Entry Balance Sheet: Total Assets = Total Liabilities + Net Capital.\n" +
                            "• Manual CMV Valuations: Current market value is strictly entered by user, never fetched externally.\n" +
                            "• Private Diary: AES-256-GCM hardware-backed cryptography with SHA-256 passcode verification.\n" +
                            "• Trading Integration: Shared ledger connecting trades to cash & P&L accounts with intraday short-selling support.\n" +
                            "• Activity Tracker: Empty by default; only user-created activities tracked.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text("Configure Record-Locking Policy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Configure how long records remain freely editable before being locked to preserve historical audit trails and prevent accidental modifications.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    listOf(
                        0 to "Disabled (Never Lock)",
                        1 to "Lock after 24 Hours",
                        7 to "Lock after 7 Days (Recommended)",
                        30 to "Lock after 30 Days"
                    ).forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setRecordLockDays(days)
                                    showLockDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = recordLockDays == days,
                                onClick = {
                                    viewModel.setRecordLockDays(days)
                                    showLockDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockDialog = false }) { Text("Close") }
            }
        )
    }

    if (showFyDialog) {
        val fyList = listOf("FY 2024-25", "FY 2025-26", "FY 2026-27", "FY 2027-28")
        AlertDialog(
            onDismissRequest = { showFyDialog = false },
            title = { Text("Select Financial Year") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fyList.forEach { fy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedFinancialYear(fy)
                                    showFyDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedFY == fy, onClick = {
                                viewModel.setSelectedFinancialYear(fy)
                                showFyDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fy, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFyDialog = false }) { Text("Close") } }
        )
    }
}
