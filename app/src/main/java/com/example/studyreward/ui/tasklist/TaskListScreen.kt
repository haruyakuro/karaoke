package com.example.studyreward.ui.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyreward.data.entity.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.JAPAN)

private fun formatDeadline(millis: Long?): String =
    if (millis == null) "締切なし"
    else "締切 " + Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormatter)

private fun difficultyLabel(level: Int): String = when (level) {
    1 -> "簡単"
    2 -> "普通"
    else -> "難しい"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: List<TaskEntity>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onComplete: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("課題一覧") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("戻る") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTask) {
                Text("課題を登録")
            }
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            Text(
                "課題がありません。「課題を登録」から追加してください。",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${formatDeadline(task.deadlineAtMillis)} ・ ${difficultyLabel(task.difficulty)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (task.completedAtMillis != null) {
                            Text("完了済み(+${task.awardedPoints}pt)", style = MaterialTheme.typography.labelMedium)
                        } else {
                            TextButton(onClick = { onComplete(task) }) { Text("完了にする") }
                        }
                    }
                }
            }
        }
    }
}