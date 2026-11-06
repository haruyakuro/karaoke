package com.example.studyreward.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "date_schedules")
data class DateScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val dateEpochDay: Long,
    val startMinutes: Int,
    val endMinutes: Int
)