package com.example.studyreward.ui.taskadd

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.ui.common.NeonButton
import com.example.studyreward.ui.common.NeonTopBar
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.ui.common.neonChipColors
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

private val difficultyLabels = mapOf(
    1 to "簡単 10pt",
    2 to "普通 20pt",
    3 to "難しい 30pt",
)

/**
 * 課題の「登録」と「編集」を兼ねる画面。
 * - 登録: initial = null
 * - 編集: initial に編集する課題を渡す(入力欄に今の値が入る)
 *
 * @param onSave 引数は (課題名, 締切[端末の時刻での当日23:59のミリ秒。未設定はnull], 難易度1〜3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddScreen(
    onBack: () -> Unit,
    onSave: (title: String, deadlineAtMillis: Long?, difficulty: Int) -> Unit,
    modifier: Modifier = Modifier,
    initial: TaskEntity? = null,
) {
    val isEdit = initial != null
    val isCompleted = initial?.completedAtMillis != null
    val initialPickerMillis = initial?.deadlineAtMillis?.let(::toPickerMillis)

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var deadlineMillis by remember { mutableStateOf(initialPickerMillis) }
    var difficulty by remember { mutableStateOf(initial?.difficulty ?: 2) }
    var showDatePicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var deadlineError by remember { mutableStateOf<String?>(null) }
    var difficultyError by remember { mutableStateOf<String?>(null) }

    // 読み取り専用の入力欄をタップしたときも日付ピッカーを開く
    val deadlineSource = remember { MutableInteractionSource() }
    LaunchedEffect(deadlineSource) {
        deadlineSource.interactions.collect {
            if (it is PressInteraction.Release) showDatePicker = true
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { NeonTopBar(if (isEdit) "課題を編集" else "課題を登録", onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text("課題名") },
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = deadlineMillis?.let(::formatDate) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("締切(任意)") },
                placeholder = { Text("日付を選ぶ") },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("選ぶ") }
                },
                interactionSource = deadlineSource,
                isError = deadlineError != null,
                supportingText = deadlineError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
            )

            if (deadlineMillis != null) {
                TextButton(onClick = { deadlineMillis = null }) { Text("締切を消す") }
            }

            Column {
                SectionLabel("難易度")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficultyLabels.forEach { (level, label) ->
                        FilterChip(
                            selected = difficulty == level,
                            onClick = {
                                difficulty = level
                                difficultyError = null
                            },
                            // 完了済みの課題はポイントが確定しているので難易度を変えられない
                            enabled = !isCompleted,
                            label = { Text(label) },
                            colors = neonChipColors(),
                        )
                    }
                }
                if (isCompleted) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "完了済みの課題は難易度を変更できません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            difficultyError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            NeonButton(
                text = if (isEdit) "保存する" else "登録する",
                onClick = {
                    val picked = deadlineMillis
                    val deadlineAt = when {
                        picked == null -> null
                        picked == initialPickerMillis -> initial?.deadlineAtMillis // 変えていなければそのまま
                        else -> toDeadlineAtMillis(picked)
                    }
                    titleError = validateTitle(title)
                    // 編集で締切を変えていないときは、過去の締切でもエラーにしない
                    deadlineError = if (picked != initialPickerMillis) validateDeadline(deadlineAt) else null
                    difficultyError = validateDifficulty(difficulty)
                    if (listOf(titleError, deadlineError, difficultyError).all { it == null }) {
                        onSave(title.trim(), deadlineAt, difficulty)
                    }
                },
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = deadlineMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMillis = pickerState.selectedDateMillis
                    deadlineError = null
                    showDatePicker = false
                }) { Text("決定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// DatePicker は UTC 0時のミリ秒を返すので、表示も UTC で整形する
private fun formatDate(millis: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(millis)

// 選んだ日の 23:59(端末のタイムゾーン)を締切にする
private fun toDeadlineAtMillis(pickerUtcMillis: Long): Long =
    Instant.ofEpochMilli(pickerUtcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atTime(23, 59)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

// 保存済みの締切(端末時刻) → DatePicker が使う「その日の UTC 0時」
private fun toPickerMillis(deadlineAtMillis: Long): Long =
    Instant.ofEpochMilli(deadlineAtMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

// ---- 入力チェック(エラーなら画面に出す文言を返す。問題なければ null) ----
private const val TITLE_MAX_LENGTH = 50

private fun validateTitle(title: String): String? = when {
    title.isBlank() -> "課題名を入力してください"
    title.trim().length > TITLE_MAX_LENGTH -> "課題名は${TITLE_MAX_LENGTH}文字以内にしてください"
    else -> null
}

private fun validateDifficulty(difficulty: Int): String? =
    if (difficulty in 1..3) null else "難易度は1〜3から選んでください"

private fun validateDeadline(deadlineAtMillis: Long?): String? =
    if (deadlineAtMillis != null && deadlineAtMillis < System.currentTimeMillis()) {
        "締切は今日以降の日付にしてください"
    } else null