package com.example.studyreward.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_settings")
data class RewardSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val pointsBalance: Int = 0
)