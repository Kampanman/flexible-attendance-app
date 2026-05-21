import { createRouter, createWebHistory } from 'vue-router';
import LoginForm from '../components/LoginForm.vue';
import RegistForm from '../components/RegisterForm.vue';
import AttendanceBoard from '../components/AttendanceBoard.vue';
import DashboardView from '../components/DashboardView.vue'; // ← 追加

const routes = [
  { path: '/login', component: LoginForm },
  { path: '/register', component: RegistForm },
  { path: '/attendance', component: AttendanceBoard },
  { path: '/dashboard', component: DashboardView }, // ← 追加
  { path: '/', redirect: '/login' } // 初期アクセスはログインへ
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// ※ここにログイン状態をチェックするナビゲーションガード（仕様書要件）を後ほど追加できます

export default router;