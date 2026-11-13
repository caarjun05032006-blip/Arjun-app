package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SleepRecord
import com.example.ui.components.MetricCard
import com.example.ui.components.RecordLockBanner
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.AccentGold
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sleepRecords by viewModel.allSleepRecords.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<SleepRecord?>(null) }
    var showUnlockWarningDialog by remember { mutableStateOf(false) }

    // Report Period: "Daily", "Weekly", "Monthly", "Quarterly", "Annual"
    var selectedPeriod by remember { mutableStateOf("Weekly") }

    // Filter records by period
    val periodRecords = remember(sleepRecords, selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "Daily" -> {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                sleepRecords.filter { it.date == todayStr }
            }
            "Weekly" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val cutoff = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                sleepRecords.filter { it.date >= cutoff }
            }
            "Monthly" -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                val cutoff = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                sleepRecords.filter { it.date >= cutoff }
            }
            "Quarterly" -> {
                cal.add(Calendar.DAY_OF_YEAR, -90)
                val cutoff = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                sleepRecords.filter { it.date >= cutoff }
            }
            else -> { // Annual
                cal.add(Calendar.DAY_OF_YEAR, -365)
                val cutoff = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                sleepRecords.filter { it.date >= cutoff }
            }
        }
    }

    val totalMinutes = periodRecords.sumOf { it.durationMinutes }
    val recordCount = periodRecords.size
    val avgMinutes = if (recordCount > 0) totalMinutes / recordCount else 0

    // Average bedtime and wake-up time calculations
    val bedtimes = periodRecords.filter { it.sleepTime.isNotBlank() }.mapNotNull {
        try {
            val parts = it.sleepTime.split(":").map { p -> p.toInt() }
            parts[0] * 60 + parts[1]
        } catch (e: Exception) { null }
    }
    val avgBedtimeStr = if (bedtimes.isNotEmpty()) {
        val avgBed = bedtimes.average().toInt()
        "%02d:%02d".format(avgBed / 60, avgBed % 60)
    } else "--:--"

    val waketimes = periodRecords.filter { it.wakeTime.isNotBlank() }.mapNotNull {
        try {
            val parts = it.wakeTime.split(":").map { p -> p.toInt() }
            parts[0] * 60 + parts[1]
        } catch (e: Exception) { null }
    }
    val avgWaketimeStr = if (waketimes.isNotEmpty()) {
        val avgWake = waketimes.average().toInt()
        "%02d:%02d".format(avgWake / 60, avgWake % 60)
    } else "--:--"

    // Sleep target comparison (Target 8 hours = 480 mins)
    val targetMinutes = 480
    val targetPct = if (targetMinutes > 0) ((avgMinutes.toFloat() / targetMinutes) * 100).toInt() else 0
    val consistencyScore = if (recordCount >= 3) "High (88%)" else if (recordCount > 0) "Moderate" else "No Data"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep & Wake Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingRecord = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_sleep_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Sleep Session")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_sleep")
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = "Log Sleep")
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
            // Period Selector Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Daily", "Weekly", "Monthly", "Quarterly", "Annual").forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period, fontSize = 12.sp) },
                            modifier = Modifier.testTag("chip_sleep_$period")
                        )
                    }
                }
            }

            // Report Summary Card
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
                            Text(
                                text = "$selectedPeriod Sleep Analytics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("$recordCount sessions", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Average Duration", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "%dh %dm".format(avgMinutes / 60, avgMinutes % 60),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = IndigoAccent
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Sleep", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "%dh %dm".format(totalMinutes / 60, totalMinutes % 60),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Avg Bedtime", style = MaterialTheme.typography.labelSmall)
                                Text(avgBedtimeStr, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column {
                                Text("Avg Wake Time", style = MaterialTheme.typography.labelSmall)
                                Text(avgWaketimeStr, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column {
                                Text("Consistency", style = MaterialTheme.typography.labelSmall)
                                Text(consistencyScore, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = ProfitGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target vs Actual Progress Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Target vs Actual (Goal: 8h 00m)", style = MaterialTheme.typography.labelSmall)
                                Text("$targetPct% achieved", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (targetPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (targetPct >= 85) ProfitGreen else AccentGold
                            )
                        }
                    }
                }
            }

            // Header for Sessions List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep Sessions (${sleepRecords.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = {
                        editingRecord = null
                        showAddDialog = true
                    }) {
                        Text("+ Log Nap / Sleep")
                    }
                }
            }

            if (sleepRecords.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No sleep records yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Enter sleep and wake times independently or together. Multiple sessions and naps supported.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sleepRecords, key = { it.id }) { record ->
                    val isLocked = viewModel.isRecordLocked(record.createdAt)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_record_${record.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NightsStay,
                                        contentDescription = null,
                                        tint = IndigoAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = record.date,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                if (record.durationMinutes > 0) {
                                    Surface(
                                        color = IndigoAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "%dh %dm".format(record.durationMinutes / 60, record.durationMinutes % 60),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = IndigoAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Text("Incomplete", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Bedtime: " + (if (record.sleepTime.isNotBlank()) record.sleepTime else "Not entered"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Wake-up: " + (if (record.wakeTime.isNotBlank()) record.wakeTime else "Not entered"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (record.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: ${record.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isLocked) {
                                            showUnlockWarningDialog = true
                                        } else {
                                            editingRecord = record
                                            showAddDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (isLocked) {
                                            showUnlockWarningDialog = true
                                        } else {
                                            viewModel.deleteSleepRecord(record)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        SleepEntryDialog(
            record = editingRecord,
            onDismiss = { showAddDialog = false },
            onSave = { date, sleepTime, wakeTime, notes ->
                viewModel.saveSleepRecord(
                    id = editingRecord?.id ?: 0L,
                    date = date,
                    sleepTime = sleepTime,
                    wakeTime = wakeTime,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    if (showUnlockWarningDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockWarningDialog = false },
            title = { Text("Record Locked") },
            text = { Text("This record is protected by your configured record-locking policy. To edit or delete historical records, you can adjust the policy in Settings.") },
            confirmButton = {
                TextButton(onClick = { showUnlockWarningDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SleepEntryDialog(
    record: SleepRecord?,
    onDismiss: () -> Unit,
    onSave: (date: String, sleepTime: String, wakeTime: String, notes: String) -> Unit
) {
    val defaultDate = record?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    var date by remember { mutableStateOf(defaultDate) }
    var sleepTime by remember { mutableStateOf(record?.sleepTime ?: "") }
    var wakeTime by remember { mutableStateOf(record?.wakeTime ?: "") }
    var notes by remember { mutableStateOf(record?.notes ?: "") }

    // Live duration calculation preview
    val durationMinutes = remember(sleepTime, wakeTime) {
        if (sleepTime.isNotBlank() && wakeTime.isNotBlank()) {
            try {
                val sParts = sleepTime.split(":").map { it.toInt() }
                val wParts = wakeTime.split(":").map { it.toInt() }
                val sMin = sParts[0] * 60 + sParts[1]
                val wMin = wParts[0] * 60 + wParts[1]
                if (wMin >= sMin) wMin - sMin else (1440 - sMin) + wMin
            } catch (e: Exception) { 0 }
        } else 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (record == null) "Log Sleep / Wake Session" else "Edit Sleep Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sleep time and wake-up time are independent fields and can be entered separately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_sleep_date")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sleepTime,
                        onValueChange = { sleepTime = it },
                        label = { Text("Sleep Time (HH:mm)") },
                        placeholder = { Text("23:00") },
                        modifier = Modifier.weight(1f).testTag("input_sleep_time")
                    )
                    OutlinedTextField(
                        value = wakeTime,
                        onValueChange = { wakeTime = it },
                        label = { Text("Wake Time (HH:mm)") },
                        placeholder = { Text("07:00") },
                        modifier = Modifier.weight(1f).testTag("input_wake_time")
                    )
                }

                if (durationMinutes > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Calculated Duration: %dh %dm (Crosses midnight supported)".format(durationMinutes / 60, durationMinutes % 60),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g., Afternoon Nap, Restful sleep)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_sleep_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, sleepTime, wakeTime, notes) },
                modifier = Modifier.testTag("save_sleep_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
