<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <h1>ダッシュボード</h1>
      <span class="user-welcome">ようこそ、{{ userName }} さん</span>
    </header>

    <section class="announcement-section">
      <h2>お知らせ・通知</h2>
      <div v-if="announcements.length === 0" class="no-announcement">
        現在新しいお知らせはありません。
      </div>
      <div v-else class="announcement-list">
        <div v-for="item in announcements" :key="item.announcementId" class="announcement-card">
          <h3>{{ item.announcementTitle }}</h3>
          <p>{{ item.announcementAbout }}</p>
        </div>
      </div>
    </section>

    <section class="main-actions">
      <button @click="navigateTo('/attendance')" class="btn-main btn-attendance">
        打刻画面へ移動
      </button>
      <button @click="handleLogout" class="btn-main btn-logout">
        ログアウト
      </button>
    </section>

    <section class="menu-section">
      <h2>各種メニュー</h2>
      <div class="menu-grid">
        <button @click="navigateTo('/schedule-demand')" class="btn-menu">
          予定申請
        </button>
        <button @click="navigateTo('/timechange-demand')" class="btn-menu">
          打刻内容編集申請
        </button>
        <button @click="navigateTo('/profile-edit')" class="btn-menu">
          アカウント情報編集
        </button>
        
        <button v-if="isAuth === 1" @click="navigateTo('/admin')" class="btn-menu btn-admin">
          【管理者】各種管理画面
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
// import apiClient from '../api/axios'; // 既存の共通APIクライアント
import apiClient from '../api.js';

// 親コンポーネントやログイン状態から受け取るプロパティ（仮受け）
const props = defineProps({
  accountId: { type: String, default: '' },
  userName: { type: String, default: 'サンプルユーザー' },
  isAuth: { type: Number, default: 0 } // 0:一般, 1:管理者
});

const router = useRouter();
const announcements = ref([]);

// お知らせデータの取得
const fetchAnnouncements = async () => {
  try {
    const response = await apiClient.get('/dashboard/announcements');
    announcements.value = response.data;
  } catch (error) {
    console.error('お知らせの取得に失敗しました', error);
  }
};

// 画面遷移ハンドラー
const navigateTo = (path) => {
  router.push(path);
};

// ログアウト処理
const handleLogout = () => {
  if (confirm('ログアウトしますか？')) {
    // セッションやトークンのクリア処理をここに記述
    localStorage.removeItem('token'); // 例
    router.push('/login');
  }
};

onMounted(() => {
  fetchAnnouncements();
});
</script>

<style scoped>
.dashboard-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 200px 20px 20px; /* ヘッダー等との兼ね合い */
  font-family: sans-serif;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #34495e;
  padding-bottom: 10px;
  margin-bottom: 20px;
}

/* 上段: お知らせスタイル */
.announcement-section {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border-left: 5px solid #3498db;
  margin-bottom: 25px;
}
.announcement-card {
  background: white;
  padding: 10px 15px;
  margin-top: 10px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

/* 中段: メインアクション */
.main-actions {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
}
.btn-main {
  flex: 1;
  padding: 15px;
  font-size: 1.2rem;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-attendance { background-color: #2ecc71; color: white; }
.btn-attendance:hover { background-color: #27ae60; }
.btn-logout { background-color: #95a5a6; color: white; }
.btn-logout:hover { background-color: #7f8c8d; }

/* 下段: メニューグリッド */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 15px;
  margin-top: 15px;
}
.btn-menu {
  padding: 20px;
  font-size: 1rem;
  background-color: #34495e;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}
.btn-menu:hover { background-color: #2c3e50; transform: translateY(-2px); }
.btn-admin { background-color: #e67e22; }
.btn-admin:hover { background-color: #d35400; }
</style>