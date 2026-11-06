package com.example.studyreward.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.studyreward.StudyRewardApplication
import kotlinx.coroutines.launch

class UsageTestActivity : ComponentActivity() {

    private lateinit var usageText: TextView

    private val usageLimitManager by lazy {
        UsageLimitManager(this)
    }

    private val youtubePackage =
        "com.google.android.youtube"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usageText = TextView(this).apply {
            text = "YouTube使用時間：未取得"
            textSize = 20f
        }

        val registerButton = Button(this).apply {
            text = "YouTubeを5分制限で登録"

            setOnClickListener {
                registerYouTube()
            }
        }

        val refreshButton = Button(this).apply {
            text = "使用時間を取得"

            setOnClickListener {
                updateUsage()
            }
        }

        val foregroundButton = Button(this).apply {
            text = "前面アプリを確認"

            setOnClickListener {
                checkForegroundApp()
            }
        }

        val homeButton = Button(this).apply {
            text = "YouTubeを制限してホームへ"

            setOnClickListener {
                goToHome()
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)

            addView(usageText)
            addView(registerButton)
            addView(refreshButton)
            addView(foregroundButton)
            addView(homeButton)
        }

        setContentView(layout)
    }

    private fun registerYouTube() {

        val container =
            (applicationContext as StudyRewardApplication)
                .container

        lifecycleScope.launch {

            container
                .appLimitRepository
                .registerApp(
                    packageName = youtubePackage,
                    dailyLimitMinutes = 5
                )

            usageText.text =
                "YouTubeを5分制限で登録しました"
        }
    }

    private fun updateUsage() {

        val usageMillis =
            usageLimitManager.getTodayUsageMillis(
                youtubePackage
            )

        val usageMinutes =
            usageMillis / 1000 / 60

        val usageSeconds =
            usageMillis / 1000 % 60

        lifecycleScope.launch {

            val remainingSeconds =
                usageLimitManager
                    .getRemainingUsageSeconds(
                        youtubePackage
                    )

            val exceeded =
                usageLimitManager
                    .isLimitExceeded(
                        youtubePackage
                    )

            usageText.text =
                if (exceeded) {
                    "今日のYouTube使用時間：${usageMinutes}分${usageSeconds}秒\n" +
                            "残り時間：0秒\n" +
                            "制限時間を超えています！"
                } else {
                    "今日のYouTube使用時間：${usageMinutes}分${usageSeconds}秒\n" +
                            "残り時間：${remainingSeconds}秒\n" +
                            "まだ制限時間内です"
                }
        }
    }

    private fun checkForegroundApp() {

        val usageStatsManager =
            getSystemService(
                USAGE_STATS_SERVICE
            ) as UsageStatsManager

        val endTime =
            System.currentTimeMillis()

        val startTime =
            endTime - 10_000

        val events =
            usageStatsManager.queryEvents(
                startTime,
                endTime
            )

        val event =
            UsageEvents.Event()

        var latestPackageName: String? = null
        var latestTimeStamp = 0L

        while (events.hasNextEvent()) {

            events.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED
            ) {

                if (
                    event.timeStamp >
                    latestTimeStamp
                ) {
                    latestTimeStamp =
                        event.timeStamp

                    latestPackageName =
                        event.packageName
                }
            }
        }

        usageText.text =
            if (latestPackageName != null) {
                "最後に前面になったアプリ：\n$latestPackageName"
            } else {
                "前面アプリを取得できませんでした"
            }
    }

    private fun goToHome() {

        val homeIntent =
            Intent(
                Intent.ACTION_MAIN
            ).apply {
                addCategory(
                    Intent.CATEGORY_HOME
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        startActivity(homeIntent)
    }
}