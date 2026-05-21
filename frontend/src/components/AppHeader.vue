<template>
  <header class="app-header">
    <div class="header-title" @click="router.push('/dashboard')">
      入退室管理システム
    </div>
    
    <button class="hamburger-btn" @click="toggleMenu" :class="{ 'is-open': isMenuOpen }">
      <span class="bar"></span>
      <span class="bar"></span>
      <span class="bar"></span>
    </button>

    <transition name="slide">
      <nav v-if="isMenuOpen" class="dropdown-menu">
        <ul>
          <li><a @click="navigate('/dashboard')">ダッシュボード</a></li>
          <li><a @click="navigate('/attendance')">打刻画面</a></li>
          <li><a @click="navigate('/schedule-demand')">予定申請</a></li>
          <li><a @click="navigate('/timechange-demand')">打刻内容編集申請</a></li>
          <li class="menu-divider"></li>
          <li><a @click="handleLogout" class="logout-link">ログアウト</a></li>
        </ul>
      </nav>
    </transition>
  </header>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const isMenuOpen = ref(false);

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

const navigate = (path) => {
  isMenuOpen.value = false; // メニューを閉じる
  router.push(path);
};

const handleLogout = () => {
  isMenuOpen.value = false;
  if (confirm('ログアウトしますか？')) {
    localStorage.removeItem('token'); // 必要に応じて
    router.push('/login');
  }
};
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 60px;
  background-color: #2c3e50;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  z-index: 1000;
  box-sizing: border-box;
}

.header-title {
  font-size: 1.2rem;
  font-weight: bold;
  cursor: pointer;
}

/* ハンバーガーボタンのスタイル */
.hamburger-btn {
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 30px;
  height: 20px;
  padding: 0;
}

.bar {
  display: block;
  width: 100%;
  height: 3px;
  background-color: white;
  border-radius: 2px;
  transition: all 0.3s ease;
}

/* メニューが開いている時の三本線の変形（X印にする演出） */
.hamburger-btn.is-open .bar:nth-child(1) {
  transform: translateY(8px) rotate(45deg);
}
.hamburger-btn.is-open .bar:nth-child(2) {
  opacity: 0;
}
.hamburger-btn.is-open .bar:nth-child(3) {
  transform: translateY(-9px) rotate(-45deg);
}

/* ドロップダウンメニューのスタイル */
.dropdown-menu {
  position: absolute;
  top: 60px;
  right: 0;
  width: 250px;
  background-color: #34495e;
  box-shadow: -2px 4px 10px rgba(0,0,0,0.3);
  border-bottom-left-radius: 8px;
}

.dropdown-menu ul {
  list-style: none;
  margin: 0;
  padding: 10px 0;
}

.dropdown-menu li a {
  display: block;
  padding: 15px 20px;
  color: #ecf0f1;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-menu li a:hover {
  background-color: #465c71;
}

.menu-divider {
  height: 1px;
  background-color: #2c3e50;
  margin: 5px 0;
}

.logout-link {
  color: #e74c3c !important;
}

/* アニメーション効果 */
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from, .slide-leave-to {
  transform: translateY(-10px);
  opacity: 0;
}
</style>