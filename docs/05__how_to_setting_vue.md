---
marp: true
style: |
  section p, section li, section th, section td {
    font-size: 24px;
  }
  section.frontpage h1 {
    text-align: center;
    margin-bottom: 2em;
  }
---
<!-- _class: frontpage -->
# フロントエンド開発の準備

バックエンドに一通りの基礎機能を構築してきたところで、ここからは **フロントエンド（Vue.js）編** に入りましょう。まずは、GitHub Codespaces上でVue.jsを動かすための土台作りから進めます。

---

## フロントエンド・プロジェクトの作成

バックエンドを起動しているターミナルとは別に（または一度止めて）、リポジトリのルートディレクトリで以下の操作を行います。

```bash
# プロジェクトのルートに移動
cd /workspaces/flexible-attendance-app

# Vue.jsのプロジェクトを作成（viteという高速ツールを使います）
# ※途中でいくつか質問されます。
npm create vite@latest frontend -- --template vue
```

### 設定の選択

コマンド実行後、もし選択肢が出た場合は以下のように進めてください。

* **Select a framework**: `Vue`
* **Select a variant**: `JavaScript` (今回はシンプルに進めるため)

---

## ライブラリのインストール

作成した `frontend` フォルダに移動し、必要な部品をインストールします。

```bash
cd frontend
npm install
# API通信を楽にするためのライブラリ「axios」も入れておきます
npm install axios
```

---

## フロントエンドの起動確認

まずは、真っさらなVue.jsが動くかどうかを確認します。

```bash
npm run dev
```

### Codespacesでの確認方法

1. 右下に「A service is running on port 5173...」というポップアップが出たら **「Open in Browser」** を押します。
2. ブラウザで Vue のロゴと「Vite + Vue」という文字が表示されれば、フロントエンドの心臓が動き出した証拠です！

---

## バックエンドと繋ぐための「最初の一歩」

フロントエンド開発をスムーズにするために、一つだけ大事なファイルを作成しておきましょう。
バックエンドのURLを毎回書かなくて済むように、**API通信の共通設定**を作ります。

* **場所**: `frontend/src` の中に新しく **`api.js`** を作成

```javascript
// frontend/src/api.js
import axios from 'axios';

// バックエンドのURLを指定（CodespacesのURLに合わせて変更が必要です）
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api', 
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;
```

---

## npm（Node Package Manager）とはなにか

`npm` は、フロントエンド開発において欠かすことのできない「相棒」のような存在です。一言でいうと **「JavaScript界の巨大な道具箱（ライブラリ）管理システム」** です。
`npm` は、**Node.js** という実行環境と一緒にインストールされるツールで、以下の役割を担っています。

### 便利な道具（パッケージ）をダウンロードする

今回実行した `npm install axios` がまさにこれです。「通信機能を一から自作するのは大変だから、世界中の誰かが作ってくれた便利な `axios` という道具を自分のプロジェクトに持ってくる」という処理を行っています。

* Javaでいうところの **Maven（pom.xml）** や **Gradle** とほぼ同じ役割です。

---

### プロジェクトの「台帳」を管理する

`frontend` フォルダにある `package.json` というファイルを見てみてください。
そこには「このプロジェクトを動かすには、どの道具のどのバージョンが必要か」というリストが書かれています。`npm install` とだけ打つと、この台帳を見て必要な道具をすべて一括で揃えてくれます。

### 開発用のコマンドを実行する

`npm run dev` のように、あらかじめ `package.json` に登録しておいたショートカット命令を実行します。

* `dev`: 開発用サーバーを立ち上げる
* `build`: 本番公開用のファイルに変換する

---

### 今回おこなった処理の意味

* **`npm create vite@latest ...`** : 「Vite（ヴィート）」という最新のプロジェクト雛形作成ツールを呼び出し、Vue.jsのフォルダ構成を自動生成しました。
* **`npm install`** : Vue.js本体や、内部で使う大量の部品を `node_modules` というフォルダにダウンロードしました。
* **`npm run dev`** : プログラムを即座にブラウザで確認できるように、ミニサーバーを起動しました。

### Javaエンジニアの視点で見ると

比較するとイメージが湧きやすいかもしれません。

| 機能 | Java (Spring Boot) | JavaScript (Vue.js) |
| --- | --- | --- |
| **管理ツール** | Maven / Gradle | **npm** / yarn / pnpm |
| **設定ファイル** | `pom.xml` / `build.gradle` | **`package.json`** |
| **部品の置き場** | `.m2` フォルダなど | **`node_modules`** フォルダ |

---

## 次のステップ：ログイン画面の作成

土台ができたら、まずは **「ログイン画面」** の作成に挑戦しましょう。
これまでは `curl` で真っ黒な画面にコマンドを打っていましたが、いよいよ「メールアドレスとパスワードを入力するボックス」を画面上に作っていきます。
