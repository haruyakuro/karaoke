package com.example.studyreward.data.repository

import androidx.room.withTransaction
import com.example.studyreward.data.AppDatabase
import com.example.studyreward.data.dao.AppLimitDao
import com.example.studyreward.data.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AppLimitRepository(
    private val db: AppDatabase,
    private val appLimitDao: AppLimitDao
) {
    companion object {
        /**
         * 日付が変わるときに翌日へ持ち越す追加時間(分)を計算する。
         * 基本上限を先に使い、超えた分から追加時間を消費したとみなす。
         * 「持ち越さず0にする」場合は、この関数を常に 0 を返すようにする。
         */
        fun carriedBonusMinutes(limit: AppLimitEntity): Int {
            val baseSeconds = limit.dailyLimitMinutes * 60L
            val bonusSeconds = limit.bonusMinutes * 60L
            val consumed = (limit.usedSecondsToday - baseSeconds).coerceIn(0L, bonusSeconds)
            return ((bonusSeconds - consumed) / 60).toInt()
        }

        private fun today(): Long = LocalDate.now().toEpochDay()
    }

    fun observeAllLimits(): Flow<List<AppLimitEntity>> = appLimitDao.observeAll()

    suspend fun getLimit(packageName: String): AppLimitEntity? =
        appLimitDao.getByPackage(packageName)

    /** 対象アプリを登録する。登録済みなら基本上限だけ更新し、追加時間や使用時間は保持する。 */
    suspend fun registerApp(
        packageName: String,
        dailyLimitMinutes: Int,
        todayEpochDay: Long = today()
    ) {
        require(packageName.isNotBlank()) { "パッケージ名が空です" }
        require(dailyLimitMinutes > 0) { "上限は1分以上にしてください" }
        val existing = appLimitDao.getByPackage(packageName)
        val newLimit = existing?.copy(dailyLimitMinutes = dailyLimitMinutes)
            ?: AppLimitEntity(
                packageName = packageName,
                dailyLimitMinutes = dailyLimitMinutes,
                usageDateEpochDay = todayEpochDay
            )
        appLimitDao.upsert(newLimit)
    }

    /** 当日の使用秒数を、その値に置き換える(UsageStatsManagerの累計値向け)。未登録なら false。 */
    suspend fun setUsedSeconds(
        packageName: String,
        usedSeconds: Long,
        todayEpochDay: Long = today()
    ): Boolean {
        require(usedSeconds >= 0) { "使用秒数は0以上にしてください" }
        return db.withTransaction {
            val limit = appLimitDao.getByPackage(packageName) ?: return@withTransaction false
            resetIfNewDay(limit, todayEpochDay)
            appLimitDao.setUsedSeconds(packageName, usedSeconds)
            true
        }
    }

    /** 当日の使用秒数に加算する(増えた分を数える場合向け)。未登録なら false。 */
    suspend fun addUsedSeconds(
        packageName: String,
        seconds: Long,
        todayEpochDay: Long = today()
    ): Boolean {
        require(seconds >= 0) { "加算する秒数は0以上にしてください" }
        return db.withTransaction {
            val limit = appLimitDao.getByPackage(packageName) ?: return@withTransaction false
            resetIfNewDay(limit, todayEpochDay)
            appLimitDao.addUsedSeconds(packageName, seconds)
            true
        }
    }

    /** 日付が変わっていれば、使用秒数を0にする。未登録なら false。 */
    suspend fun resetUsageForNewDay(packageName: String, epochDay: Long = today()): Boolean =
        db.withTransaction {
            val limit = appLimitDao.getByPackage(packageName) ?: return@withTransaction false
            resetIfNewDay(limit, epochDay)
            true
        }

    /** 登録済みの全アプリについて、日付が変わっていればリセットする。 */
    suspend fun resetAllIfNewDay(todayEpochDay: Long = today()) {
        db.withTransaction {
            appLimitDao.getAll().forEach { resetIfNewDay(it, todayEpochDay) }
        }
    }

    private suspend fun resetIfNewDay(limit: AppLimitEntity, todayEpochDay: Long) {
        if (limit.usageDateEpochDay == todayEpochDay) return
        appLimitDao.resetForNewDay(
            packageName = limit.packageName,
            epochDay = todayEpochDay,
            bonusMinutes = carriedBonusMinutes(limit)
        )
    }
}