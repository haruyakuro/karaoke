package com.example.studyreward.data.repository

import com.example.studyreward.data.entity.AppLimitEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLimitCarryOverTest {

    private fun limit(used: Long, bonus: Int = 10, base: Int = 30) = AppLimitEntity(
        packageName = "a",
        dailyLimitMinutes = base,
        bonusMinutes = bonus,
        usedSecondsToday = used,
        usageDateEpochDay = 0L
    )

    @Test
    fun baseNotExceeded_carriesAllBonus() {
        assertEquals(10, AppLimitRepository.carriedBonusMinutes(limit(used = 20 * 60L)))
    }

    @Test
    fun partOfBonusUsed_carriesRemainder() {
        // 基本30分 + 35分使用 → 追加5分を消費、5分が残る
        assertEquals(5, AppLimitRepository.carriedBonusMinutes(limit(used = 35 * 60L)))
    }

    @Test
    fun allBonusUsed_carriesNothing() {
        assertEquals(0, AppLimitRepository.carriedBonusMinutes(limit(used = 40 * 60L)))
        assertEquals(0, AppLimitRepository.carriedBonusMinutes(limit(used = 90 * 60L)))
    }
}