package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_records ORDER BY date DESC, id DESC")
    fun getAllRecords(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records WHERE date = :date ORDER BY id ASC")
    fun getRecordsForDate(date: String): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SleepRecord): Long

    @Update
    suspend fun updateRecord(record: SleepRecord)

    @Delete
    suspend fun deleteRecord(record: SleepRecord)

    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM financial_transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<FinancialTransaction>>

    @Query("SELECT * FROM financial_transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, id ASC")
    fun getTransactionsBetween(startDate: String, endDate: String): Flow<List<FinancialTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: FinancialTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: FinancialTransaction)

    @Query("DELETE FROM financial_transactions WHERE linkedTradeId = :tradeId")
    suspend fun deleteByLinkedTradeId(tradeId: Long)

    // Assets
    @Query("SELECT * FROM asset_accounts ORDER BY category ASC, name ASC")
    fun getAllAssets(): Flow<List<AssetAccount>>

    @Query("SELECT * FROM asset_accounts WHERE id = :id")
    suspend fun getAssetById(id: Long): AssetAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetAccount): Long

    @Update
    suspend fun updateAsset(asset: AssetAccount)

    @Delete
    suspend fun deleteAsset(asset: AssetAccount)

    // Liabilities
    @Query("SELECT * FROM liability_accounts ORDER BY outstandingAmount DESC")
    fun getAllLiabilities(): Flow<List<LiabilityAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiability(liability: LiabilityAccount): Long

    @Update
    suspend fun updateLiability(liability: LiabilityAccount)

    @Delete
    suspend fun deleteLiability(liability: LiabilityAccount)

    // Reserves
    @Query("SELECT * FROM reserve_accounts ORDER BY name ASC")
    fun getAllReserves(): Flow<List<ReserveAccount>>

    @Query("SELECT * FROM reserve_accounts WHERE id = :id")
    suspend fun getReserveById(id: Long): ReserveAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReserve(reserve: ReserveAccount): Long

    @Update
    suspend fun updateReserve(reserve: ReserveAccount)

    @Delete
    suspend fun deleteReserve(reserve: ReserveAccount)

    @Query("SELECT * FROM reserve_transactions WHERE reserveId = :reserveId ORDER BY date DESC, id DESC")
    fun getReserveTransactions(reserveId: Long): Flow<List<ReserveTransaction>>

    @Query("SELECT * FROM reserve_transactions ORDER BY date DESC, id DESC")
    fun getAllReserveTransactions(): Flow<List<ReserveTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReserveTransaction(tx: ReserveTransaction): Long

    @Delete
    suspend fun deleteReserveTransaction(tx: ReserveTransaction)
}

@Dao
interface TradingDao {
    @Query("SELECT * FROM trade_records ORDER BY date DESC, time DESC, id DESC")
    fun getAllTrades(): Flow<List<TradeRecord>>

    @Query("SELECT * FROM trade_records WHERE id = :id")
    suspend fun getTradeById(id: Long): TradeRecord?

    @Query("SELECT * FROM trade_records WHERE isShortSale = 1 AND isClosed = 0 ORDER BY date ASC")
    fun getOpenShortTrades(): Flow<List<TradeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeRecord): Long

    @Update
    suspend fun updateTrade(trade: TradeRecord)

    @Delete
    suspend fun deleteTrade(trade: TradeRecord)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY date DESC, time DESC, id DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_items ORDER BY name ASC")
    fun getAllActivities(): Flow<List<ActivityItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(item: ActivityItem): Long

    @Update
    suspend fun updateActivity(item: ActivityItem)

    @Delete
    suspend fun deleteActivity(item: ActivityItem)

    @Query("SELECT * FROM activity_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ActivitySession>>

    @Query("SELECT * FROM activity_sessions WHERE activityId = :activityId ORDER BY startTime DESC")
    fun getSessionsForActivity(activityId: Long): Flow<List<ActivitySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ActivitySession): Long

    @Update
    suspend fun updateSession(session: ActivitySession)

    @Delete
    suspend fun deleteSession(session: ActivitySession)

    @Query("DELETE FROM activity_sessions WHERE activityId = :activityId")
    suspend fun deleteSessionsForActivity(activityId: Long)
}

@Dao
interface PlannerDao {
    @Query("SELECT * FROM planned_tasks ORDER BY date ASC, startTime ASC")
    fun getAllTasks(): Flow<List<PlannedTask>>

    @Query("SELECT * FROM planned_tasks WHERE date = :date ORDER BY startTime ASC, id ASC")
    fun getTasksForDate(date: String): Flow<List<PlannedTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannedTask): Long

    @Update
    suspend fun updateTask(task: PlannedTask)

    @Delete
    suspend fun deleteTask(task: PlannedTask)
}

@Dao
interface ScreenTimeDao {
    @Query("SELECT * FROM screen_time_records ORDER BY date DESC, durationMinutes DESC")
    fun getAllRecords(): Flow<List<ScreenTimeRecord>>

    @Query("SELECT * FROM screen_time_records WHERE date = :date ORDER BY durationMinutes DESC")
    fun getRecordsForDate(date: String): Flow<List<ScreenTimeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScreenTimeRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ScreenTimeRecord>)

    @Update
    suspend fun updateRecord(record: ScreenTimeRecord)

    @Delete
    suspend fun deleteRecord(record: ScreenTimeRecord)

    @Query("DELETE FROM screen_time_records WHERE date = :date AND isManual = 0")
    suspend fun deleteAutomaticForDate(date: String)
}
