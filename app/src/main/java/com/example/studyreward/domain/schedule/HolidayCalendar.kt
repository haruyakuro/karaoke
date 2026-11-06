package com.example.studyreward.domain.schedule

import java.time.DayOfWeek
import java.time.LocalDate

object HolidayCalendar {
    /**
     * 土日以外の休日(祝日など)。必要な日付を追加する。
     * 例: LocalDate.of(2026, 11, 23)  // 勤労感謝の日
     */
    private val extraOffDays: Set<LocalDate> = emptySet()

    fun isOffDay(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY ||
                date.dayOfWeek == DayOfWeek.SUNDAY ||
                date in extraOffDays
}