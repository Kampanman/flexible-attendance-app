<template>
  <div class="admin-container">
    <div class="admin-header-box">
      <h2>管理者用：アカウント一覧・退会管理</h2>
      <p class="admin-subtitle">登録されているユーザーの確認、検索・ソート、および退会申請の承認を行えます。</p>
    </div>

    <div v-if="isLoading" class="loading-box">
      データを読み込み中...
    </div>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div v-if="!isLoading && users.length > 0" class="control-panel">
      <div class="search-box">
        <label for="searchQuery" class="control-label">アカウント検索</label>
        <input 
          id="searchQuery"
          type="text" 
          v-model="searchQuery" 
          placeholder="名前やIDで検索（半角スペース区切りで複数ワード検索対応）" 
          class="input-search"
        />
      </div>

      <div class="per-page-box">
        <label for="perPage" class="control-label">表示件数</label>
        <select id="perPage" v-model="perPage" class="select-per-page">
          <option :value="10">10件</option>
          <option :value="50">50件</option>
          <option :value="100">100件</option>
          <option :value="500">500件</option>
        </select>
      </div>
    </div>

    <div v-if="!isLoading && filteredAndSortedUsers.length > 0" class="table-responsive">
      <table class="user-table">
        <thead>
          <tr>
            <th @click="toggleSort('userName')" class="sortable-header">
              ユーザー名 {{ getSortIcon('userName') }}
            </th>
            <th @click="toggleSort('userId')" class="sortable-header">
              ユーザーID (メールアドレス) {{ getSortIcon('userId') }}
            </th>
            <th @click="toggleSort('isAuth')" class="sortable-header">
              権限 {{ getSortIcon('isAuth') }}
            </th>
            <th @click="toggleSort('quitDemand')" class="sortable-header">
              ステータス {{ getSortIcon('quitDemand') }}
            </th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="user.accountId" :class="{ 'row-quit-pending': user.quitDemand === 1 }">
            <td><strong>{{ user.userName }}</strong></td>
            <td>{{ user.userId }}</td>
            <td>
              <span :class="['badge', user.isAuth === 1 ? 'badge-admin' : 'badge-general']">
                {{ user.isAuth === 1 ? '統括管理者' : '一般ユーザー' }}
              </span>
            </td>
            <td>
              <span v-if="user.quitDemand === 1" class="badge badge-warning animate-pulse">
                ⚠ 退会申請中
              </span>
              <span v-else class="badge badge-normal">
                正常稼働
              </span>
            </td>
            <td>
              <button 
                v-if="user.quitDemand === 1 && user.userId !== 'admin@example.com'" 
                @click="handleApproveQuit(user)" 
                class="btn-approve"
              >
                退会を承認する
              </button>
              <span v-else-if="user.userId === 'admin@example.com'" class="text-muted-info">-</span>
              <span v-else class="text-muted">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!isLoading && filteredAndSortedUsers.length > 0" class="pagination-panel">
      <div class="pagination-info">
        全 {{ filteredAndSortedUsers.length }} 件中 {{ startIndex + 1 }} 〜 {{ endIndex }} 件目を表示
      </div>
      <div class="pagination-buttons">
        <button :disabled="currentPage === 1" @click="currentPage--" class="btn-page">◀ 前へ</button>
        <button 
          v-for="page in totalPages" 
          :key="page" 
          @click="currentPage = page" 
          :class="['btn-page-number', { 'active': currentPage === page }]"
        >
          {{ page }}
        </button>
        <button :disabled="currentPage === totalPages" @click="currentPage++" class="btn-page">次へ ▶</button>
      </div>
    </div>

    <div v-else-if="!isLoading" class="empty-box">
      条件に一致するアカウントが存在しません。
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';

const router = useRouter();

const users = ref([]);
const isLoading = ref(true);
const errorMessage = ref('');
const successMessage = ref('');

