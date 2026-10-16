package com.example.studyreward.domain.task

sealed interface CompleteTaskResult {
    data class Success(val pointsAwarded: Int) : CompleteTaskResult
    data object AlreadyCompleted : CompleteTaskResult
    data object NotFound : CompleteTaskResult
}