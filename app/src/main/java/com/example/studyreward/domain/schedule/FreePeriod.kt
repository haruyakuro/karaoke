package com.example.studyreward.domain.schedule

data class FreePeriod(
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int
)