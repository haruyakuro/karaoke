package com.example.studyreward.domain.task

import com.example.studyreward.data.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PointCalculatorTest {

    private val hour = 60L * 60 * 1000
    private val now = 1_000_000_000_000L

    private fun task(difficulty: Int, deadline: Long?) = TaskEntity(
        title = "テスト課題",
        deadlineAtMillis = deadline,
        difficulty = difficulty,
        createdAtMillis = now
    )

    @Test
    fun 締切なしなら基本ポイントのみ() {
        assertEquals(10, PointCalculator.calculatePoints(task(1, null), now))
        assertEquals(20, PointCalculator.calculatePoints(task(2, null), now))
        assertEquals(30, PointCalculator.calculatePoints(task(3, null), now))
    }

    @Test
    fun 締切3日前に完了するとボーナスが付く() {
        val t = task(3, now + 72 * hour)
        assertEquals(40, PointCalculator.calculatePoints(t, now))
    }

    @Test
    fun ちょうど48時間前ならボーナスが付く() {
        val t = task(1, now + 48 * hour)
        assertEquals(20, PointCalculator.calculatePoints(t, now))
    }

    @Test
    fun 残り48時間に1分足りないならボーナスなし() {
        val t = task(1, now + 48 * hour - 60_000)
        assertEquals(10, PointCalculator.calculatePoints(t, now))
    }

    @Test
    fun 締切を過ぎていたらボーナスなし() {
        val t = task(2, now - hour)
        assertEquals(20, PointCalculator.calculatePoints(t, now))
    }
}