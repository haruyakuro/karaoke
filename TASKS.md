# TASKS.md(誰が何をするか)

「自分は今、何をすればいいの?」に答えるためのファイル。
自分の担当のところを読んで、**上から順に**進める。
終わったら `[ ]` を `[x]` にしてよい。

関数の名前・引数・戻り値は **SPEC.md** を見る。Gitのルールは **CONTRIBUTING.md** を見る。


---


## 全員がまずやること(最初の1回)

- [ ] GitHubの招待を承認する
- [ ] リポジトリをCloneして、Android Studioで開く
- [ ] Gradle Syncが終わるのを待ち、**ビルドできる**ことを確認する(Build → Make Project)
- [ ] エミュレーターか実機で、アプリが起動することを確認する
- [ ] `main` から**自分用のブランチ**を作る(下の担当別を参照)
- [ ] README.md、CONTRIBUTING.md、SPEC.md に一度目を通す
- [ ] エラーが出たら、スクリーンショットをBに送る

### Repositoryの呼び出し方

```kotlin
val container = (context.applicationContext as StudyRewardApplication).container

container.taskRepository
container.rewardRepository
container.scheduleRepository
container.appLimitRepository
```

---

## ファイルをどこに作るか

### フォルダ(パッケージ)の作り方

1. 左のツリーで `com.example.studyreward` を右クリックする
   (**`(androidTest)` や `(test)` と付いていないほう**を選ぶ)
2. **New → Package** を選ぶ
3. 名前を入力してEnterを押す(例: `ui.task` と書くと、`ui` の中に `task` ができる)
4. 作ったパッケージを右クリック → **New → Kotlin Class/File** → 名前を入力 → **File** を選ぶ
5. 新しいファイルの先頭に `package ...` の行が入っていることを確認する

> `ui`、`notification`、`usage` はまだ存在しない。**自分の担当のフォルダは、自分で最初に作る。**
> フォルダの場所とファイルの `package` 行は、必ず一致させる。


### 担当別の置き場所

| 担当 | 作る場所 | 作るファイルの例 |
|---|---|---|
| **A** | `ui/home` | `HomeScreen.kt` |
| | `ui/task` | `TaskListScreen.kt`、`TaskFormScreen.kt`、`TaskViewModel.kt` |
| | `ui/reward` | `RewardScreen.kt`、`RewardViewModel.kt` |
| | `ui/schedule` | `ScheduleScreen.kt`、`ScheduleViewModel.kt` |
| | `ui`(直下) | `AppNavigation.kt`(画面遷移) |
| | `MainActivity.kt` | 画面遷移を呼び出す。**Aだけが編集する** |
| **B** | `data/`、`domain/` | 作成済み。新しい関数が必要になったときだけ追加 |
| **C** | `notification` | `ReminderScheduler.kt`、`ReminderWorker.kt`(WorkManagerの処理) |
| **D** | `usage` | `UsageLimitManager.kt`、使用状況を取得するクラス |

### 完成後のフォルダ構成(全体)

```
com.example.studyreward
├── MainActivity.kt          A
├── AppContainer.kt          B
├── StudyRewardApplication.kt B
├── data/                    B(作成済み)
├── domain/                  B(作成済み)
├── ui/                      A
│   ├── AppNavigation.kt
│   ├── home/
│   ├── task/
│   ├── reward/
│   └── schedule/
├── notification/            C
└── usage/                   D
```

### ルール

- **自分の担当のフォルダの中だけ**に、ファイルを作る。
- 他の担当のフォルダにファイルを足したくなったら、先にその担当に相談する(競合を防ぐため)。
- ほかの人も使うクラス(共通のデータ型など)は、自分で作らず、Bに相談する。
- `AndroidManifest.xml` を変更する必要がある場合(通知の権限、使用状況の権限など)は、
  **C・Dが自分の分だけ追加してよい**。変更箇所が重ならないよう、PRで内容を伝える。
- ファイル名・クラス名は、SPEC.mdに書かれているものを使う。

---

## A:画面・UI担当

**ブランチ**: `feature/ui`(細かく分けるなら `feature/task-form` など)
**触るフォルダ**: `ui/`(DAO・Databaseは直接触らない。必ずRepository経由)

### 今週(第1週)

- [ ] ホーム画面の見た目を作る(ポイント残高、課題の件数など。データは仮でよい)
- [ ] 画面遷移の仕組み(`navigation-compose`)を入れて、ホーム → 課題一覧 → 課題登録を移動できるようにする
- [ ] 課題登録画面の入力欄を作る(課題名、締切、難易度1〜3)

### 第2週

- [ ] 課題一覧画面: `taskRepository.getAllTasks()` で表示する
- [ ] 課題登録: `taskRepository.addTask(...)` で保存する
- [ ] 課題の完了ボタン: `taskRepository.completeTask(...)` を呼び、`CompleteTaskResult` の結果(獲得ポイント / 完了済み / 見つからない)に応じて表示を変える
- [ ] ポイント残高の表示: `rewardRepository.getPointsBalance()`
- [ ] 入力チェック(課題名が空、難易度が範囲外など)

### 第3週

- [ ] ポイント交換画面: `rewardRepository.exchangePointsForBonus(...)` の結果(`ExchangeResult`)を表示する
- [ ] 時間割画面: `scheduleRepository` を使う。登録時の `IllegalArgumentException` は `e.message` をエラーとして表示する
- [ ] 空きコマの表示

