package com.example.studyreward.data.repository

import com.example.studyreward.data.dao.ClassScheduleDao
import com.example.studyreward.data.entity.ClassScheduleEntity
import com.example.studyreward.domain.schedule.FreePeriod
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(
    private val classScheduleDao: ClassScheduleDao
) {
    companion object {
        private const val MIN_FREE_MINUTES = 30
    }

    fun getAllSchedules(): Flow<List<ClassScheduleEntity>> = classScheduleDao.getAll()

    suspend fun addSchedule(schedule: ClassScheduleEntity): Long {
        require(schedule.dayOfWeek in 1..7) { "曜日は1〜7で指定してください" }
        require(schedule.startMinutes in 0 until 24 * 60) { "開始時刻が不正です" }
        require(schedule.endMinutes in 1..24 * 60) { "終了時刻が不正です" }
        require(schedule.startMinutes < schedule.endMinutes) {
            "終了時刻は開始時刻より後にしてください"
        }
        val sameDay = classScheduleDao.getByDay(schedule.dayOfWeek)
        val overlaps = sameDay.any {
            schedule.startMinutes < it.endMinutes && it.startMinutes < schedule.endMinutes
        }
        require(!overlaps) { "ほかの授業と時間が重なっています" }
        return classScheduleDao.insert(schedule)
    }

    suspend fun deleteSchedule(scheduleId: Long) {
        classScheduleDao.deleteById(scheduleId)
    }

    suspend fun getFreePeriods(dayOfWeek: Int): List<FreePeriod> {
        val classes = classScheduleDao.getByDay(dayOfWeek)
        return classes.zipWithNext().mapNotNull { (prev, next) ->
            if (next.startMinutes - prev.endMinutes >= MIN_FREE_MINUTES) {
                FreePeriod(dayOfWeek, prev.endMinutes, next.startMinutes)
            } else null
        }
    }
}