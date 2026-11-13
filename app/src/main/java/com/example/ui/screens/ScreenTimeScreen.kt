package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScreenTimeRecord
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.util.ScreenTimeHelper
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val records by viewModel.allScreenTimeRecords.collectAsState()
    val hasPermission by viewModel.hasUsagePermission.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showManualDialog by remember { mutableStateOf(false) }
    var targetMinutes by remember { mutableStateOf(240L) } // 4 hours daily target

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayRecords = records.filter { it.date == todayStr }
    val totalMinutes = todayRecords.sumOf { it.durationMinutes }

    val categoryBreakdown = remember(todayRecords) {
        todayRecords.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
    }

    val targetPct = if (targetMinutes > 0) ((totalMinutes.toFloat() / targetMinutes) * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Time Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshScreenTimeForDate(todayStr) },
                        modifier = Modifier.testTag("btn_refresh_screentime")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Usage")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualDialog = true },
                containerColor = PurpleAccent,
                modifier = Modifier.testTag("fab_add_screentime")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Manual Entry")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            // Permission Banner if not granted
            if (!hasPermission) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Android Usage Access Required", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "To automatically monitor application screen time and categorise social, productivity, and entertainment usage, grant Usage Access in Android Settings. Manual entries are also supported.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    com.example.util.ScreenTimeHelper.openUsageAccessSettings(context)
                                },
                                modifier = Modifier.testTag("btn_grant_usage_access")
                            ) {
                                Text("Open Usage Access Settings")
                            }
                        }
                    }
                }
            }

            // Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Today's Total Screen Time", style = MaterialTheme.typography.labelSmall)
                            Badge(containerColor = PurpleAccent) { Text("Target: %dh".format(targetMinutes / 60)) }
                        }
                        Text(
                            "%dh %dm".format(totalMinutes / 60, totalMinutes % 60),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = PurpleAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Usage ($targetPct% used)", style = MaterialTheme.typography.labelSmall)
                            Text(if (totalMinutes <= targetMinutes) "Within Limit" else "Exceeded Target", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (totalMinutes <= targetMinutes) ProfitGreen else LossRed)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (targetPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (totalMinutes <= targetMinutes) PurpleAccent else LossRed
                        )
                    }
                }
            }

            // Category Breakdown Grid
            item {
                Text("Category Usage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val social = categoryBreakdown["Social"] ?: 0L
                    val prod = categoryBreakdown["Productivity"] ?: 0L
                    MetricCard(
                        title = "Social Media",
                        value = "%dh %dm".format(social / 60, social % 60),
                        icon = Icons.Default.Chat,
                        iconTint = IndigoAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Productivity",
                        value = "%dh %dm".format(prod / 60, prod % 60),
                        icon = Icons.Default.Work,
                        iconTint = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ent = categoryBreakdown["Entertainment"] ?: 0L
                    val edu = categoryBreakdown["Education"] ?: 0L
                    MetricCard(
                        title = "Entertainment",
                        value = "%dh %dm".format(ent / 60, ent % 60),
                        icon = Icons.Default.Movie,
                        iconTint = AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Education",
                        value = "%dh %dm".format(edu / 60, edu % 60),
                        icon = Icons.Default.School,
                        iconTint = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // App Breakdown List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Usage Breakdown (${todayRecords.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    TextButton(onClick = { showManualDialog = true }) {
                        Text("+ Manual Entry")
                    }
                }
            }

            if (todayRecords.isEmpty()) {
                item {
                    Text("No usage recorded for today. Tap Sync or add manual entry.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(todayRecords, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rec.appName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge { Text(rec.category, fontSize = 9.sp) }
                                    if (rec.isManual) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.outline) { Text("Manual", fontSize = 8.sp) }
                                    }
                                }
                                Text(rec.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "%dh %dm".format(rec.durationMinutes / 60, rec.durationMinutes % 60),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PurpleAccent
                                )
                                IconButton(onClick = { viewModel.deleteScreenTimeRecord(rec) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        AddManualScreenTimeDialog(
            onDismiss = { showManualDialog = false },
            onSave = { date, appName, cat, durationMins ->
                viewModel.addManualScreenTime(date, appName, cat, durationMins)
                showManualDialog = false
            }
        )
    }
}

@Composable
fun AddManualScreenTimeDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, appName: String, category: String, durationMinutes: Long) -> Unit
) {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var appName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Productivity") }
    var minutesStr by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log App Screen Time Manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name (e.g. Chrome, WhatsApp, Kindle)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_manual_app_name")
                )
                Text("Category:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Social", "Entertainment", "Productivity", "Education", "Other").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = minutesStr,
                    onValueChange = { minutesStr = it },
                    label = { Text("Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_manual_duration")
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val mins = minutesStr.toLongOrNull() ?: 0L
                if (appName.isNotBlank() && mins > 0) {
                    onSave(date, appName, category, mins)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
