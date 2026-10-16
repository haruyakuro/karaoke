package com.example.studyreward.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val deadlineAtMillis: Long?,
    val difficulty: Int,
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null,
    val awardedPoints: Int = 0
)