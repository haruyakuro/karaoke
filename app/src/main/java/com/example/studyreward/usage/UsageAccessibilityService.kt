package com.example.studyreward.usage

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.studyreward.StudyRewardApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UsageAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "UsageAccessibility"
    }

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(
            TAG,
            "Service Connected"
        )
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {

        if (
            event?.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val packageName =
            event.packageName?.toString()
                ?: return

        Log.d(
            TAG,
            "Foreground : $packageName"
        )

        serviceScope.launch {

            val container =
                (
                        applicationContext
                                as StudyRewardApplication
                        ).container

            val limit =
                container
                    .appLimitRepository
                    .getLimit(packageName)

            // 登録されていないアプリは何もしない
            if (limit == null) {
                return@launch
            }

            // 無効になっているアプリも何もしない
            if (!limit.isEnabled) {
                Log.d(
                    TAG,
                    "Limit disabled : $packageName"
                )
                return@launch
            }

            Log.d(
                TAG,
                "Limited app detected : $packageName"
            )

            val usageLimitManager =
                UsageLimitManager(
                    applicationContext
                )

            val remainingSeconds =
                usageLimitManager
                    .getRemainingUsageSeconds(
                        packageName
                    )

            Log.d(
                TAG,
                "Remaining seconds : $remainingSeconds"
            )

            if (remainingSeconds <= 0L) {

                Log.d(
                    TAG,
                    "Limit exceeded : $packageName"
                )

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
    }

    override fun onInterrupt() {

        Log.d(
            TAG,
            "AccessibilityService interrupted"
        )
    }

    override fun onDestroy() {

        serviceScope.cancel()

        super.onDestroy()
    }
}