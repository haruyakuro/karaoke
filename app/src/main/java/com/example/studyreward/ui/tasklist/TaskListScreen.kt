package com.example.studyreward.ui.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonConfirmDialog
import com.example.studyreward.ui.common.NeonPanel
import com.example.studyreward.ui.common.NeonTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.JAPAN)
private const val DUE_SOON_MILLIS = 48L * 60 * 60 * 1000

private fun formatDeadline(millis: Long?): String =
    if (millis == null) "締切なし"
    else "締切 " + Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormatter)

private fun difficultyLabel(level: Int): String = when (level) {
    1 -> "簡単"
    2 -> "普通"
    else -> "難しい"
}

@Composable
fun TaskListScreen(
    tasks: List<TaskEntity>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onComplete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 未完了を締切が近い順に上へ、完了済みは下へ
    val sorted = remember(tasks) {
        tasks.sortedWith(
            compareBy<TaskEntity>({ it.completedAtMillis != null }, { it.deadlineAtMillis ?: Long.MAX_VALUE })
        )
    }

    var deleteTarget by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NeonTopBar("課題一覧", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                containerColor = Neon.Accent,
                contentColor = Neon.Bg,
            ) {
                Text("課題を登録", fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "課題がありません\n「課題を登録」から追加してください",
                    color = Neon.TextSub,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sorted, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onComplete = { onComplete(task) },
                    onEdit = { onEdit(task) },
                    onDelete = { deleteTarget = task },
                )
            }
        }
    }

    deleteTarget?.let { target ->
        NeonConfirmDialog(
            title = "課題を削除しますか?",
            message = "「${target.title}」を削除します。この操作は取り消せません。",
            confirmText = "削除する",
            onConfirm = {
                onDelete(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun TaskCard(task: TaskEntity, onComplete: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val done = task.completedAtMillis != null
    val now = System.currentTimeMillis()
    val deadline = task.deadlineAtMillis

    // 締切の色: 期限切れ=赤 / 48時間以内=アクセント / それ以外=グレー(完了済みはグレー)
    val deadlineColor: Color = when {
        done || deadline == null -> Neon.TextSub
        deadline < now -> MaterialTheme.colorScheme.error
        deadline - now <= DUE_SOON_MILLIS -> Neon.Accent
        else -> Neon.TextSub
    }

    NeonPanel(onClick = onEdit) {
        Text(
            text = task.title,
            color = if (done) Neon.TextSub else Neon.TextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (done) TextDecoration.LineThrough else null,
        )
        Text(text = formatDeadline(deadline), color = deadlineColor, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = buildAnnotatedString {
                    for (i in 1..3) {
                        withStyle(SpanStyle(color = if (i <= task.difficulty) Neon.Accent else Neon.Track)) {
                            append("● ")
                        }
                    }
                    withStyle(SpanStyle(color = Neon.TextSub)) {
                        append(" ${difficultyLabel(task.difficulty)}")
                    }
                },
                fontSize = 13.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) {
                    Text("完了 +${task.awardedPoints}pt", color = Neon.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    TextButton(onClick = onComplete) {
                        Text("完了にする", color = Neon.Accent, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}