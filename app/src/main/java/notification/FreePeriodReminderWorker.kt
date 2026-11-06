
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

class FreePeriodReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val dayOfWeek = inputData.getInt(KEY_DAY_OF_WEEK, 1)
        val startMinutes = inputData.getInt(KEY_START_MINUTES, 0)
        val endMinutes = inputData.getInt(KEY_END_MINUTES, 0)

        val startTime = String.format(
            "%02d:%02d",
            startMinutes / 60,
            startMinutes % 60
        )

        val endTime = String.format(
            "%02d:%02d",
            endMinutes / 60,
            endMinutes % 60
        )

        val channelId = "free_period_reminder_channel"

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "空きコマ通知",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("空きコマが始まります")
            .setContentText(
                "$startTime〜$endTime の空きコマです"
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId = dayOfWeek * 10000 + startMinutes

        NotificationManagerCompat
            .from(applicationContext)
            .notify(notificationId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_DAY_OF_WEEK = "day_of_week"
        const val KEY_START_MINUTES = "start_minutes"
        const val KEY_END_MINUTES = "end_minutes"
    }
}
