# SPEC.md(StudyReward 共通仕様)

4人で同じコードを扱うための取り決め。**ここに書いた名前・型・関数は、各自の判断で変更しない。**
変更したいときは、先にこのファイルを更新して関係者に連絡してから、コードを直す。

最終更新: Bが管理する。

---

## 1. 環境

| 項目 | 内容 |
|---|---|
| Package | `com.example.studyreward` |
| 言語 / UI | Kotlin / Jetpack Compose |
| DB | Room(端末内のみ。サーバーなし) |
| minSdk | 27 |
| バージョン類 | `gradle/libs.versions.toml` を参照 |

- Gradle設定・ライブラリのバージョンを変更できるのは **Bのみ**。必要なときはBに連絡する。
- `gradle.properties` に `android.disallowKotlinSourceSets=false` を入れている(KSPとAGP 9の互換のため。消さない)。
- ビルド時にKSP関連の警告が出ても、`BUILD SUCCESSFUL` なら問題ない。

---

## 2. パッケージ構成

```
com.example.studyreward/
├── MainActivity.kt
├── AppContainer.kt              ← Repositoryをまとめて作る場所
├── StudyRewardApplication.kt
├── data/
│   ├── AppDatabase.kt
│   ├── dao/        (TaskDao, ClassScheduleDao, RewardSettingsDao, AppLimitDao)
│   ├── entity/     (TaskEntity, ClassScheduleEntity, RewardSettingsEntity, AppLimitEntity)
│   └── repository/ (TaskRepository, RewardRepository, ScheduleRepository, AppLimitRepository)
├── domain/
│   ├── task/       (CompleteTaskResult, PointCalculator)
│   ├── reward/     (ExchangeResult)
│   └── schedule/   (FreePeriod)
├── ui/             (A担当)
├── notification/   (C担当: ReminderScheduler)
└── usage/          (D担当: UsageLimitManager)
```

**ルール**
- UI担当は `ui/` を編集し、**DAO・Databaseを直接触らない**。データの読み書きは必ずRepository経由。
- 同じ機能のクラスを複数人が重複して作らない。

---

## 3. Repositoryの取得方法(A・C・D向け)

```kotlin
val container = (context.applicationContext as StudyRewardApplication).container

container.taskRepository
container.rewardRepository
container.scheduleRepository
container.appLimitRepository
```

Composeでは `val context = LocalContext.current` から `context.applicationContext` を使う。

---

## 4. データクラス

日時は **Unix時刻のミリ秒(Long)** で保存し、表示するときに変換する。

### TaskEntity(課題)

| 項目 | 型 | 内容 |
|---|---|---|
| `id` | `Long` | 自動採番(新規作成時は0) |
| `title` | `String` | 課題名 |
| `deadlineAtMillis` | `Long?` | 締切。未設定は `null` |
| `difficulty` | `Int` | 1:簡単 / 2:普通 / 3:難しい |
| `createdAtMillis` | `Long` | 登録日時 |
| `completedAtMillis` | `Long?` | 完了日時。`null` なら未完了 |
| `awardedPoints` | `Int` | 実際に付与されたポイント |

完了状態は別の `isCompleted` ではなく **`completedAtMillis` で判断**する(`null` = 未完了)。

### ClassScheduleEntity(時間割)

| 項目 | 型 | 内容 |
|---|---|---|
| `id` | `Long` | 自動採番 |
| `title` | `String` | 授業名 |
| `dayOfWeek` | `Int` | 月曜=1 〜 日曜=7 |
| `startMinutes` | `Int` | 開始時刻(0時からの分。9:10 → 550) |
| `endMinutes` | `Int` | 終了時刻(同上) |

### RewardSettingsEntity(ポイント残高)

| 項目 | 型 | 内容 |
|---|---|---|
| `id` | `Int` | 常に1 |
| `pointsBalance` | `Int` | ポイント残高 |

DB作成時にid=1のレコードを自動作成する。

### AppLimitEntity(アプリ利用制限)

