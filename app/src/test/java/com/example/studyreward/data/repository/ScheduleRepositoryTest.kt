package com.example.studyreward.data.repository

import com.example.studyreward.data.dao.ClassScheduleDao
import com.example.studyreward.data.entity.ClassScheduleEntity
import com.example.studyreward.domain.schedule.FreePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

private class FakeClassScheduleDao : ClassScheduleDao {
    private val items = mutableListOf<ClassScheduleEntity>()
    private var nextId = 1L

    override fun getAll(): Flow<List<ClassScheduleEntity>> =
        flowOf(items.sortedWith(compareBy({ it.dayOfWeek }, { it.startMinutes })))

    override suspend fun getByDay(dayOfWeek: Int): List<ClassScheduleEntity> =
        items.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startMinutes }

    override suspend fun insert(schedule: ClassScheduleEntity): Long {
        val id = nextId++
        items.add(schedule.copy(id = id))
        return id
    }

    override suspend fun deleteById(id: Long) {
        items.removeAll { it.id == id }
    }
}

class ScheduleRepositoryTest {

    private lateinit var repo: ScheduleRepository

    private fun cls(title: String, start: Int, end: Int, day: Int = 1) =
        ClassScheduleEntity(title = title, dayOfWeek = day, startMinutes = start, endMinutes = end)

    @Before
    fun setUp() {
        repo = ScheduleRepository(FakeClassScheduleDao())
    }

    @Test
    fun freePeriod_betweenSecondAndThirdClass() = runBlocking {
        repo.addSchedule(cls("1限", 9 * 60, 10 * 60 + 30))
        repo.addSchedule(cls("2限", 10 * 60 + 40, 12 * 60 + 10))
        repo.addSchedule(cls("3限", 13 * 60, 14 * 60 + 30))

        val result = repo.getFreePeriods(1)

        assertEquals(listOf(FreePeriod(1, 12 * 60 + 10, 13 * 60)), result)
    }

    @Test
    fun gapOfExactly30Minutes_isFreePeriod() = runBlocking {
        repo.addSchedule(cls("A", 540, 600))
        repo.addSchedule(cls("B", 630, 700))

        assertEquals(1, repo.getFreePeriods(1).size)
    }

    @Test
    fun gapOf29Minutes_isNotFreePeriod() = runBlocking {
        repo.addSchedule(cls("A", 540, 600))
        repo.addSchedule(cls("B", 629, 700))

        assertEquals(0, repo.getFreePeriods(1).size)
    }

    @Test
    fun singleClass_hasNoFreePeriod() = runBlocking {
        repo.addSchedule(cls("A", 540, 600))

        assertEquals(0, repo.getFreePeriods(1).size)
    }

    @Test
    fun overlappingClass_isRejected() {
        runBlocking { repo.addSchedule(cls("A", 540, 630)) }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.addSchedule(cls("B", 600, 700)) }
        }
    }

    @Test
    fun sameTimeOnDifferentDay_isAllowed() = runBlocking {
        repo.addSchedule(cls("A", 540, 630, day = 1))
        repo.addSchedule(cls("B", 540, 630, day = 2))

        assertEquals(0, repo.getFreePeriods(1).size)
    }

    @Test
    fun startAfterEnd_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.addSchedule(cls("A", 700, 600)) }
        }
    }
}