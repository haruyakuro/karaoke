
package com.example.studyreward.data.repository

import androidx.room.withTransaction
import com.example.studyreward.data.AppDatabase
import com.example.studyreward.data.dao.RewardSettingsDao
import com.example.studyreward.data.dao.TaskDao
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.domain.task.CompleteTaskResult
import com.example.studyreward.domain.task.PointCalculator
import com.example.studyreward.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val db: AppDatabase,
    private val taskDao: TaskDao,
    private val rewardSettingsDao: RewardSettingsDao,
    private val reminderScheduler: ReminderScheduler
) {
    fun getAllTasks(): Flow<List<TaskEntity>> =
        taskDao.getAll()

    suspend fun addTask(task: TaskEntity): Long {
        val taskId = taskDao.insert(task)

        reminderScheduler.scheduleTaskReminder(
            task.copy(id = taskId)
        )

        return taskId
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
        reminderScheduler.scheduleTaskReminder(task)
    }

    suspend fun deleteTask(taskId: Long) {
        reminderScheduler.cancelTaskReminders(taskId)
        taskDao.deleteById(taskId)
    }

    suspend fun completeTask(
        taskId: Long,
        completedAtMillis: Long
    ): CompleteTaskResult {
        val result = db.withTransaction {
            val task = taskDao.getById(taskId)
                ?: return@withTransaction CompleteTaskResult.NotFound

            if (task.completedAtMillis != null) {
                return@withTransaction CompleteTaskResult.AlreadyCompleted
            }

            val points = PointCalculator.calculatePoints(
                task,
                completedAtMillis
            )

            taskDao.update(
                task.copy(
                    completedAtMillis = completedAtMillis,
                    awardedPoints = points
                )
            )

            rewardSettingsDao.addPoints(points)

            CompleteTaskResult.Success(points)
        }

        if (result is CompleteTaskResult.Success) {
            reminderScheduler.cancelTaskReminders(taskId)
        }

        return result
    }
}
