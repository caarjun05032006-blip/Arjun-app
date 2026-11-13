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
import com.example.data.model.*
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Dashboard", "P&L / Cashflow", "Balance Sheet", "Assets & CMV", "Liabilities", "Reserves")

    val transactions by viewModel.allTransactions.collectAsState()
    val assets by viewModel.allAssets.collectAsState()
    val liabilities by viewModel.allLiabilities.collectAsState()
    val reserves by viewModel.allReserves.collectAsState()
    val reserveTransactions by viewModel.allReserveTransactions.collectAsState()
    val selectedFY by viewModel.selectedFinancialYear.collectAsState()

    // Dialog states
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var showAddLiabDialog by remember { mutableStateOf(false) }
    var showAddReserveDialog by remember { mutableStateOf(false) }
    var showTransferReserveDialog by remember { mutableStateOf(false) }
    var editingAssetCmv by remember { mutableStateOf<AssetAccount?>(null) }

    // Aggregate Calculations
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netProfitLoss = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netProfitLoss / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    val totalAssetsValue = assets.sumOf { it.currentTotalValue }
    val totalLiabilitiesValue = liabilities.sumOf { it.outstandingAmount }
    val netWorth = totalAssetsValue - totalLiabilitiesValue

    val totalReservesValue = reserves.sumOf { it.currentBalance }
    // Double-entry Capital: Opening Capital + Net Profit - Reserves Allocation = Free Capital
    // Total Equity/Capital = Net Worth = Assets - Liabilities
    val capitalEquity = netWorth

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Management & Statements", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick Financial Year selector
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = selectedFY,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("finance_tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> FinanceOverviewTab(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netWorth = netWorth,
                    totalAssets = totalAssetsValue,
                    totalLiabilities = totalLiabilitiesValue,
                    savingsRate = savingsRate,
                    assets = assets,
                    liabilities = liabilities,
                    onAddTransaction = { showAddTxDialog = true }
                )
                1 -> ProfitLossTab(
                    transactions = transactions,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    netProfitLoss = netProfitLoss,
                    onAddTransaction = { showAddTxDialog = true },
                    onDeleteTransaction = { viewModel.deleteFinancialTransaction(it) }
                )
                2 -> BalanceSheetTab(
                    assets = assets,
                    liabilities = liabilities,
                    reserves = reserves,
                    totalAssets = totalAssetsValue,
                    totalLiabilities = totalLiabilitiesValue,
                    netWorth = netWorth,
                    selectedFY = selectedFY,
                    onUpdateCmv = { editingAssetCmv = it }
                )
                3 -> AssetsTab(
                    assets = assets,
                    totalAssets = totalAssetsValue,
                    onAddAsset = { showAddAssetDialog = true },
                    onEditCmv = { editingAssetCmv = it },
                    onDeleteAsset = { viewModel.deleteAsset(it) }
                )
                4 -> LiabilitiesTab(
                    liabilities = liabilities,
                    totalLiabilities = totalLiabilitiesValue,
                    onAddLiability = { showAddLiabDialog = true },
                    onDeleteLiability = { viewModel.deleteLiability(it) }
                )
                5 -> ReservesTab(
                    reserves = reserves,
                    reserveTransactions = reserveTransactions,
                    totalReserves = totalReservesValue,
                    onCreateReserve = { showAddReserveDialog = true },
                    onTransfer = { showTransferReserveDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showAddTxDialog) {
        AddFinancialTxDialog(
            assets = assets.filter { it.category in listOf("CASH", "BANK") },
            onDismiss = { showAddTxDialog = false },
            onSave = { date, type, cat, sub, amt, method, source, desc, recur, linkedAssetId ->
                viewModel.addFinancialTransaction(
                    date = date,
                    type = type,
                    category = cat,
                    subcategory = sub,
                    amount = amt,
                    paymentMethod = method,
                    sourceOrPayee = source,
                    description = desc,
                    isRecurring = recur,
                    linkedAssetId = linkedAssetId
                )
                showAddTxDialog = false
            }
        )
    }

    if (showAddAssetDialog) {
        AddAssetDialog(
            onDismiss = { showAddAssetDialog = false },
            onSave = { name, cat, qty, price, cmv, pDate, vDate, desc, balance ->
                viewModel.saveAssetAccount(
                    name = name,
                    category = cat,
                    quantity = qty,
                    purchasePrice = price,
                    cmv = cmv,
                    purchaseDate = pDate,
                    valuationDate = vDate,
                    description = desc,
                    balance = balance
                )
                showAddAssetDialog = false
            }
        )
    }

    if (editingAssetCmv != null) {
        EditCmvDialog(
            asset = editingAssetCmv!!,
            onDismiss = { editingAssetCmv = null },
            onSave = { newCmv, valDate ->
                viewModel.updateAssetCmv(editingAssetCmv!!, newCmv, valDate)
                editingAssetCmv = null
            }
        )
    }

    if (showAddLiabDialog) {
        AddLiabilityDialog(
            onDismiss = { showAddLiabDialog = false },
            onSave = { name, cat, amt, rate, due, repay, desc ->
                viewModel.saveLiability(
                    name = name,
                    category = cat,
                    outstandingAmount = amt,
                    interestRate = rate,
                    dueDate = due,
                    repaymentDetails = repay,
                    description = desc
                )
                showAddLiabDialog = false
            }
        )
    }

    if (showAddReserveDialog) {
        CreateReserveDialog(
            onDismiss = { showAddReserveDialog = false },
            onSave = { name, openBal, desc ->
                viewModel.createReserveAccount(name, openBal, desc)
                showAddReserveDialog = false
            }
        )
    }

    if (showTransferReserveDialog) {
        ReserveTransferDialog(
            reserves = reserves,
            onDismiss = { showTransferReserveDialog = false },
            onSave = { resId, date, amt, type, desc ->
                viewModel.addReserveTransaction(resId, date, amt, type, desc)
                showTransferReserveDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: OVERVIEW & CASHFLOW
// -------------------------------------------------------------
@Composable
fun FinanceOverviewTab(
    totalIncome: Double,
    totalExpense: Double,
    netWorth: Double,
    totalAssets: Double,
    totalLiabilities: Double,
    savingsRate: Double,
    assets: List<AssetAccount>,
    liabilities: List<LiabilityAccount>,
    onAddTransaction: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("NET WORTH (TOTAL ASSETS - TOTAL LIABILITIES)", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "₹%,.2f".format(netWorth),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (netWorth >= 0) ProfitGreen else LossRed
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Assets: ₹%,.2f".format(totalAssets), style = MaterialTheme.typography.bodySmall)
                        Text("Total Liabilities: ₹%,.2f".format(totalLiabilities), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Total Income",
                    value = "₹%,.2f".format(totalIncome),
                    subtitle = "Realised trading included",
                    icon = Icons.Default.ArrowDownward,
                    iconTint = ProfitGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Expenses",
                    value = "₹%,.2f".format(totalExpense),
                    subtitle = "Charges & losses included",
                    icon = Icons.Default.ArrowUpward,
                    iconTint = LossRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Net Cash Flow",
                    value = "₹%,.2f".format(totalIncome - totalExpense),
                    isProfit = (totalIncome - totalExpense) >= 0,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Savings Rate",
                    value = "%.1f%%".format(savingsRate),
                    subtitle = if (savingsRate >= 20) "Healthy" else "Caution",
                    isProfit = savingsRate >= 20,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Button(
                onClick = onAddTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_log_income_expense")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Income / Expense Entry")
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: PROFIT & LOSS ACCOUNT
// -------------------------------------------------------------
@Composable
fun ProfitLossTab(
    transactions: List<FinancialTransaction>,
    totalIncome: Double,
    totalExpense: Double,
    netProfitLoss: Double,
    onAddTransaction: () -> Unit,
    onDeleteTransaction: (FinancialTransaction) -> Unit
) {
    val incomeList = transactions.filter { it.type == "INCOME" }
    val expenseList = transactions.filter { it.type == "EXPENSE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PROFIT & LOSS STATEMENT", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Button(onClick = onAddTransaction, modifier = Modifier.testTag("pnl_add_entry")) {
                            Text("+ Entry")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Total Revenue / Income: ₹%,.2f".format(totalIncome), style = MaterialTheme.typography.bodyMedium, color = ProfitGreen)
                    Text("Total Expenses & Trading Charges: ₹%,.2f".format(totalExpense), style = MaterialTheme.typography.bodyMedium, color = LossRed)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Net Profit / Loss: ₹%,.2f".format(netProfitLoss),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (netProfitLoss >= 0) ProfitGreen else LossRed
                    )
                    Text(
                        "Note: Unrealised gains from manual CMV updates are not treated as realised income.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text("Ledger Transactions (${transactions.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        items(transactions, key = { it.id }) { tx ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tx.category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (tx.linkedTradeId != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = AccentGold) { Text("Linked Trade", fontSize = 10.sp) }
                            }
                        }
                        Text(
                            text = "${tx.date}  •  ${tx.paymentMethod}  •  ${tx.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = (if (tx.type == "INCOME") "+ " else "- ") + "₹%,.2f".format(tx.amount),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (tx.type == "INCOME") ProfitGreen else LossRed
                        )
                        IconButton(onClick = { onDeleteTransaction(tx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: BALANCE SHEET & ACCOUNTING EQUATION
// -------------------------------------------------------------
@Composable
fun BalanceSheetTab(
    assets: List<AssetAccount>,
    liabilities: List<LiabilityAccount>,
    reserves: List<ReserveAccount>,
    totalAssets: Double,
    totalLiabilities: Double,
    netWorth: Double,
    selectedFY: String,
    onUpdateCmv: (AssetAccount) -> Unit
) {
    val totalReserves = reserves.sumOf { it.currentBalance }
    val freeCapital = netWorth - totalReserves

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Equation verification banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BALANCE SHEET EQUATION ($selectedFY)", style = MaterialTheme.typography.labelSmall)
                    Text("Total Assets = Total Liabilities + Net Capital / Equity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = ProfitGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "₹%,.2f  =  ₹%,.2f  +  ₹%,.2f  (Balanced)".format(totalAssets, totalLiabilities, netWorth),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ProfitGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Left Side: ASSETS SCHEDULE
        item {
            Text("ASSETS SCHEDULE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
        }

        items(assets, key = { it.id }) { asset ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(asset.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge { Text(asset.category, fontSize = 9.sp) }
                        }
                        if (asset.category == "CASH" || asset.category == "BANK") {
                            Text(
                                text = "Current Balance (Negative allowed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Qty: ${asset.quantity}  •  Cost: ₹${asset.purchasePrice}  •  CMV: ₹${asset.cmv}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (asset.valuationDate.isNotBlank()) {
                                Text(
                                    text = "Valuation Date: ${asset.valuationDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val valColor = if (asset.currentTotalValue < 0) LossRed else MaterialTheme.colorScheme.onSurface
                        Text(
                            text = "₹%,.2f".format(asset.currentTotalValue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = valColor
                        )
                        if (asset.category !in listOf("CASH", "BANK")) {
                            TextButton(onClick = { onUpdateCmv(asset) }, modifier = Modifier.height(28.dp)) {
                                Text("Update CMV", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Right Side: LIABILITIES & CAPITAL SCHEDULE
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("LIABILITIES & CAPITAL SCHEDULE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AccentGold)
        }

        // Liabilities List
        items(liabilities, key = { it.id }) { liab ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(liab.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "${liab.category}  •  Due: ${if (liab.dueDate.isNotBlank()) liab.dueDate else "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₹%,.2f".format(liab.outstandingAmount),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = LossRed
                    )
                }
            }
        }

        // Capital & Reserves Breakup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CAPITAL & RESERVES BREAKUP", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Unallocated Capital / Net Worth", style = MaterialTheme.typography.bodySmall)
                        Text("₹%,.2f".format(freeCapital), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• Reserves & Surplus", style = MaterialTheme.typography.bodySmall)
                        Text("₹%,.2f".format(totalReserves), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = IndigoAccent)
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL CAPITAL & RESERVES", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("₹%,.2f".format(netWorth), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: ASSETS WITH MANUAL CMV & NEGATIVE BALANCES
// -------------------------------------------------------------
@Composable
fun AssetsTab(
    assets: List<AssetAccount>,
    totalAssets: Double,
    onAddAsset: () -> Unit,
    onEditCmv: (AssetAccount) -> Unit,
    onDeleteAsset: (AssetAccount) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL ASSETS", style = MaterialTheme.typography.labelSmall)
                        Text("₹%,.2f".format(totalAssets), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Negative balances permitted in Cash & Bank", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = onAddAsset, modifier = Modifier.testTag("btn_add_asset")) {
                        Text("+ Asset")
                    }
                }
            }
        }

        items(assets, key = { it.id }) { asset ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(asset.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge { Text(asset.category, fontSize = 9.sp) }
                            }
                            if (asset.description.isNotBlank()) {
                                Text(asset.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        val isNegative = asset.currentTotalValue < 0
                        Text(
                            text = (if (isNegative) "- ₹%,.2f (Overdrawn)".format(-asset.currentTotalValue) else "₹%,.2f".format(asset.currentTotalValue)),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isNegative) LossRed else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (asset.category !in listOf("CASH", "BANK")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cost: ₹%,.2f".format(asset.totalCost), style = MaterialTheme.typography.bodySmall)
                            Text("Manual CMV: ₹%,.2f".format(asset.cmv), style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                            val unrealised = asset.unrealisedProfitLoss
                            Text(
                                "Unrealised: " + (if (unrealised >= 0) "+ " else "") + "₹%,.2f".format(unrealised),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (unrealised >= 0) ProfitGreen else LossRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (asset.category !in listOf("CASH", "BANK")) {
                            TextButton(onClick = { onEditCmv(asset) }) {
                                Text("Edit CMV")
                            }
                        }
                        IconButton(onClick = { onDeleteAsset(asset) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: LIABILITIES
// -------------------------------------------------------------
@Composable
fun LiabilitiesTab(
    liabilities: List<LiabilityAccount>,
    totalLiabilities: Double,
    onAddLiability: () -> Unit,
    onDeleteLiability: (LiabilityAccount) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL LIABILITIES", style = MaterialTheme.typography.labelSmall)
                        Text("₹%,.2f".format(totalLiabilities), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = LossRed)
                    }
                    Button(onClick = onAddLiability, modifier = Modifier.testTag("btn_add_liability")) {
                        Text("+ Liability")
                    }
                }
            }
        }

        items(liabilities, key = { it.id }) { liab ->
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
                            Text(liab.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = LossRedLight) { Text(liab.category, fontSize = 9.sp) }
                        }
                        if (liab.interestRate > 0) {
                            Text("Interest: ${liab.interestRate}%  •  Due: ${liab.dueDate}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (liab.repaymentDetails.isNotBlank()) {
                            Text("Repayment: ${liab.repaymentDetails}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹%,.2f".format(liab.outstandingAmount), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = LossRed)
                        IconButton(onClick = { onDeleteLiability(liab) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: RESERVES & SURPLUS
// -------------------------------------------------------------
@Composable
fun ReservesTab(
    reserves: List<ReserveAccount>,
    reserveTransactions: List<ReserveTransaction>,
    totalReserves: Double,
    onCreateReserve: () -> Unit,
    onTransfer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("RESERVES & SURPLUS", style = MaterialTheme.typography.labelSmall)
                            Text("₹%,.2f".format(totalReserves), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = IndigoAccent)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onCreateReserve, modifier = Modifier.testTag("btn_create_reserve")) {
                                Text("New Reserve")
                            }
                            Button(onClick = onTransfer, modifier = Modifier.testTag("btn_transfer_reserve")) {
                                Text("Transfer / Utilise")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Classified under Capital/Equity in Balance Sheet. Transfers are not treated as ordinary expenses or income.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text("Reserve Accounts (${reserves.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        items(reserves, key = { it.id }) { res ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(res.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("₹%,.2f".format(res.currentBalance), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = IndigoAccent)
                    }
                    Text("Opening Balance: ₹%,.2f".format(res.openingBalance), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (res.description.isNotBlank()) {
                        Text(res.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Reserve Transfer & Utilisation History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        items(reserveTransactions, key = { it.id }) { tx ->
            val res = reserves.find { it.id == tx.reserveId }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("${res?.name ?: "Reserve"} • ${tx.type}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        Text("${tx.date}  •  ${tx.description}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("₹%,.2f".format(tx.amount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------
@Composable
fun AddFinancialTxDialog(
    assets: List<AssetAccount>,
    onDismiss: () -> Unit,
    onSave: (date: String, type: String, cat: String, sub: String, amt: Double, method: String, source: String, desc: String, recur: Boolean, linkedAssetId: Long?) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Food & Grocery") }
    var subcategory by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var sourceOrPayee by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedAssetId by remember { mutableStateOf(assets.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Income or Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE"; category = "Personal Expense" },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME"; category = "Salary & Remuneration" },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_tx_amount")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Settlement Account (Cash / Bank):", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    assets.forEach { a ->
                        FilterChip(
                            selected = selectedAssetId == a.id,
                            onClick = { selectedAssetId = a.id; paymentMethod = a.name },
                            label = { Text(a.name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(date, type, category, subcategory, amt, paymentMethod, sourceOrPayee, description, isRecurring, selectedAssetId)
                    }
                },
                modifier = Modifier.testTag("save_financial_tx")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddAssetDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, cat: String, qty: Double, price: Double, cmv: Double, pDate: String, vDate: String, desc: String, balance: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("INVESTMENTS") }
    var quantityStr by remember { mutableStateOf("1") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var cmvStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var balanceStr by remember { mutableStateOf("0") }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Asset Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Asset Name (e.g. Gold, SBI Bank, Reliance)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_asset_name")
                )

                Text("Category:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("CASH", "BANK", "INVESTMENTS", "OTHER_ASSETS").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                if (category == "CASH" || category == "BANK") {
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("Current Balance (₹, negative allowed)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            label = { Text("Cost Price (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = cmvStr,
                        onValueChange = { cmvStr = it },
                        label = { Text("Manual CMV (Current Market Value)") },
                        placeholder = { Text("Entered manually by you") },
                        modifier = Modifier.fillMaxWidth().testTag("input_asset_cmv")
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val qty = quantityStr.toDoubleOrNull() ?: 1.0
                        val price = purchasePriceStr.toDoubleOrNull() ?: 0.0
                        val cmv = cmvStr.toDoubleOrNull() ?: price
                        val bal = balanceStr.toDoubleOrNull() ?: 0.0
                        onSave(name, category, qty, price, cmv, today, today, description, bal)
                    }
                },
                modifier = Modifier.testTag("save_asset_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditCmvDialog(
    asset: AssetAccount,
    onDismiss: () -> Unit,
    onSave: (newCmv: Double, valuationDate: String) -> Unit
) {
    var cmvStr by remember { mutableStateOf(asset.cmv.toString()) }
    var valuationDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Manual CMV: ${asset.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "CMV is strictly entered manually by user and never fetched via external APIs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cmvStr,
                    onValueChange = { cmvStr = it },
                    label = { Text("Current Market Value Per Unit (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_cmv_input")
                )
                OutlinedTextField(
                    value = valuationDate,
                    onValueChange = { valuationDate = it },
                    label = { Text("Valuation Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newCmv = cmvStr.toDoubleOrNull() ?: asset.cmv
                onSave(newCmv, valuationDate)
            }) {
                Text("Update CMV")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddLiabilityDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, cat: String, amt: Double, rate: Double, due: String, repay: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("LOAN") }
    var amountStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("0") }
    var dueDate by remember { mutableStateOf("") }
    var repaymentDetails by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Liability Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Liability Name (e.g. Home Loan, Credit Card)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("LOAN", "BORROWING", "CREDIT_CARD", "OTHER").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Outstanding (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Interest Rate %") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repaymentDetails,
                    onValueChange = { repaymentDetails = it },
                    label = { Text("Repayment Terms / EMI") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                val rate = rateStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amt > 0) {
                    onSave(name, category, amt, rate, dueDate, repaymentDetails, desc)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CreateReserveDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, openBal: Double, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var openBalStr by remember { mutableStateOf("0") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Reserve Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reserve Name (e.g. General Reserve, Capital Reserve)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = openBalStr,
                    onValueChange = { openBalStr = it },
                    label = { Text("Opening Balance (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Purpose") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val bal = openBalStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name, bal, desc)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReserveTransferDialog(
    reserves: List<ReserveAccount>,
    onDismiss: () -> Unit,
    onSave: (reserveId: Long, date: String, amount: Double, type: String, desc: String) -> Unit
) {
    var selectedResId by remember { mutableStateOf(reserves.firstOrNull()?.id ?: 0L) }
    var amountStr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PROFIT_TRANSFER") }
    var desc by remember { mutableStateOf("") }
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer to / Utilise Reserve") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Action Type:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("PROFIT_TRANSFER", "UTILISATION", "ADDITION", "REDUCTION").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.replace("_", " "), fontSize = 9.sp) }
                        )
                    }
                }
                Text("Reserve Account:", style = MaterialTheme.typography.labelSmall)
                reserves.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedResId = r.id }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedResId == r.id, onClick = { selectedResId = r.id })
                        Text("${r.name} (Bal: ₹%,.2f)".format(r.currentBalance), style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (selectedResId != 0L && amt > 0) {
                    onSave(selectedResId, date, amt, type, desc)
                }
            }) { Text("Confirm Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
