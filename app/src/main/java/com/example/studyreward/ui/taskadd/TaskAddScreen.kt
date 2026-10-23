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
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

private val difficultyLabels = mapOf(
    1 to "1(かんたん)",
    2 to "2(ふつう)",
    3 to "3(むずかしい)",
)

/**
 * @param onSave 引数は (課題名, 締切[端末の時刻での当日23:59のミリ秒。未設定はnull], 難易度1〜3)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddScreen(
    onBack: () -> Unit,
    onSave: (title: String, deadlineAtMillis: Long?, difficulty: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var deadlineMillis by remember { mutableStateOf<Long?>(null) }
    var difficulty by remember { mutableStateOf(2) }
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
        topBar = {
            TopAppBar(
                title = { Text("課題を登録") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("戻る") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                isError = titleError != null,
                supportingText = titleError?.let { msg -> { Text(msg) } },
                label = { Text("課題名") },
                singleLine = true,
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
                Text("難易度", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficultyLabels.forEach { (level, label) ->
                        FilterChip(
                            selected = difficulty == level,
                            onClick = {
                                difficulty = level
                                difficultyError = null
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }

            difficultyError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val deadlineAt = deadlineMillis?.let(::toDeadlineAtMillis)
                    titleError = validateTitle(title)
                    deadlineError = validateDeadline(deadlineAt)
                    difficultyError = validateDifficulty(difficulty)
                    if (listOf(titleError, deadlineError, difficultyError).all { it == null }) {
                        onSave(title.trim(), deadlineAt, difficulty)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("登録する")
            }
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