package com.example.studyreward.data.repository

import androidx.room.withTransaction
import com.example.studyreward.data.AppDatabase
import com.example.studyreward.data.dao.AppLimitDao
import com.example.studyreward.data.dao.RewardSettingsDao
import com.example.studyreward.domain.reward.ExchangeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RewardRepository(
    private val db: AppDatabase,
    private val rewardSettingsDao: RewardSettingsDao,
    private val appLimitDao: AppLimitDao
) {
    companion object {
        private const val POINTS_UNIT = 10
        private const val MINUTES_PER_UNIT = 5
    }

    fun getPointsBalance(): Flow<Int> =
        rewardSettingsDao.observeBalance().map { it ?: 0 }

    suspend fun exchangePointsForBonus(
        packageName: String,
        pointsToSpend: Int
    ): ExchangeResult = db.withTransaction {
        if (pointsToSpend <= 0 || pointsToSpend % POINTS_UNIT != 0) {
            return@withTransaction ExchangeResult.InvalidAmount
        }
        if (appLimitDao.getByPackage(packageName) == null) {
            return@withTransaction ExchangeResult.AppNotFound
        }
        val balance = rewardSettingsDao.getBalance() ?: 0
        if (balance < pointsToSpend) {
            return@withTransaction ExchangeResult.InsufficientPoints
        }
        val minutes = pointsToSpend / POINTS_UNIT * MINUTES_PER_UNIT
        rewardSettingsDao.subtractPoints(pointsToSpend)
        appLimitDao.addBonusMinutes(packageName, minutes)
        ExchangeResult.Success(minutes)
    }
}