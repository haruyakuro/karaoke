package com.example.studyreward.ui.schedule

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyreward.data.entity.ClassScheduleEntity
import com.example.studyreward.ui.common.NeonButton
import com.example.studyreward.ui.common.NeonTopBar
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.ui.common.neonChipColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * @param onAdd 授業を登録する。不正な入力や時間の重なりでは IllegalArgumentException を投げる(SPEC.md 6章)。
 */
@Composable
fun ScheduleAddScreen(
    onBack: () -> Unit,
    onAdd: suspend (ClassScheduleEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = { NeonTopBar("授業を追加", onBack) },
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
                label = { Text("授業名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("曜日")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = dayOfWeek == index + 1,
                            onClick = { dayOfWeek = index + 1; errorMessage = null },
                            label = { Text(label) },
                            colors = neonChipColors(),
                        )
                    }
                }
            }

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
                onClick = {
                    val start = parseMinutes(startText)
                    val end = parseMinutes(endText)
                    when {
                        title.isBlank() -> errorMessage = "授業名を入力してください"
                        start == null || end == null ->
                            errorMessage = "時刻は「9:10」の形式で入力してください"
                        else -> scope.launch {
                            try {
                                onAdd(
                                    ClassScheduleEntity(
                                        id = 0,
                                        title = title.trim(),
                                        dayOfWeek = dayOfWeek,
                                        startMinutes = start,
                                        endMinutes = end,
                                    )
                                )
                                onBack()
                            } catch (e: IllegalArgumentException) {
                                errorMessage = e.message ?: "登録できませんでした"
                            }
                        }
                    }
                },
            )
        }
    }
}

// "9:10" → 550。形式が違えば null。値の範囲チェックは Repository 側(IllegalArgumentException)に任せる。
private fun parseMinutes(text: String): Int? {
    val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(text.trim()) ?: return null
    return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
}