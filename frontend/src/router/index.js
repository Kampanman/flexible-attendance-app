import { createRouter, createWebHistory } from 'vue-router';
import AccountEdit from '../components/AccountEditView.vue';
import AdminAttendanceApproval from '../components/AdminAttendanceApprovalView.vue';
import AdminUserListView from '../components/AdminUserListView.vue';
import AttendanceBoard from '../components/AttendanceBoard.vue';
import AttendanceEdit from '../components/AttendanceEditView.vue';
import DashboardView from '../components/DashboardView.vue';
import FuturePlan from '../components/FuturePlanView.vue';
import LoginForm from '../components/LoginForm.vue';
import RegistForm from '../components/RegisterForm.vue';

const routes = [
  { path: '/login', component: LoginForm },
  { path: '/register', component: RegistForm },
  { path: '/attendance', component: AttendanceBoard },
  { path: '/attendance-edit', component: AttendanceEdit },
  { path: '/dashboard', component: DashboardView },
  { path: '/schedule-request', component: FuturePlan },
  { path: '/account-edit', component: AccountEdit },
  { path: '/admin', component: AdminUserListView },
  { path: '/admin/approval', component: AdminAttendanceApproval },
  { path: '/', redirect: '/login' } // 初期アクセスはログインへ
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// ※ここにログイン状態をチェックするナビゲーションガード（仕様書要件）を後ほど追加できます

export default router;