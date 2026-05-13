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
    <header v-if="user" class="app-header">
      <div class="logo">勤怠システム</div>
      <div class="menu-container">
        <button @click="isMenuOpen = !isMenuOpen" class="hamburger">
          <span></span><span></span><span></span>
        </button>
        <div v-if="isMenuOpen" class="dropdown-menu">
          <p class="user-info">{{ user.userName }} さん</p>
          <hr>
          <button @click="logout" class="logout-btn">ログアウト</button>
        </div>
      </div>
    </header>
    <main>
      <LoginForm v-if="!user" @login-success="handleLoginSuccess" />
      <AttendanceBoard v-else :accountId="user.accountId" :userName="user.userName" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import LoginForm from './components/LoginForm.vue';
import AttendanceBoard from './components/AttendanceBoard.vue';

const user = ref(null);
const isMenuOpen = ref(false);

const handleLoginSuccess = (userData) => {
  user.value = userData;
};

const logout = () => {
  if (confirm('ログアウトしますか？')) {
    user.value = null; // ユーザー情報を空にするだけで、v-ifによりログイン画面に戻ります
    isMenuOpen.value = false;
  }
};
</script>

<style scoped>
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
</style>