| 項目 | 型 | 内容 |
|---|---|---|
| `packageName` | `String` | 対象アプリのパッケージ名(主キー) |
| `dailyLimitMinutes` | `Int` | 1日の基本上限(分) |
| `bonusMinutes` | `Int` | ポイント交換で得た追加時間(分) |
| `usedSecondsToday` | `Long` | 当日の使用秒数 |
| `usageDateEpochDay` | `Long` | 使用時間を計測している日(`LocalDate.toEpochDay()`) |
| `isEnabled` | `Boolean` | 制限が有効か |

残り秒数 = (`dailyLimitMinutes` + `bonusMinutes`) × 60 − `usedSecondsToday`
0以下なら上限超過。日付が変わったら使用時間をリセットする処理が必要(D担当)。

### FreePeriod(空きコマ。DBに保存しない)

| 項目 | 型 |
|---|---|
| `dayOfWeek` | `Int` |
| `startMinutes` | `Int` |
| `endMinutes` | `Int` |

---

## 5. 結果の型

```kotlin
sealed interface CompleteTaskResult {
    data class Success(val pointsAwarded: Int) : CompleteTaskResult
    data object AlreadyCompleted : CompleteTaskResult
    data object NotFound : CompleteTaskResult
}

sealed interface ExchangeResult {
    data class Success(val minutesAdded: Int) : ExchangeResult
    data object InsufficientPoints : ExchangeResult
    data object AppNotFound : ExchangeResult
    data object InvalidAmount : ExchangeResult
}
```

画面側は `when` で全パターンを処理する。

---

## 6. Repositoryの関数一覧

`suspend` が付くものは、コルーチン(`viewModelScope.launch` や `LaunchedEffect` など)の中で呼ぶ。

### TaskRepository(B)

| 関数 | 引数 | 戻り値 | 内容 |
|---|---|---|---|
| `getAllTasks()` | なし | `Flow<List<TaskEntity>>` | 課題一覧を監視(新しい順) |
| `suspend addTask(task)` | `TaskEntity` | `Long` | 課題を登録。採番されたIDを返す |
| `suspend updateTask(task)` | `TaskEntity` | `Unit` | 課題を更新 |
| `suspend deleteTask(taskId)` | `Long` | `Unit` | 課題を削除 |
| `suspend completeTask(taskId, completedAtMillis)` | `Long`, `Long` | `CompleteTaskResult` | 完了にしてポイント付与 |

- `completeTask` は、完了状態の更新とポイント加算を**1つのトランザクション**で行う。
- すでに完了済みなら `AlreadyCompleted`(ポイントは増えない)。
- 通知(C担当)との連動: 課題の登録・更新・削除のあと、`ReminderScheduler` の呼び出しも行うこと。

### PointCalculator(B)

| 関数 | 引数 | 戻り値 |
|---|---|---|
| `calculatePoints(task, completedAtMillis)` | `TaskEntity`, `Long` | `Int` |

- 基本ポイント: 難易度1=10pt / 2=20pt / 3=30pt
- 締切まで48時間以上残して完了 → +10pt
- 締切なし・締切超過 → ボーナスなし
- 計算だけを行い、DBは触らない。

### RewardRepository(B)

| 関数 | 引数 | 戻り値 | 内容 |
|---|---|---|---|
| `getPointsBalance()` | なし | `Flow<Int>` | ポイント残高を監視 |
| `suspend exchangePointsForBonus(packageName, pointsToSpend)` | `String`, `Int` | `ExchangeResult` | ポイントを追加時間に交換 |

- 交換レート: **10pt = 5分**。10pt単位のみ(それ以外は `InvalidAmount`)。
- 対象アプリが `registerApp` で登録済みであること(未登録は `AppNotFound`)。
- ポイント減算と追加時間の加算は1つのトランザクション。

### AppLimitRepository(B)

| 関数 | 引数 | 戻り値 | 内容 |
|---|---|---|---|
| `observeAllLimits()` | なし | `Flow<List<AppLimitEntity>>` | 登録アプリの一覧を監視 |
| `suspend getLimit(packageName)` | `String` | `AppLimitEntity?` | 1件取得 |
| `suspend registerApp(packageName, dailyLimitMinutes, todayEpochDay)` | `String`, `Int`, `Long`(省略可) | `Unit` | 対象アプリを登録。登録済みなら基本上限のみ更新 |

