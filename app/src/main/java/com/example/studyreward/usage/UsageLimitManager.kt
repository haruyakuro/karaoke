package com.example.studyreward.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.studyreward.StudyRewardApplication
import java.time.LocalDate
import java.time.ZoneId

class UsageLimitManager(
    private val context: Context
) {

    fun getTodayUsageMillis(packageName: String): Long {

        val usageStatsManager =
            context.getSystemService(
                Context.USAGE_STATS_SERVICE
            ) as UsageStatsManager

        val zoneId = ZoneId.systemDefault()

        val startOfDay =
            LocalDate.now()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

        val endOfDay =
            System.currentTimeMillis()

        val stats =
            usageStatsManager.queryAndAggregateUsageStats(
                startOfDay,
                endOfDay
            )

        return stats[packageName]
            ?.totalTimeInForeground
            ?: 0L
    }

    suspend fun recordUsageSeconds(
        packageName: String,
        seconds: Long
    ): Boolean {

        require(seconds >= 0) {
            "使用秒数は0以上にしてください"
        }

        val container =
            (context.applicationContext
                    as StudyRewardApplication)
                .container

        return container
            .appLimitRepository
            .setUsedSeconds(
                packageName = packageName,
                usedSeconds = seconds
            )
    }

    suspend fun isLimitTarget(
        packageName: String
    ): Boolean {

        val container =
            (context.applicationContext
                    as StudyRewardApplication)
                .container

        val limit =
            container
                .appLimitRepository
                .getLimit(packageName)

        return limit != null &&
                limit.isEnabled
    }

    suspend fun getRemainingUsageSeconds(
        packageName: String
    ): Long? {

        val container =
            (context.applicationContext
                    as StudyRewardApplication)
                .container

        container
            .appLimitRepository
            .resetUsageForNewDay(
                packageName
            )

        val limit =
            container
                .appLimitRepository
                .getLimit(packageName)
                ?: return null

        if (!limit.isEnabled) {
            return null
        }

        val currentUsedSeconds =
            getTodayUsageMillis(packageName) / 1000L

        container
            .appLimitRepository
            .setUsedSeconds(
                packageName = packageName,
                usedSeconds = currentUsedSeconds
            )

        val totalAvailableSeconds =
            (
                    limit.dailyLimitMinutes +
                            limit.bonusMinutes
                    ) * 60L

        return (
                totalAvailableSeconds -
                        currentUsedSeconds
                ).coerceAtLeast(0L)
    }

    suspend fun isLimitExceeded(
        packageName: String
    ): Boolean {

        if (!isLimitTarget(packageName)) {
            return false
        }

        val remainingSeconds =
            getRemainingUsageSeconds(
                packageName
            ) ?: return false

        return remainingSeconds <= 0L
    }
}