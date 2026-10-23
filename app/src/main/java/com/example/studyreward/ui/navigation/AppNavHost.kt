package com.example.studyreward.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studyreward.StudyRewardApplication
import com.example.studyreward.data.entity.TaskEntity
import com.example.studyreward.domain.task.CompleteTaskResult
import com.example.studyreward.ui.home.HomeScreen
import com.example.studyreward.ui.taskadd.TaskAddScreen
import com.example.studyreward.ui.tasklist.TaskListScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val TASK_LIST = "task_list"
    const val TASK_ADD = "task_add"
}

private const val DUE_SOON_MILLIS = 48L * 60 * 60 * 1000

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // SPEC.md 3章: Repository は container から取得する
    val container =
        (LocalContext.current.applicationContext as StudyRewardApplication).container
    val taskRepository = container.taskRepository
    val rewardRepository = container.rewardRepository

    val tasks by remember(taskRepository) { taskRepository.getAllTasks() }
        .collectAsState(initial = emptyList())
    val pointsBalance by remember(rewardRepository) { rewardRepository.getPointsBalance() }
        .collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val now = System.currentTimeMillis()
            val pending = tasks.filter { it.completedAtMillis == null }
            HomeScreen(
                pointsBalance = pointsBalance,
                pendingCount = pending.size,
                dueSoonCount = pending.count {
                    val d = it.deadlineAtMillis
                    d != null && d >= now && d - now <= DUE_SOON_MILLIS
                },
                doneCount = tasks.size - pending.size,
                onOpenTaskList = { navController.navigate(Routes.TASK_LIST) },
            )
        }
        composable(Routes.TASK_LIST) {
            TaskListScreen(
                tasks = tasks,
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onAddTask = { navController.navigate(Routes.TASK_ADD) },
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
    }
}