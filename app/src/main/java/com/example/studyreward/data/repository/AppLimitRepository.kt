package com.example.studyreward.data.repository

import com.example.studyreward.data.dao.AppLimitDao
import com.example.studyreward.data.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AppLimitRepository(
    private val appLimitDao: AppLimitDao
) {
    fun observeAllLimits(): Flow<List<AppLimitEntity>> = appLimitDao.observeAll()

    suspend fun getLimit(packageName: String): AppLimitEntity? =
        appLimitDao.getByPackage(packageName)

    /** 対象アプリを登録する。登録済みなら基本上限だけ更新し、追加時間や使用時間は保持する。 */
    suspend fun registerApp(
        packageName: String,
        dailyLimitMinutes: Int,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        require(packageName.isNotBlank()) { "パッケージ名が空です" }
        require(dailyLimitMinutes > 0) { "上限は1分以上にしてください" }
        val existing = appLimitDao.getByPackage(packageName)
        val newLimit = existing?.copy(dailyLimitMinutes = dailyLimitMinutes)
            ?: AppLimitEntity(
                packageName = packageName,
                dailyLimitMinutes = dailyLimitMinutes,
                usageDateEpochDay = todayEpochDay
            )
        appLimitDao.upsert(newLimit)
    }
}