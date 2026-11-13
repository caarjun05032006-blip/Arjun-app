package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    data class ReportData(
        val periodLabel: String,
        val reportType: String, // "OVERALL_SUMMARY", "FINANCE_DETAILED", "TRADING_DETAILED", "SLEEP_DETAILED", "ACTIVITY_DETAILED", "PLANNER_DETAILED", "SECTION_SUMMARY"
        val sleepRecords: List<SleepRecord> = emptyList(),
        val financialTransactions: List<FinancialTransaction> = emptyList(),
        val assets: List<AssetAccount> = emptyList(),
        val liabilities: List<LiabilityAccount> = emptyList(),
        val reserves: List<ReserveAccount> = emptyList(),
        val reserveTransactions: List<ReserveTransaction> = emptyList(),
        val trades: List<TradeRecord> = emptyList(),
        val activities: List<ActivityItem> = emptyList(),
        val activitySessions: List<ActivitySession> = emptyList(),
        val plannedTasks: List<PlannedTask> = emptyList(),
        val screenTimeRecords: List<ScreenTimeRecord> = emptyList(),
        val diaryEntries: List<Pair<DiaryEntry, String>> = emptyList() // Pair of entry and decrypted content
    )

    fun generateAndSharePdf(context: Context, data: ReportData) {
        val file = generatePdfFile(context, data)
        sharePdf(context, file, "OmniLife_${data.reportType}_${System.currentTimeMillis()}.pdf")
    }

    fun generatePdfFile(context: Context, data: ReportData): File {
        val pdfDocument = PdfDocument()
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "OmniLife_${data.reportType}_${System.currentTimeMillis()}.pdf"
        val outputFile = File(reportsDir, fileName)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = MARGIN + 10f

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // Primary Blue
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyBold = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyRegular = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val bgCardPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        fun drawHeaderAndFooter() {
            // Top Accent Bar
            val barPaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, barPaint)

            // Header info
            canvas.drawText("OMNILIFE | PERSONAL LIFE MANAGEMENT REPORT", MARGIN.toFloat(), 28f, titlePaint)
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("Period: ${data.periodLabel}  •  Generated: $dateStr", MARGIN.toFloat(), 42f, subTitlePaint)
            canvas.drawLine(MARGIN.toFloat(), 48f, (PAGE_WIDTH - MARGIN).toFloat(), 48f, dividerPaint)

            // Footer
            val footerText = "OmniLife Personal Accounting & Life OS  •  Page $pageNumber"
            canvas.drawText(footerText, MARGIN.toFloat(), (PAGE_HEIGHT - 18).toFloat(), subTitlePaint)
            canvas.drawLine(MARGIN.toFloat(), (PAGE_HEIGHT - 28).toFloat(), (PAGE_WIDTH - MARGIN).toFloat(), (PAGE_HEIGHT - 28).toFloat(), dividerPaint)
        }

        drawHeaderAndFooter()
        yPos = 65f

        fun checkNewPage(neededHeight: Float) {
            if (yPos + neededHeight > PAGE_HEIGHT - 45) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndFooter()
                yPos = 65f
            }
        }

        fun drawSectionHeader(title: String) {
            checkNewPage(30f)
            canvas.drawRoundRect(
                RectF(MARGIN.toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos + 22f),
                4f, 4f,
                Paint().apply { color = Color.rgb(238, 242, 255); style = Paint.Style.FILL }
            )
            canvas.drawText(title, (MARGIN + 8).toFloat(), yPos + 15f, sectionPaint)
            yPos += 30f
        }

        // Draw Summary Cards if Overall Summary
        if (data.reportType == "OVERALL_SUMMARY" || data.reportType == "SECTION_SUMMARY") {
            drawSectionHeader("EXECUTIVE SUMMARY OVERVIEW")
            checkNewPage(85f)

            // Calculations
            val totalIncome = data.financialTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = data.financialTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val netSavings = totalIncome - totalExpense
            val totalAssets = data.assets.sumOf { it.currentTotalValue }
            val totalLiab = data.liabilities.sumOf { it.outstandingAmount }
            val netWorth = totalAssets - totalLiab
            val totalSleepMinutes = data.sleepRecords.sumOf { it.durationMinutes }
            val avgSleepMinutes = if (data.sleepRecords.isNotEmpty()) totalSleepMinutes / data.sleepRecords.size else 0
            val totalActivitySeconds = data.activitySessions.sumOf { it.durationSeconds }
            val totalScreenMinutes = data.screenTimeRecords.sumOf { it.durationMinutes }
            val tradingNetProfit = data.trades.sumOf { it.netProfitLoss }

            // Card 1: Net Worth & Savings
            canvas.drawRoundRect(RectF(MARGIN.toFloat(), yPos, (MARGIN + 250).toFloat(), yPos + 70f), 6f, 6f, bgCardPaint)
            canvas.drawText("FINANCIAL POSITION", (MARGIN + 12).toFloat(), yPos + 18f, bodyBold)
            canvas.drawText("Total Assets: ₹%,.2f".format(totalAssets), (MARGIN + 12).toFloat(), yPos + 34f, bodyRegular)
            canvas.drawText("Total Liabilities: ₹%,.2f".format(totalLiab), (MARGIN + 12).toFloat(), yPos + 48f, bodyRegular)
            canvas.drawText("Net Worth: ₹%,.2f  |  Savings: ₹%,.2f".format(netWorth, netSavings), (MARGIN + 12).toFloat(), yPos + 62f, bodyBold)

            // Card 2: Life Metrics
            canvas.drawRoundRect(RectF((MARGIN + 260).toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos + 70f), 6f, 6f, bgCardPaint)
            canvas.drawText("TIME & WELLNESS ALLOCATION", (MARGIN + 272).toFloat(), yPos + 18f, bodyBold)
            canvas.drawText("Avg Sleep: %dh %dm (%d sessions)".format(avgSleepMinutes / 60, avgSleepMinutes % 60, data.sleepRecords.size), (MARGIN + 272).toFloat(), yPos + 34f, bodyRegular)
            canvas.drawText("Stopwatch Activity: %dh %dm (%d sessions)".format(totalActivitySeconds / 3600, (totalActivitySeconds % 3600) / 60, data.activitySessions.size), (MARGIN + 272).toFloat(), yPos + 48f, bodyRegular)
            canvas.drawText("Screen Time: %dh %dm  |  Trading P&L: ₹%,.2f".format(totalScreenMinutes / 60, totalScreenMinutes % 60, tradingNetProfit), (MARGIN + 272).toFloat(), yPos + 62f, bodyBold)

            yPos += 82f
        }

        // Section: Balance Sheet & Reserves
        if (data.reportType in listOf("OVERALL_SUMMARY", "FINANCE_DETAILED", "SECTION_SUMMARY")) {
            drawSectionHeader("BALANCE SHEET & CAPITAL SCHEDULE")
            checkNewPage(120f)

            val totalAssets = data.assets.sumOf { it.currentTotalValue }
            val totalLiab = data.liabilities.sumOf { it.outstandingAmount }
            val totalReserves = data.reserves.sumOf { it.currentBalance }
            val netCapital = totalAssets - totalLiab

            canvas.drawText("ASSETS", MARGIN.toFloat(), yPos, bodyBold)
            canvas.drawText("LIABILITIES & CAPITAL", (MARGIN + 270).toFloat(), yPos, bodyBold)
            yPos += 14f

            // Cash and Bank
            val cashBank = data.assets.filter { it.category in listOf("CASH", "BANK") }
            val investments = data.assets.filter { it.category !in listOf("CASH", "BANK") }

            canvas.drawText("• Cash & Bank Balances: ₹%,.2f".format(cashBank.sumOf { it.currentTotalValue }), (MARGIN + 8).toFloat(), yPos, bodyRegular)
            canvas.drawText("• Loans & Borrowings: ₹%,.2f".format(data.liabilities.filter { it.category in listOf("LOAN", "BORROWING") }.sumOf { it.outstandingAmount }), (MARGIN + 278).toFloat(), yPos, bodyRegular)
            yPos += 13f

            canvas.drawText("• Investments & Holdings (CMV): ₹%,.2f".format(investments.sumOf { it.currentTotalValue }), (MARGIN + 8).toFloat(), yPos, bodyRegular)
            canvas.drawText("• Credit Cards & Other: ₹%,.2f".format(data.liabilities.filter { it.category !in listOf("LOAN", "BORROWING") }.sumOf { it.outstandingAmount }), (MARGIN + 278).toFloat(), yPos, bodyRegular)
            yPos += 13f

            canvas.drawText("• Manual CMV Unrealised P&L: ₹%,.2f".format(data.assets.sumOf { it.unrealisedProfitLoss }), (MARGIN + 8).toFloat(), yPos, bodyRegular)
            canvas.drawText("• Reserves & Surplus: ₹%,.2f".format(totalReserves), (MARGIN + 278).toFloat(), yPos, bodyRegular)
            yPos += 13f

            canvas.drawText("TOTAL ASSETS: ₹%,.2f".format(totalAssets), MARGIN.toFloat(), yPos, bodyBold)
            canvas.drawText("TOTAL CAPITAL & LIAB: ₹%,.2f".format(totalLiab + netCapital), (MARGIN + 270).toFloat(), yPos, bodyBold)
            yPos += 18f
            canvas.drawLine(MARGIN.toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos, dividerPaint)
            yPos += 12f
        }

        // Section: Trading Journal Summary & Open Shorts
        if (data.reportType in listOf("OVERALL_SUMMARY", "TRADING_DETAILED", "SECTION_SUMMARY")) {
            drawSectionHeader("TRADING JOURNAL & HOLDINGS")
            checkNewPage(80f)

            val closedTrades = data.trades.filter { it.isClosed }
            val openTrades = data.trades.filter { !it.isClosed }
            val grossProfit = data.trades.sumOf { it.grossProfitLoss }
            val totalCharges = data.trades.sumOf { it.charges }
            val netRealisedProfit = data.trades.sumOf { it.netProfitLoss }

            canvas.drawText("Total Trades: %d (Closed: %d, Open: %d)".format(data.trades.size, closedTrades.size, openTrades.size), (MARGIN + 8).toFloat(), yPos, bodyRegular)
            yPos += 13f
            canvas.drawText("Gross Realised P&L: ₹%,.2f  |  Brokerage & Charges: ₹%,.2f".format(grossProfit, totalCharges), (MARGIN + 8).toFloat(), yPos, bodyRegular)
            yPos += 13f
            canvas.drawText("Net Realised Trading Profit/Loss: ₹%,.2f".format(netRealisedProfit), (MARGIN + 8).toFloat(), yPos, bodyBold)
            yPos += 16f

            // Open short positions if any
            val openShorts = data.trades.filter { it.isShortSale && !it.isClosed }
            if (openShorts.isNotEmpty()) {
                canvas.drawText("OPEN INTRADAY SHORT POSITIONS:", (MARGIN + 8).toFloat(), yPos, bodyBold)
                yPos += 13f
                for (sh in openShorts) {
                    canvas.drawText("• ${sh.instrumentName} - Qty: ${sh.openQuantity} @ ₹${sh.price} (Sell Date: ${sh.date})", (MARGIN + 16).toFloat(), yPos, bodyRegular)
                    yPos += 12f
                }
            }
        }

        // Section: Sleep & Wake-up Analysis
        if (data.reportType in listOf("OVERALL_SUMMARY", "SLEEP_DETAILED", "SECTION_SUMMARY")) {
            drawSectionHeader("SLEEP & WAKE-UP TRACKING")
            checkNewPage(60f)

            val totalMins = data.sleepRecords.sumOf { it.durationMinutes }
            val count = data.sleepRecords.size
            val avgMins = if (count > 0) totalMins / count else 0

            canvas.drawText("Recorded Sleep Sessions: %d  |  Total Sleep Duration: %dh %dm".format(count, totalMins / 60, totalMins % 60), (MARGIN + 8).toFloat(), yPos, bodyRegular)
            yPos += 13f
            canvas.drawText("Average Sleep Duration: %dh %dm per session".format(avgMins / 60, avgMins % 60), (MARGIN + 8).toFloat(), yPos, bodyBold)
            yPos += 18f
        }

        // Section: Activity Time Tracker
        if (data.reportType in listOf("OVERALL_SUMMARY", "ACTIVITY_DETAILED", "SECTION_SUMMARY")) {
            drawSectionHeader("ACTIVITY TRACKING (STOPWATCH SESSIONS)")
            checkNewPage(60f)

            if (data.activities.isEmpty()) {
                canvas.drawText("No custom activities created yet.", (MARGIN + 8).toFloat(), yPos, bodyRegular)
                yPos += 15f
            } else {
                for (act in data.activities) {
                    val actSessions = data.activitySessions.filter { it.activityId == act.id }
                    val totalSec = actSessions.sumOf { it.durationSeconds }
                    val hrs = totalSec / 3600
                    val mins = (totalSec % 3600) / 60
                    canvas.drawText("• ${act.name} (${act.category}): %dh %dm across %d session(s)".format(hrs, mins, actSessions.size), (MARGIN + 8).toFloat(), yPos, bodyRegular)
                    yPos += 13f
                }
            }
            yPos += 10f
        }

        // Section: Screen Time
        if (data.reportType in listOf("OVERALL_SUMMARY", "SECTION_SUMMARY")) {
            drawSectionHeader("SCREEN TIME SUMMARY")
            checkNewPage(50f)
            val byCategory = data.screenTimeRecords.groupBy { it.category }
            for ((cat, recs) in byCategory) {
                val catMins = recs.sumOf { it.durationMinutes }
                canvas.drawText("• $cat: %dh %dm".format(catMins / 60, catMins % 60), (MARGIN + 8).toFloat(), yPos, bodyRegular)
                yPos += 12f
            }
            yPos += 10f
        }

        // Section: Planner
        if (data.reportType in listOf("OVERALL_SUMMARY", "PLANNER_DETAILED", "SECTION_SUMMARY")) {
            drawSectionHeader("DAILY PLANNER – PLANNED VS ACTUAL")
            checkNewPage(50f)
            val completed = data.plannedTasks.count { it.isCompleted }
            val total = data.plannedTasks.size
            canvas.drawText("Planned Tasks: $total  |  Completed: $completed  |  Pending: ${total - completed}", (MARGIN + 8).toFloat(), yPos, bodyRegular)
            yPos += 14f
            val pct = if (total > 0) (completed * 100) / total else 0
            canvas.drawText("Task Completion Rate: $pct%", (MARGIN + 8).toFloat(), yPos, bodyBold)
            yPos += 18f
        }

        // If Diary Export is requested (isolated with authentication)
        if (data.diaryEntries.isNotEmpty()) {
            drawSectionHeader("CONFIDENTIAL PERSONAL DIARY ENTRIES")
            for ((entry, content) in data.diaryEntries) {
                checkNewPage(60f)
                canvas.drawText("${entry.date} ${entry.time}  •  Mood: ${entry.mood}  •  Tags: ${entry.tags}", (MARGIN + 8).toFloat(), yPos, bodyBold)
                yPos += 13f
                val lines = content.chunked(70)
                for (line in lines) {
                    checkNewPage(14f)
                    canvas.drawText(line, (MARGIN + 12).toFloat(), yPos, bodyRegular)
                    yPos += 12f
                }
                yPos += 8f
                canvas.drawLine(MARGIN.toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos, dividerPaint)
                yPos += 10f
            }
        }

        pdfDocument.finishPage(page)

        try {
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return outputFile
    }

    private fun sharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share PDF Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
