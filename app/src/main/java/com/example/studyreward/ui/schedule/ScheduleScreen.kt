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
import com.example.studyreward.data.entity.DateScheduleEntity
import com.example.studyreward.data.repository.DateScheduleRepository
import com.example.studyreward.domain.schedule.FreePeriod
import com.example.studyreward.domain.schedule.HolidayCalendar
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonConfirmDialog
import com.example.studyreward.ui.common.NeonPanel
import com.example.studyreward.ui.common.NeonTopBar
import com.example.studyreward.ui.common.SectionLabel
import com.example.studyreward.ui.common.neonChipColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// 月曜=1 〜 日曜=7(SPEC.md 4章)
internal val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")

// 0時からの分 → "9:10"
internal fun formatMinutes(minutes: Int): String = "%d:%02d".format(minutes / 60, minutes % 60)

private val dateLabelFormatter = DateTimeFormatter.ofPattern("M/d(E)", Locale.JAPAN)

// 選んだ曜日の「直近の日付」(今日がその曜日なら今日)
private fun nearestDateOf(dayOfWeek: Int): LocalDate =
    LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))

/**
 * 時間割画面。空きコマは「曜日」ではなく「選んだ曜日の直近の日付」で計算する。
 * (土日・祝日は 8:00〜22:00 の空きコマも出る。SPEC.md 6章 DateScheduleRepository)
 *
 * @param loadFreePeriods 日付(LocalDate.toEpochDay())を受け取って、その日の空きコマを返す
 * @param onAddDateSchedule 日付指定の予定の登録画面を開く。引数は表示中の日付(toEpochDay)
 */
@Composable
fun ScheduleScreen(
    schedules: List<ClassScheduleEntity>,
    dateSchedules: List<DateScheduleEntity>,
    loadFreePeriods: suspend (dateEpochDay: Long) -> List<FreePeriod>,
    onBack: () -> Unit,
    onAddSchedule: () -> Unit,
    onAddDateSchedule: (dateEpochDay: Long) -> Unit,
    onDelete: (ClassScheduleEntity) -> Unit,
    onDeleteDateSchedule: (DateScheduleEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    var freePeriods by remember { mutableStateOf<List<FreePeriod>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<ClassScheduleEntity?>(null) }
    var deleteDateTarget by remember { mutableStateOf<DateScheduleEntity?>(null) }

    val targetDate = remember(selectedDay) { nearestDateOf(selectedDay) }
    val targetEpochDay = targetDate.toEpochDay()
    val dateLabel = targetDate.format(dateLabelFormatter)

    // 曜日を切り替えたとき・毎週の予定や日付指定の予定が変わったときに、空きコマを取り直す
    LaunchedEffect(targetEpochDay, schedules, dateSchedules) {
        freePeriods = loadFreePeriods(targetEpochDay)
    }

    val classes = schedules.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startMinutes }
    val datedToday = dateSchedules
        .filter { it.dateEpochDay == targetEpochDay }
        .sortedBy { it.startMinutes }

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
            Text("表示中の日付: $dateLabel", color = Neon.TextSub, fontSize = 13.sp)

            SectionLabel("授業(毎週)")
            if (classes.isEmpty()) {
                Text("この曜日の授業はありません。", color = Neon.TextSub, fontSize = 14.sp)
            } else {
                classes.forEach { c ->
                    ScheduleRow(
                        time = "${formatMinutes(c.startMinutes)}〜${formatMinutes(c.endMinutes)}",
                        title = c.title,
                        onDelete = { deleteTarget = c },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("$dateLabel の予定(日付指定)")
            if (datedToday.isEmpty()) {
                Text("この日だけの予定はありません。", color = Neon.TextSub, fontSize = 14.sp)
            } else {
                datedToday.forEach { d ->
                    ScheduleRow(
                        time = "${formatMinutes(d.startMinutes)}〜${formatMinutes(d.endMinutes)}",
                        title = d.title,
                        onDelete = { deleteDateTarget = d },
                    )
                }
            }
            TextButton(onClick = { onAddDateSchedule(targetEpochDay) }) {
                Text("この日の予定を追加", color = Neon.Accent, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("空きコマ(30分以上)・$dateLabel")
            if (HolidayCalendar.isOffDay(targetDate)) {
                Text(
                    "休日は ${formatMinutes(DateScheduleRepository.OFF_DAY_START_MINUTES)}〜" +
                            "${formatMinutes(DateScheduleRepository.OFF_DAY_END_MINUTES)} の範囲で表示します。",
                    color = Neon.TextSub,
                    fontSize = 12.sp,
                )
            }
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
            deleteDateTarget?.let { target ->
                NeonConfirmDialog(
                    title = "予定を削除しますか?",
                    message = "「${target.title}」(${LocalDate.ofEpochDay(target.dateEpochDay).format(dateLabelFormatter)})を削除します。",
                    confirmText = "削除する",
                    onConfirm = {
                        onDeleteDateSchedule(target)
                        deleteDateTarget = null
                    },
                    onDismiss = { deleteDateTarget = null },
                )
            }
        }
    }
}

@Composable
private fun ScheduleRow(time: String, title: String, onDelete: () -> Unit) {
    NeonPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(time, color = Neon.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(title, color = Neon.TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDelete) {
                Text("削除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}