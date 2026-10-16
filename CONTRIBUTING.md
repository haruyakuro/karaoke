# CONTRIBUTING.md(開発のルール)

4人で同じアプリを作るためのGitの使い方。迷ったらここを見る。

## 守る約束(これだけは必ず)

1. **`main` に直接Pushしない。** 必ずブランチ→PR→Mergeの順で入れる。
2. **ビルドが通らない状態でPRを出さない。**
3. **Gradle設定(`build.gradle.kts`、`libs.versions.toml`、`gradle.properties`)は自分で変更しない。** ライブラリが必要ならBに連絡する。
4. **`.idea/`、`local.properties`、`build/` はCommitしない。**
5. **共通仕様(`SPEC.md`)の名前・型・関数を勝手に変えない。** 変えるときは、先にSPEC.mdを更新して関係者に連絡する。
6. **担当外のファイルを大きく編集しない。** 競合の原因になる。

## ブランチの名前

| 担当 | ブランチの例 |
|---|---|
| A | `feature/ui`、`feature/task-form` |
| B | `feature/task-data` など |
| C | `feature/notification`、`feature/schedule` |
| D | `feature/usage-limit` |

**1つのブランチ = 1つの作業 = 1つのPR。** PRをMergeしたら、そのブランチは使い回さず、次の作業は `main` から新しいブランチを作る。

## 作業の流れ

### 作業を始めるとき

1. Android Studio上部の `main` をクリック → `main` に切り替える
2. **Update**(Pull)で最新にする
3. `main` → **New Branch...** で、自分の作業用ブランチを作る

### 作業中

1. コードを書く
2. こまめにCommitする(メッセージは何をしたか短く書く。例: `課題登録画面を追加`)
3. 区切りがついたら Push する

### PRを出すとき

1. **Build → Make Project** で `BUILD SUCCESSFUL` を確認する
2. アプリを実行して、自分が作った部分が動くことを確認する
3. Commit画面で、**必要なファイルだけ**にチェックが入っているか確認する
4. Commit and Push する
5. GitHubを開き、**Compare & pull request**(または Pull requests → New pull request)を押す
6. タイトルと説明を書く(何を作ったか、どう確認したか)
7. **Create pull request**
8. グループチャットで「PR出しました」とURLを共有する
9. 他のメンバー(またはB)が確認したら、**Merge pull request** → **Confirm merge**

### Merge後

1. `main` に切り替えて Update する
2. 他の人のブランチで作業中なら、自分のブランチに `main` を取り込む(`main` を選んで **Merge 'main' into '自分のブランチ'**)

## 他の人の変更を取り込む

誰かが `main` にMergeしたら、**自分の作業ブランチにも取り込む**。放っておくと、あとで大きな競合になる。

1. `main` に切り替えて **Update**
2. 自分のブランチに戻る
3. `main` を選んで **Merge 'main' into '自分のブランチ'**
4. ビルドが通ることを確認する

## 競合(conflict)が出たら

- **慌てて上書きしない。** よく分からないまま片方を採用すると、相手の作業が消える。
- どちらの変更を残すか確認してから解決する。
- 迷ったらBに相談する(スクリーンショットを送る)。

## Commitしないファイル

Commit画面で、次のものはチェックを外す。

- `.idea/` 以下のすべて
- `local.properties`
- `build/`、`.gradle/`
- `androidTestResultsUserPreferences.xml`、`deploymentTargetSelector.xml` など、自分のPC用の設定

## コミットメッセージ

何をしたかが分かる短い文にする。

- 良い例: `課題一覧画面を追加`、`空きコマの計算を修正`
- 悪い例: `更新`、`修正`、`あああ`

## 仕様を変えたいとき

1. `SPEC.md` を先に直す
2. 関係する担当に連絡する
3. 呼び出している側のコードも、**同じPRで**直す
4. ビルドが通ることを確認してからPRを出す

## 困ったとき

次の情報を添えて、グループチャットで聞く。

- どの操作をしたか(例: Syncを押した、Pushした)
- エラーメッセージ(スクリーンショットか、最初の数行)
- 今のブランチ名

**1人で30分以上悩まない。** 早く聞くほうが、チーム全体では速い。
