package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        SleepRecord::class,
        AssetAccount::class,
        LiabilityAccount::class,
        ReserveAccount::class,
        ReserveTransaction::class,
        FinancialTransaction::class,
        TradeRecord::class,
        DiaryEntry::class,
        ActivityItem::class,
        ActivitySession::class,
        PlannedTask::class,
        ScreenTimeRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun financeDao(): FinanceDao
    abstract fun tradingDao(): TradingDao
    abstract fun diaryDao(): DiaryDao
    abstract fun activityDao(): ActivityDao
    abstract fun plannerDao(): PlannerDao
    abstract fun screenTimeDao(): ScreenTimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnilife_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
