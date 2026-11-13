package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.security.DiarySecurityManager
import com.example.util.ScreenTimeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sleepDao = db.sleepDao()
    private val financeDao = db.financeDao()
    private val tradingDao = db.tradingDao()
    private val diaryDao = db.diaryDao()
    private val activityDao = db.activityDao()
    private val plannerDao = db.plannerDao()
    private val screenTimeDao = db.screenTimeDao()

    // Date Formatters
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Selected Date Filter across modules (default today)
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Configurable Record-Locking Policy (Days: 0 = Off, 1 = 24h, 7 = 7 days, 30 = 30 days)
    private val _recordLockDays = MutableStateFlow(7)
    val recordLockDays: StateFlow<Int> = _recordLockDays.asStateFlow()

    // Selected Financial Year (e.g. "FY 2026-27")
    private val _selectedFinancialYear = MutableStateFlow(getCurrentFinancialYear())
    val selectedFinancialYear: StateFlow<String> = _selectedFinancialYear.asStateFlow()

    // Data Flows from Room
    val allSleepRecords = sleepDao.getAllRecords().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTransactions = financeDao.getAllTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAssets = financeDao.getAllAssets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allLiabilities = financeDao.getAllLiabilities().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReserves = financeDao.getAllReserves().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReserveTransactions = financeDao.getAllReserveTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTrades = tradingDao.getAllTrades().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDiaryEntries = diaryDao.getAllEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allActivities = activityDao.getAllActivities().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allActivitySessions = activityDao.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPlannedTasks = plannerDao.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allScreenTimeRecords = screenTimeDao.getAllRecords().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stopwatch Runtime State
    private val _activeStopwatchActivityId = MutableStateFlow<Long?>(null)
    val activeStopwatchActivityId: StateFlow<Long?> = _activeStopwatchActivityId.asStateFlow()

    private val _stopwatchElapsedSeconds = MutableStateFlow(0L)
    val stopwatchElapsedSeconds: StateFlow<Long> = _stopwatchElapsedSeconds.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private var stopwatchJob: Job? = null
    private var stopwatchStartTimeEpoch: Long = 0L

    // Screen Time permission state
    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    // Diary Auth state
    private val _isDiaryUnlocked = MutableStateFlow(false)
    val isDiaryUnlocked: StateFlow<Boolean> = _isDiaryUnlocked.asStateFlow()

    init {
        checkUsagePermission()
        checkInitialAssets()
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        refreshScreenTimeForDate(date)
    }

    fun setRecordLockDays(days: Int) {
        _recordLockDays.value = days
    }

    fun setSelectedFinancialYear(fy: String) {
        _selectedFinancialYear.value = fy
    }

    fun isRecordLocked(createdAt: Long): Boolean {
        val days = _recordLockDays.value
        if (days <= 0) return false
        val thresholdMillis = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return createdAt < thresholdMillis
    }

    private fun checkInitialAssets() {
        viewModelScope.launch {
            // Seed default Cash in Hand and Primary Bank if user has 0 assets
            val existing = allAssets.first()
            if (existing.isEmpty()) {
                financeDao.insertAsset(
                    AssetAccount(
                        name = "Cash in Hand",
                        category = "CASH",
                        balance = 0.0,
                        description = "Cash currency notes & coins"
                    )
                )
                financeDao.insertAsset(
                    AssetAccount(
                        name = "Primary Bank Account",
                        category = "BANK",
                        balance = 0.0,
                        description = "Operating checking/savings account"
                    )
                )
            }
        }
    }

    // -------------------------------------------------------------
    // 1. SLEEP TRACKER
    // -------------------------------------------------------------
    fun calculateSleepDurationMinutes(sleepTime: String, wakeTime: String): Int {
        if (sleepTime.isBlank() || wakeTime.isBlank()) return 0
        return try {
            val sParts = sleepTime.split(":").map { it.toInt() }
            val wParts = wakeTime.split(":").map { it.toInt() }
            val sMin = sParts[0] * 60 + sParts[1]
            val wMin = wParts[0] * 60 + wParts[1]

            if (wMin >= sMin) {
                wMin - sMin
            } else {
                // Crossed midnight: e.g. 23:00 to 07:00
                (1440 - sMin) + wMin
            }
        } catch (e: Exception) {
            0
        }
    }

    fun saveSleepRecord(
        id: Long = 0,
        date: String,
        sleepTime: String,
        wakeTime: String,
        notes: String
    ) {
        val duration = calculateSleepDurationMinutes(sleepTime, wakeTime)
        viewModelScope.launch {
            val record = SleepRecord(
                id = id,
                date = date,
                sleepTime = sleepTime,
                wakeTime = wakeTime,
                durationMinutes = duration,
                notes = notes
            )
            if (id == 0L) {
                sleepDao.insertRecord(record)
            } else {
                sleepDao.updateRecord(record)
            }
        }
    }

    fun deleteSleepRecord(record: SleepRecord) {
        viewModelScope.launch {
            sleepDao.deleteRecord(record)
        }
    }

    // -------------------------------------------------------------
    // 2. FINANCIAL MANAGEMENT & DOUBLE-ENTRY
    // -------------------------------------------------------------
    fun addFinancialTransaction(
        date: String,
        type: String, // INCOME, EXPENSE
        category: String,
        subcategory: String,
        amount: Double,
        paymentMethod: String,
        sourceOrPayee: String,
        description: String,
        isRecurring: Boolean,
        linkedAssetId: Long?
    ) {
        viewModelScope.launch {
            val txId = financeDao.insertTransaction(
                FinancialTransaction(
                    date = date,
                    type = type,
                    category = category,
                    subcategory = subcategory,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    sourceOrPayee = sourceOrPayee,
                    description = description,
                    isRecurring = isRecurring,
                    linkedAssetId = linkedAssetId
                )
            )

            // Double-entry balance adjustment:
            // If linked to an Asset Account (e.g. Cash in Hand or Bank Account):
            // Income increases asset balance, Expense reduces asset balance.
            // Negative balances are fully supported!
            if (linkedAssetId != null) {
                val asset = financeDao.getAssetById(linkedAssetId)
                if (asset != null && (asset.category == "CASH" || asset.category == "BANK")) {
                    val newBalance = if (type == "INCOME") asset.balance + amount else asset.balance - amount
                    financeDao.updateAsset(asset.copy(balance = newBalance, valuationDate = date))
                }
            }
        }
    }

    fun deleteFinancialTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            // Revert linked asset balance
            if (transaction.linkedAssetId != null) {
                val asset = financeDao.getAssetById(transaction.linkedAssetId)
                if (asset != null && (asset.category == "CASH" || asset.category == "BANK")) {
                    val reverted = if (transaction.type == "INCOME") {
                        asset.balance - transaction.amount
                    } else {
                        asset.balance + transaction.amount
                    }
                    financeDao.updateAsset(asset.copy(balance = reverted))
                }
            }
            financeDao.deleteTransaction(transaction)
        }
    }

    fun saveAssetAccount(
        id: Long = 0,
        name: String,
        category: String,
        quantity: Double,
        purchasePrice: Double,
        cmv: Double,
        purchaseDate: String,
        valuationDate: String,
        description: String,
        balance: Double
    ) {
        viewModelScope.launch {
            val asset = AssetAccount(
                id = id,
                name = name,
                category = category,
                quantity = quantity,
                purchasePrice = purchasePrice,
                cmv = cmv, // Manual entry only!
                purchaseDate = purchaseDate,
                valuationDate = valuationDate,
                description = description,
                balance = balance
            )
            if (id == 0L) {
                financeDao.insertAsset(asset)
            } else {
                financeDao.updateAsset(asset)
            }
        }
    }

    fun updateAssetCmv(asset: AssetAccount, newCmv: Double, valuationDate: String) {
        viewModelScope.launch {
            financeDao.updateAsset(asset.copy(cmv = newCmv, valuationDate = valuationDate))
        }
    }

    fun deleteAsset(asset: AssetAccount) {
        viewModelScope.launch {
            financeDao.deleteAsset(asset)
        }
    }

    fun saveLiability(
        id: Long = 0,
        name: String,
        category: String,
        outstandingAmount: Double,
        interestRate: Double,
        dueDate: String,
        repaymentDetails: String,
        description: String
    ) {
        viewModelScope.launch {
            val liab = LiabilityAccount(
                id = id,
                name = name,
                category = category,
                outstandingAmount = outstandingAmount,
                interestRate = interestRate,
                dueDate = dueDate,
                repaymentDetails = repaymentDetails,
                description = description
            )
            if (id == 0L) {
                financeDao.insertLiability(liab)
            } else {
                financeDao.updateLiability(liab)
            }
        }
    }

    fun deleteLiability(liability: LiabilityAccount) {
        viewModelScope.launch {
            financeDao.deleteLiability(liability)
        }
    }

    // Reserves & Surplus
    fun createReserveAccount(name: String, openingBalance: Double, description: String) {
        viewModelScope.launch {
            financeDao.insertReserve(
                ReserveAccount(
                    name = name,
                    openingBalance = openingBalance,
                    currentBalance = openingBalance,
                    description = description
                )
            )
        }
    }

    fun addReserveTransaction(
        reserveId: Long,
        date: String,
        amount: Double,
        type: String, // PROFIT_TRANSFER, ADDITION, UTILISATION, REDUCTION
        description: String
    ) {
        viewModelScope.launch {
            val reserve = financeDao.getReserveById(reserveId) ?: return@launch
            val newBalance = when (type) {
                "PROFIT_TRANSFER", "ADDITION" -> reserve.currentBalance + amount
                "UTILISATION", "REDUCTION" -> reserve.currentBalance - amount
                else -> reserve.currentBalance
            }
            financeDao.updateReserve(reserve.copy(currentBalance = newBalance))
            financeDao.insertReserveTransaction(
                ReserveTransaction(
                    reserveId = reserveId,
                    date = date,
                    amount = amount,
                    type = type,
                    description = description
                )
            )
        }
    }

    // -------------------------------------------------------------
    // 3. TRADING JOURNAL & FINANCIAL INTEGRATION
    // -------------------------------------------------------------
    fun recordTrade(
        date: String,
        time: String,
        instrumentName: String,
        tradingType: String, // INTRADAY, DELIVERY, FUTURES, OPTIONS
        transactionType: String, // BUY, SELL
        quantity: Int,
        price: Double,
        charges: Double,
        isShortSale: Boolean = false,
        buybackTradeId: Long? = null,
        notes: String = "",
        settlementAssetId: Long? = null
    ) {
        viewModelScope.launch {
            val totalCost = quantity * price
            var grossPL = 0.0
            var netPL = 0.0
            var isClosed = false
            var openQty = quantity

            // 1. If Normal LONG BUY:
            if (transactionType == "BUY" && !isShortSale && buybackTradeId == null) {
                val tradeId = tradingDao.insertTrade(
                    TradeRecord(
                        date = date,
                        time = time,
                        instrumentName = instrumentName,
                        tradingType = tradingType,
                        transactionType = "BUY",
                        quantity = quantity,
                        price = price,
                        charges = charges,
                        grossProfitLoss = 0.0,
                        netProfitLoss = -charges,
                        isShortSale = false,
                        isClosed = false,
                        openQuantity = quantity,
                        notes = notes
                    )
                )

                // Add or update holdings asset:
                val existingHolding = allAssets.value.find { it.name.equals(instrumentName, ignoreCase = true) && it.category == "TRADING_HOLDINGS" }
                if (existingHolding != null) {
                    val newQty = existingHolding.quantity + quantity
                    val newAvgPrice = ((existingHolding.quantity * existingHolding.purchasePrice) + totalCost) / newQty
                    financeDao.updateAsset(
                        existingHolding.copy(
                            quantity = newQty,
                            purchasePrice = newAvgPrice,
                            valuationDate = date
                        )
                    )
                } else {
                    financeDao.insertAsset(
                        AssetAccount(
                            name = instrumentName,
                            category = "TRADING_HOLDINGS",
                            quantity = quantity.toDouble(),
                            purchasePrice = price,
                            cmv = price, // Initial manual CMV
                            purchaseDate = date,
                            valuationDate = date,
                            description = "$tradingType holding"
                        )
                    )
                }

                // Financial Integration:
                // Reduce cash/bank balance by purchase cost + charges.
                // Record charges as Financial Expense. (Purchase is asset acquisition, NOT an expense).
                if (settlementAssetId != null) {
                    val asset = financeDao.getAssetById(settlementAssetId)
                    if (asset != null) {
                        financeDao.updateAsset(asset.copy(balance = asset.balance - totalCost - charges))
                    }
                }
                if (charges > 0) {
                    financeDao.insertTransaction(
                        FinancialTransaction(
                            date = date,
                            type = "EXPENSE",
                            category = "Trading Charges",
                            amount = charges,
                            description = "Brokerage & charges on BUY $instrumentName",
                            linkedTradeId = tradeId
                        )
                    )
                }
            }

            // 2. If Normal LONG SELL (Closing holding):
            else if (transactionType == "SELL" && !isShortSale) {
                val existingHolding = allAssets.value.find { it.name.equals(instrumentName, ignoreCase = true) && it.category == "TRADING_HOLDINGS" }
                val costBasis = existingHolding?.purchasePrice ?: price
                grossPL = (price - costBasis) * quantity
                netPL = grossPL - charges
                isClosed = true

                val tradeId = tradingDao.insertTrade(
                    TradeRecord(
                        date = date,
                        time = time,
                        instrumentName = instrumentName,
                        tradingType = tradingType,
                        transactionType = "SELL",
                        quantity = quantity,
                        price = price,
                        charges = charges,
                        buyPrice = costBasis,
                        sellPrice = price,
                        grossProfitLoss = grossPL,
                        netProfitLoss = netPL,
                        isShortSale = false,
                        isClosed = true,
                        openQuantity = 0,
                        notes = notes
                    )
                )

                // Update holdings
                if (existingHolding != null) {
                    val remQty = (existingHolding.quantity - quantity).coerceAtLeast(0.0)
                    if (remQty == 0.0) {
                        financeDao.deleteAsset(existingHolding)
                    } else {
                        financeDao.updateAsset(existingHolding.copy(quantity = remQty, valuationDate = date))
                    }
                }

                // Financial Integration:
                // Cash received = (quantity * price) - charges.
                if (settlementAssetId != null) {
                    val asset = financeDao.getAssetById(settlementAssetId)
                    if (asset != null) {
                        financeDao.updateAsset(asset.copy(balance = asset.balance + (totalCost - charges)))
                    }
                }

                // If net profit > 0 -> Income (Realised Trading Profit)
                // If net profit < 0 -> Expense (Realised Trading Loss)
                if (netPL > 0) {
                    financeDao.insertTransaction(
                        FinancialTransaction(
                            date = date,
                            type = "INCOME",
                            category = "Realised Trading Profit",
                            amount = netPL,
                            description = "Profit on sale of $instrumentName",
                            linkedTradeId = tradeId
                        )
                    )
                } else if (netPL < 0) {
                    financeDao.insertTransaction(
                        FinancialTransaction(
                            date = date,
                            type = "EXPENSE",
                            category = "Realised Trading Loss",
                            amount = -netPL,
                            description = "Loss on sale of $instrumentName",
                            linkedTradeId = tradeId
                        )
                    )
                }
                if (charges > 0) {
                    financeDao.insertTransaction(
                        FinancialTransaction(
                            date = date,
                            type = "EXPENSE",
                            category = "Trading Charges",
                            amount = charges,
                            description = "Brokerage on SELL $instrumentName",
                            linkedTradeId = tradeId
                        )
                    )
                }
            }

            // 3. Intraday Short SELL (Selling without prior holding):
            else if (isShortSale && transactionType == "SELL") {
                // Short selling: Sell first, buy back later.
                // Open short position tracked separately, not an ordinary long holding.
                val tradeId = tradingDao.insertTrade(
                    TradeRecord(
                        date = date,
                        time = time,
                        instrumentName = instrumentName,
                        tradingType = "INTRADAY",
                        transactionType = "SELL",
                        quantity = quantity,
                        price = price,
                        charges = charges,
                        grossProfitLoss = 0.0,
                        netProfitLoss = -charges,
                        isShortSale = true,
                        isClosed = false,
                        openQuantity = quantity,
                        notes = "Intraday Short Position: $notes"
                    )
                )

                // Cash/Settlement account receives sale proceeds minus charges:
                if (settlementAssetId != null) {
                    val asset = financeDao.getAssetById(settlementAssetId)
                    if (asset != null) {
                        financeDao.updateAsset(asset.copy(balance = asset.balance + (totalCost - charges)))
                    }
                }
                if (charges > 0) {
                    financeDao.insertTransaction(
                        FinancialTransaction(
                            date = date,
                            type = "EXPENSE",
                            category = "Trading Charges",
                            amount = charges,
                            description = "Brokerage on Intraday Short SELL $instrumentName",
                            linkedTradeId = tradeId
                        )
                    )
                }
            }

            // 4. Intraday Short Buyback (Covering an open short position):
            else if (buybackTradeId != null && transactionType == "BUY") {
                val shortTrade = tradingDao.getTradeById(buybackTradeId)
                if (shortTrade != null) {
                    val sellPrice = shortTrade.price
                    val buybackPrice = price
                    grossPL = (sellPrice - buybackPrice) * quantity
                    netPL = grossPL - (charges + shortTrade.charges)

                    // Close short trade
                    tradingDao.updateTrade(
                        shortTrade.copy(
                            isClosed = true,
                            openQuantity = 0,
                            buyPrice = buybackPrice,
                            sellPrice = sellPrice,
                            grossProfitLoss = grossPL,
                            netProfitLoss = netPL
                        )
                    )

                    // Deduct buyback cost from cash/settlement
                    if (settlementAssetId != null) {
                        val asset = financeDao.getAssetById(settlementAssetId)
                        if (asset != null) {
                            financeDao.updateAsset(asset.copy(balance = asset.balance - totalCost - charges))
                        }
                    }

                    // Record realised profit / loss in P&L
                    if (netPL > 0) {
                        financeDao.insertTransaction(
                            FinancialTransaction(
                                date = date,
                                type = "INCOME",
                                category = "Realised Trading Profit",
                                amount = netPL,
                                description = "Profit on short cover of $instrumentName",
                                linkedTradeId = buybackTradeId
                            )
                        )
                    } else if (netPL < 0) {
                        financeDao.insertTransaction(
                            FinancialTransaction(
                                date = date,
                                type = "EXPENSE",
                                category = "Realised Trading Loss",
                                amount = -netPL,
                                description = "Loss on short cover of $instrumentName",
                                linkedTradeId = buybackTradeId
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteTrade(trade: TradeRecord) {
        viewModelScope.launch {
            // Reverse linked financial transactions
            financeDao.deleteByLinkedTradeId(trade.id)
            tradingDao.deleteTrade(trade)
        }
    }

    // -------------------------------------------------------------
    // 4. PASSWORD-PROTECTED PERSONAL DIARY
    // -------------------------------------------------------------
    fun isDiaryPinConfigured(): Boolean {
        return DiarySecurityManager.isPinConfigured(getApplication())
    }

    fun setupDiaryPin(pin: String): Boolean {
        val success = DiarySecurityManager.setupPin(getApplication(), pin)
        _isDiaryUnlocked.value = success
        return success
    }

    fun unlockDiary(pin: String): Boolean {
        val success = DiarySecurityManager.unlockWithPin(getApplication(), pin)
        _isDiaryUnlocked.value = success
        return success
    }

    fun lockDiary() {
        DiarySecurityManager.lock()
        _isDiaryUnlocked.value = false
    }

    fun saveDiaryEntry(
        id: Long = 0,
        date: String,
        time: String,
        plainTitle: String,
        plainContent: String,
        mood: String,
        tags: String
    ) {
        if (!DiarySecurityManager.isUnlocked) return
        val encTitle = DiarySecurityManager.encrypt(plainTitle)
        val encContent = DiarySecurityManager.encrypt(plainContent)

        viewModelScope.launch {
            val entry = DiaryEntry(
                id = id,
                date = date,
                time = time,
                encryptedTitle = encTitle.cipherTextBase64,
                encryptedContent = encContent.cipherTextBase64,
                ivBase64 = encContent.ivBase64, // Used for decryption
                mood = mood,
                tags = tags
            )
            if (id == 0L) {
                diaryDao.insertEntry(entry)
            } else {
                diaryDao.updateEntry(entry)
            }
        }
    }

    fun decryptDiaryTitle(entry: DiaryEntry): String {
        return DiarySecurityManager.decrypt(entry.encryptedTitle, entry.ivBase64)
    }

    fun decryptDiaryContent(entry: DiaryEntry): String {
        return DiarySecurityManager.decrypt(entry.encryptedContent, entry.ivBase64)
    }

    fun deleteDiaryEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryDao.deleteEntry(entry)
        }
    }

    // -------------------------------------------------------------
    // 5. SCREEN TIME TRACKER
    // -------------------------------------------------------------
    fun checkUsagePermission() {
        _hasUsagePermission.value = ScreenTimeHelper.hasUsageStatsPermission(getApplication())
    }

    fun refreshScreenTimeForDate(dateStr: String) {
        checkUsagePermission()
        if (_hasUsagePermission.value) {
            viewModelScope.launch {
                val stats = ScreenTimeHelper.getUsageStatsForDay(getApplication(), dateStr)
                if (stats.isNotEmpty()) {
                    screenTimeDao.deleteAutomaticForDate(dateStr)
                    screenTimeDao.insertAll(stats)
                }
            }
        }
    }

    fun addManualScreenTime(
        date: String,
        appName: String,
        category: String,
        durationMinutes: Long
    ) {
        viewModelScope.launch {
            screenTimeDao.insertRecord(
                ScreenTimeRecord(
                    date = date,
                    packageName = "manual." + appName.lowercase().replace(" ", ""),
                    appName = appName,
                    category = category,
                    durationMinutes = durationMinutes,
                    isManual = true
                )
            )
        }
    }

    fun deleteScreenTimeRecord(record: ScreenTimeRecord) {
        viewModelScope.launch {
            screenTimeDao.deleteRecord(record)
        }
    }

    // -------------------------------------------------------------
    // 6. ACTIVITY TRACKER (STOPWATCH)
    // IMPORTANT: Empty by default!
    // -------------------------------------------------------------
    fun createActivity(name: String, category: String, colorHex: String) {
        viewModelScope.launch {
            activityDao.insertActivity(
                ActivityItem(
                    name = name,
                    category = category,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteActivity(activity: ActivityItem) {
        viewModelScope.launch {
            if (_activeStopwatchActivityId.value == activity.id) {
                stopStopwatch(save = false)
            }
            activityDao.deleteSessionsForActivity(activity.id)
            activityDao.deleteActivity(activity)
        }
    }

    fun startStopwatch(activityId: Long) {
        if (_activeStopwatchActivityId.value != null && _activeStopwatchActivityId.value != activityId) {
            // Only ONE stopwatch can run at a time!
            stopStopwatch(save = true)
        }
        _activeStopwatchActivityId.value = activityId
        _isStopwatchRunning.value = true
        stopwatchStartTimeEpoch = System.currentTimeMillis() - (_stopwatchElapsedSeconds.value * 1000)

        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (_isStopwatchRunning.value) {
                _stopwatchElapsedSeconds.value = (System.currentTimeMillis() - stopwatchStartTimeEpoch) / 1000
                delay(1000)
            }
        }
    }

    fun pauseStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
    }

    fun resumeStopwatch() {
        val currentId = _activeStopwatchActivityId.value ?: return
        startStopwatch(currentId)
    }

    fun stopStopwatch(save: Boolean = true, notes: String = "") {
        val actId = _activeStopwatchActivityId.value
        val durationSec = _stopwatchElapsedSeconds.value
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()

        if (save && actId != null && durationSec > 0) {
            val act = allActivities.value.find { it.id == actId }
            val now = System.currentTimeMillis()
            val start = now - (durationSec * 1000)
            viewModelScope.launch {
                activityDao.insertSession(
                    ActivitySession(
                        activityId = actId,
                        activityName = act?.name ?: "Activity",
                        startTime = start,
                        endTime = now,
                        durationSeconds = durationSec,
                        notes = notes
                    )
                )
            }
        }

        _activeStopwatchActivityId.value = null
        _stopwatchElapsedSeconds.value = 0L
    }

    fun addManualActivitySession(
        activityId: Long,
        activityName: String,
        durationMinutes: Long,
        notes: String
    ) {
        val durationSec = durationMinutes * 60
        val now = System.currentTimeMillis()
        val start = now - (durationSec * 1000)
        viewModelScope.launch {
            activityDao.insertSession(
                ActivitySession(
                    activityId = activityId,
                    activityName = activityName,
                    startTime = start,
                    endTime = now,
                    durationSeconds = durationSec,
                    notes = notes
                )
            )
        }
    }

    fun deleteActivitySession(session: ActivitySession) {
        viewModelScope.launch {
            activityDao.deleteSession(session)
        }
    }

    // -------------------------------------------------------------
    // 7. DAILY PLANNER (PLANNED VS ACTUAL)
    // -------------------------------------------------------------
    fun addPlannedTask(
        date: String,
        title: String,
        startTime: String,
        endTime: String,
        priority: String,
        notes: String
    ) {
        viewModelScope.launch {
            plannerDao.insertTask(
                PlannedTask(
                    date = date,
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    priority = priority,
                    notes = notes
                )
            )
        }
    }

    fun toggleTaskCompletion(task: PlannedTask) {
        viewModelScope.launch {
            plannerDao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: PlannedTask) {
        viewModelScope.launch {
            plannerDao.deleteTask(task)
        }
    }

    companion object {
        fun getCurrentFinancialYear(): String {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) // 0-based, April is 3
            return if (month >= Calendar.APRIL) {
                "FY $year-${(year + 1) % 100}"
            } else {
                "FY ${year - 1}-${year % 100}"
            }
        }
    }
}
