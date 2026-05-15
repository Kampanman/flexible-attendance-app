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
        <button v-if="status === 'CLOCKED_IN'" @click="punch('clock-out', labels)" class="btn-out">
          {{ labels.outAction }}
        </button>
      </div>
    </div>

    <hr class="divider">

    <div class="history-section">
      <h4>最近の履歴</h4>
      <div v-if="history.length === 0" class="no-data">履歴はありません</div>
      <ul v-else class="history-list">
        <li v-for="record in history" :key="record.id" class="history-item">
          <span class="type-badge" :class="record.type">
            {{ record.type === 'CLOCK_IN' ? labels.inAction : labels.outAction }}
          </span>
          <span class="timestamp">
            {{ new Date(record.createdAt).toLocaleString('ja-JP') }}
          </span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import apiClient from '../api';
const history = ref([]);

const props = defineProps(['accountId', 'userName', 'mode']);

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
    const response = await apiClient.get(`/attendance/status?accountId=${props.accountId}`);
    status.value = response.data; // "CLOCKED_IN" or "CLOCKED_OUT"
  } catch (error) {
    console.error('ステータス取得失敗', error);
  }
};

// 履歴取得
const fetchHistory = async () => {
  try {
    const response = await apiClient.get(`/attendance/history?accountId=${props.accountId}`);
    history.value = response.data;
  } catch (error) {
    console.error('履歴取得失敗', error);
  }
};

// 打刻処理
const punch = async (type) => {
  try {
    await apiClient.post(`/attendance/${type}?accountId=${props.accountId}`);
    // 打刻が成功したら、ステータスと履歴の両方を更新する
    await fetchStatus();
    await fetchHistory();
  } catch (error) {
    alert('打刻に失敗しました。');
  }
};

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
</style>