// 💡 状態管理用のリアクティブ変数
const searchQuery = ref('');    // 検索窓の文字列
const perPage = ref(10);        // 1ページあたりの表示件数 [10, 50, 100, 500]
const currentPage = ref(1);     // 現在のページ番号
const sortKey = ref('quitDemand'); // 初期ソートは「退会申請」を優先させるためのキー
const sortOrder = ref('desc');   // 1（申請中）が上に来るように初期は降順（desc）

// データ取得ロジック（変更なし）
const fetchUserList = async () => {
  try {
    errorMessage.value = '';
    const response = await apiClient.get('/users/admin/list');
    users.value = response.data;
  } catch (error) {
    console.error(error);
    errorMessage.value = 'アカウント一覧の取得に失敗しました。';
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    if (user.isAuth !== 1) {
      alert('この画面は管理者専用です。');
      router.push('/dashboard');
      return;
    }
    fetchUserList();
  } else {
    router.push('/login');
  }
});

// 💡 核心ロジック1：【検索 ＆ ソート】を動的に同時計算するロジック
const filteredAndSortedUsers = computed(() => {
  let result = [...users.value];

  // 【仕様通り：半角スペース区切りの複数ワード検索】
  if (searchQuery.value.trim()) {
    // 全角スペースがあれば半角スペースに変換し、配列に分解
    const keywords = searchQuery.value.replace(/ /g, ' ').toLowerCase().split(' ').filter(w => w);
    
    result = result.filter(user => {
      // ユーザー名とユーザーID（メールアドレス）を検索対象にする
      const targetText = `${user.userName} ${user.userId}`.toLowerCase();
      // 分解したキーワード「すべて」が含まれている行だけ残す（AND検索）
      return keywords.every(keyword => targetText.includes(keyword));
    });
  }

  // 【動的ソート処理】
  result.sort((a, b) => {
    let modifier = sortOrder.value === 'desc' ? -1 : 1;
    
    // 文字列または数値の比較
    let valA = a[sortKey.value];
    let valB = b[sortKey.value];

    // 文字列の場合は大文字小文字を区別せず比較
    if (typeof valA === 'string') valA = valA.toLowerCase();
    if (typeof valB === 'string') valB = valB.toLowerCase();

    if (valA < valB) return -1 * modifier;
    if (valA > valB) return 1 * modifier;
    return 0;
  });

  return result;
});

// 💡 核心ロジック2：【ページネーション】現在のページに該当するデータだけを切り出すロジック
const totalPages = computed(() => {
  return Math.ceil(filteredAndSortedUsers.value.length / perPage.value) || 1;
});

const startIndex = computed(() => (currentPage.value - 1) * perPage.value);
const endIndex = computed(() => {
  const end = startIndex.value + perPage.value;
  return end > filteredAndSortedUsers.value.length ? filteredAndSortedUsers.value.length : end;
});

const paginatedUsers = computed(() => {
  return filteredAndSortedUsers.value.slice(startIndex.value, endIndex.value);
});

// 検索条件や表示件数が変わったら、ページ番号を強制的に1ページ目に戻す親切設計
watch([searchQuery, perPage], () => {
  currentPage.value = 1;
});

// ソートキーを切り替える関数
const toggleSort = (key) => {
  if (sortKey.value === key) {
    // 同じヘッダーが叩かれたら 昇順 ⇄ 降順 を反転
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    // 新しいヘッダーなら昇順からスタート
    sortKey.value = key;
    sortOrder.value = 'asc';
  }
};

// ソート状態を視覚的に表現するアイコン
const getSortIcon = (key) => {
  if (sortKey.value !== key) return '↕';
  return sortOrder.value === 'asc' ? '🔼' : '🔽';
};

