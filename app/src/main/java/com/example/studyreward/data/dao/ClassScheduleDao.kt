package com.example.studyreward.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.studyreward.data.entity.ClassScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassScheduleDao {
    @Query("SELECT * FROM class_schedules ORDER BY dayOfWeek, startMinutes")
    fun getAll(): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek ORDER BY startMinutes")
    suspend fun getByDay(dayOfWeek: Int): List<ClassScheduleEntity>

    @Insert
    suspend fun insert(schedule: ClassScheduleEntity): Long

    @Query("SELECT * FROM class_schedules WHERE id = :id")
    suspend fun getById(id: Long): ClassScheduleEntity?

    @Query("DELETE FROM class_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)
}