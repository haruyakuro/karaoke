package com.example.studyreward.data.repository

import com.example.studyreward.data.dao.ClassScheduleDao
import com.example.studyreward.data.dao.DateScheduleDao
import com.example.studyreward.data.entity.DateScheduleEntity
import com.example.studyreward.domain.schedule.FreePeriod
import com.example.studyreward.domain.schedule.HolidayCalendar
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DateScheduleRepository(
    private val classScheduleDao: ClassScheduleDao,
    private val dateScheduleDao: DateScheduleDao
) {
    companion object {
        private const val MIN_FREE_MINUTES = 30
        const val OFF_DAY_START_MINUTES = 8 * 60
        const val OFF_DAY_END_MINUTES = 22 * 60
    }

    fun getAllDateSchedules(): Flow<List<DateScheduleEntity>> = dateScheduleDao.getAll()

    suspend fun addDateSchedule(schedule: DateScheduleEntity): Long {
        require(schedule.startMinutes in 0 until 24 * 60) { "開始時刻が不正です" }
        require(schedule.endMinutes in 1..24 * 60) { "終了時刻が不正です" }
        require(schedule.startMinutes < schedule.endMinutes) {
            "終了時刻は開始時刻より後にしてください"
        }
        val overlaps = intervalsOf(schedule.dateEpochDay).any { (start, end) ->
            schedule.startMinutes < end && start < schedule.endMinutes
        }
        require(!overlaps) { "ほかの予定と時間が重なっています" }
        return dateScheduleDao.insert(schedule)
    }

    suspend fun deleteDateSchedule(id: Long) {
        dateScheduleDao.deleteById(id)
    }

    /** その日付の空きコマ。毎週の予定と日付指定の予定を合わせて計算する。 */
    suspend fun getFreePeriodsForDate(dateEpochDay: Long): List<FreePeriod> {
        val date = LocalDate.ofEpochDay(dateEpochDay)
        val intervals = intervalsOf(dateEpochDay)

        val gaps = if (HolidayCalendar.isOffDay(date)) {
            gapsIn(intervals, OFF_DAY_START_MINUTES, OFF_DAY_END_MINUTES)
        } else if (intervals.isEmpty()) {
            emptyList()
        } else {
            gapsIn(intervals, intervals.minOf { it.first }, intervals.maxOf { it.second })
        }

        return gaps.map { (start, end) ->
            FreePeriod(date.dayOfWeek.value, start, end, dateEpochDay)
        }
    }

    /** その日の予定(毎週の予定 + 日付指定の予定)を、開始時刻順の (開始, 終了) で返す。 */
    private suspend fun intervalsOf(dateEpochDay: Long): List<Pair<Int, Int>> {
        val dayOfWeek = LocalDate.ofEpochDay(dateEpochDay).dayOfWeek.value
        val weekly = classScheduleDao.getByDay(dayOfWeek).map { it.startMinutes to it.endMinutes }
        val dated = dateScheduleDao.getByDate(dateEpochDay).map { it.startMinutes to it.endMinutes }
        return (weekly + dated).sortedBy { it.first }
    }

    private fun gapsIn(
        intervals: List<Pair<Int, Int>>,
        windowStart: Int,
        windowEnd: Int
    ): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var cursor = windowStart
        for ((rawStart, rawEnd) in intervals) {
            val start = rawStart.coerceIn(windowStart, windowEnd)
            val end = rawEnd.coerceIn(windowStart, windowEnd)
            if (start - cursor >= MIN_FREE_MINUTES) result.add(cursor to start)
            cursor = maxOf(cursor, end)
        }
        if (windowEnd - cursor >= MIN_FREE_MINUTES) result.add(cursor to windowEnd)
        return result
    }
}
