<template>
  <div class="admin-plan-panel">
    <div class="panel-header">
      <h3>ユーザー 予定申請承認パネル</h3>
      <p class="panel-subtitle">全ユーザーから提出された「未来の行動予定」の確認と承認・差戻し処理が行えます。</p>
    </div>

    <div v-if="pendingPlans.length === 0" class="no-data-alert">
      現在、未処理の予定申請はありません。
    </div>
    <div v-else class="plans-grid">
      <div v-for="plan in pendingPlans" :key="plan.planId" class="plan-card">
        <div class="card-header">
          <span class="user-id">申請者: {{ plan.userName }}</span>
          <span class="target-date">対象日: {{ formatPlanDate(plan.planDate) }}</span>
        </div>

        <div class="card-body">
          <div class="plan-title-area">
            <span class="title-label">予定区分:</span>
            <span class="title-value">{{ plan.planTitle }}</span>
          </div>

          <div v-if="plan.planDetail" class="plan-detail-area">
            <strong>理由・詳細備考:</strong>
            <p class="detail-text">{{ plan.planDetail }}</p>
          </div>

          <div v-if="activeRejectId === plan.planId" class="reject-comment-area">
            <label>差戻し理由を入力してください <span class="required">※必須</span></label>
            <textarea 
              v-model="adminComment" 
              placeholder="例: この日は全体会議があるため、別日への変更をお願いします。" 
              rows="2"
            ></textarea>
          </div>
        </div>

        <div class="card-actions">
          <template v-if="activeRejectId !== plan.planId">
            <button @click="handleJudge(plan.planId, 2)" class="btn-approve">承認する</button>
            <button @click="showRejectInput(plan.planId)" class="btn-trigger-reject">差戻す</button>
          </template>
          <template v-else>
            <button @click="handleJudge(plan.planId, 1)" class="btn-reject-confirm">この理由で差戻しを確定</button>
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

const pendingPlans = ref([]);
const activeRejectId = ref(null);
const adminComment = ref('');

// 1. 未承認の予定一覧を取得
const fetchPendingPlans = async () => {
  try {
    const response = await apiClient.get('/admin/plans/pending-list');
    pendingPlans.value = response.data;
  } catch (error) {
    console.error('予定申請リストの取得に失敗しました:', error);
  }
};

const showRejectInput = (planId) => {
  activeRejectId.value = planId;
  adminComment.value = '';
};

const cancelReject = () => {
  activeRejectId.value = null;
  adminComment.value = '';
};

// 2. 承認（2）または差戻し（1）の実行
const handleJudge = async (planId, targetStatus) => {
  if (targetStatus == 1 && !adminComment.value.trim()) {
    alert('ユーザーへ伝える「差戻し理由」を入力してください。');
    return;
  }

  const confirmMsg = targetStatus === 2 ? 'この予定申請を承認しますか？' : 'この予定申請を差し戻しますか？';
  if (!confirm(confirmMsg)) return;

  try {
    const response = await apiClient.post('/admin/plans/judge', {
      planId: planId,
      targetStatus: targetStatus,
      adminComment: targetStatus === 1 ? adminComment.value : ''
    });
    alert(response.data);
    cancelReject();
    fetchPendingPlans(); // 最新状態にリフレッシュ
  } catch (error) {
    console.error('判定処理エラー:', error);
    alert('処理に失敗しました。');
  }
};

const formatPlanDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

onMounted(() => {
  fetchPendingPlans();
});
</script>

<style scoped>
.admin-plan-panel { padding: 5px; text-align: left; font-family: sans-serif; }
.panel-header { margin-bottom: 20px; border-bottom: 1px solid #eceff1; padding-bottom: 10px; }
.panel-header h3 { margin: 0; color: #2c3e50; font-size: 1.2rem; }
.panel-subtitle { color: #666; font-size: 0.9rem; margin: 4px 0 0 0; }

.no-data-alert { background-color: #e8f5e9; color: #2e7d32; padding: 20px; border-radius: 6px; text-align: center; font-weight: bold; border: 1px solid #c8e6c9; }

.plans-grid { display: flex; flex-direction: column; gap: 20px; }
.plan-card { background: white; border-radius: 8px; border: 1px solid #cfd8dc; box-shadow: 0 4px 10px rgba(0,0,0,0.05); overflow: hidden; }

.card-header { background-color: #455a64; color: white; padding: 12px 20px; display: flex; justify-content: space-between; font-weight: bold; font-size: 0.95rem; }
.card-body { padding: 20px; }

.plan-title-area { font-size: 1.1rem; font-weight: bold; color: #102a43; margin-bottom: 12px; }
.title-label { color: #627d98; margin-right: 8px; font-size: 0.95rem; }

.plan-detail-area { background: #f8f9fa; border-left: 4px solid #b0bec5; padding: 10px 15px; border-radius: 4px; }
.plan-detail-area strong { font-size: 0.85rem; color: #486581; }
.detail-text { margin: 4px 0 0 0; font-size: 0.95rem; color: #334e68; line-height: 1.4; }

.reject-comment-area { margin-top: 15px; background: #fff3e0; padding: 12px; border-radius: 4px; border: 1px solid #ffe0b2; }
.reject-comment-area label { display: block; font-weight: bold; font-size: 0.85rem; color: #e65100; margin-bottom: 6px; }
.reject-comment-area textarea { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
.required { color: #d32f2f; }

.card-actions { background: #f5f7f8; padding: 12px 20px; display: flex; gap: 12px; justify-content: flex-end; border-top: 1px solid #eceff1; }
.btn-approve { background-color: #4caf50; color: white; border: none; padding: 8px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-approve:hover { background-color: #388e3c; }
.btn-trigger-reject { background-color: white; color: #d32f2f; border: 1px solid #d32f2f; padding: 8px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-trigger-reject:hover { background-color: #ffebee; }
.btn-reject-confirm { background-color: #e65100; color: white; border: none; padding: 8px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; }
.btn-cancel { background-color: #cfd8dc; color: #37474f; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
</style>