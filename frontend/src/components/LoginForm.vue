<template>
  <div class="login-container">
    <h2>勤怠管理システム ログイン</h2>
    <form @submit.prevent="handleLogin">
      <div class="field">
        <label>ユーザーID (Email)</label>
        <input type="email" v-model="userId" required placeholder="example@example.com">
      </div>
      <div class="field">
        <label>パスワード</label>
        <input type="password" v-model="password" required>
      </div>
    <button type="submit">ログイン</button>
    </form>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import apiClient from '../api'; // 先ほど作った設定ファイルを読み込む

const userId = ref('');
const password = ref('');
const errorMessage = ref('');

// LoginForm.vue の handleLogin メソッド内
const handleLogin = async () => {
  try {
    const response = await apiClient.post('/users/login', {
      userId: userId.value,
      password: password.value
    });
    
    // alertは消して、親コンポーネントにイベントを送る
    emit('login-success', response.data); 
  } catch (error) {
    console.error('ログイン失敗:', error);
    errorMessage.value = 'ユーザーIDまたはパスワードが正しくありません。';
  }
};

// script setup の冒頭に emit の定義を追加
const emit = defineEmits(['login-success']);
</script>

<style scoped>
.login-container {
  max-width: 400px;
  margin: 50px auto;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
}
.field { margin-bottom: 15px; }
label { display: block; margin-bottom: 5px; }
input { width: 100%; padding: 8px; box-sizing: border-box; }
button { width: 100%; padding: 10px; background-color: #42b983; color: white; border: none; cursor: pointer; }
.error { color: red; margin-top: 10px; }
</style>