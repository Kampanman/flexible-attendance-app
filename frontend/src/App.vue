<!-- 
<script setup>
import HelloWorld from './components/HelloWorld.vue'
</script>

<template>
  <HelloWorld />
</template>
 -->
<template>
  <div id="app">
    <AppHeader v-if="showHeader" />
    
    <main class="main-content" :class="{ 'has-header': showHeader }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppHeader from './components/AppHeader.vue';
import LoginForm from './components/LoginForm.vue';
import AttendanceBoard from './components/AttendanceBoard.vue';
import RegisterForm from './components/RegisterForm.vue';

// ログイン前（初期・登録）か、ログイン後（打刻・ダッシュボード）かをURLパスで判定
const showHeader = computed(() => {
  // window.location.pathname を使うことで、ルーターの初期化前でも確実に現在のURLを取得します
  const currentPath = window.location.pathname;

  // ルートパス「/」はログイン画面にリダイレクトされる想定のため、
  // 「/」「/login」「/register」の3つのときはヘッダーを「非表示（false）」にします
  if (currentPath === '/' || currentPath === '/login' || currentPath === '/register') {
    return false;
  }
  
  // それ以外の画面（/dashboard や /attendance など）では「表示（true）」にします
  return true;
});
</script>

<style scoped>
/* ヘッダー固定に伴い、コンテンツが下に隠れないように余白を作る */
.main-content.has-header {
  padding-top: 80px; 
}

.app-header { display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #333; color: white;
}
.hamburger { background: none; border: none; cursor: pointer; display: flex; flex-direction: column; gap: 5px; }
.hamburger span { display: block; width: 25px; height: 3px; background: white; }
.menu-container { position: relative; }
.dropdown-menu {
  position: absolute;
  right: 0; top: 40px;
  background: white;
  color: #333;
  border: 1px solid #ddd; border-radius: 5px;
  padding: 10px;
  min-width: 150px;
  z-index: 100;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}
.logout-btn { width: 100%;
  padding: 8px;
  background: #f44336; color: white;
  border: none; border-radius: 3px;
  cursor: pointer;
}
.user-info { font-size: 0.9rem; margin: 5px 0; }
.switch-mode-link {
  margin-top: 30px; /* メッセージとの間隔を広げる */
  text-align: center;
}
.text-button {
  background-color: #007bff; /* ログインボタンと同色（青系） */
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 20px; /* ボタンを大きく */
  font-size: 1rem;
  cursor: pointer; /* マウスカーソルを指マークに */
  transition: background-color 0.3s;
}
.text-button:hover {
  background-color: #0056b3; /* ホバー時に少し暗く */
}
</style>