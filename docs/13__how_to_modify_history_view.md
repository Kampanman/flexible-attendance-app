---
marp: true
style: |
  section.frontpage h1 {
    text-align: center
  }
  section p, section li {
    font-size: 24px;
  }
---
<!-- _class: frontpage -->
# 打刻画面に履歴が反映されない問題の修正

画面に履歴が表示されない原因は、フロントエンド側で以下のどちらかの問題が起きていることです。

- バックエンドから受け取ったデータを、画面が監視している変数に正しくセットできていない
- データの文字形式がペアリング判定ロジックと一致していない

この問題を解決するために、Vue.js側で確認・修正すべき2つのポイントを提示します。

---

## `fetchHistory` でのデータ代入先を確認する

`AttendanceBoard.vue` の中で、バックエンドから取得したデータを**どの変数に代入しているか**を確認してください。

冒頭の修正で、`computed`（`pairedHistory`）は `props.history` を見に行くように作成しました。しかし、`AttendanceBoard.vue` 自体が直接APIからデータを取得している場合、`props.history` ではなく、コンネント自身が持つローカルの `ref`（例: `history`）を監視させる必要があります。

**`AttendanceBoard.vue` の `<script setup>` 内の修正・確認案:**

```javascript
import { ref, computed } from 'vue';

// 1. 履歴を保持するローカルのリアクティブ変数を定義しているか確認
const history.= ref([]);

const fetchHistory = async () => {
  try {
    const response = await apiClient.get(`/attendance/history/${props.accountId}`);
    console.log("バックエンドから届いた生の履歴データ:", response.data);
```

---

```javascript
    // ★重要：取得したデータをローカルの変数に代入する！
    history.value = response.data; 
  } catch (error) {
    console.error("履歴取得失敗", error);
  }
};

// 2. computed（pairedHistory）が参照する先を `history.value` に変更する
const pairedHistory = computed(() => {
  // props.history ではなく、上で定義した history.value を見に行くようにします
  if (!history.value || !Array.isArray(history.value)) return [];

  const result = [];
  const sortedHistory = [...history.value].reverse();

  for (let i = 0; i < sortedHistory.length; i++) {
    const record = sortedHistory[i];
    
    // 先ほど大文字・小文字のブレがあったため、安全のために .toUpperCase() を挟む
    const currentType = record.type ? record.type.toUpperCase() : '';
```

---

```javascript
    if (currentType === 'clock-in' || currentType === 'CLOCK_IN') {
      const nextRecord = sortedHistory[i + 1];
      const nextType = nextRecord && nextRecord.type ? nextRecord.type.toUpperCase() : '';
      
      if (nextType === 'clock-out' || nextType === 'CLOCK_OUT') {
        result.push({
          date: formatDate(record.createdAt),
          inTime: formatTime(record.createdAt),
          outTime: formatTime(nextRecord.createdAt)
        });
        i++; // 退室分をスキップ
      } else {
        result.push({
          date: formatDate(record.createdAt),
          inTime: formatTime(record.createdAt),
          outTime: '-'
        });
      }
    }
  }
  return result.reverse().slice(0, 10);
});
```

---

## テンプレート（HTML）の参照先を確認する

`AttendanceBoard.vue` の `<template>` 側で、ループ処理している変数が `pairedHistory` になっているか確認してください。

```html
<template>
  <div class="history-section">
    <h3>最近の履歴（直近10件）</h3>
    <ul class="history-list">
      <li v-for="(entry, index) in pairedHistory" :key="index" class="history-item">
        <span class="date">{{ entry.date }}</span>
        <span class="time">入室: {{ entry.inTime }}</span>
        <span class="separator"> ～ </span>
        <span class="time">退室: {{ entry.outTime }}</span>
      </li>
    </ul>
  </div>
</template>
```

---

### まとめ：何が起きていたか？

これまでは `App.vue` から `props` 経由で履歴を渡そうとしていましたが、`AttendanceBoard.vue` は自ら `fetchHistory()` を実行してデータを取得する構造になっています。

そのため、**「APIで取ってきた4件のデータ（`response.data`）を、`computed` が読み込んでいる変数（`history.value`）にパッと入れてあげる」** ように繋ぎ変えるだけで、Vueがデータの変更を検知し、画面に履歴が自動で描画されるようになります。
