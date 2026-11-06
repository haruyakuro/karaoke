package com.example.studyreward.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studyreward.StudyRewardApplication
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.domain.reward.ExchangeResult
import com.example.studyreward.domain.task.CompleteTaskResult
import com.example.studyreward.ui.common.Neon
import com.example.studyreward.ui.common.NeonTheme
import com.example.studyreward.ui.exchange.ExchangeScreen
import com.example.studyreward.ui.schedule.ScheduleAddScreen
import com.example.studyreward.ui.schedule.ScheduleScreen
import com.example.studyreward.ui.home.HomeScreen
import com.example.studyreward.ui.taskadd.TaskAddScreen
import com.example.studyreward.ui.tasklist.TaskListScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val TASK_LIST = "task_list"
    const val TASK_ADD = "task_add"
    const val EXCHANGE = "exchange"
    const val SCHEDULE = "schedule"
    const val SCHEDULE_ADD = "schedule_add"
    const val TASK_EDIT = "task_edit/{taskId}"

    fun taskEdit(taskId: Long) = "task_edit/$taskId"
}

private const val DUE_SOON_MILLIS = 48L * 60 * 60 * 1000

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // SPEC.md 3章: Repository は container から取得する
    val container =
        (LocalContext.current.applicationContext as StudyRewardApplication).container
    val taskRepository = container.taskRepository
    val rewardRepository = container.rewardRepository
    val scheduleRepository = container.scheduleRepository
    val appLimitRepository = container.appLimitRepository

    val tasks by remember(taskRepository) { taskRepository.getAllTasks() }
        .collectAsState(initial = emptyList())
    val pointsBalance by remember(rewardRepository) { rewardRepository.getPointsBalance() }
        .collectAsState(initial = 0)
    val schedules by remember(scheduleRepository) { scheduleRepository.getAllSchedules() }
        .collectAsState(initial = emptyList())
    val appLimits by remember(appLimitRepository) { appLimitRepository.observeAllLimits() }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 全画面をダーク × ネオンのテーマで包む
    NeonTheme {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // 遷移中に下地が白く見えないよう、背景も黒にしておく
            modifier = Modifier.background(Neon.Bg),
            // 進む: 右から少しスライドして現れ、前の画面は左へ薄れていく
            enterTransition = {
                slideInHorizontally(animationSpec = tween(350), initialOffsetX = { it / 5 }) +
                        fadeIn(tween(350))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { -it / 8 }) +
                        fadeOut(tween(250))
            },
            // 戻る: 逆向きの動き
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(350), initialOffsetX = { -it / 8 }) +
                        fadeIn(tween(350))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { it / 5 }) +
                        fadeOut(tween(250))
            },
        ) {
            composable(Routes.HOME) {
                val now = System.currentTimeMillis()
                val pending = tasks.filter { it.completedAtMillis == null }
                // 次の締切: 未完了のうち、これから来る締切で一番近いもの
                val nextTask = pending
                    .filter { val d = it.deadlineAtMillis; d != null && d >= now }
                    .minByOrNull { it.deadlineAtMillis ?: Long.MAX_VALUE }
                HomeScreen(
                    pointsBalance = pointsBalance,
                    pendingCount = pending.size,
                    nextTaskTitle = nextTask?.title,
                    nextDeadlineMillis = nextTask?.deadlineAtMillis,
                    onAddTask = { navController.navigate(Routes.TASK_ADD) },
                    dueSoonCount = pending.count {
                        val d = it.deadlineAtMillis
                        d != null && d >= now && d - now <= DUE_SOON_MILLIS
                    },
                    doneCount = tasks.size - pending.size,
                    onOpenTaskList = { navController.navigate(Routes.TASK_LIST) },
                    onOpenExchange = { navController.navigate(Routes.EXCHANGE) },
                    onOpenSchedule = { navController.navigate(Routes.SCHEDULE) },
                )
            }
            composable(Routes.TASK_LIST) {
                TaskListScreen(
                    tasks = tasks,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onAddTask = { navController.navigate(Routes.TASK_ADD) },
                    onEdit = { navController.navigate(Routes.taskEdit(it.id)) },
                    onDelete = { task ->
                        scope.launch {
                            taskRepository.deleteTask(task.id)
                            snackbarHostState.showSnackbar("課題を削除しました")
                        }
                    },
                    onComplete = { task ->
                        scope.launch {
                            // 結果ごとに表示を変える(SPEC.md 5章: when で全パターン処理)
                            val message = when (
                                val result = taskRepository.completeTask(task.id, System.currentTimeMillis())
                            ) {
                                is CompleteTaskResult.Success -> "${result.pointsAwarded}pt 獲得しました"
                                is CompleteTaskResult.AlreadyCompleted -> "この課題はすでに完了しています"
                                is CompleteTaskResult.NotFound -> "課題が見つかりませんでした"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                )
            }
            composable(Routes.TASK_ADD) {
                TaskAddScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { title, deadlineAtMillis, difficulty ->
                        scope.launch {
                            taskRepository.addTask(
                                TaskEntity(
                                    id = 0,
                                    title = title,
                                    deadlineAtMillis = deadlineAtMillis,
                                    difficulty = difficulty,
                                    createdAtMillis = System.currentTimeMillis(),
                                    completedAtMillis = null,
                                    awardedPoints = 0,
                                )
                            )
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable(Routes.EXCHANGE) {
                ExchangeScreen(
                    pointsBalance = pointsBalance,
                    registeredApps = appLimits,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() },
                    onExchange = { packageName, points ->
                        scope.launch {
                            val message = when (
                                val result = rewardRepository.exchangePointsForBonus(packageName, points)
                            ) {
                                is ExchangeResult.Success -> "${result.minutesAdded}分 追加しました"
                                is ExchangeResult.InsufficientPoints -> "ポイントが足りません"
                                is ExchangeResult.AppNotFound -> "対象アプリが登録されていません"
                                is ExchangeResult.InvalidAmount -> "ポイントは10pt単位で入力してください"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                )
            }
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    schedules = schedules,
                    loadFreePeriods = { day -> scheduleRepository.getFreePeriods(day) },
                    onBack = { navController.popBackStack() },
                    onAddSchedule = { navController.navigate(Routes.SCHEDULE_ADD) },
                    onDelete = { schedule -> scope.launch { scheduleRepository.deleteSchedule(schedule.id) } },
                )
            }
            composable(Routes.SCHEDULE_ADD) {
                ScheduleAddScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { scheduleRepository.addSchedule(it) },
                )
            }
            composable(
                route = Routes.TASK_EDIT,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId")
                // 開いた時点の課題を覚えておく(保存後に一覧が更新されても画面が崩れないように)
                val initial = remember(taskId) { tasks.find { it.id == taskId } }
                if (initial == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    TaskAddScreen(
                        initial = initial,
                        onBack = { navController.popBackStack() },
                        onSave = { title, deadlineAtMillis, difficulty ->
                            scope.launch {
                                taskRepository.updateTask(
                                    initial.copy(
                                        title = title,
                                        deadlineAtMillis = deadlineAtMillis,
                                        difficulty = difficulty,
                                    )
                                )
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
        }
    }
}