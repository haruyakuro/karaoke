package com.example.studyreward

import android.app.Application
import com.example.studyreward.usage.TargetApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyRewardApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            val repo = container.appLimitRepository
            TargetApps.defaults.forEach { app ->
                // 未登録のときだけ登録する(あとで上限を変えても上書きしない)
                if (repo.getLimit(app.packageName) == null) {
                    repo.registerApp(app.packageName, app.defaultLimitMinutes)
                }
            }
        }
    }
}