package com.example.studyreward.ui.common

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** アプリ共通の色。アクセント色を変えたいときは Accent の1か所だけ変える。 */
object Neon {
    val Bg = Color(0xFF0B0D10)
    val Panel = Color(0xFF14181D)
    val Track = Color(0xFF222831)
    val Accent = Color(0xFFC6FF3D) // ネオンライム(シアンなら 0xFF3DF0FF、紫なら 0xFFB46BFF など)
    val TextMain = Color(0xFFF2F4F7)
    val TextSub = Color(0xFF8B94A1)
}

private val NeonColors = darkColorScheme(
    primary = Neon.Accent,
    onPrimary = Neon.Bg,
    secondaryContainer = Neon.Accent,
    onSecondaryContainer = Neon.Bg,
    background = Neon.Bg,
    onBackground = Neon.TextMain,
    surface = Neon.Panel,
    onSurface = Neon.TextMain,
    onSurfaceVariant = Neon.TextSub,
    outline = Color(0xFF3A424D),
    outlineVariant = Color(0xFF2A313A),
    surfaceContainer = Neon.Panel,
    surfaceContainerHigh = Neon.Panel,
    surfaceContainerHighest = Neon.Track,
)

/** 全画面に適用するダーク × ネオンのテーマ。AppNavHost で1回だけ包む。 */
@Composable
fun NeonTheme(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography // アプリ側の文字設定を引き継ぐ
    MaterialTheme(colorScheme = NeonColors, typography = typography) {
        LightSystemBarIcons()
        content()
    }
}

/** 戻るボタン付きの上部バー(背景に溶け込むデザイン) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            TextButton(onClick = onBack) { Text("戻る", color = Neon.Accent) }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Neon.Bg,
            titleContentColor = Neon.TextMain,
        ),
    )
}

/** 角丸のダークカード。onClick を渡すとタップできるカードになる。 */
@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Neon.Panel, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** アクセント色のメインボタン */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Neon.Accent,
            contentColor = Neon.Bg,
            disabledContainerColor = Neon.Track,
            disabledContentColor = Neon.TextSub,
        ),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** 削除などの確認ダイアログ。確定ボタンは赤で表示する。 */
@Composable
fun NeonConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
        containerColor = Neon.Panel,
        titleContentColor = Neon.TextMain,
        textContentColor = Neon.TextSub,
    )
}

/** 小さなグレーの見出し */
@Composable
fun SectionLabel(text: String) {
    Text(text, color = Neon.TextSub, fontSize = 12.sp, letterSpacing = 1.sp)
}

/** 選択中がアクセント色になるチップの配色 */
@Composable
fun neonChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = Neon.Panel,
    labelColor = Neon.TextMain,
    selectedContainerColor = Neon.Accent,
    selectedLabelColor = Neon.Bg,
)

// 背景が暗いので、ステータスバー・ナビゲーションバーのアイコンを白にする(アプリを離れたら元に戻す)
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null && prevStatus != null && prevNav != null) {
                controller.isAppearanceLightStatusBars = prevStatus
                controller.isAppearanceLightNavigationBars = prevNav
            }
        }
    }
}