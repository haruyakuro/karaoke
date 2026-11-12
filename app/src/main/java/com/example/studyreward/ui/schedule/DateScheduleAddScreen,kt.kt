package com.example.studyreward.ui.schedule

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyreward.data.entity.DateScheduleEntity
import com.example.studyreward.ui.common.NeonButton
import com.example.studyreward.ui.common.NeonTopBar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFieldFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd(E)", Locale.JAPAN)

/**
 * 日付を指定した予定(祝日・特定日用)を登録する画面。項目は 予定名・日付・開始・終了。
 *
 * @param initialDate 日付欄の初期値(時間割画面で表示していた日付)
 * @param onAdd 予定を登録する。不正な入力や時間の重なりでは IllegalArgumentException を投げる(SPEC.md 6章)。
 *              その `e.message` をそのままエラー表示に使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScheduleAddScreen(
    initialDate: LocalDate,
    onBack: () -> Unit,
    onAdd: suspend (DateScheduleEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 読み取り専用の入力欄をタップしたときも日付ピッカーを開く
    val dateSource = remember { MutableInteractionSource() }
    LaunchedEffect(dateSource) {
        dateSource.interactions.collect {
            if (it is PressInteraction.Release) showDatePicker = true
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { NeonTopBar("日付指定の予定を追加", onBack) },
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
                onValueChange = { title = it; errorMessage = null },
                label = { Text("予定名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = date.format(dateFieldFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("日付") },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("選ぶ") }
                },
                interactionSource = dateSource,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it; errorMessage = null },
                    label = { Text("開始") },
                    placeholder = { Text("9:10") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it; errorMessage = null },
                    label = { Text("終了") },
                    placeholder = { Text("10:40") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.weight(1f))

            NeonButton(
                text = "登録する",
                enabled = !saving,
                onClick = {
                    val start = parseMinutes(startText)
                    val end = parseMinutes(endText)
                    when {
                        title.isBlank() -> errorMessage = "予定名を入力してください"
                        start == null || end == null ->
                            errorMessage = "時刻は「9:10」の形式で入力してください"
                        else -> scope.launch {
                            saving = true
                            try {
                                onAdd(
                                    DateScheduleEntity(
                                        title = title.trim(),
                                        dateEpochDay = date.toEpochDay(),
                                        startMinutes = start,
                                        endMinutes = end,
                                    )
                                )
                                onBack()
                            } catch (e: IllegalArgumentException) {
                                // 時刻の不正・他の予定との重なり(SPEC.md 6章)
                                errorMessage = e.message ?: "登録できませんでした"
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
            )
        }
    }

    if (showDatePicker) {
        // DatePicker は「その日の UTC 0時」のミリ秒でやりとりする
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        errorMessage = null
                    }
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