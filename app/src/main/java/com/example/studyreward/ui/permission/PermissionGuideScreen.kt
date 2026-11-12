package com.example.studyreward.ui.permission

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonButton
import com.example.studyreward.ui.common.NeonPanel
import com.example.studyreward.ui.common.NeonTopBar
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.usage.UsageAccessibilityService
import kotlinx.coroutines.delay

/**
 * 利用制限に必要な2つの権限の案内画面。
 *  - 使用状況へのアクセス: 他アプリの使用時間を測る(UsageStatsManager)
 *  - アクセシビリティ: 対象アプリが開かれたことを検知し、上限超過ならホームへ戻す(UsageAccessibilityService)
 * どちらもアプリ内から許可できないので、設定画面へ飛ばす。設定から戻ってきたら状態が更新される。
 */
@Composable
fun PermissionGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var usageGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityEnabled(context)) }

    // 設定画面から戻ってきたときに表示を更新する(1秒ごとに確認。軽い処理)
    LaunchedEffect(Unit) {
        while (true) {
            usageGranted = hasUsageAccess(context)
            accessibilityGranted = isAccessibilityEnabled(context)
            delay(1000)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { NeonTopBar("権限の設定", onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "アプリの利用制限を使うには、次の2つを有効にしてください。",
                color = Neon.TextMain,
                fontSize = 15.sp,
            )

            PermissionCard(
                title = "使用状況へのアクセス",
                granted = usageGranted,
                description = "対象アプリ(YouTubeなど)をどれだけ使ったかを調べるために必要です。",
                steps = "設定画面で「StudyReward」を選び、「使用状況へのアクセスを許可」をオンにします。",
                onOpenSettings = { openUsageAccessSettings(context) },
            )

            PermissionCard(
                title = "アクセシビリティ",
                granted = accessibilityGranted,
                description = "対象アプリが開かれたことを検知し、上限を超えていたらホーム画面に戻すために必要です。",
                steps = "設定画面の「インストール済みのアプリ」などから「StudyReward」を選び、オンにします。",
                note = "オンにできない(灰色になっている)ときは、端末の「アプリ情報」→ このアプリ → 右上の「︙」から" +
                        "「制限付き設定を許可」を選んでから、もう一度試してください。",
                onOpenSettings = { openSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    description: String,
    steps: String,
    onOpenSettings: () -> Unit,
    note: String? = null,
) {
    NeonPanel {
        SectionLabel(if (granted) "許可済み" else "未設定")
        Text(
            title,
            color = if (granted) Neon.Accent else Neon.TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(description, color = Neon.TextMain, fontSize = 14.sp)
        Text(steps, color = Neon.TextSub, fontSize = 13.sp)
        note?.let { Text(it, color = Neon.TextSub, fontSize = 12.sp) }
        NeonButton(
            text = if (granted) "設定画面を開く(確認・解除)" else "設定画面を開く",
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// ---- 状態の確認と、設定画面への移動 ----

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= 29) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val ours = ComponentName(context, UsageAccessibilityService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
}

private fun openUsageAccessSettings(context: Context) {
    // Android 10以上は、このアプリの項目を直接開ける。開けない端末では一覧画面にする
    if (Build.VERSION.SDK_INT >= 29) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
            )
            return
        } catch (ignored: ActivityNotFoundException) {
            // 下の一覧画面へ
        } catch (ignored: SecurityException) {
        }
    }
    openSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
}

private fun openSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action))
    } catch (ignored: ActivityNotFoundException) {
        // 専用の画面がない端末では、設定のトップを開く
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}