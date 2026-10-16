package com.example.studyreward

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.studyreward.data.AppDatabase
import com.example.studyreward.data.entity.AppLimitEntity
import com.example.studyreward.data.entity.RewardSettingsEntity
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.data.repository.RewardRepository
import com.example.studyreward.data.repository.TaskRepository
import com.example.studyreward.domain.reward.ExchangeResult
import com.example.studyreward.domain.task.CompleteTaskResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryDbTest {

    private lateinit var db: AppDatabase
    private lateinit var taskRepo: TaskRepository
    private lateinit var rewardRepo: RewardRepository
    private val pkg = "com.example.app"
    private val now = 1_000_000_000_000L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        db.rewardSettingsDao().insertInitial(RewardSettingsEntity())
        taskRepo = TaskRepository(db, db.taskDao(), db.rewardSettingsDao())
        rewardRepo = RewardRepository(db, db.rewardSettingsDao(), db.appLimitDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun addTask(difficulty: Int = 2): Long = taskRepo.addTask(
        TaskEntity(
            title = "課題",
            deadlineAtMillis = null,
            difficulty = difficulty,
            createdAtMillis = now
        )
    )

    private suspend fun balance() = db.rewardSettingsDao().getBalance()

    private suspend fun registerApp(bonus: Int = 0) = db.appLimitDao().upsert(
        AppLimitEntity(
            packageName = pkg,
            dailyLimitMinutes = 30,
            bonusMinutes = bonus,
            usageDateEpochDay = 0L
        )
    )

    @Test
    fun 完了するとポイントが付く() = runBlocking {
        val id = addTask(difficulty = 2)

        val result = taskRepo.completeTask(id, now)

        assertEquals(CompleteTaskResult.Success(20), result)
        assertEquals(20, balance())
    }

    @Test
    fun 同じ課題を2回完了してもポイントは増えない() = runBlocking {
        val id = addTask(difficulty = 2)
        taskRepo.completeTask(id, now)

        val second = taskRepo.completeTask(id, now)

        assertEquals(CompleteTaskResult.AlreadyCompleted, second)
        assertEquals(20, balance())
    }

    @Test
    fun 存在しない課題はNotFound() = runBlocking {
        assertEquals(CompleteTaskResult.NotFound, taskRepo.completeTask(999L, now))
    }

    @Test
    fun ポイント交換で残高が減り追加時間が増える() = runBlocking {
        registerApp()
        val id = addTask(difficulty = 3)
        taskRepo.completeTask(id, now)   // 30pt

        val result = rewardRepo.exchangePointsForBonus(pkg, 20)

        assertEquals(ExchangeResult.Success(10), result)
        assertEquals(10, balance())
        assertEquals(10, db.appLimitDao().getByPackage(pkg)?.bonusMinutes)
    }

    @Test
    fun ポイント不足なら交換できない() = runBlocking {
        registerApp()

        val result = rewardRepo.exchangePointsForBonus(pkg, 10)

        assertEquals(ExchangeResult.InsufficientPoints, result)
        assertEquals(0, balance())
    }

    @Test
    fun 交換は10pt単位でない交換は無効() = runBlocking {
        registerApp()
        val id = addTask(difficulty = 3)
        taskRepo.completeTask(id, now)

        assertEquals(ExchangeResult.InvalidAmount, rewardRepo.exchangePointsForBonus(pkg, 15))
        assertEquals(ExchangeResult.InvalidAmount, rewardRepo.exchangePointsForBonus(pkg, 0))
        assertEquals(30, balance())
    }

    @Test
    fun 未登録アプリへの交換はAppNotFound() = runBlocking {
        val id = addTask(difficulty = 3)
        taskRepo.completeTask(id, now)

        val result = rewardRepo.exchangePointsForBonus("unknown.app", 10)

        assertEquals(ExchangeResult.AppNotFound, result)
        assertTrue(balance() == 30)
    }
}