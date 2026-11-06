package com.example.studyreward.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.studyreward.data.dao.AppLimitDao
import com.example.studyreward.data.dao.ClassScheduleDao
import com.example.studyreward.data.dao.RewardSettingsDao
import com.example.studyreward.data.dao.TaskDao
import com.example.studyreward.data.entity.AppLimitEntity
import com.example.studyreward.data.entity.ClassScheduleEntity
import com.example.studyreward.data.entity.RewardSettingsEntity
import com.example.studyreward.data.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ClassScheduleEntity::class,
        RewardSettingsEntity::class,
        AppLimitEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun rewardSettingsDao(): RewardSettingsDao
    abstract fun appLimitDao(): AppLimitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_reward.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO reward_settings (id, pointsBalance) VALUES (1, 0)")
                    }
                }).build().also { INSTANCE = it }
            }
    }
}