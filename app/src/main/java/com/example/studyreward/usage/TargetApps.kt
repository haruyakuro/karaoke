package com.example.studyreward.usage

data class TargetApp(
    val packageName: String,
    val displayName: String,
    val defaultLimitMinutes: Int
)

object TargetApps {
    val defaults = listOf(
        TargetApp("com.google.android.youtube", "YouTube", 30)
    )
}