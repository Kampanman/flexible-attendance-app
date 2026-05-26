import { createRouter, createWebHistory } from 'vue-router';
import AccountEdit from '../components/AccountEditView.vue';
import AdminUserListView from '../components/AdminUserListView.vue';
import AttendanceBoard from '../components/AttendanceBoard.vue';
import DashboardView from '../components/DashboardView.vue';
import LoginForm from '../components/LoginForm.vue';
import RegistForm from '../components/RegisterForm.vue';

const routes = [
  { path: '/login', component: LoginForm },
  { path: '/register', component: RegistForm },
  { path: '/attendance', component: AttendanceBoard },
  { path: '/dashboard', component: DashboardView },
  { path: '/account-edit', component: AccountEdit },
  { path: '/admin', component: AdminUserListView },
  { path: '/', redirect: '/login' } // 初期アクセスはログインへ
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// ※ここにログイン状態をチェックするナビゲーションガード（仕様書要件）を後ほど追加できます

export default router;