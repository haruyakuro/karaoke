# StudyReward(仮)

課題をこなすとポイントが貯まり、そのポイントで娯楽アプリの利用時間を増やせる、学生向けのタスク管理アプリ(Android)。

> やるべきことを終わらせて、好きなことを楽しむ。

## 読む順番

迷ったら、次の順で読む。

1. **README.md**(このファイル): アプリの概要と、動かし方
2. **TASKS.md**: 自分の担当は何をすればいいか
3. **CONTRIBUTING.md**: ブランチ・PRなど、Gitの使い方のルール
4. **SPEC.md**: データクラスと関数の仕様(コードを書くときに見る)

## アプリの流れ

1. 課題を登録する(課題名・締切・難易度)
2. 課題を完了するとポイントを獲得する(難易度10/20/30pt、締切まで48時間以上あれば+10pt)
3. ポイントを娯楽アプリの追加利用時間に交換する(10pt = 5分)
4. 時間を使い切ったら、利用制限を試みる

## 機能の優先順位

| 優先度 | 内容 |
|---|---|
| A(必須) | 課題の登録・一覧・完了、ポイント獲得、ポイント残高表示、ポイント交換、再起動してもデータが残る |
| B(できれば) | 時間割・空きコマ、締切通知・空きコマ通知、使用時間の計測、残り時間の表示 |
| C(余裕があれば) | 上限到達時の利用制限、Shortsだけの判定、グラフ |

**最優先は「課題登録 → ポイント獲得 → ポイント交換」を完成させること。**

## 技術構成

| 項目 | 内容 |
|---|---|
| 言語 | Kotlin |
| 画面 | Jetpack Compose |
| データ保存 | Room(端末内のみ。サーバー・ログインなし) |
| 通知 | WorkManager |
| ビルド | Android Studio / Gradle |

バージョンの詳細は `gradle/libs.versions.toml` を見る。**Gradle設定を変更できるのはBのみ。**

## 動かし方

1. このリポジトリをCloneする(Android Studio → Clone Repository)
2. Android Studioで開き、Gradle Syncが終わるまで待つ
3. エミュレーターか実機を選び、▶ で実行する

### よくある警告

- ビルド時にKSP・Kotlinに関する警告が出ても、`BUILD SUCCESSFUL` なら問題ない。
- `gradle.properties` の `android.disallowKotlinSourceSets=false` は消さない。

## フォルダ構成

```
com.example.studyreward/
├── MainActivity.kt
├── AppContainer.kt            Repositoryをまとめて作る場所
├── StudyRewardApplication.kt
├── data/                      データベース(Entity・DAO・Repository)
├── domain/                    ポイント計算などのルール
├── ui/                        画面(A担当)
├── notification/              通知(C担当)
└── usage/                     使用時間の制限(D担当)
```

## 担当

| 担当 | 役割 |
|---|---|
| A | 画面・UI |
| B | 共通データ・保存処理、Gradle、GitHub運用 |
| C | 時間割・通知 |
| D | 使用時間の計測・制限(技術検証) |

## テストの実行

- ポイント計算・空きコマ: `app/src/test` のテストを右クリック → Run
- DBを使うテスト: エミュレーターを起動して、`app/src/androidTest` のテストを実行

## 今回やらないこと

ログイン、クラウド同期、複数端末対応、ランキング、決済、ShortsやReelsの100%正確な判定、他アプリのOSレベルでの強制終了。
