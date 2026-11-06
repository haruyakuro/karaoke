package com.example.studyreward.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studyreward.data.entity.DateScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DateScheduleDao {
    @Query("SELECT * FROM date_schedules ORDER BY dateEpochDay, startMinutes")
    fun getAll(): Flow<List<DateScheduleEntity>>

    @Query("SELECT * FROM date_schedules WHERE dateEpochDay = :dateEpochDay ORDER BY startMinutes")
    suspend fun getByDate(dateEpochDay: Long): List<DateScheduleEntity>

    @Insert
    suspend fun insert(schedule: DateScheduleEntity): Long

    @Query("DELETE FROM date_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}