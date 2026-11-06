package com.example.studyreward.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE)
            ?: "課題"

        val reminderType = inputData.getString(KEY_REMINDER_TYPE)
            ?: TYPE_DUE

        val channelId = "task_reminder_channel"

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "課題の締切通知",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val message = when (reminderType) {
            TYPE_DAY_BEFORE -> "明日が締切です。早めに取り組もう！"
            else -> "今日が締切です。課題を確認しよう！"
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(taskTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId =
            inputData.getLong(KEY_TASK_ID, 0L).toInt() * 2 +
                    if (reminderType == TYPE_DAY_BEFORE) 0 else 1

        NotificationManagerCompat
            .from(applicationContext)
            .notify(notificationId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_REMINDER_TYPE = "reminder_type"

        const val TYPE_DAY_BEFORE = "day_before"
        const val TYPE_DUE = "due"
    }
}
