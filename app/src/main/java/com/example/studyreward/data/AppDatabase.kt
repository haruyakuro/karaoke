package com.example.studyreward.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.studyreward.data.dao.AppLimitDao
import com.example.studyreward.data.dao.ClassScheduleDao
import com.example.studyreward.data.dao.DateScheduleDao
import com.example.studyreward.data.dao.RewardSettingsDao
import com.example.studyreward.data.dao.TaskDao
import com.example.studyreward.data.entity.AppLimitEntity
import com.example.studyreward.data.entity.ClassScheduleEntity
import com.example.studyreward.data.entity.DateScheduleEntity
import com.example.studyreward.data.entity.RewardSettingsEntity
import com.example.studyreward.data.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ClassScheduleEntity::class,
        RewardSettingsEntity::class,
        AppLimitEntity::class,
        DateScheduleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun classScheduleDao(): ClassScheduleDao
    abstract fun rewardSettingsDao(): RewardSettingsDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun dateScheduleDao(): DateScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** バージョン1 → 2: 日付指定の予定テーブルを追加する。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `date_schedules` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`dateEpochDay` INTEGER NOT NULL, " +
                            "`startMinutes` INTEGER NOT NULL, " +
                            "`endMinutes` INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_reward.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            db.execSQL("INSERT INTO reward_settings (id, pointsBalance) VALUES (1, 0)")
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}