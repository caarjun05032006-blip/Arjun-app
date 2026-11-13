package com.example.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.model.ScreenTimeRecord
import java.util.Calendar

object ScreenTimeHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun getUsageStatsForDay(context: Context, dateStr: String): List<ScreenTimeRecord> {
        if (!hasUsageStatsPermission(context)) return emptyList()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val cal = Calendar.getInstance()
        // Try parsing dateStr (YYYY-MM-DD)
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH))
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val pm = context.packageManager
        val records = mutableListOf<ScreenTimeRecord>()

        for (u in stats) {
            val totalTimeMs = u.totalTimeInForeground
            val minutes = totalTimeMs / (1000 * 60)
            if (minutes > 0) {
                val pkg = u.packageName
                // Skip system launcher or framework if unnecessary
                val appName = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast(".")
                }

                val category = categorizeApp(context, pkg)

                records.add(
                    ScreenTimeRecord(
                        date = dateStr,
                        packageName = pkg,
                        appName = appName,
                        category = category,
                        durationMinutes = minutes,
                        isManual = false
                    )
                )
            }
        }

        return records.sortedByDescending { it.durationMinutes }
    }

    private fun categorizeApp(context: Context, packageName: String): String {
        val lowerPkg = packageName.lowercase()
        if (lowerPkg.contains("whatsapp") || lowerPkg.contains("instagram") ||
            lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("telegram") || lowerPkg.contains("snapchat") ||
            lowerPkg.contains("tiktok") || lowerPkg.contains("reddit") ||
            lowerPkg.contains("linkedin")
        ) {
            return "Social"
        }

        if (lowerPkg.contains("youtube") || lowerPkg.contains("netflix") ||
            lowerPkg.contains("spotify") || lowerPkg.contains("hotstar") ||
            lowerPkg.contains("primevideo") || lowerPkg.contains("twitch") ||
            lowerPkg.contains("game") || lowerPkg.contains("play")
        ) {
            return "Entertainment"
        }

        if (lowerPkg.contains("coursera") || lowerPkg.contains("duolingo") ||
            lowerPkg.contains("khan") || lowerPkg.contains("udemy") ||
            lowerPkg.contains("quiz") || lowerPkg.contains("learn")
        ) {
            return "Education"
        }

        if (lowerPkg.contains("gmail") || lowerPkg.contains("chrome") ||
            lowerPkg.contains("drive") || lowerPkg.contains("docs") ||
            lowerPkg.contains("sheets") || lowerPkg.contains("keep") ||
            lowerPkg.contains("calendar") || lowerPkg.contains("office") ||
            lowerPkg.contains("notion") || lowerPkg.contains("slack")
        ) {
            return "Productivity"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                return when (appInfo.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_GAME -> "Entertainment"
                    ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_NEWS -> "Productivity"
                    else -> "Other"
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return "Other"
    }
}
