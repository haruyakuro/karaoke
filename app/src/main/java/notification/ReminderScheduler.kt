
package com.example.studyreward.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.domain.schedule.FreePeriod
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import com.example.studyreward.notification.ReminderScheduler

class ReminderScheduler(
    private val context: Context
) {

    /**
     * 課題の締切通知を予約する。
     * 締切前日と締切当日の朝9時に通知する。
     */
    fun scheduleTaskReminder(task: TaskEntity) {
        val workManager = WorkManager.getInstance(context)
        cancelTaskReminders(task.id)

        if (task.completedAtMillis != null) return

        val deadline = task.deadlineAtMillis ?: return
        val now = System.currentTimeMillis()

        if (deadline <= now) return

        // 端末のタイムゾーンで締切日を取得
        val zone = ZoneId.systemDefault()
        val deadlineDate = java.time.Instant.ofEpochMilli(deadline)
            .atZone(zone)
            .toLocalDate()

        // 締切前日の朝9時
        val dayBefore = deadlineDate.minusDays(1)
            .atTime(9, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        if (dayBefore > now) {
            enqueueTaskReminder(
                task = task,
                reminderTime = dayBefore,
                reminderType = TaskReminderWorker.TYPE_DAY_BEFORE,
                uniqueName = dayBeforeWorkName(task.id)
            )
        }

        // 締切当日の朝9時
        val dueDay = deadlineDate
            .atTime(9, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        if (dueDay > now) {
            enqueueTaskReminder(
                task = task,
                reminderTime = dueDay,
                reminderType = TaskReminderWorker.TYPE_DUE,
                uniqueName = dueWorkName(task.id)
            )
        }
    }
    /**
     * 指定した課題の締切通知をすべて取り消す。
     */
    fun cancelTaskReminders(taskId: Long) {
        val workManager = WorkManager.getInstance(context)

        workManager.cancelUniqueWork(dayBeforeWorkName(taskId))
        workManager.cancelUniqueWork(dueWorkName(taskId))
    }

    /**
     * 空きコマの開始時刻に通知を予約する。
     */
    fun scheduleFreePeriodReminder(period: FreePeriod) {
        val now = System.currentTimeMillis()
        val triggerTime = getNextFreePeriodStart(period, now)

        if (triggerTime <= now) {
            return
        }

        val uniqueName = freePeriodWorkName(period, triggerTime)

        val input = Data.Builder()
            .putInt(
                FreePeriodReminderWorker.KEY_DAY_OF_WEEK,
                period.dayOfWeek
            )
            .putInt(
                FreePeriodReminderWorker.KEY_START_MINUTES,
                period.startMinutes
            )
            .putInt(
                FreePeriodReminderWorker.KEY_END_MINUTES,
                period.endMinutes
            )
            .build()

        val periodTag =
            "free_period_${period.dayOfWeek}_${period.startMinutes}_${period.endMinutes}"

        val request =
            OneTimeWorkRequestBuilder<FreePeriodReminderWorker>()
                .setInputData(input)
                .addTag("free_period_day_${period.dayOfWeek}")
                .addTag(periodTag)
                .setInitialDelay(
                    triggerTime - now,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueName,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    /**
     * 指定した空きコマの通知予約を取り消す。
     */
    fun cancelFreePeriodReminder(period: FreePeriod) {
        val periodTag =
            "free_period_${period.dayOfWeek}_${period.startMinutes}_${period.endMinutes}"

        WorkManager.getInstance(context)
            .cancelAllWorkByTag(periodTag)
    }

    fun cancelFreePeriodRemindersForDay(dayOfWeek: Int) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("free_period_day_$dayOfWeek")
    }

private fun freePeriodWorkName(
    period: FreePeriod,
    triggerTime: Long
): String {
    return "free_period_${period.dayOfWeek}_${period.startMinutes}_${period.endMinutes}_$triggerTime"
}

    /**
     * 次に来る指定曜日の空きコマ開始時刻を取得する。
     *
     * 曜日の番号は Java の DayOfWeek に合わせて
     * 1=月曜日、7=日曜日と仮定している。
     */
    private fun getNextFreePeriodStart(
        period: FreePeriod,
        nowMillis: Long
    ): Long {
        require(period.dayOfWeek in 1..7) {
            "曜日は1〜7で指定してください"
        }

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(nowMillis),
            zone
        )

        var target = now.toLocalDate()
            .with(
                java.time.temporal.TemporalAdjusters.nextOrSame(
                    DayOfWeek.of(period.dayOfWeek)
                )
            )
            .atStartOfDay()
            .plusMinutes(period.startMinutes.toLong())

        if (!target.isAfter(now)) {
            target = target.plusWeeks(1)
        }

        return target.atZone(zone).toInstant().toEpochMilli()
    }


    private fun enqueueTaskReminder(
        task: TaskEntity,
        reminderTime: Long,
        reminderType: String,
        uniqueName: String
    ) {
        val delay = reminderTime - System.currentTimeMillis()

        if (delay <= 0L) {
            return
        }

        val input = Data.Builder()
            .putLong(TaskReminderWorker.KEY_TASK_ID, task.id)
            .putString(TaskReminderWorker.KEY_TASK_TITLE, task.title)
            .putString(
                TaskReminderWorker.KEY_REMINDER_TYPE,
                reminderType
            )
            .build()

        val request =
            OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInputData(input)
                .setInitialDelay(
                    delay,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueName,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    private fun dayBeforeWorkName(taskId: Long): String {
        return "task_${taskId}_day_before"
    }

    private fun dueWorkName(taskId: Long): String {
        return "task_${taskId}_due"
    }
}
