<template>
  <div class="future-plan-container">
    <h2>行動予定申請・管理</h2>
    <p class="subtitle">未来の行動予定の申請と、提出した申請の状況確認が行えます。</p>

    <div class="history-section-box">
      <div class="history-header">
        <h3>あなたの申請履歴一覧</h3>
        
        <div class="filter-controls">
          <label for="statusFilter">状況で絞り込み:</label>
          <select id="statusFilter" v-model="filterStatus" class="select-filter">
            <option value="all">すべて表示</option>
            <option value="0">申請中</option>
            <option value="2">承認済み</option>
            <option value="1">差戻し</option>
          </select>
        </div>
      </div>

      <div v-if="filteredPlans.length > 0" class="table-wrapper">
        <table class="history-table">
          <thead>
            <tr>
              <th>予定対象日</th>
              <th>予定の区分</th>
              <th>理由・備考詳細</th>
              <th>承認状況</th>
              <th>管理者からの伝言</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="plan in filteredPlans" :key="plan.planId">
              <td class="date-td">{{ formatPlanDate(plan.planDate) }}</td>
              <td class="title-td">{{ plan.planTitle }}</td>
              <td class="detail-td">{{ plan.planDetail || '-' }}</td>
              <td>
                <span class="status-badge" :class="getPlanStatusClass(plan.planStatus)">
                  {{ getPlanStatusLabel(plan.planStatus) }}
                </span>
              </td>
              <td class="comment-td" :class="{ 'has-comment': plan.adminComment }">
                {{ plan.adminComment || '-' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-table-alert">
        該当する予定申請データは見つかりませんでした。
      </div>
    </div>

    <div class="plan-form-card">
      <h3>新規予定申請フォーム</h3>
      <div class="form-group">
        <label>予定対象日 <span class="required">※必須</span></label>
        <input v-model="form.planDate" type="date" :min="todayStr" />
      </div>

      <div class="form-group">
        <label>予定の区分・タイトル <span class="required">※必須</span></label>
        <input 
          v-model="form.planTitle" 
          type="text" 
          placeholder="例: 有給休暇、出張、在宅勤務、午前半休 など"
          maxlength="50"
        />
      </div>

      <div class="form-group">
        <label>理由・備考詳細</label>
        <textarea 
          v-model="form.planDetail" 
          placeholder="例: 私用のため。〇〇社訪問のため。" 
          rows="3"
          maxlength="200"
        ></textarea>
      </div>

      <div class="form-actions">
        <button @click="submitPlan" class="btn-submit">予定申請を提出する</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import apiClient from '../api';

const todayStr = ref('');
const loggedInAccountId = ref('');
const myPlans = ref([]);      // サーバーから取得した全履歴
const filterStatus = ref('all'); // 絞り込み条件（'all', '0', '1', '2'）

const form = ref({
  planDate: '',
  planTitle: '',
  planDetail: ''
});

// 自分の全予定履歴をロードする関数
const fetchMyPlans = async () => {
  if (!loggedInAccountId.value) return;
  try {
    const response = await apiClient.get('/plans/my-list', {
      params: { accountId: loggedInAccountId.value }
    });
    myPlans.value = response.data;
  } catch (error) {
    console.error('履歴の取得に失敗しました:', error);
  }
};

// リアルタイム絞り込み検索ロジック (computed)
const filteredPlans = computed(() => {
  if (filterStatus.value === 'all') {
    return myPlans.value;
  }
  return myPlans.value.filter(plan => plan.planStatus === Number(filterStatus.value));
});

const initForm = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  todayStr.value = `${y}-${m}-${d}`;
  form.value.planDate = todayStr.value;
};

const submitPlan = async () => {
  if (!form.value.planDate) { alert('予定対象日を指定してください。'); return; }
  if (!form.value.planTitle.trim()) { alert('予定の区分・タイトルを入力してください。'); return; }

  if (!confirm('この内容で未来の予定申請を提出してもよろしいですか？')) return;

  try {
    await apiClient.post('/plans/request', {
      accountId: loggedInAccountId.value,
      planDate: form.value.planDate,
      planTitle: form.value.planTitle,
      planDetail: form.value.planDetail
    });

    alert('予定申請を提出しました！');
    form.value.planTitle = '';
    form.value.planDetail = '';
    fetchMyPlans(); // 提出成功後に一覧を自動再ロード！
  } catch (error) {
    console.error('予定申請エラー:', error);
    alert(error.response?.data || '予定申請の提出に失敗しました。');
  }
};

const formatPlanDate = (dateStr) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-');
  return `${Number(m)}月${Number(d)}日`;
};

