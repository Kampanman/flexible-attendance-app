<template>
  <div class="admin-approval-container">
    <h2>勤怠・打刻修正承認パネル</h2>
    <p class="subtitle">全国の従業員から提出された「打刻内容の修正申請」の一覧確認と、承認・差戻し処理が行えます。</p>

    <div v-if="pendingRequests.length === 0" class="no-data-alert">
      現在、未処理の修正申請はありません。
    </div>

    <div v-else class="requests-grid">
      <div v-for="req in pendingRequests" :key="req.recordId" class="request-card">
        <div class="card-header">
          <span class="user-id">申請者: {{ req.userName }}</span>
          <span class="target-date">対象日: {{ formatDate(req.recordDate) }}</span>
        </div>

        <div class="card-body">
          <div class="time-comparison-box">
            <div class="time-row origin-time">
              <span class="time-label">現在の確定打刻</span>
              <span class="time-values">Entry: <b>{{ req.entryTime }}</b> / Exit: <b>{{ req.exitTime }}</b></span>
            </div>
            <div class="arrow-down">修正希望（この内容へ差し替え）</div>
            <div class="time-row target-time">
              <span class="time-label">修正後の希望</span>
              <span class="time-values">Entry: <b class="highlight">{{ req.tmpEntryTime }}</b> / Exit: <b class="highlight">{{ req.tmpExitTime }}</b></span>
            </div>
          </div>

          <div class="reason-section">
            <strong>従業員からの申請理由・備考:</strong>
            <p class="reason-text">{{ req.reason }}</p>
          </div>

          <div v-if="activeRejectId === req.recordId" class="reject-comment-area">
            <label>差戻し理由を入力してください <span class="required">※必須</span></label>
            <textarea 
              v-model="adminComment" 
              placeholder="例: 打刻原簿のログと時間が大幅に乖離しています。再度確認して申請し直してください。" 
              rows="2"
            ></textarea>
          </div>
        </div>

        <div class="card-actions">
          <template v-if="activeRejectId !== req.recordId">
            <button @click="handleApprove(req.recordId)" class="btn-approve">承認する</button>
            <button @click="showRejectInput(req.recordId)" class="btn-trigger-reject">差戻す</button>
          </template>

          <template v-else>
            <button @click="handleReject(req.recordId)" class="btn-reject-confirm">この理由で差戻しを確定</button>
            <button @click="cancelReject" class="btn-cancel">キャンセル</button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import apiClient from '../api';

// 状態管理用変数
const pendingRequests = ref([]); // 届いている未承認申請のリスト
const activeRejectId = ref(null); // 現在どこの行の「差戻し理由」を入力中かをIDで保持
const adminComment = ref('');    // 管理者が入力する差戻しコメント

// 1. サーバーから未承認の申請一覧をシュッと引っ張ってくる関数
const fetchPendingRequests = async () => {
  try {
    const response = await apiClient.get('/admin/attendance/requests');
    pendingRequests.value = response.data;
  } catch (error) {
    console.error('申請一覧の取得に失敗しました:', error);
    alert('申請データの取得に失敗しました。');
  }
};

// 2. 【承認ボタン】が押されたときの処理
const handleApprove = async (recordId) => {
  if (!confirm('この修正申請を承認してもよろしいですか？\n(承認すると、本番のカレンダー確定時刻が書き換わります)')) {
    return;
  }

  try {
    // バックエンドの POST /api/admin/attendance/approve へ recordId を送信
    const response = await apiClient.post('/admin/attendance/approve', { recordId });
    alert(response.data); // 「申請を承認しました」メッセージを表示
    fetchPendingRequests(); // 処理が終わったのでリストを最新に更新
  } catch (error) {
    console.error('承認エラー:', error);
    alert('承認処理に失敗しました。');
  }
};

// 差戻し入力欄の表示切り替え
const showRejectInput = (recordId) => {
  activeRejectId.value = recordId;
  adminComment.value = ''; // コメント欄をクリア
};

const cancelReject = () => {
  activeRejectId.value = null;
  adminComment.value = '';
};

