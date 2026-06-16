<template>
  <div class="admin-container">
    <div class="admin-header-box">
      <h2>統括管理者用 コントロールパネル</h2>
      <p class="admin-subtitle">システムの全体管理、モード一括変更、およびアカウント制御を行えます。</p>
    </div>

    <div class="tab-menu">
      <button 
        :class="['tab-btn', { active: currentTab === 'users' }]" 
        @click="currentTab = 'users'"
      >
        アカウント一覧・退会管理
      </button>
      <button 
        @click="currentTab = 'requests'" 
        :class="['tab-btn', { active: currentTab === 'requests' }]"
      >
        打刻申請確認
      </button>
      <button
        @click="currentTab = 'adminPlans'"
        :class="['tab-btn', { active: currentTab === 'adminPlans' }]"
      >
        予定申請確認
      </button>
      <button 
        :class="['tab-btn', { active: currentTab === 'mode' }]" 
        @click="currentTab = 'mode'"
      >
        打刻モード一括制御
      </button>
      <button 
        @click="currentTab = 'announcements'" 
        :class="['tab-btn', { active: currentTab === 'announcements' }]"
      >
        お知らせ管理
      </button>
    </div>
    <div class="tab-content">
      
      <div v-if="currentTab === 'users'">
        <AdminUserList />
      </div>

      <div v-if="currentTab === 'requests'">
        <AdminAttendanceApprovalView />
      </div>

      <div v-if="currentTab === 'adminPlans'">
        <AdminFuturePlanPanel />
      </div>

      <div v-if="currentTab === 'mode'" class="mode-control-box">
        <h3>現在のアプリケーション打刻モード設定</h3>
        <p class="section-desc">ここでモードを切り替えると、アプリを利用する全ユーザーの打刻画面が即座に統一されます。</p>

        <div v-if="modeSuccessMessage" class="alert alert-success">{{ modeSuccessMessage }}</div>
        <div v-if="modeErrorMessage" class="alert alert-danger">{{ modeErrorMessage }}</div>

        <div class="mode-cards-container">
          <div 
            :class="['mode-card', { 'active-card': systemMode === 0 }]"
            @click="saveSystemMode(0)"
          >
            <div class="card-icon">💼</div>
            <h4>勤怠モード</h4>
            <p>「出勤」「退勤」「休憩開始」「休憩終了」を管理する標準的なモードです。</p>
            <span class="status-indicator">{{ systemMode === 0 ? '稼働中' : '選択する' }}</span>
          </div>

          <div 
            :class="['mode-card', { 'active-card': systemMode === 1 }]"
            @click="saveSystemMode(1)"
          >
            <div class="card-icon">🚪</div>
            <h4>入退室モード</h4>
            <p>オフィスのセキュリティや「入室」「退室」のログ選定に特化したモードです。</p>
            <span class="status-indicator">{{ systemMode === 1 ? '稼働中' : '選択する' }}</span>
          </div>

          <div 
            :class="['mode-card', { 'active-card': systemMode === 2 }]"
            @click="saveSystemMode(2)"
          >
            <div class="card-icon">🏫</div>
            <h4>出席退席モード</h4>
            <p>講義やイベント、集会などの「出席」「退席」をシンプルに記録するモードです。</p>
            <span class="status-indicator">{{ systemMode === 2 ? '稼働中' : '選択する' }}</span>
          </div>
        </div>
      </div>

      <div v-if="currentTab === 'announcements'">
        <AdminAnnouncementPanel />
      </div>

    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
// コンポーネントをインポート
import AdminAnnouncementPanel from '../components/AdminAnnouncementPanel.vue';
import AdminAttendanceApprovalView from '../components/AdminAttendanceApprovalView.vue';
import AdminUserList from '../components/AdminUserList.vue';
import AdminFuturePlanPanel from './AdminFuturePlanPanel.vue';

const router = useRouter();

// 現在開いているタブ管理 ('users' または 'mode')
const currentTab = ref('users');

// 打刻モードの状態管理 (0:勤怠, 1:入退室, 2:出席退席)
const systemMode = ref(0); 

