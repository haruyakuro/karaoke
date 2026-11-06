package com.example.studyreward.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.studyreward.StudyRewardApplication
import java.time.LocalDate
import java.time.ZoneId

class UsageLimitManager(
    private val context: Context
) {

    /**
     * 今日の対象アプリの使用時間をミリ秒で取得する。
     */
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

    /**
     * 現在の使用時間をDBに記録する。
     *
     * UsageStatsManagerから取得した累計使用時間を
     * usedSecondsToday に保存する。
     */
    suspend fun recordUsageSeconds(
        packageName: String,
        seconds: Long
    ): Boolean {

        require(seconds >= 0) {
            "使用秒数は0以上にしてください"
        }

        val container =
            (
                    context.applicationContext
                            as StudyRewardApplication
                    ).container

        return container
            .appLimitRepository
            .setUsedSeconds(
                packageName = packageName,
                usedSeconds = seconds
            )
    }

    /**
     * 今日の残り使用可能時間を秒で返す。
     *
     * 残り時間 =
     * (基本制限時間 + ボーナス時間) - 今日の使用時間
     */
    suspend fun getRemainingUsageSeconds(
        packageName: String
    ): Long {

        val container =
            (
                    context.applicationContext
                            as StudyRewardApplication
                    ).container

        val limit =
            container
                .appLimitRepository
                .getLimit(packageName)
                ?: return 0L

        // UsageStatsManagerから現在の使用時間を取得
        val currentUsedSeconds =
            getTodayUsageMillis(packageName) / 1000L

        // DBにも現在の使用時間を保存
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

        return totalAvailableSeconds -
                currentUsedSeconds
    }

    /**
     * 今日の制限時間を超えているか確認する。
     */
    suspend fun isLimitExceeded(
        packageName: String
    ): Boolean {

        return getRemainingUsageSeconds(
            packageName
        ) <= 0L
    }
}