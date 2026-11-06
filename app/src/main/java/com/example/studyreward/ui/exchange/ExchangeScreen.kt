package com.example.studyreward.ui.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyreward.data.entity.AppLimitEntity
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonButton
import com.example.studyreward.ui.common.NeonPanel
import com.example.studyreward.ui.common.NeonTopBar
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.ui.common.neonChipColors

/**
 * @param onExchange (対象アプリのパッケージ名, 使うポイント)。結果の表示は呼び出し側(Snackbar)で行う。
 */
@Composable
fun ExchangeScreen(
    pointsBalance: Int,
    registeredApps: List<AppLimitEntity>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onExchange: (packageName: String, points: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var packageName by remember { mutableStateOf("") }
    var pointsText by remember { mutableStateOf("10") }
    var packageError by remember { mutableStateOf<String?>(null) }
    var pointsError by remember { mutableStateOf<String?>(null) }


    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NeonTopBar("ポイント交換", onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NeonPanel {
                SectionLabel("ポイント残高")
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Neon.TextMain)) {
                            append("$pointsBalance")
                        }
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Neon.Accent)) {
                            append(" pt")
                        }
                    },
                )
                Text("10pt = 5分(10pt単位で交換できます)", color = Neon.TextSub, fontSize = 13.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("対象アプリ")
                if (registeredApps.isEmpty()) {
                    Text(
                        "利用制限の対象アプリがまだ登録されていません。",
                        color = Neon.TextSub,
                        fontSize = 14.sp,
                    )
                } else {
                    registeredApps.forEach { app ->
                        FilterChip(
                            selected = packageName == app.packageName,
                            onClick = {
                                packageName = app.packageName
                                packageError = null
                            },
                            label = {
                                Text("${app.packageName}(上限 ${app.dailyLimitMinutes}分 + 追加 ${app.bonusMinutes}分)")
                            },
                            colors = neonChipColors(),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    packageError = null
                },
                label = { Text("パッケージ名") },
                singleLine = true,
                isError = packageError != null,
                supportingText = packageError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = pointsText,
                onValueChange = {
                    pointsText = it
                    pointsError = null
                },
                label = { Text("使うポイント") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = pointsError != null,
                supportingText = pointsError?.let { msg -> { Text(msg) } },
                modifier = Modifier.fillMaxWidth(),
            )

            NeonButton(
                text = "交換する",
                onClick = {
                    val points = pointsText.trim().toIntOrNull()
                    packageError = if (packageName.isBlank()) "アプリを選ぶか、パッケージ名を入力してください" else null
                    pointsError = if (points == null) "数字で入力してください" else null
                    if (packageError == null && points != null) {
                        onExchange(packageName.trim(), points)
                    }
                },
            )
        }
    }
}