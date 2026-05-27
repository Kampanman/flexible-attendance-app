<template>
  <div class="attendance-board">
    
    <div class="attendance-header-box">
      <h3>ようこそ、{{ userName }} さん</h3>
    </div>

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

    <div class="system-mode-indicator">
      <span class="indicator-tag">アプリケーション稼働モード：</span>
      <strong class="indicator-text">
        {{ currentMode === 'attendance' ? '勤怠モード' : currentMode === 'room' ? '入退室モード' : '出席退席モード' }}
      </strong>
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
import { computed, onMounted, ref } from 'vue';
import apiClient from '../api';

const history = ref([]);
const loggedInAccountId = ref('');
const userName = ref('');

// 💡 変更点：初期値は 'attendance' とし、文字列で管理する構造を維持します
const currentMode = ref('attendance'); 
const attendanceHistory = ref([]); 

// モードの数値(0, 1, 2)と、既存の文字列キー('attendance', 'room', 'session')をマッピングする辞書
const modeMapping = {
  0: 'attendance', // 勤怠モード
  1: 'room',       // 入退室モード
  2: 'session'     // 出席退席モード
};

// 💡 追加：バックエンドから最新の一括打刻モードを取得する関数
const fetchSystemMode = async () => {
  try {
    // 先ほどJava側で作成した GET /api/system/mode を呼び出す
    const response = await apiClient.get('/system/mode');
    const modeNumber = response.data.mode; // 0, 1, 2 が返ってくる
    
    // 数値を対応する文字列キーに変換して、currentModeに格納
    currentMode.value = modeMapping[modeNumber] || 'attendance';
    console.log(`[System] 最新の打刻モードを自動適用しました: ${currentMode.value}`);
  } catch (error) {
    console.error('打刻モードの取得に失敗しました。デフォルト（勤怠）で動作します:', error);
  }
};

// バックエンドから打刻履歴を取得する関数
const fetchAttendanceHistory = async (id) => {
  if (!id) return;
  try {
    const response = await apiClient.get(`/attendance/history/${id}`);
    attendanceHistory.value = response.data;
  } catch (error) {
    console.error('打刻履歴の取得に失敗しました:', error);
  }
};

// モードに応じた各種ラベルの定義（既存の定義をそのまま活用）
const labelSettings = {
  attendance: { inActive: '勤務中', outActive: '未出勤', inAction: '出勤', outAction: '退勤' },
  room:       { inActive: '入室中', outActive: '退室済', inAction: '入室', outAction: '退室' },
  session:    { inActive: '出席中', outActive: '退席中', inAction: '出席', outAction: '退席' }
};

// 現在選択されているモードのラベル群を返す
const currentLabels = computed(() => {
  return labelSettings[currentMode.value] || labelSettings.attendance;
});

const status = ref('CLOCKED_OUT');

// ステータス取得API
const fetchStatus = async () => {
  if (!loggedInAccountId.value) return;
  try {
    const response = await apiClient.get(`/attendance/status?accountId=${loggedInAccountId.value}`);
    status.value = response.data; 
  } catch (error) {
    console.error('ステータス取得失敗', error);
  }
};

// 履歴取得
const fetchHistory = async () => {
  try {
    const response = await apiClient.get(`/attendance/history/${loggedInAccountId.value}`);
    history.value = response.data;
  } catch (error) {
    console.error('履歴取得失敗', error);
  }
};

// 履歴をペアリングして直近10件分を返すロジック
const pairedHistory = computed(() => {
  if (!history.value || !Array.isArray(history.value)) return [];

  const result = [];
  const sortedHistory = [...history.value].reverse();
  
  for (let i = 0; i < sortedHistory.length; i++) {
    const record = sortedHistory[i];

    if (record.type === 'clock-in') {
      const nextRecord = sortedHistory[i + 1];
      if (nextRecord && nextRecord.type === 'clock-out') {
        result.push({
          date: formatDate(record.createdAt),
          inTime: formatTime(record.createdAt),
          outTime: formatTime(nextRecord.createdAt)
        });
        i++; 
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

const emit = defineEmits(['refresh-history']);

// 打刻処理
const punch = async (type) => {
  try {
    await apiClient.post('/attendance/punch', {
      accountId: loggedInAccountId.value,
      type: type  
    });

    await fetchStatus();
    await fetchHistory();
    emit('refresh-history');
    
    fetchAttendanceHistory(loggedInAccountId.value);
  } catch (error) {
    console.error('打刻エラー:', error);
    alert('打刻に失敗しました。');
  }
};

const formatDate = (dateStr) => new Date(dateStr).toLocaleDateString('ja-JP');
const formatTime = (dateStr) => new Date(dateStr).toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

// 💡 画面立ち上げ時の処理をブラッシュアップ
onMounted(async () => {
  // 1. まずは管理者が指定した「最新のモード」を裏でサッと取得
  await fetchSystemMode();

  // 2. ユーザー情報の復元と、ステータス・履歴の読み込み
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId;
    userName.value = user.userName;
    
    // ユーザーに紐づくデータをロード
    fetchStatus();
    fetchHistory();
    fetchAttendanceHistory(loggedInAccountId.value);
  } else {
    console.error("ユーザー情報が見つかりません。");
  }
});
</script>

<style scoped>
.attendance-board { text-align: center; margin-top: 50px; max-width: 600px; margin-left: auto; margin-right: auto; padding: 20px; }
.attendance-header-box { margin-bottom: 20px; }
.status-display { margin-bottom: 25px; font-size: 1.1rem; font-weight: bold; }
.status-in { background-color: #e3f2fd; color: #1976d2; padding: 6px 16px; border-radius: 20px; }
.status-out { background-color: #f5f5f5; color: #616161; padding: 6px 16px; border-radius: 20px; }

.punch-actions { margin-bottom: 30px; }
.btn-in { background-color: #4caf50; color: white; padding: 18px 40px; font-size: 1.4rem; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; box-shadow: 0 4px 6px rgba(76,175,80,0.2); transition: background-color 0.2s; }
.btn-in:hover { background-color: #43a047; }
.btn-out { background-color: #f44336; color: white; padding: 18px 40px; font-size: 1.4rem; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; box-shadow: 0 4px 6px rgba(244,67,54,0.2); transition: background-color 0.2s; }
.btn-out:hover { background-color: #e53935; }

/* 💡 追加：現在のシステム稼働モード案内用インジケーターの装飾 */
.system-mode-indicator {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 10px 15px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  margin-top: 10px;
  margin-bottom: 10px;
}
.indicator-tag {
  color: #6c757d;
  font-size: 0.8rem;
  background-color: #e9ecef;
  padding: 2px 8px;
  border-radius: 4px;
}
.indicator-text {
  color: #212529;
}

.divider { margin: 30px 0; border: none; border-top: 1px solid #e9ecef; }
.history-section h3 { color: #495057; margin-bottom: 15px; }
.history-list { list-style: none; padding: 0; margin: 0; }
.history-item {
  display: flex;
  justify-content: center;
  gap: 15px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f3f5;
  font-family: monospace;
  font-size: 0.95rem;
  color: #495057;
}
.separator { color: #ced4da; }
</style>