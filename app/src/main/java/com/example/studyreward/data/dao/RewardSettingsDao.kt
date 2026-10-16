package com.example.studyreward.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.studyreward.data.entity.RewardSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardSettingsDao {
    @Query("SELECT pointsBalance FROM reward_settings WHERE id = 1")
    fun observeBalance(): Flow<Int?>

    @Query("SELECT pointsBalance FROM reward_settings WHERE id = 1")
    suspend fun getBalance(): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitial(settings: RewardSettingsEntity)

    @Query("UPDATE reward_settings SET pointsBalance = pointsBalance + :points WHERE id = 1")
    suspend fun addPoints(points: Int)

    @Query("UPDATE reward_settings SET pointsBalance = pointsBalance - :points WHERE id = 1")
    suspend fun subtractPoints(points: Int)
}