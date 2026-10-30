package com.example.studyreward.usage

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UsageAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "UsageAccessibility"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
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

        Log.d(
            TAG,
            "Event received : ${event?.eventType}"
        )

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

        if (packageName != YOUTUBE_PACKAGE) {
            return
        }

        Log.d(
            TAG,
            "YouTube detected"
        )

        val usageLimitManager =
            UsageLimitManager(applicationContext)

        serviceScope.launch {

            val exceeded =
                usageLimitManager.isLimitExceeded(
                    YOUTUBE_PACKAGE
                )

            Log.d(
                TAG,
                "Exceeded = $exceeded"
            )

            if (exceeded) {

                Log.d(
                    TAG,
                    "Go Home"
                )

                val homeIntent =
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
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