const modeSuccessMessage = ref('');
const modeErrorMessage = ref('');

// 1. バックエンドから現在のモードを取得する関数
const fetchCurrentMode = async () => {
  try {
    // Java側で作成した GET /api/system/mode を呼び出す
    const response = await apiClient.get('/system/mode');
    systemMode.value = response.data.mode;
  } catch (error) {
    console.error(error);
    modeErrorMessage.value = '現在の打刻モードの取得に失敗しました。';
  }
};

// 2. 統括管理者がモードを更新・保存する関数
const saveSystemMode = async (targetMode) => {
  modeSuccessMessage.value = '';
  modeErrorMessage.value = '';
  
  try {
    // Java側で作成した PUT /api/system/mode を呼び出す
    await apiClient.put('/system/mode', { mode: targetMode });
    
    // リアクティブ変数を更新
    systemMode.value = targetMode;
    modeSuccessMessage.value = 'アプリケーション全体の打刻モードを正常に一括更新しました！';
  } catch (error) {
    modeErrorMessage.value = 'モードの更新中に通信エラーが発生しました。';
  }
};

// 画面表示時の管理者セキュリティガード
onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    if (user.isAuth !== 1) {
      alert('この画面は管理者専用です。');
      router.push('/dashboard');
      return;
    }
    // 管理者であれば、現在の打刻モードを初期ロード
    fetchCurrentMode();
  } else {
    router.push('/login');
  }
});
</script>

<style scoped>
.admin-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 20px;
}
.admin-header-box {
  background: #ffffff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  margin-bottom: 25px;
  border-left: 5px solid #dc3545;
}
h2 { margin: 0 0 5px 0; color: #333; }
.admin-subtitle { margin: 0; color: #6c757d; font-size: 0.95rem; }

/* 💡 タブメニューのスタイル */
.tab-menu {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  border-bottom: 2px solid #dee2e6;
  padding-bottom: 10px;
}
.tab-btn {
  padding: 10px 20px;
  font-size: 1rem;
  font-weight: bold;
  background: none;
  border: none;
  color: #6c757d;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;
}
.tab-btn:hover {
  background-color: #f8f9fa;
  color: #495057;
}
.tab-btn.active {
  background-color: #dc3545;
  color: white;
  box-shadow: 0 2px 6px rgba(220, 53, 69, 0.3);
}

/* モード制御コンテンツのスタイル */
.mode-control-box {
  background: #ffffff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
h3 { margin-top: 0; color: #333; }
.section-desc { color: #6c757d; font-size: 0.95rem; margin-bottom: 25px; }

/* カード配置エリア */
.mode-cards-container {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}
.mode-card {
  flex: 1;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  padding: 25px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background: #fff;
  position: relative;
}
.mode-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 15px rgba(0,0,0,0.1);
  border-color: #ced4da;
}
.card-icon { font-size: 3rem; margin-bottom: 15px; }
.mode-card h4 { margin: 0 0 10px 0; font-size: 1.2rem; color: #333; }
.mode-card p { font-size: 0.88rem; color: #6c757d; line-height: 1.5; margin-bottom: 20px; }

.status-indicator {
  display: inline-block;
  padding: 6px 16px;
  font-size: 0.85rem;
  font-weight: bold;
  border-radius: 20px;
  background-color: #f1f3f5;
  color: #6c757d;
  transition: all 0.2s;
}

/* 現在アクティブ（稼働中）なモードカードのスタイル変更 */
.active-card {
  border-color: #28a745 !important; /* 稼働中は安心のグリーン */
  background-color: #f4fbf6;
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.15);
}
.active-card .status-indicator {
  background-color: #28a745;
  color: white;
}

/* メッセージ用 */
.alert { padding: 15px; margin-bottom: 20px; border-radius: 4px; font-weight: bold; }
.alert-danger { background-color: #f8d7da; color: #721c24; border-left: 5px solid #dc3545; }
.alert-success { background-color: #d4edda; color: #155724; border-left: 5px solid #28a745; }
</style>
