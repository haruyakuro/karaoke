package com.example.studyreward.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_schedules")
data class ClassScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int
)