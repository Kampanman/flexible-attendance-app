<template>
  <div class="register-container">
    <div class="register-card">
      <h2 class="form-title">ユーザー登録</h2>
      
      <form @submit.prevent="handleRegister" class="registration-form">
        <div class="form-group">
          <label for="userName">ユーザー名</label>
          <input id="userName" v-model="userName" type="text" required placeholder="例：田中 太郎" />
        </div>

        <div class="form-group">
          <label for="userId">ユーザーID (メールアドレス)</label>
          <input id="userId" v-model="userId" type="email" required placeholder="example@mail.com" />
        </div>

        <div class="form-group">
          <label for="password">パスワード</label>
          <input id="password" v-model="password" type="password" required />
        </div>
        <div class="form-group">
          <label for="passwordConfirm">パスワード (確認用)</label>
          <input id="passwordConfirm" v-model="passwordConfirm" type="password" required />
        </div>

        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <div class="button-group">
          <button type="submit" class="submit-btn">これで登録する</button>
          <button type="button" @click="resetForm" class="reset-btn">リセット</button>
        </div>
      </form>

      <div class="footer-link">
        <span @click="$emit('switch-to-login')" class="login-link">
          既にアカウントをお持ちの方はこちら（ログイン画面へ）
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const userName = ref('');
const userId = ref('');
const password = ref('');
const passwordConfirm = ref('');
const errorMessage = ref('');

const emit = defineEmits(['register-success', 'switch-to-login']);

const handleRegister = async () => {
  // 1. パスワード一致チェック
  if (password.value !== passwordConfirm.value) {
    errorMessage.value = "パスワードが一致しません。";
    return;
  }
  try {
    const response = await fetch('https://ubiquitous-spork-4vq65g5rr79c5j6q-8080.app.github.dev/api/users/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userName: userName.value,
        userId: userId.value,
        password: password.value
      })
    });

    if (response.ok) {
      alert("登録が完了しました！ログインしてください。");
      emit('switch-to-login'); // 成功したらログイン画面に切り替える
    } else {
      const errorText = await response.text();
      errorMessage.value = errorText || "登録に失敗しました。";
    }
  } catch (error) {
    errorMessage.value = "サーバーとの通信に失敗しました。";
  }
};

const resetForm = () => {
  userName.value = '';
  userId.value = '';
  password.value = '';
  passwordConfirm.value = '';
  errorMessage.value = '';
};
</script>

<style scoped>
/* コンテナ全体の余白 */
.register-container {
  padding-top: 40px; /* 見出し上部のスペース */
  display: flex;
  justify-content: center;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 20px;
  /* 背景が黒い環境を想定し、カード形式で浮き立たせる場合はここを調整 */
}

.form-title {
  text-align: center;
  margin-bottom: 30px;
}

/* フォームグループ（ラベルと入力欄のセット）の間隔 */
.form-group {
  margin-bottom: 20px; /* 上下の間隔を確保 */
  display: flex;
  flex-direction: column; /* ラベルを上に、入力欄を下に配置 */
  text-align: left; /* 左寄せにしてフォームらしさを強調 */
}

.form-group label {
  margin-bottom: 8px;
  font-weight: bold;
}

.form-group input {
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ccc;
  font-size: 1rem;
}

.error-message {
  color: #ff4444;
  margin-bottom: 15px;
  text-align: center;
}

/* ボタンエリアの設定 */
.button-group {
  margin-top: 30px; /* フォームとの間隔 */
  display: flex;
  gap: 15px; /* ボタン同士が接触しないように間隔を設ける */
  justify-content: center;
}

.submit-btn {
  background-color: #007bff; /* App.vueのボタンと同色 */
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  flex: 1; /* ボタンの幅を揃える */
}

.reset-btn {
  background-color: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  flex: 1;
}

/* 下部リンクの調整 */
.footer-link {
  margin-top: 25px;
  text-align: center;
}

.login-link {
  color: #007bff; /* 視認性を高めるため、ログインボタンと同色の青系に設定 */
  text-decoration: underline;
  cursor: pointer;
  font-size: 0.9rem;
}

.submit-btn:hover, .login-link:hover {
  filter: brightness(1.2); /* ホバー時に少し明るくして反応を示す */
}
</style>