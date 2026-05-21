<template>
  <div class="attendance-board">

    <div class="attendance-board">
      <h3>ようこそ、{{ userName }} さん</h3>

    <div class="status-display">
      <p>現在の状態：
        <span :class="status === 'CLOCKED_IN' ? 'status-in' : 'status-out'">
          {{ status === 'CLOCKED_IN' ? currentLabels.inActive : currentLabels.outActive }}
        </span>
      </p>
    </div>

    <div class="punch-actions">
      <button 
        v-if="status === 'CLOCKED_OUT'" 
        @click="punch('CLOCK_IN')" 
        class="btn btn-in"
      >
        {{ currentLabels.inAction }}する
      </button>

      <button 
        v-else 
        @click="punch('CLOCK_OUT')" 
        class="btn btn-out"
      >
        {{ currentLabels.outAction }}する
      </button>
    </div>

    <div class="mode-switcher" v-if="status === 'CLOCKED_OUT'">
      <button @click="currentMode = 'attendance'" :class="{ active: currentMode === 'attendance' }">勤怠モード</button>
      <button @click="currentMode = 'room'" :class="{ active: currentMode === 'room' }">入退室モード</button>
      <button @click="currentMode = 'session'" :class="{ active: currentMode === 'session' }">出席退席モード</button>
    </div>

    </div>

    <hr class="divider">

    <div class="history-section">
      <h3>最近の履歴（直近10件）</h3>
      <ul class="history-list">
        <li v-for="(entry, index) in pairedHistory" :key="index" class="history-item">
          <span class="date">{{ entry.date }}</span>
          <span class="time">Entry: {{ entry.inTime }}</span>
          <span class="separator"> ～ </span>
          <span class="time">Exit: {{ entry.outTime }}</span>
        </li>
      </ul>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import apiClient from '../api';
const history = ref([]);

// ローカルストレージから取得したIDを保持するリアクティブ変数
const loggedInAccountId = ref('');
const userName = ref('');

// 現在の表示モードを管理する変数（初期値を 'attendance' に設定）
const currentMode = ref('attendance'); 

// 履歴データを格納する配列
const attendanceHistory = ref([]); 

/**
 * バックエンドから打刻履歴を取得する関数
 * @param {String} id - ログインユーザーのaccountId
 */
const fetchAttendanceHistory = async (id) => {
  if (!id) return;
  try {
    // バックエンドの履歴取得API（GET /api/attendance/history/{accountId}）を叩く
    const response = await apiClient.get(`/attendance/history/${id}`);
    
    // 取得したデータを履歴配列にセットする
    attendanceHistory.value = response.data;
  } catch (error) {
    console.error('打刻履歴の取得に失敗しました:', error);
  }
};

onMounted(() => {
  // LoginForm.vue が保存した 'user' データを読み出す
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId; // ★これで正しいIDがセットされます
    userName.value = user.userName;
  } else {
    console.error("ユーザー情報が見つかりません。");
  }

  // 既存の初期化処理（履歴の取得など）があれば、loggedInAccountId.value を使って呼び出す
  if (loggedInAccountId.value) fetchAttendanceHistory(loggedInAccountId.value);
});

// モードに応じたラベルの定義
const labelSettings = {
  attendance: { inActive: '勤務中', outActive: '未出勤', inAction: '出勤', outAction: '退勤' },
  room:       { inActive: '入室中', outActive: '退室済', inAction: '入室', outAction: '退室' },
  session:    { inActive: '出席中', outActive: '退席中', inAction: '出席', outAction: '退席' }
};

// 現在選択されているモードのラベル群を返す
const currentLabels = computed(() => {
  return labelSettings[currentMode.value] || labelSettings.attendance;
});

// 現在のモード（propsから受け取る。デフォルトは 'attendance'）
const labels = computed(() => labelSettings[props.mode || 'attendance']);

const status = ref('CLOCKED_OUT');

// 現在の就業ステータス
const currentStatus = ref(0)

// ステータス取得API
const fetchStatus = async () => {
  if (!loggedInAccountId.value) return;
  try {
    const response = await apiClient.get(`/attendance/status?accountId=${loggedInAccountId.value}`);
    status.value = response.data; // バックエンドから "CLOCKED_IN" または "CLOCKED_OUT" が届く
  } catch (error) {
    console.error('ステータス取得失敗', error);
  }
};

// 履歴取得
const fetchHistory = async () => {
  try {
    const response = await apiClient.get(`/attendance/history/${loggedInAccountId.value}`);
    console.log("バックエンドから届いた生の履歴データ:", response.data);

    history.value = response.data;
  } catch (error) {
    console.error('履歴取得失敗', error);
  }
};

