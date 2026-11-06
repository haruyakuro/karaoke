package com.example.studyreward.ui.schedule

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyreward.data.entity.ClassScheduleEntity
import com.example.studyreward.domain.schedule.FreePeriod
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonConfirmDialog
import com.example.studyreward.ui.common.NeonPanel
import com.example.studyreward.ui.common.NeonTopBar
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.ui.common.neonChipColors
import java.time.LocalDate

// 月曜=1 〜 日曜=7(SPEC.md 4章)
internal val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")

// 0時からの分 → "9:10"
internal fun formatMinutes(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

@Composable
fun ScheduleScreen(
    schedules: List<ClassScheduleEntity>,
    loadFreePeriods: suspend (dayOfWeek: Int) -> List<FreePeriod>,
    onBack: () -> Unit,
    onAddSchedule: () -> Unit,
    onDelete: (ClassScheduleEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    var freePeriods by remember { mutableStateOf<List<FreePeriod>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<ClassScheduleEntity?>(null) }

    // 曜日を切り替えたとき・時間割が変わったときに空きコマを取り直す
    LaunchedEffect(selectedDay, schedules) {
        freePeriods = loadFreePeriods(selectedDay)
    }

    val classes = schedules.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startMinutes }

    Scaffold(
        modifier = modifier,
        topBar = { NeonTopBar("時間割", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSchedule,
                containerColor = Neon.Accent,
                contentColor = Neon.Bg,
            ) {
                Text("授業を追加", fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val day = index + 1
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text(label) },
                        colors = neonChipColors(),
                    )
                }
            }

            SectionLabel("授業")
            if (classes.isEmpty()) {
                Text("この曜日の授業はありません。", color = Neon.TextSub, fontSize = 14.sp)
            } else {
                classes.forEach { c ->
                    NeonPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${formatMinutes(c.startMinutes)}〜${formatMinutes(c.endMinutes)}",
                                    color = Neon.Accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(c.title, color = Neon.TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { deleteTarget = c }) {
                                Text("削除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("空きコマ(30分以上)")
            if (freePeriods.isEmpty()) {
                Text("空きコマはありません。", color = Neon.TextSub, fontSize = 14.sp)
            } else {
                freePeriods.forEach { p ->
                    NeonPanel {
                        Text(
                            "${formatMinutes(p.startMinutes)}〜${formatMinutes(p.endMinutes)}",
                            color = Neon.TextMain,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("${p.endMinutes - p.startMinutes}分", color = Neon.TextSub, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(72.dp)) // 右下のボタンと重ならないように

            deleteTarget?.let { target ->
                NeonConfirmDialog(
                    title = "授業を削除しますか?",
                    message = "「${target.title}」を時間割から削除します。",
                    confirmText = "削除する",
                    onConfirm = {
                        onDelete(target)
                        deleteTarget = null
                    },
                    onDismiss = { deleteTarget = null },
                )
            }
        }
    }
}