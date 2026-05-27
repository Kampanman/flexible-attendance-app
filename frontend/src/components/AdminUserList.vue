<template>
  <div class="admin-sub-container">
    <div class="control-panel">
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

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
    <div v-if="successMessage" class="alert alert-success">{{ successMessage }}</div>

    <div v-if="filteredAndSortedUsers.length > 0" class="table-responsive">
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
                退会申請中
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

    <div v-if="filteredAndSortedUsers.length > 0" class="pagination-panel">
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

    <div v-else class="empty-box">
      条件に一致するアカウントが存在しません。
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import apiClient from '../api';

const users = ref([]);
const errorMessage = ref('');
const successMessage = ref('');

const searchQuery = ref('');
const perPage = ref(10);
const currentPage = ref(1);
const sortKey = ref('quitDemand');
const sortOrder = ref('desc');

const fetchUserList = async () => {
  try {
    const response = await apiClient.get('/users/admin/list');
    users.value = response.data;
  } catch (error) {
    errorMessage.value = 'アカウント一覧の取得に失敗しました。';
  }
};

onMounted(() => {
  fetchUserList();
});

const filteredAndSortedUsers = computed(() => {
  let result = [...users.value];
  if (searchQuery.value.trim()) {
    const keywords = searchQuery.value.replace(/　/g, ' ').toLowerCase().split(' ').filter(w => w);
    result = result.filter(user => {
      const targetText = `${user.userName} ${user.userId}`.toLowerCase();
      return keywords.every(keyword => targetText.includes(keyword));
    });
  }
  result.sort((a, b) => {
    let modifier = sortOrder.value === 'desc' ? -1 : 1;
    let valA = a[sortKey.value];
    let valB = b[sortKey.value];
    if (typeof valA === 'string') valA = valA.toLowerCase();
    if (typeof valB === 'string') valB = valB.toLowerCase();
    if (valA < valB) return -1 * modifier;
    if (valA > valB) return 1 * modifier;
    return 0;
  });
  return result;
});

const totalPages = computed(() => Math.ceil(filteredAndSortedUsers.value.length / perPage.value) || 1);
const startIndex = computed(() => (currentPage.value - 1) * perPage.value);
const endIndex = computed(() => {
  const end = startIndex.value + perPage.value;
  return end > filteredAndSortedUsers.value.length ? filteredAndSortedUsers.value.length : end;
});
const paginatedUsers = computed(() => filteredAndSortedUsers.value.slice(startIndex.value, endIndex.value));

watch([searchQuery, perPage], () => { currentPage.value = 1; });

const toggleSort = (key) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortKey.value = key;
    sortOrder.value = 'asc';
  }
};

const getSortIcon = (key) => {
  if (sortKey.value !== key) return '↕';
  return sortOrder.value === 'asc' ? '🔼' : '🔽';
};

const handleApproveQuit = async (targetUser) => {
  if (!confirm(`本当に「${targetUser.userName}」さんの退会申請を承認しますか？\nこの操作を行うと、アカウント情報は完全に物理削除されます。`)) return;
  try {
    await apiClient.delete(`/users/approve-quit/${targetUser.accountId}`);
    successMessage.value = `「${targetUser.userName}」さんの退会承認が完了しました。`;
    await fetchUserList();
  } catch (error) {
    errorMessage.value = '承認処理に失敗しました。';
  }
};
</script>

<style scoped>
/* スタイルは前回同様（親から切り離したため幅いっぱいに広がるよう調整） */
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
.control-label { display: block; font-size: 0.85rem; font-weight: bold; color: #495057; margin-bottom: 5px; }
.input-search, .select-per-page {
  width: 100%; 
  padding: 8px 12px; 
  border: 1px solid #ced4da; 
  border-radius: 4px; 
  font-size: 0.9rem; 
  box-sizing: border-box;
}
.sortable-header { cursor: pointer; user-select: none; }
.sortable-header:hover { background-color: #e9ecef !important; }
.table-responsive { background: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); overflow: hidden; }
.user-table { width: 100%; border-collapse: collapse; text-align: left; }
.user-table th, .user-table td { padding: 15px 20px; border-bottom: 1px solid #dee2e6; }
.user-table th { background-color: #f8f9fa; color: #495057; font-weight: bold; }
.row-quit-pending { background-color: #fffdf5; }
.badge { display: inline-block; padding: 5px 10px; font-size: 0.8rem; font-weight: bold; border-radius: 20px; }
.badge-admin { background-color: #e3f2fd; color: #0d47a1; }
.badge-general { background-color: #e8f5e9; color: #1b5e20; }
.badge-normal { background-color: #f1f3f5; color: #6c757d; }
.badge-warning { background-color: #fff3cd; color: #856404; border: 1px solid #ffeeba; }
@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.6; } 100% { opacity: 1; } }
.animate-pulse { animation: pulse 2s infinite; }
.btn-approve { 
  background-color: #dc3545; 
  color: white; 
  border: none; 
  padding: 8px 14px; 
  border-radius: 4px; 
  font-size: 0.85rem; 
  font-weight: bold; 
  cursor: pointer;
}
.btn-approve:hover { background-color: #bd2130; }
.text-muted { color: #ced4da; }
.text-muted-info { color: #adb5bd; font-size: 0.85rem; font-style: italic; }
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
.btn-page, .btn-page-number { 
  padding: 6px 12px; 
  background-color: #ffffff; 
  border: 1px solid #ced4da; 
  border-radius: 4px; 
  font-size: 0.85rem; 
  cursor: pointer;
}
.btn-page:disabled { color: #ced4da; cursor: not-allowed; }
.btn-page-number.active { background-color: #007bff; color: white; border-color: #007bff; font-weight: bold; }
.alert { padding: 15px; margin-bottom: 20px; border-radius: 4px; font-weight: bold; }
.alert-danger { background-color: #f8d7da; color: #721c24; border-left: 5px solid #dc3545; }
.alert-success { background-color: #d4edda; color: #155724; border-left: 5px solid #28a745; }
.empty-box { text-align: center; padding: 40px; color: #6c757d; }
</style>
