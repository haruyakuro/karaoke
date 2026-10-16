package com.example.studyreward.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    val packageName: String,
    val dailyLimitMinutes: Int,
    val bonusMinutes: Int = 0,
    val usedSecondsToday: Long = 0L,
    val usageDateEpochDay: Long,
    val isEnabled: Boolean = true
)