// 3. 【差戻し確定ボタン】が押されたときの処理
const handleReject = async (recordId) => {
  if (!adminComment.value.trim()) {
    alert('従業員へ伝える「差戻し理由」を入力してください。');
    return;
  }

  try {
    // バックエンドの POST /api/admin/attendance/reject へ送信
    const response = await apiClient.post('/admin/attendance/reject', {
      recordId: recordId,
      adminComment: adminComment.value
    });
    alert(response.data); // 「申請を差し戻しました」を表示
    cancelReject(); // 入力状態を閉じる
    fetchPendingRequests(); // リストを最新に更新
  } catch (error) {
    console.error('差戻しエラー:', error);
    alert('差戻し処理に失敗しました。');
  }
};

// 日付を「〇月〇日」に見やすく整形する関数
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

// 画面展開時に自動で申請一覧をロード
onMounted(() => {
  fetchPendingRequests();
});
</script>

<style scoped>
.admin-approval-container {
  max-width: 850px;
  margin: 40px auto;
  padding: 20px;
  font-family: sans-serif;
  color: #333;
}
.subtitle { color: #666; margin-bottom: 30px; }

.no-data-alert {
  background-color: #e8f5e9;
  color: #2e7d32;
  padding: 20px;
  border-radius: 6px;
  text-align: center;
  font-weight: bold;
  font-size: 1.1rem;
  border: 1px solid #c8e6c9;
}

/* 申請カードのレイアウト */
.requests-grid { display: flex; flex-direction: column; gap: 20px; }
.request-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #cfd8dc;
  box-shadow: 0 4px 10px rgba(0,0,0,0.05);
  overflow: hidden;
  text-align: left;
}

.card-header {
  background-color: #37474f;
  color: white;
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  font-size: 0.95rem;
}

.card-body { padding: 20px; }

/* 時刻比較ボックスの装飾 */
.time-comparison-box { background: #f8f9fa; border: 1px dashed #b0bec5; border-radius: 6px; padding: 15px; margin-bottom: 15px; }
.time-row { display: flex; justify-content: space-between; font-size: 0.95rem; padding: 4px 0; }
.origin-time { color: #78909c; }
.target-time { font-size: 1.05rem; font-weight: bold; color: #2c3e50; }
.arrow-down { text-align: center; font-size: 0.85rem; color: #007bff; margin: 4px 0; font-weight: bold; }
.highlight { color: #d32f2f; background: #ffebee; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
.time-values b { font-family: monospace; font-size: 1.05rem; }

/* 理由表示エリア */
.reason-section { background: #fffde7; border-left: 4px solid #fbc02d; padding: 10px 15px; border-radius: 4px; margin-bottom: 15px; }
.reason-section strong { font-size: 0.9rem; color: #f57f17; }
.reason-text { margin: 6px 0 0 0; font-size: 0.95rem; line-height: 1.4; color: #424242; }

/* 差戻しエリア */
.reject-comment-area { margin-top: 15px; background: #fff3e0; padding: 12px; border-radius: 4px; border: 1px solid #ffe0b2; }
.reject-comment-area label { display: block; font-weight: bold; font-size: 0.85rem; color: #e65100; margin-bottom: 6px; }
.reject-comment-area textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
  font-size: 0.95rem;
}
.required { color: #d32f2f; }

/* 各種ボタンのスタイリング */
.card-actions {
  background: #f5f7f8;
  padding: 12px 20px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  border-top: 1px solid #eceff1;
}

.btn-approve {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-approve:hover { background-color: #388e3c; }

.btn-trigger-reject {
  background-color: white;
  color: #d32f2f;
  border: 1px solid #d32f2f;
  padding: 8px 20px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-trigger-reject:hover { background-color: #ffebee; }

.btn-reject-confirm {
  background-color: #e65100;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}
.btn-reject-confirm:hover { background-color: #c62828; }

.btn-cancel {
  background-color: #cfd8dc;
  color: #37474f;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}
.btn-cancel:hover { background-color: #b0bec5; }
</style>