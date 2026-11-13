package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,             // YYYY-MM-DD
    val sleepTime: String = "",   // HH:mm
    val wakeTime: String = "",    // HH:mm
    val durationMinutes: Int = 0, // Auto calculated
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "asset_accounts")
data class AssetAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // CASH, BANK, INVESTMENTS, TRADING_HOLDINGS, OTHER_ASSETS
    val quantity: Double = 1.0,
    val purchasePrice: Double = 0.0,
    val cmv: Double = 0.0, // Manual Current Market Value ONLY
    val purchaseDate: String = "",
    val valuationDate: String = "", // Date of latest manual CMV update
    val description: String = "",
    val balance: Double = 0.0, // Used for Cash and Bank; negative balances supported!
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalCost: Double
        get() = if (category == "CASH" || category == "BANK") balance else quantity * purchasePrice

    val currentTotalValue: Double
        get() = if (category == "CASH" || category == "BANK") balance else quantity * cmv

    val unrealisedProfitLoss: Double
        get() = if (category == "CASH" || category == "BANK") 0.0 else (cmv - purchasePrice) * quantity
}

@Entity(tableName = "liability_accounts")
data class LiabilityAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // LOAN, BORROWING, CREDIT_CARD, OTHER
    val outstandingAmount: Double = 0.0,
    val interestRate: Double = 0.0,
    val dueDate: String = "",
    val repaymentDetails: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reserve_accounts")
data class ReserveAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reserve_transactions")
data class ReserveTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reserveId: Long,
    val date: String,
    val amount: Double,
    val type: String, // PROFIT_TRANSFER, ADDITION, UTILISATION, REDUCTION
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_transactions")
data class FinancialTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val type: String, // INCOME, EXPENSE
    val category: String,
    val subcategory: String = "",
    val amount: Double,
    val paymentMethod: String = "Cash", // Cash, Bank Account, etc.
    val sourceOrPayee: String = "",
    val description: String = "",
    val isRecurring: Boolean = false,
    val linkedAssetId: Long? = null,
    val linkedTradeId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_records")
data class TradeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: String = "",
    val instrumentName: String,
    val tradingType: String, // INTRADAY, DELIVERY, FUTURES, OPTIONS
    val transactionType: String, // BUY, SELL
    val quantity: Int,
    val price: Double,
    val charges: Double = 0.0,
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val grossProfitLoss: Double = 0.0,
    val netProfitLoss: Double = 0.0,
    val isShortSale: Boolean = false,
    val isClosed: Boolean = false,
    val openQuantity: Int = 0,
    val linkedFinancialTxId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val encryptedTitle: String,
    val encryptedContent: String,
    val ivBase64: String,
    val mood: String = "Neutral", // Happy, Peaceful, Productive, Neutral, Stressed, Low, Excited
    val tags: String = "", // Comma-separated
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_items")
data class ActivityItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "General",
    val colorHex: String = "#2563EB",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_sessions")
data class ActivitySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val activityName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "planned_tasks")
data class PlannedTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val title: String,
    val startTime: String = "", // HH:mm
    val endTime: String = "",   // HH:mm
    val priority: String = "Medium", // High, Medium, Low
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "screen_time_records")
data class ScreenTimeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val packageName: String,
    val appName: String,
    val category: String, // Social, Entertainment, Productivity, Education, Other
    val durationMinutes: Long,
    val isManual: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