// 履歴をペアリングして直近10件分を返すロジック
const pairedHistory = computed(() => {
  // historyが存在しない、または空の場合は即座に空配列を返す
  // if (!props.history || !Array.isArray(props.history)) return [];
  
  // props.history ではなく、history.valueを見に行くようにします
  if (!history.value || !Array.isArray(history.value)) return [];

  const result = [];
  const sortedHistory = [...history.value].reverse(); // 存在する時だけスプレッド演算子でコピーを作る
  
  for (let i = 0; i < sortedHistory.length; i++) {
    const record = sortedHistory[i];

    if (record.type === 'clock-in') {
      // 次のレコードが clock-out かつ同じペアになるべきものかチェック
      const nextRecord = sortedHistory[i + 1];
      if (nextRecord && nextRecord.type === 'clock-out') {
        result.push({
          date: formatDate(record.createdAt),
          inTime: formatTime(record.createdAt),
          outTime: formatTime(nextRecord.createdAt)
        });
        i++; // 退室レコード分をスキップ
      } else {
        // 退室がない場合は進行中として表示
        result.push({
          date: formatDate(record.createdAt),
          inTime: formatTime(record.createdAt),
          outTime: '-'
        });
      }
    }
  }

  // 最新の10件を返す（再び最新が上に来るように逆転させる）
  return result.reverse().slice(0, 10);
});

// emit の定義
const emit = defineEmits(['refresh-history']);

// 打刻処理
const punch = async (type) => {
  try {
    // 1. クエリパラメータ形式から、JSON ボディ形式に変更
    // バックエンドの @RequestBody Map<String, String> request と一致させます
    console.log("accountId: " + loggedInAccountId.value + ", type: " + type);
    await apiClient.post('/attendance/punch', {
      accountId: loggedInAccountId.value, // route設定後はpropsではなく、ローカルストレージから復元した変数を指定
      type: type  // 'CLOCK_IN' または 'CLOCK_OUT'
    });

    // 2. 打刻が成功したら、ステータスと履歴を更新
    // fetchStatus は「現在の状態：入室中」などの表示更新
    // fetchHistory は「最近の履歴」リストの更新を担当します
    await fetchStatus();
    await fetchHistory();

    // 3. 親（App.vue）側でも履歴を管理している場合は、イベントを飛ばす
    // ※もしApp.vueのattendanceHistoryを更新したい場合に有効です
    emit('refresh-history');
    
    // 打刻が成功したので、最新の履歴を再取得して画面をパッと更新する！
    fetchAttendanceHistory(loggedInAccountId.value);
    
    // ステータス（現在入室中か退室中か）の再取得関数があればそれも呼ぶ
    if (typeof fetchStatus === 'function') fetchStatus();

    // 成功のフィードバック（任意）
    console.log(`${type} 成功`);
  } catch (error) {
    console.error('打刻エラー:', error);
    alert('打刻に失敗しました。');
  }
};

// 日時フォーマット用の補助関数
const formatDate = (dateStr) => new Date(dateStr).toLocaleDateString('ja-JP');
const formatTime = (dateStr) => new Date(dateStr).toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

onMounted(() => {
  fetchStatus();
  fetchHistory(); // 画面が開いたときに履歴も読み込む
});
</script>

<style scoped>
.attendance-board { text-align: center; margin-top: 50px; }
.status-badge { display: inline-block; padding: 10px 20px; border-radius: 20px; margin-bottom: 20px; font-weight: bold; }
.CLOCKED_IN { background-color: #e3f2fd; color: #1976d2; }
.CLOCKED_OUT { background-color: #f5f5f5; color: #616161; }
.btn-in { background-color: #4caf50; color: white; padding: 15px 30px; font-size: 1.2rem; border: none; border-radius: 5px; cursor: pointer; }
.btn-out { background-color: #f44336; color: white; padding: 15px 30px; font-size: 1.2rem; border: none; border-radius: 5px; cursor: pointer; }
.label { color: #aaa; font-size: 0.9em; }
.time { min-width: 80px; }
.separator { margin: 0 5px; color: #888; }
.history-list { list-style: none; padding: 0; margin: 20px 0; }
.history-item {
  display: flex;
  justify-content: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #444; /* 各行に薄い区切り線 */
  font-family: monospace; /* 時間の数字を揃えやすくするため */
}

/* モード切り替えボタンのコンテナ（親要素） */
.mode-switcher {
  display: flex;            /* ボタンを横並びにする */
  justify-content: center;  /* 中央寄せにする */
  gap: 12px;                /* ★ボタンとボタンの間の余白（隙間）を適度にとる */
  margin-top: 25px;         /* 打刻エリアとの上下のディスタンス */
  margin-bottom: 20px;
  flex-wrap: wrap;          /* 画面幅が狭いときは自動で綺麗に折り返すようにする安全策 */
}

/* モード切り替えボタン単体のデザイン */
.mode-switcher button {
  background-color: #f8f9fa; /* 普段は主張しすぎない上品な薄いグレー */
  color: #495057;
  border: 1px solid #ced4da;
  padding: 10px 16px;        /* ★上下10px、左右16pxの内側余白をとり、押しやすい大きさに */
  border-radius: 6px;        /* 角を少し丸めてモダンな印象に */
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease; /* マウスを乗せた時やクリックした時の変化を滑らかにする */
}

/* マウスホバー時のエフェクト */
.mode-switcher button:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
}

/* 現在アクティブ（選択中）なモードのボタンを強調するスタイル */
/* もしHTML側で「:class="{ active: currentMode === 'attendance' }"」のように制御する場合に輝きます */
.mode-switcher button.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
  box-shadow: 0 2px 4px rgba(0, 123, 255, 0.2);
}
</style>