package com.example.studyreward.domain.task

import com.example.studyreward.data.entity.TaskEntity

object PointCalculator {
    private const val EARLY_BONUS_THRESHOLD_MILLIS = 48L * 60 * 60 * 1000
    private const val EARLY_BONUS_POINTS = 10

    fun calculatePoints(task: TaskEntity, completedAtMillis: Long): Int {
        val base = when (task.difficulty) {
            1 -> 10
            2 -> 20
            3 -> 30
            else -> 0
        }
        val deadline = task.deadlineAtMillis ?: return base
        val remaining = deadline - completedAtMillis
        val bonus = if (remaining >= EARLY_BONUS_THRESHOLD_MILLIS) EARLY_BONUS_POINTS else 0
        return base + bonus
    }
}