### 注意

- 画面を先に作りたいときは、仮のデータで見た目だけ作ってよい。
- 保存処理を自分で書かない。足りない関数があればBに相談する。

---

## B:共通データ・保存処理担当

**ブランチ**: 作業ごとに新しく作る
**触るもの**: `data/`、`domain/`、Gradle設定、GitHub運用

### 完了済み

- [x] プロジェクト作成、GitHub運用
- [x] Room(Entity・DAO・Database)
- [x] Repository(Task・Reward・Schedule・AppLimit)、PointCalculator
- [x] AppContainer、Application
- [x] SPEC.md、依存関係(WorkManager・Navigation・ViewModel)
- [x] 単体テスト(ポイント計算、空きコマ、完了・交換のDBテスト)

### これから

- [ ] README.md・CONTRIBUTING.md・TASKS.md を `main` に入れる
- [ ] 3人の進捗を確認する(ビルドできたか、ブランチを作ったか)
- [ ] **Dの検証結果を聞く**(使用時間の計測・制限ができそうか)
- [ ] Dに必要なDAO関数を追加する(使用時間の加算、日付リセットなど。Dと相談)
- [ ] 課題の登録・更新・削除と、Cの通知の連動を確認する
- [ ] 週1回以上、`main` で全員の変更を統合し、ビルド・動作を確認する
- [ ] 第4週: 全体のテスト、バグ修正、最終ビルド

### いつもやること

- PRのMerge前に、ビルドが通るか確認する
- 誰かが詰まっていないか見る(Git・Gradleの質問窓口)

---

## C:時間割・通知担当

**ブランチ**: `feature/notification`(時間割なら `feature/schedule` など)
**触るもの**: `notification/`(時間割の保存と空きコマ計算はBが作成済みの `ScheduleRepository` を使う)

### 今週(第1週)

- [ ] `ReminderScheduler.kt` を `notification/` に作る(関数の形は SPEC.md の第7章)
- [ ] WorkManager で、**テスト通知を1回出せる**ところまで試す
- [ ] 通知の権限(Android 13以上は `POST_NOTIFICATIONS`)の取り方を確認する

### 第2週

- [ ] 課題の締切通知: 締切の前日と当日
  - 締切なし・締切超過の課題には通知しない
  - 課題を削除したら通知も取り消す
  - 締切を変更したら、前の通知を取り消して再設定する
- [ ] 空きコマ通知: `scheduleRepository.getFreePeriods(dayOfWeek)` の結果から、開始時刻に通知する
- [ ] 時間割が変わったら通知を更新する

### 第3週

- [ ] Aの画面・Bの課題処理と通知を接続する(課題の登録・更新・削除と連動)
- [ ] 通知のテスト(権限あり・なし、アプリ終了後など)

### 注意

- 通知は指定時刻ぴったりに届くとは限らない。**まずは届くことを目標**にする。
- 時間割の保存や空きコマ計算が足りない場合は、自分で作らずBに相談する。

---

## D:使用時間制限担当

**ブランチ**: `feature/usage-limit`
**触るもの**: `usage/`

**このアプリで一番不確実な部分。第1週のうちに「できそうか」を判断する。**

### 今週(第1週) ― 技術検証

- [ ] 他のアプリの使用時間を取得できるか、**実機**で試す(使用状況へのアクセス許可、`UsageStatsManager` など)
- [ ] 権限がない場合に、設定画面へ案内できるか確認する
- [ ] 上限に達したとき、ホーム画面に戻すなどで「使いにくくする」ことが実機でできるか試す
- [ ] 結果を **Bに報告する**(できた / 一部できた / 難しい)
- [ ] 難しい場合: 「YouTubeやInstagramのアプリ全体」を対象にした制限に絞る相談をする

### 第2週

- [ ] `UsageLimitManager.kt` を `usage/` に作る(関数の形は SPEC.md の第7章)
- [ ] 残り使用時間の計算: (基本上限 + 追加時間) × 60 − 使用秒数
- [ ] 日付が変わったときの使用時間のリセット
- [ ] 対象アプリの登録: `appLimitRepository.registerApp(packageName, dailyLimitMinutes)`
- [ ] 必要な関数があれば Bに相談する(DAOへの追加が必要な場合がある)

### 第3週

- [ ] 上限到達時の利用制限を、他の機能と統合する
- [ ] 難しければ制限の範囲を縮小する(警告表示まで、など)

### 注意

- 端末・Androidのバージョン・権限で動作が変わる。**実機で動いた範囲**を完成とする。
- 「Shortsだけを制限する」は保証しない。

---

## 週ごとの目標

| 週 | 全体の目標 |
|---|---|
| 第1週 | 全員がビルドできる。基盤ができる。Dが「制限できそうか」を判断する |
| 第2週 | 各自の機能を並行して作る(課題登録・一覧、完了でポイント、時間割と空きコマ、制限の基本方式) |
| 第3週 | 機能をつなぐ。必須機能(課題 → ポイント → 交換)が最初から最後まで操作できる |
| 第4週 | テスト・修正・発表準備。**新しい大きな機能は増やさない** |

## 迷ったら

- 何をすればいいか分からない → このファイルの自分の担当を、上から順に
- 関数の名前や型が分からない → SPEC.md
- Gitで困った → CONTRIBUTING.md
- それでも分からない → グループチャットで聞く(**30分以上1人で悩まない**)
