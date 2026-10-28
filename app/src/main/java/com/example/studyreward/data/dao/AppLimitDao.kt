package com.example.studyreward.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.studyreward.data.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits")
    fun observeAll(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAll(): List<AppLimitEntity>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: AppLimitEntity)

    @Query("UPDATE app_limits SET bonusMinutes = bonusMinutes + :minutes WHERE packageName = :packageName")
    suspend fun addBonusMinutes(packageName: String, minutes: Int)

    @Query("UPDATE app_limits SET usedSecondsToday = :seconds WHERE packageName = :packageName")
    suspend fun setUsedSeconds(packageName: String, seconds: Long)

    @Query("UPDATE app_limits SET usedSecondsToday = usedSecondsToday + :seconds WHERE packageName = :packageName")
    suspend fun addUsedSeconds(packageName: String, seconds: Long)

    @Query(
        "UPDATE app_limits SET usedSecondsToday = 0, usageDateEpochDay = :epochDay, " +
                "bonusMinutes = :bonusMinutes WHERE packageName = :packageName"
    )
    suspend fun resetForNewDay(packageName: String, epochDay: Long, bonusMinutes: Int)

    @Query("UPDATE app_limits SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)
}