- `packageName` が空、`dailyLimitMinutes` が0以下のときは `IllegalArgumentException`。
**使用時間の更新用(D担当が使う)**

| 関数 | 引数 | 戻り値 | 内容 |
|---|---|---|---|
| `suspend setUsedSeconds(packageName, usedSeconds, todayEpochDay)` | `String`, `Long`, `Long`(省略可) | `Boolean` | 当日の使用秒数を**その値に置き換える**(`UsageStatsManager` の累計値向け) |
| `suspend addUsedSeconds(packageName, seconds, todayEpochDay)` | `String`, `Long`, `Long`(省略可) | `Boolean` | 当日の使用秒数に**加算する**(Accessibilityなど、増分を数える場合向け) |
| `suspend resetUsageForNewDay(packageName, epochDay)` | `String`, `Long` | `Boolean` | 日付が変わっていれば、使用秒数を0にする |
| `suspend resetAllIfNewDay(todayEpochDay)` | `Long`(省略可) | `Unit` | 登録済みの全アプリに対して、上の処理を行う(監視開始時などに呼ぶ) |

- 戻り値の `Boolean` は、**アプリが登録済みなら `true`、未登録なら `false`**。
- `setUsedSeconds` と `addUsedSeconds` は、**日付が変わっていれば自動で先にリセット**する。呼ぶ側でリセットを意識しなくてよい。
- **同じ期間に `set` と `add` を混ぜない。** 二重に数えてしまう。1つのアプリについて、どちらか一方の方式に決める。
- `todayEpochDay` は `LocalDate.now().toEpochDay()`(省略すると自動で入る)。

**追加時間(`bonusMinutes`)の日またぎのルール**

- 日付が変わるとき、**使い切っていない追加時間だけを翌日に持ち越す**。使った分は消える。
- 基本上限を先に使い、超えた分から追加時間を消費する、として計算する。
- 例: 基本30分、追加10分、前日に35分使用 → 追加時間は5分を消費、**5分を持ち越す**。
- 例: 前日に20分しか使わなかった → 追加10分は**そのまま持ち越す**。

### ScheduleRepository(Bが実装、Cが利用)

| 関数 | 引数 | 戻り値 | 内容 |
|---|---|---|---|
| `getAllSchedules()` | なし | `Flow<List<ClassScheduleEntity>>` | 時間割一覧を監視 |
| `suspend addSchedule(schedule)` | `ClassScheduleEntity` | `Long` | 時間割を登録 |
| `suspend deleteSchedule(scheduleId)` | `Long` | `Unit` | 時間割を削除 |
| `suspend getFreePeriods(dayOfWeek)` | `Int` | `List<FreePeriod>` | 指定曜日の空きコマ |

- `addSchedule` は、曜日・時刻が不正、または他の授業と時間が重なる場合に **`IllegalArgumentException`** を投げる。
  画面側は `try/catch` して `e.message` をエラー表示に使える。
- 空きコマ: 同じ曜日の授業の間が**30分以上**のもの。最初の授業より前・最後の授業より後は含めない。

---

## 7. まだ実装されていない契約(C・D担当)

### ReminderScheduler(C)
`scheduleTaskReminder(task)` / `cancelTaskReminders(taskId)` / `scheduleFreePeriodReminder(period)` / `cancelFreePeriodReminder(period)`

- 締切の前日と当日に通知。締切なし・締切超過の課題には通知しない。
- 課題の削除・締切変更時は、以前の通知を取り消して再設定する。

### UsageLimitManager(D)
`getRemainingUsageSeconds(packageName)` / `recordUsageSeconds(packageName, seconds)` / `isLimitExceeded(packageName)` / `startMonitoring()` / `stopMonitoring()`

- 実装中に関数が必要になった場合は、Dだけで決めず、関係する担当とこのファイルを更新してから進める。

---

## 8. 変更するときのルール

1. 名前・型・関数を変えたいときは、**先にこのSPEC.mdを更新**する。
2. 関係する担当に連絡する。
3. 呼び出している側のコードも**同じPRで**直す。
4. ビルドが通ることを確認してからPRを出す。
