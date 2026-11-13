package com.example

import com.example.security.DiarySecurityManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun sleepDuration_standardDayCalculation() {
        val sMin = 14 * 60 + 0 // 14:00
        val wMin = 15 * 60 + 30 // 15:30
        val duration = wMin - sMin
        assertEquals(90, duration)
    }

    @Test
    fun sleepDuration_midnightCrossingCalculation() {
        // e.g. 23:00 to 07:00
        val sMin = 23 * 60 // 1380
        val wMin = 7 * 60  // 420
        val duration = (1440 - sMin) + wMin
        assertEquals(480, duration) // 8 hours
    }

    @Test
    fun balanceSheetEquation_holdsWithNegativeBankBalance() {
        // Assets: Cash + Bank (Overdraft -5,000) + Investments 50,000 = 45,000
        val cash = 0.0
        val bankOverdraft = -5000.0
        val investments = 50000.0
        val totalAssets = cash + bankOverdraft + investments // 45,000.0

        // Liabilities: Loan 15,000
        val loans = 15000.0
        val totalLiabilities = loans

        // Net Worth / Capital = Total Assets - Total Liabilities
        val netCapital = totalAssets - totalLiabilities // 30,000.0

        // Balance Sheet Equation: Total Assets = Total Liabilities + Capital
        assertEquals(totalAssets, totalLiabilities + netCapital, 0.001)
    }

    @Test
    fun tradingProfitCalculation_longTrade() {
        val buyPrice = 100.0
        val sellPrice = 125.0
        val quantity = 50
        val charges = 20.0

        val grossPL = (sellPrice - buyPrice) * quantity // (125 - 100) * 50 = 1250.0
        val netPL = grossPL - charges // 1230.0

        assertEquals(1250.0, grossPL, 0.001)
        assertEquals(1230.0, netPL, 0.001)
    }

    @Test
    fun tradingProfitCalculation_intradayShortSell() {
        val shortSellPrice = 150.0
        val buybackPrice = 130.0
        val quantity = 100
        val charges = 40.0

        val grossPL = (shortSellPrice - buybackPrice) * quantity // (150 - 130) * 100 = 2000.0
        val netPL = grossPL - charges // 1960.0

        assertEquals(2000.0, grossPL, 0.001)
        assertEquals(1960.0, netPL, 0.001)
    }
}