const getPlanStatusLabel = (status) => {
  if (status === 0) return '申請中';
  if (status === 1) return '差戻し';
  if (status === 2) return '承認済み';
  return '不明';
};

const getPlanStatusClass = (status) => {
  if (status === 0) return 'status-pending';
  if (status === 1) return 'status-rejected';
  if (status === 2) return 'status-approved';
  return '';
};

onMounted(() => {
  initForm();
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    loggedInAccountId.value = user.accountId;
    fetchMyPlans(); // 画面展開時に履歴を引く
  }
});
</script>

<style scoped>
.future-plan-container { max-width: 850px; margin: 40px auto; padding: 0 20px; font-family: sans-serif; text-align: left; }
.subtitle { color: #666; margin-bottom: 30px; }

/* 履歴セクションのデザイン */
.history-section-box {
  background: white;
  border-radius: 8px;
  border: 1px solid #cfd8dc;
  padding: 20px;
  margin-bottom: 35px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.04);
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #eceff1;
  padding-bottom: 12px;
  margin-bottom: 15px;
}
.history-header h3 { margin: 0; color: #37474f; font-size: 1.15rem; }

.filter-controls { display: flex; align-items: center; gap: 8px; font-size: 0.9rem; font-weight: bold; color: #546e7a; }
.select-filter {
  padding: 6px 12px;
  border: 1px solid #b0bec5;
  border-radius: 4px;
  font-size: 0.9rem;
  background: #fafafa;
  cursor: pointer;
}

/* 検索機能付きテーブルのデザイン */
.table-wrapper { overflow-x: auto; }
.history-table { width: 100%; border-collapse: collapse; font-size: 0.95rem; text-align: left; }
.history-table th { background-color: #f5f7f8; color: #37474f; padding: 12px 10px; font-weight: bold; border-bottom: 2px solid #cfd8dc; }
.history-table td { padding: 12px 10px; border-bottom: 1px solid #eceff1; vertical-align: middle; }

.date-td { font-weight: bold; color: #263238; }
.title-td { font-weight: bold; color: #0288d1; }
.detail-td { color: #546e7a; font-size: 0.9rem; }
.comment-td { font-size: 0.9rem; color: #78909c; font-style: italic; }
.comment-td.has-comment { color: #e65100; font-weight: 500; font-style: normal; }

/* ステータスバッジ */
.status-badge { display: inline-block; padding: 4px 8px; border-radius: 12px; font-size: 0.8rem; font-weight: bold; }
.status-pending { background-color: #fff3e0; color: #f57c00; }
.status-rejected { background-color: #ffebee; color: #d32f2f; }
.status-approved { background-color: #e8f5e9; color: #388e3c; }

.empty-table-alert { text-align: center; padding: 25px; color: #90a4ae; font-style: italic; background: #fafafa; border-radius: 4px; }

/* フォームのデザイン */
.plan-form-card {
  background: white;
  padding: 25px;
  border-radius: 8px;
  border: 1px solid #b0bec5;
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.plan-form-card h3 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #263238;
  font-size: 1.1rem;
  border-left: 4px solid #007bff;
  padding-left: 10px;
}
.form-group { margin-bottom: 18px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: bold; color: #455a64; font-size: 0.9rem; }
.form-group input[type="date"], .form-group input[type="text"], .form-group textarea {
  width: 100%; padding: 10px; border: 1px solid #cfd8dc; border-radius: 4px; box-sizing: border-box; font-size: 0.95rem;
}
.required { color: #d32f2f; }
.btn-submit {
  width: 100%;
  background-color: #007bff;
  color: white;
  border: none;
  padding: 12px;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-submit:hover { background-color: #0056b3; }
</style>