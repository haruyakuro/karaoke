package com.example.studyreward.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonButton
import com.example.studyreward.ui.common.NeonPanel
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.ui.common.appDisplayName
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val deadlineFormatter = DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.JAPAN)

/**
 * ホームに出す、対象アプリ1つ分の残り時間。
 * @param remainingSeconds 残り秒数。制限対象外なら null(「制限なし」と表示。SPEC.md 7章)
 */
data class AppRemaining(
    val packageName: String,
    val remainingSeconds: Long?,
)

@Composable
fun HomeScreen(
    pointsBalance: Int,
    pendingCount: Int,
    dueSoonCount: Int,
    doneCount: Int,
    nextTaskTitle: String?,
    nextDeadlineMillis: Long?,
    onAddTask: () -> Unit,
    onOpenTaskList: () -> Unit,
    onOpenExchange: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPermission: () -> Unit = {},
    // 空のときは「アプリの残り時間」の欄を出さない
    limitedApps: List<AppRemaining> = emptyList(),
) {
    val total = doneCount + pendingCount
    val progress = if (total == 0) 0f else doneCount.toFloat() / total

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Neon.TextMain)) { append("STUDY") }
                    withStyle(SpanStyle(color = Neon.Accent)) { append("REWARD") }
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )

            PointRing(
                points = pointsBalance,
                exchangeableMinutes = pointsBalance / 10 * 5,
                progress = progress,
            )

            Text(text = "課題 完了 $doneCount / $total", color = Neon.TextSub, fontSize = 13.sp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("未完了", pendingCount)
                Stat("締切間近", dueSoonCount, highlight = dueSoonCount > 0)
                Stat("完了", doneCount)
            }

            NextDeadlineCard(nextTaskTitle, nextDeadlineMillis)

            if (limitedApps.isNotEmpty()) {
                RemainingTimeCard(limitedApps)
            }

            NeonButton(text = "課題を登録", onClick = onAddTask)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SubButton("課題一覧", onOpenTaskList)
                SubButton("ポイント交換", onOpenExchange)
                SubButton("時間割", onOpenSchedule)
                SubButton("権限", onOpenPermission)
            }
        }
    }
}

@Composable
private fun PointRing(points: Int, exchangeableMinutes: Int, progress: Float) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "ring",
    )

    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke * 1.4f // 光の太い線が欠けない余白
            val topLeft = Offset(inset, inset)
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)

            drawArc(
                color = Neon.Track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (animated > 0f) {
                drawArc( // 光のにじみ
                    color = Neon.Accent.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Neon.Accent,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ポイント残高", color = Neon.TextSub, fontSize = 13.sp)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Neon.TextMain)) {
                        append("$points")
                    }
                    withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Neon.Accent)) {
                        append(" pt")
                    }
                },
            )
            Text("交換すると ${exchangeableMinutes}分", color = Neon.TextSub, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Stat(label: String, count: Int, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            color = if (highlight) Neon.Accent else Neon.TextMain,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(label, color = Neon.TextSub, fontSize = 12.sp)
    }
}

@Composable
private fun NextDeadlineCard(title: String?, deadlineMillis: Long?) {
    NeonPanel {
        SectionLabel("次の締切")
        if (title == null || deadlineMillis == null) {
            Text("締切のある課題はありません", color = Neon.TextMain, fontSize = 16.sp)
        } else {
            val dateText = Instant.ofEpochMilli(deadlineMillis)
                .atZone(ZoneId.systemDefault())
                .format(deadlineFormatter)
            Text(title, color = Neon.TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$dateText ・ ${remainingLabel(deadlineMillis, System.currentTimeMillis())}",
                color = Neon.Accent,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun RemainingTimeCard(apps: List<AppRemaining>) {
    NeonPanel {
        SectionLabel("アプリの残り時間(今日)")
        apps.forEach { app ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    appDisplayName(app.packageName),
                    color = Neon.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val seconds = app.remainingSeconds
                Text(
                    text = formatRemainingTime(seconds),
                    color = if (seconds == null) Neon.TextSub else Neon.Accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SubButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = Neon.TextMain, fontSize = 14.sp)
    }
}

private fun remainingLabel(deadlineMillis: Long, nowMillis: Long): String {
    val hours = (deadlineMillis - nowMillis) / 3_600_000L
    return if (hours < 24) "あと${maxOf(hours, 1L)}時間" else "あと${hours / 24}日"
}

// 残り秒数 → 表示用の文字。null=制限なし、0以下=上限到達(負の値は0として扱う)
internal fun formatRemainingTime(remainingSeconds: Long?): String {
    if (remainingSeconds == null) return "制限なし"
    if (remainingSeconds <= 0L) return "上限に達しました"
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    return when {
        hours > 0 -> "あと${hours}時間${minutes}分"
        minutes > 0 -> "あと${minutes}分"
        else -> "あと1分未満"
    }
}