<template>
  <div class="attendance-board">

    <div class="attendance-board">
      <h3>ようこそ、{{ userName }} さん</h3>
      <div class="status-badge" :class="status">
        現在の状態: {{ status === 'CLOCKED_IN' ? labels.inActive : labels.outActive }}
      </div>

      <div class="actions">
        <button v-if="status === 'CLOCKED_OUT'" @click="punch('clock-in', labels)" class="btn-in">
          {{ labels.inAction }}
        </button>
        <button v-else-if="status === 'CLOCKED_IN'" @click="punch('clock-out', labels)" class="btn-out">
          {{ labels.outAction }}
        </button>
        <button v-else disabled class="btn btn-disabled">読み込み中...</button>
      </div>
    </div>

    <hr class="divider">

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

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import apiClient from '../api';
const history = ref([]);

// ローカルストレージから取得したIDを保持するリアクティブ変数
const loggedInAccountId = ref('');
const userName = ref('');

// AttendanceBoard.vue の <script setup> 内に追加するコード

// 履歴データを格納する配列（※もし既存の変数名があればそれに合わせてください）
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

const props = defineProps({
  accountId: String,
  userName: String,
  mode: String,
  history: {
    type: Array,
    default: () => [] // 渡されない場合は空配列をデフォルトにする
  }
});

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

// 現在のモード（propsから受け取る。デフォルトは 'attendance'）
const labels = computed(() => labelSettings[props.mode || 'attendance']);

const status = ref('CLOCKED_OUT');

// ステータス取得
const fetchStatus = async () => {
  try {
    const response = await apiClient.get(`/attendance/status?accountId=${loggedInAccountId.value}`);
    status.value = response.data; // "CLOCKED_IN" or "CLOCKED_OUT"
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
const punch = async (type, labels) => {
  try {
    // 1. クエリパラメータ形式から、JSON ボディ形式に変更
    // バックエンドの @RequestBody Map<String, String> request と一致させます
    await apiClient.post('/attendance/punch', {
      // accountId: props.accountId,      // route設定前であればprops.accountIdから取得するのでよい
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
</style>