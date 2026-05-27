<template>
  <div class="attendance-edit-container">
    <h2>打刻内容編集申請</h2>
    <p class="subtitle">当月1か月分の打刻履歴の確認と、修正申請が行えます。</p>

    <div class="month-selector">
      <button @click="changeMonth(-1)" class="btn-arrow">◀ 前月</button>
      <span class="current-month-text">{{ currentYearMonth }}</span>
      <button @click="changeMonth(1)" class="btn-arrow">次月 ▶</button>
    </div>

    <div class="calendar-list-wrapper">
      <table class="calendar-table">
        <thead>
          <tr>
            <th>日付</th>
            <th>確定 Entry</th>
            <th>確定 Exit</th>
            <th>申請状態</th>
            <th>操作</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="day in calendarList" :key="day.recordDate" :class="getRowClass(day.recordDate)">
            <td class="date-col">{{ formatDate(day.recordDate) }}</td>
            
            <td><span class="time-text">{{ day.entryTime }}</span></td>
            <td><span class="time-text">{{ day.exitTime }}</span></td>
            
            <td>
              <span v-if="day.isTimechangeDemand === 1" class="status-badge" :class="getStatusClass(day.timechangeStatus)">
                {{ getStatusLabel(day.timechangeStatus) }}
              </span>
              <span v-else class="status-none">-</span>
            </td>
            
            <td>
              <button @click="openEditForm(day)" class="btn-action-edit">
                {{ day.isTimechangeDemand === 1 ? '再申請' : '編集申請' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showForm" class="edit-form-overlay" @click.self="closeForm">
      <div class="edit-form-card">
        <h3>{{ formatDate(selectedDay.recordDate) }} の修正申請</h3>
        
        <div class="form-body">
          <div class="form-group">
            <label>修正後の Entry 時刻 (hh:mm:ss)</label>
            <input 
              v-model="form.entryTime" 
              type="text" 
              placeholder="例: 09:00:00" 
              maxlength="8"
            />
          </div>

          <div class="form-group">
            <label>修正後の Exit 時刻 (hh:mm:ss)</label>
            <input 
              v-model="form.exitTime" 
              type="text" 
              placeholder="例: 18:00:00" 
              maxlength="8"
            />
          </div>

          <div class="form-group">
            <label>備考・修正理由 <span class="required">※必須</span></label>
            <textarea 
              v-model="form.reason" 
              placeholder="例: 出勤時に打刻を失念したため。〇〇クライアント直行のため。" 
              rows="3"
            ></textarea>
          </div>

          <div v-if="selectedDay.timechangeStatus === 1 && selectedDay.adminComment" class="admin-comment-box">
            <strong>管理者からの差戻理由:</strong>
            <p>{{ selectedDay.adminComment }}</p>
          </div>
        </div>

        <div class="form-actions">
          <button @click="submitRequest" class="btn-submit">申請を提出する</button>
          <button @click="closeForm" class="btn-cancel">キャンセル</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import apiClient from '../api';

// 状態管理変数
const calendarList = ref([]);
const currentYearMonth = ref(''); // "2026-05" 形式で保持
const loggedInAccountId = ref('');

// フォーム用のリアクティブ変数
const showForm = ref(false);
const selectedDay = ref(null);
const form = ref({
  entryTime: '',
  exitTime: '',
  reason: ''
});

// 初期設定（現在の年月をセット）
const initCurrentMonth = () => {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  currentYearMonth.value = `${year}-${month}`;
};

// 1か月分のカレンダーリストをバックエンドから取得する関数
const fetchMonthlyData = async () => {
  if (!loggedInAccountId.value) return;
  try {
    const response = await apiClient.get('/attendance/monthly-list', {
      params: {
        accountId: loggedInAccountId.value,
        yearMonth: currentYearMonth.value
      }
    });
    calendarList.value = response.data;
  } catch (error) {
    console.error('カレンダーデータの取得に失敗しました:', error);
  }
};

// 月の切り替え（前月 / 次月）
const changeMonth = (offset) => {
  const [year, month] = currentYearMonth.value.split('-').map(Number);
  const date = new Date(year, month - 1 + offset, 1);
  const nextYear = date.getFullYear();
  const nextMonth = String(date.getMonth() + 1).padStart(2, '0');
  currentYearMonth.value = `${nextYear}-${nextMonth}`;
  fetchMonthlyData(); // 月が変わったらデータを再読み込み
};

// 編集フォームを開く処理（既存の打刻内容を初期値として反映する親切UX設計）
const openEditForm = (day) => {
  selectedDay.value = day;
  
  // 既に申請中のデータがあればそれを、なければ確定時刻を、それもなければ空欄（または現在の時刻等）をセット
  form.value.entryTime = day.isTimechangeDemand === 1 ? day.tmpEntryTime : (day.entryTime === '-' ? '09:00:00' : day.entryTime);
  form.value.exitTime = day.isTimechangeDemand === 1 ? day.tmpExitTime : (day.exitTime === '-' ? '18:00:00' : day.exitTime);
  form.value.reason = day.reason || '';
  
  showForm.value = true;
};

const closeForm = () => {
  showForm.value = false;
  selectedDay.value = null;
};

// 仕様書要件：フロントエンド側での厳格なバリデーション＆確認アラート
const submitRequest = async () => {
  // 1. 備考欄の空チェック
  if (!form.value.reason.trim()) {
    alert('修正理由（備考）を入力してください。');
    return;
  }

  // 2. 時刻形式チェック (hh:mm:ss)
  const timeRegex = "^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$";
  if (!form.value.entryTime.match(timeRegex) || !form.value.exitTime.match(timeRegex)) {
    alert('時刻は「hh:mm:ss」の形式（例: 09:00:00）で入力してください。');
    return;
  }

  // 3. 仕様書要件：申請時は必ず確認アラートを表示する
  if (!confirm('この内容で打刻内容の編集申請を提出してもよろしいですか？')) {
    return;
  }

  try {
    // バックエンドの POST /api/attendance/request-edit へ送信
    await apiClient.post('/attendance/request-edit', {
      accountId: loggedInAccountId.value,
      recordDate: selectedDay.value.recordDate,
      targetEntryTime: form.value.entryTime,
      targetExitTime: form.value.exitTime,
      reason: form.value.reason
    });

    alert('申請が正常に提出されました！');
    closeForm();
    fetchMonthlyData(); // リストを最新状態に更新
  } catch (error) {
    console.error('申請エラー:', error);
    alert(error.response?.data || '申請の提出に失敗しました。');
  }
};

// 補助・装飾用ユーティリティ関数群
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

// 仕様書通りのステータスコードをテキストに変換
const getStatusLabel = (status) => {
  if (status === 0) return '申請中 (未承認)';
  if (status === 1) return '差戻し';
  if (status === 2) return '承認済み';
  return '不明';
};

const getStatusClass = (status) => {
  if (status === 0) return 'badge-pending';
  if (status === 1) return 'badge-rejected';
  if (status === 2) return 'badge-approved';
  return '';
};

const getRowClass = (dateStr) => {
  if (!dateStr) return '';
  const dayNum = new Date(dateStr).getDay();
  if (dayNum === 6) return 'row-saturday';
  if (dayNum === 0) return 'row-sunday';
  return '';
};

// 画面展開時の初期ロード
onMounted(() => {
  initCurrentMonth();
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId;
    fetchMonthlyData(); // データ取得開始
  } else {
    alert('ユーザー情報が取得できません。再ログインしてください。');
  }
});
</script>

<style scoped>
.attendance-edit-container {
  max-width: 900px;
  margin: 40px auto;
  padding: 20px;
  font-family: sans-serif;
  color: #333;
}
.subtitle { color: #666; margin-bottom: 30px; }

/* 月選択のデザイン */
.month-selector { 
  display: flex; 
  justify-content: center; 
  align-items: center; 
  gap: 20px; 
  margin-bottom: 20px;
  background-color: #343a40; padding: 10px; border-radius: 6px;
}
.btn-arrow { 
  background-color: #6c757d;
  border: 1px solid #5a6268;
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background-color 0.2s, border-color 0.2s; /* ホバー時の変化を滑らかに */
}
.btn-arrow:hover { 
  background-color: #5a6268;
  border-color: #4e555b;
}
.current-month-text { 
  font-size: 1.3rem; 
  font-weight: bold; 
  min-width: 120px; 
  text-align: center; 
  color: #e0e0e0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

/* テーブルカレンダーのデザイン */
.calendar-list-wrapper { 
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  overflow: hidden;
}
.calendar-table { width: 100%; border-collapse: collapse; text-align: center; }
.calendar-table th {
  background-color: #f8f9fa;
  color: #495057;
  padding: 14px;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
}
.calendar-table td { padding: 12px; border-bottom: 1px solid #eceff1; vertical-align: middle; }

/* 土日の行に優しい背景色を塗る安全策 */
.row-saturday { background-color: #f7faff; }
.row-sunday { background-color: #fff7f7; }
.date-col { font-weight: bold; color: #455a64; }
.time-text { font-family: monospace; font-size: 1rem; }

/* ステータスバッジの装飾 */
.status-badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 0.85rem; font-weight: bold; }
.badge-pending { background-color: #fff3e0; color: #f57c00; }  /* 申請中：薄いオレンジ */
.badge-rejected { background-color: #ffebee; color: #d32f2f; } /* 差戻：薄い赤 */
.badge-approved { background-color: #e8f5e9; color: #388e3c; } /* 承認：薄い緑 */
.status-none { color: #ccc; }

.btn-action-edit { 
  background-color: #ffffff; 
  border: 1px solid #007bff; 
  color: #007bff; 
  padding: 6px 12px; 
  border-radius: 4px; 
  cursor: pointer; 
  transition: all 0.2s;
}
.btn-action-edit:hover { background-color: #007bff; color: white; }

/* 編集申請ポップアップフォームの装飾 */
.edit-form-overlay {
  position: fixed; 
  top: 0; left: 0; 
  width: 100%; 
  height: 100%; 
  background: rgba(0,0,0,0.4); 
  display: flex; 
  justify-content: center; 
  align-items: center; 
  z-index: 1000;
}
.edit-form-card { 
  background: white; 
  padding: 30px; 
  border-radius: 8px; 
  width: 100%; 
  max-width: 450px; 
  box-shadow: 0 10px 25px rgba(0,0,0,0.15); 
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn { 
  from { transform: translateY(10px); opacity: 0; } 
  to { transform: translateY(0); opacity: 1; } 
}

.form-body { margin: 20px 0; text-align: left; }
.form-group { margin-bottom: 18px; }
.form-group label { 
  display: block; 
  margin-bottom: 6px; 
  font-weight: bold; 
  font-size: 0.9rem; 
  color: #555; 
}
.form-group input[type="text"], .form-group textarea { 
  width: 100%; 
  padding: 10px; 
  border: 1px solid #ccc; 
  border-radius: 4px; 
  box-sizing: border-box; 
  font-size: 1rem; 
}
.form-group input[type="text"] { font-family: monospace; }
.required { color: #d32f2f; }

.admin-comment-box { 
  border-left: 4px solid #f57c00; 
  padding: 10px; 
  margin-top: 15px; 
  border-radius: 4px; 
  font-size: 0.9rem; 
  background: #fff8f0; 
}

.form-actions { display: flex; gap: 10px; margin-top: 25px; }
.btn-submit { 
  flex: 1; 
  background-color: #4caf50; 
  color: white; 
  border: none; 
  padding: 12px; 
  border-radius: 4px; 
  font-size: 1rem; 
  font-weight: bold; 
  cursor: pointer; 
}
.btn-submit:hover { background-color: #43a047; }
.btn-cancel { 
  background-color: #e0e0e0; 
  color: #333; 
  border: none; 
  padding: 12px 20px; 
  border-radius: 4px; 
  font-size: 1rem; 
  cursor: pointer;
}
.btn-cancel:hover { background-color: #d5d5d5; }
</style>