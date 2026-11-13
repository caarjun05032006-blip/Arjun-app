package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.ScreenTimeHelper
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PdfReportGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val sleepRecords by viewModel.allSleepRecords.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val assets by viewModel.allAssets.collectAsState()
    val liabilities by viewModel.allLiabilities.collectAsState()
    val reserves by viewModel.allReserves.collectAsState()
    val reserveTransactions by viewModel.allReserveTransactions.collectAsState()
    val trades by viewModel.allTrades.collectAsState()
    val activities by viewModel.allActivities.collectAsState()
    val activitySessions by viewModel.allActivitySessions.collectAsState()
    val plannedTasks by viewModel.allPlannedTasks.collectAsState()
    val screenTimeRecords by viewModel.allScreenTimeRecords.collectAsState()
    val diaryEntries by viewModel.allDiaryEntries.collectAsState()

    var selectedPeriod by remember { mutableStateOf("Monthly") }
    var selectedFormat by remember { mutableStateOf("FORMAT 1: Overall Summary") }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Diary export auth dialog
    var showDiaryAuthDialog by remember { mutableStateOf(false) }
    var diaryAuthPin by remember { mutableStateOf("") }
    var diaryAuthError by remember { mutableStateOf<String?>(null) }

    fun buildAndExportPdf(includeDiary: Boolean = false) {
        val periodLabel = "$selectedPeriod Report (${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())})"
        val reportTypeKey = when {
            includeDiary -> "DIARY_CONFIDENTIAL"
            selectedFormat.contains("FORMAT 1") -> "OVERALL_SUMMARY"
            selectedFormat.contains("FORMAT 2") -> "SECTION_DETAILED"
            else -> "SECTION_SUMMARY"
        }

        val decryptedDiaryList = if (includeDiary) {
            diaryEntries.map { entry ->
                Pair(entry, viewModel.decryptDiaryContent(entry))
            }
        } else emptyList()

        val data = PdfReportGenerator.ReportData(
            periodLabel = periodLabel,
            reportType = reportTypeKey,
            sleepRecords = sleepRecords,
            financialTransactions = transactions,
            assets = assets,
            liabilities = liabilities,
            reserves = reserves,
            reserveTransactions = reserveTransactions,
            trades = trades,
            activities = activities,
            activitySessions = activitySessions,
            plannedTasks = plannedTasks,
            screenTimeRecords = screenTimeRecords,
            diaryEntries = decryptedDiaryList
        )

        PdfReportGenerator.generateAndSharePdf(context, data)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Reports & PDF Export", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Reporting Period Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. SELECT REPORTING PERIOD", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Daily", "Weekly", "Monthly", "Quarterly", "Annual").forEach { p ->
                                FilterChip(
                                    selected = selectedPeriod == p,
                                    onClick = { selectedPeriod = p },
                                    label = { Text(p, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("report_period_$p")
                                )
                            }
                        }
                    }
                }
            }

            // Report Format Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. SELECT REPORT FORMAT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(10.dp))

                        listOf(
                            "FORMAT 1: Overall Summary" to "Combined report covering Sleep, Finance, Trading, Screen Time, Activity, and Planner.",
                            "FORMAT 2: Section-Wise Detailed Report" to "Complete line-item ledger, individual trades, Balance Sheet, P&L schedules, and session logs.",
                            "FORMAT 3: Section-Wise Summary Report" to "High-level totals, averages, comparisons, and key trends across individual modules."
                        ).forEach { (fmt, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    modifier = Modifier.testTag("radio_${fmt.take(8)}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(fmt, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // PDF Export Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Generate Official PDF Document", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("A4 Multi-page, formatted schedules, headers & page numbers", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { buildAndExportPdf(includeDiary = false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_pdf_share")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate & Share PDF Report")
                        }
                    }
                }
            }

            // Gated Confidential Diary Export
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE11D48).copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Confidential Diary Export", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFE11D48))
                                Text("Strictly isolated from main reports. Requires PIN authentication.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                diaryAuthPin = ""
                                diaryAuthError = null
                                showDiaryAuthDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_diary_auth")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE11D48))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticate & Export Personal Diary PDF", color = Color(0xFFE11D48))
                        }
                    }
                }
            }
        }
    }

    if (showDiaryAuthDialog) {
        AlertDialog(
            onDismissRequest = { showDiaryAuthDialog = false },
            title = { Text("Diary Export Authentication") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your diary passcode to verify identity and export confidential entries to PDF.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = diaryAuthPin,
                        onValueChange = { diaryAuthPin = it; diaryAuthError = null },
                        label = { Text("Diary Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_diary_export_pin")
                    )
                    if (diaryAuthError != null) {
                        Text(diaryAuthError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ok = viewModel.unlockDiary(diaryAuthPin)
                    if (ok) {
                        showDiaryAuthDialog = false
                        buildAndExportPdf(includeDiary = true)
                    } else {
                        diaryAuthError = "Incorrect passcode."
                    }
                }) {
                    Text("Verify & Export PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiaryAuthDialog = false }) { Text("Cancel") }
            }
        )
    }
}
