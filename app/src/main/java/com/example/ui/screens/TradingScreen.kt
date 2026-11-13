package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.data.model.AssetAccount
import com.example.data.model.TradeRecord
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Trade Journal", "Holdings & CMV", "Open Shorts", "Trading Reports")

    val trades by viewModel.allTrades.collectAsState()
    val assets by viewModel.allAssets.collectAsState()

    var showTradeDialog by remember { mutableStateOf(false) }
    var buybackShortTrade by remember { mutableStateOf<TradeRecord?>(null) }
    var editingHoldingCmv by remember { mutableStateOf<AssetAccount?>(null) }

    // Filter holdings: only assets with category TRADING_HOLDINGS
    val holdings = assets.filter { it.category == "TRADING_HOLDINGS" }
    val openShorts = trades.filter { it.isShortSale && !it.isClosed }

    // Analytics
    val totalGrossPL = trades.sumOf { it.grossProfitLoss }
    val totalCharges = trades.sumOf { it.charges }
    val totalNetPL = trades.sumOf { it.netProfitLoss }
    val holdingsValue = holdings.sumOf { it.currentTotalValue }
    val holdingsUnrealisedPL = holdings.sumOf { it.unrealisedProfitLoss }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trading Journal & Holdings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTradeDialog = true },
                        modifier = Modifier.testTag("btn_log_trade_action")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Log Trade")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTradeDialog = true },
                containerColor = AccentGold,
                modifier = Modifier.testTag("fab_add_trade")
            ) {
                Icon(Icons.Default.CandlestickChart, contentDescription = "Record Trade", tint = Slate900)
            }
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
                        modifier = Modifier.testTag("trading_tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> TradesListTab(
                    trades = trades,
                    onDeleteTrade = { viewModel.deleteTrade(it) },
                    onCoverShort = {
                        buybackShortTrade = it
                        showTradeDialog = true
                    }
                )
                1 -> TradingHoldingsTab(
                    holdings = holdings,
                    totalValue = holdingsValue,
                    totalUnrealised = holdingsUnrealisedPL,
                    onEditCmv = { editingHoldingCmv = it }
                )
                2 -> OpenShortsTab(
                    openShorts = openShorts,
                    onCoverShort = {
                        buybackShortTrade = it
                        showTradeDialog = true
                    }
                )
                3 -> TradingReportsTab(
                    trades = trades,
                    holdings = holdings,
                    totalGrossPL = totalGrossPL,
                    totalCharges = totalCharges,
                    totalNetPL = totalNetPL,
                    holdingsValue = holdingsValue,
                    holdingsUnrealisedPL = holdingsUnrealisedPL,
                    openShortsCount = openShorts.size
                )
            }
        }
    }

    if (showTradeDialog) {
        TradeEntryDialog(
            settlementAccounts = assets.filter { it.category in listOf("CASH", "BANK") },
            existingHoldings = holdings,
            buybackTrade = buybackShortTrade,
            onDismiss = {
                showTradeDialog = false
                buybackShortTrade = null
            },
            onSave = { date, time, instrument, type, txType, qty, price, charges, isShort, buybackId, notes, assetId ->
                viewModel.recordTrade(
                    date = date,
                    time = time,
                    instrumentName = instrument,
                    tradingType = type,
                    transactionType = txType,
                    quantity = qty,
                    price = price,
                    charges = charges,
                    isShortSale = isShort,
                    buybackTradeId = buybackId,
                    notes = notes,
                    settlementAssetId = assetId
                )
                showTradeDialog = false
                buybackShortTrade = null
            }
        )
    }

    if (editingHoldingCmv != null) {
        EditCmvDialog(
            asset = editingHoldingCmv!!,
            onDismiss = { editingHoldingCmv = null },
            onSave = { newCmv, valDate ->
                viewModel.updateAssetCmv(editingHoldingCmv!!, newCmv, valDate)
                editingHoldingCmv = null
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: TRADES LIST
// -------------------------------------------------------------
@Composable
fun TradesListTab(
    trades: List<TradeRecord>,
    onDeleteTrade: (TradeRecord) -> Unit,
    onCoverShort: (TradeRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (trades.isEmpty()) {
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
                        Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No trades recorded yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Record BUY/SELL for Intraday, Delivery, Futures, or Options. Intraday short selling supported.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(trades, key = { it.id }) { trade ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                Text(trade.instrumentName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.width(6.dp))
                                val txBadgeColor = if (trade.transactionType == "BUY") ProfitGreen else LossRed
                                Badge(containerColor = txBadgeColor) { Text(trade.transactionType, fontSize = 9.sp, color = Color.White) }
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) { Text(trade.tradingType, fontSize = 9.sp) }
                            }
                            if (trade.isShortSale && !trade.isClosed) {
                                Badge(containerColor = AccentGold) { Text("OPEN SHORT", fontSize = 10.sp, color = Slate900) }
                            } else if (trade.netProfitLoss != 0.0) {
                                val plColor = if (trade.netProfitLoss >= 0) ProfitGreen else LossRed
                                Text(
                                    (if (trade.netProfitLoss >= 0) "+ ₹" else "- ₹") + "%,.2f".format(kotlin.math.abs(trade.netProfitLoss)),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = plColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${trade.date} ${trade.time}  •  Qty: ${trade.quantity} @ ₹${trade.price}  •  Charges: ₹${trade.charges}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (trade.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Note: ${trade.notes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (trade.isShortSale && !trade.isClosed) {
                                Button(
                                    onClick = { onCoverShort(trade) },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                                ) {
                                    Text("Buyback / Cover Short", fontSize = 11.sp, color = Slate900)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(onClick = { onDeleteTrade(trade) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: HOLDINGS WITH MANUAL CMV
// -------------------------------------------------------------
@Composable
fun TradingHoldingsTab(
    holdings: List<AssetAccount>,
    totalValue: Double,
    totalUnrealised: Double,
    onEditCmv: (AssetAccount) -> Unit
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
                    Text("CURRENT TRADING HOLDINGS VALUE (MANUAL CMV)", style = MaterialTheme.typography.labelSmall)
                    Text("₹%,.2f".format(totalValue), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Total Unrealised P&L: " + (if (totalUnrealised >= 0) "+ ₹" else "- ₹") + "%,.2f".format(kotlin.math.abs(totalUnrealised)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (totalUnrealised >= 0) ProfitGreen else LossRed
                    )
                    Text(
                        "CMV is strictly user-entered. Real live market prices are never automatically fetched.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (holdings.isEmpty()) {
            item {
                Text("No current delivery / investment holdings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(holdings, key = { it.id }) { h ->
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
                            Text(h.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("CMV Total: ₹%,.2f".format(h.currentTotalValue), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Qty: ${h.quantity.toInt()} @ Avg ₹${h.purchasePrice}", style = MaterialTheme.typography.bodySmall)
                            Text("Manual CMV: ₹${h.cmv}", style = MaterialTheme.typography.bodySmall)
                        }
                        val unPL = h.unrealisedProfitLoss
                        Text(
                            "Unrealised P&L: " + (if (unPL >= 0) "+ ₹" else "- ₹") + "%,.2f".format(kotlin.math.abs(unPL)),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (unPL >= 0) ProfitGreen else LossRed
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onEditCmv(h) }) {
                                Text("Edit Manual CMV")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: OPEN SHORTS
// -------------------------------------------------------------
@Composable
fun OpenShortsTab(
    openShorts: List<TradeRecord>,
    onCoverShort: (TradeRecord) -> Unit
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("OPEN INTRADAY SHORT POSITIONS (${openShorts.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    Text("Intraday positions sold first without prior holding. Tracked separately from long holdings as unsettled obligations.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (openShorts.isEmpty()) {
            item {
                Text("No open short positions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(openShorts, key = { it.id }) { s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.instrumentName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Badge(containerColor = AccentGold) { Text("SELL FIRST", fontSize = 10.sp, color = Slate900) }
                        }
                        Text("Sold Qty: ${s.openQuantity} @ ₹${s.price} on ${s.date}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onCoverShort(s) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                        ) {
                            Text("Buyback to Close Short Position", color = Slate900, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: TRADING REPORTS
// -------------------------------------------------------------
@Composable
fun TradingReportsTab(
    trades: List<TradeRecord>,
    holdings: List<AssetAccount>,
    totalGrossPL: Double,
    totalCharges: Double,
    totalNetPL: Double,
    holdingsValue: Double,
    holdingsUnrealisedPL: Double,
    openShortsCount: Int
) {
    val totalTrades = trades.size
    val longTrades = trades.filter { !it.isShortSale }
    val shortTrades = trades.filter { it.isShortSale }

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
                    Text("TRADING PERFORMANCE OVERVIEW", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Realised P&L", style = MaterialTheme.typography.bodyMedium)
                        Text("₹%,.2f".format(totalGrossPL), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brokerage & Exchange Charges", style = MaterialTheme.typography.bodyMedium)
                        Text("- ₹%,.2f".format(totalCharges), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = LossRed)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Realised P&L", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("₹%,.2f".format(totalNetPL), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (totalNetPL >= 0) ProfitGreen else LossRed)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Total Trades",
                    value = "$totalTrades",
                    subtitle = "Long: ${longTrades.size} | Short: ${shortTrades.size}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Open Shorts",
                    value = "$openShortsCount",
                    subtitle = "Pending Buyback",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Holdings CMV",
                    value = "₹%,.0f".format(holdingsValue),
                    subtitle = "${holdings.size} Instruments",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Unrealised P&L",
                    value = "₹%,.2f".format(holdingsUnrealisedPL),
                    isProfit = holdingsUnrealisedPL >= 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TRADE ENTRY DIALOG WITH INTRADAY SHORT SELL
// -------------------------------------------------------------
@Composable
fun TradeEntryDialog(
    settlementAccounts: List<AssetAccount>,
    existingHoldings: List<AssetAccount>,
    buybackTrade: TradeRecord?,
    onDismiss: () -> Unit,
    onSave: (date: String, time: String, instrument: String, type: String, txType: String, qty: Int, price: Double, charges: Double, isShort: Boolean, buybackId: Long?, notes: String, assetId: Long?) -> Unit
) {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    var instrumentName by remember { mutableStateOf(buybackTrade?.instrumentName ?: "") }
    var tradingType by remember { mutableStateOf(buybackTrade?.tradingType ?: "INTRADAY") } // INTRADAY, DELIVERY, FUTURES, OPTIONS
    var transactionType by remember { mutableStateOf(if (buybackTrade != null) "BUY" else "BUY") } // BUY, SELL
    var quantityStr by remember { mutableStateOf(buybackTrade?.openQuantity?.toString() ?: "100") }
    var priceStr by remember { mutableStateOf("") }
    var chargesStr by remember { mutableStateOf("20") }
    var isShortSale by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var selectedSettlementId by remember { mutableStateOf(settlementAccounts.firstOrNull()?.id) }

    // If Delivery is selected, Short Selling is forbidden
    val canShortSell = tradingType == "INTRADAY" && transactionType == "SELL" && buybackTrade == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (buybackTrade != null) "Cover Short Position" else "Record Trade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (buybackTrade != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Covering Short Sale of ${buybackTrade.openQuantity} units @ ₹${buybackTrade.price}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = transactionType == "BUY",
                            onClick = { transactionType = "BUY"; isShortSale = false },
                            label = { Text("BUY") },
                            modifier = Modifier.weight(1f).testTag("chip_trade_buy")
                        )
                        FilterChip(
                            selected = transactionType == "SELL",
                            onClick = { transactionType = "SELL" },
                            label = { Text("SELL") },
                            modifier = Modifier.weight(1f).testTag("chip_trade_sell")
                        )
                    }

                    Text("Trading Product Type:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("INTRADAY", "DELIVERY", "FUTURES", "OPTIONS").forEach { t ->
                            FilterChip(
                                selected = tradingType == t,
                                onClick = {
                                    tradingType = t
                                    if (t == "DELIVERY") isShortSale = false
                                },
                                label = { Text(t, fontSize = 9.sp) }
                            )
                        }
                    }

                    if (canShortSell) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isShortSale, onCheckedChange = { isShortSale = it })
                            Text("Intraday Short Sell (Sell first without holding)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = instrumentName,
                    onValueChange = { instrumentName = it },
                    label = { Text("Instrument Name (e.g., TATASTEEL, NIFTY 24000 CE)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_instrument_name")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f).testTag("input_trade_qty")
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)") },
                        modifier = Modifier.weight(1f).testTag("input_trade_price")
                    )
                }

                OutlinedTextField(
                    value = chargesStr,
                    onValueChange = { chargesStr = it },
                    label = { Text("Brokerage & STT Charges (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_trade_charges")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Strategy") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toIntOrNull() ?: 0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val charges = chargesStr.toDoubleOrNull() ?: 0.0
                    if (instrumentName.isNotBlank() && qty > 0 && price > 0) {
                        onSave(date, time, instrumentName, tradingType, transactionType, qty, price, charges, isShortSale, buybackTrade?.id, notes, selectedSettlementId)
                    }
                },
                modifier = Modifier.testTag("save_trade_btn")
            ) {
                Text(if (buybackTrade != null) "Confirm Buyback" else "Record Trade")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
