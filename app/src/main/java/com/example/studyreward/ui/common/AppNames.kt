package com.example.studyreward.ui.common

import com.example.studyreward.usage.TargetApps

/**
 * パッケージ名 → 画面に出す名前。
 * TargetApps.defaults に載っていないアプリは、パッケージ名をそのまま返す。
 */
fun appDisplayName(packageName: String): String =
    TargetApps.defaults.firstOrNull { it.packageName == packageName }?.displayName ?: packageName