// 退会承認処理（変更なし）
const handleApproveQuit = async (targetUser) => {
  successMessage.value = '';
  errorMessage.value = '';

  const confirmAction = confirm(`【最終警告】\n本当に「${targetUser.userName}」さんの退会申請を承認しますか？\nこの操作を行うと、アカウント情報はシステムから完全に物理削除されます。`);
  if (!confirmAction) return;

  try {
    await apiClient.delete(`/users/approve-quit/${targetUser.accountId}`);
    successMessage.value = `「${targetUser.userName}」さんのアカウント削除（退会承認）が完了しました。`;
    await fetchUserList();
  } catch (error) {
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data;
    } else {
      errorMessage.value = '承認処理の通信中にエラーが発生しました。';
    }
  }
};
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

/* 💡 追加：コントロールパネルのスタイル */
.control-panel {
  display: flex;
  gap: 20px;
  background: #ffffff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  margin-bottom: 15px;
  align-items: flex-end;
}
.search-box { flex: 1; }
.per-page-box { width: 120px; }
.control-label {
  display: block;
  font-size: 0.85rem;
  font-weight: bold;
  color: #495057;
  margin-bottom: 5px;
}
.search-box .control-label { text-align: left; }
.input-search, .select-per-page {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
  box-sizing: border-box;
}

/* 追加：ソート可能なヘッダーのカーソル演出 */
.sortable-header {
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}
.sortable-header:hover {
  background-color: #e9ecef !important;
}

/* テーブルデザイン */
.table-responsive {
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  overflow: hidden;
}
.user-table { width: 100%; border-collapse: collapse; text-align: left; }
.user-table th, .user-table td { padding: 15px 20px; border-bottom: 1px solid #dee2e6; }
.user-table th { background-color: #f8f9fa; color: #495057; font-weight: bold; }
.row-quit-pending { background-color: #fffdf5; }

/* バッジとアニメーション */
.badge { display: inline-block; padding: 5px 10px; font-size: 0.8rem; font-weight: bold; border-radius: 20px; }
.badge-admin { background-color: #e3f2fd; color: #0d47a1; }
.badge-general { background-color: #e8f5e9; color: #1b5e20; }
.badge-normal { background-color: #f1f3f5; color: #6c757d; }
.badge-warning { background-color: #fff3cd; color: #856404; border: 1px solid #ffeeba; }

@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.6; } 100% { opacity: 1; } }
.animate-pulse { animation: pulse 2s infinite; }

.btn-approve {
  background-color: #dc3545; color: white; border: none; padding: 8px 14px;
  border-radius: 4px; font-size: 0.85rem; font-weight: bold; cursor: pointer;
  transition: background-color 0.15s;
}
.btn-approve:hover { background-color: #bd2130; }
.text-muted { color: #ced4da; }
.text-muted-info { color: #adb5bd; font-size: 0.85rem; font-style: italic; }

/* 💡 追加：ページネーションパネルのスタイル */
.pagination-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  background: #ffffff;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}
.pagination-info { font-size: 0.9rem; color: #495057; }
.pagination-buttons { display: flex; gap: 5px; }
.btn-page {
  padding: 6px 12px; background-color: #ffffff; border: 1px solid #ced4da;
  border-radius: 4px; font-size: 0.85rem; cursor: pointer; transition: all 0.2s;
}
.btn-page:hover:not(:disabled) { background-color: #f8f9fa; border-color: #b5bbc1; }
.btn-page:disabled { color: #ced4da; cursor: not-allowed; }

.btn-page-number {
  padding: 6px 12px; background-color: #ffffff; border: 1px solid #ced4da;
  border-radius: 4px; font-size: 0.85rem; cursor: pointer;
}
.btn-page-number:hover { background-color: #f8f9fa; }
.btn-page-number.active {
  background-color: #007bff; color: white; border-color: #007bff; font-weight: bold;
}

.alert { padding: 15px; margin-bottom: 20px; border-radius: 4px; font-weight: bold; }
.alert-danger { background-color: #f8d7da; color: #721c24; border-left: 5px solid #dc3545; }
.alert-success { background-color: #d4edda; color: #155724; border-left: 5px solid #28a745; }
.loading-box, .empty-box { text-align: center; padding: 40px; color: #6c757d; font-size: 1.1rem; }
</style>
