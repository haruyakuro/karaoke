package com.example.studyreward

import android.content.Context
import com.example.studyreward.data.AppDatabase
import com.example.studyreward.data.repository.AppLimitRepository
import com.example.studyreward.data.repository.RewardRepository
import com.example.studyreward.data.repository.ScheduleRepository
import com.example.studyreward.data.repository.TaskRepository
import com.example.studyreward.data.repository.DateScheduleRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.getInstance(context)

    val taskRepository: TaskRepository by lazy {
        TaskRepository(db, db.taskDao(), db.rewardSettingsDao())
    }
    val rewardRepository: RewardRepository by lazy {
        RewardRepository(db, db.rewardSettingsDao(), db.appLimitDao())
    }
    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(db.classScheduleDao())
    }
    val appLimitRepository: AppLimitRepository by lazy {
        AppLimitRepository(db, db.appLimitDao())
    }
    val dateScheduleRepository: DateScheduleRepository by lazy {
        DateScheduleRepository(db.classScheduleDao(), db.dateScheduleDao())
    }
}