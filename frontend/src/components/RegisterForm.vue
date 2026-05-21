<template>
  <div class="register-container">
    <div class="register-card">
      <h2 class="form-title">ユーザー登録</h2>
      
      <form @submit.prevent="handleRegister" class="registration-form">
        <div class="form-group">
          <label for="userName">ユーザー名</label>
          <input id="userName" v-model="userName" type="text" :disabled="isRedirecting" required placeholder="例：田中 太郎" />
        </div>

        <div class="form-group">
          <label for="userId">ユーザーID (メールアドレス)</label>
          <input id="userId" v-model="userId" type="email" :disabled="isRedirecting" required placeholder="example@mail.com" />
        </div>

        <div class="form-group">
          <label for="password">パスワード</label>
          <input id="password" v-model="password" type="password" :disabled="isRedirecting" required />
        </div>
        <div class="form-group">
          <label for="passwordConfirm">パスワード (確認用)</label>
          <input id="passwordConfirm" v-model="passwordConfirm" type="password" :disabled="isRedirecting" required />
        </div>

        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

        <div class="button-group">
          <button type="submit" class="submit-btn" :disabled="isRedirecting">これで登録する</button>
          <button type="button" @click="resetForm" class="reset-btn" :disabled="isRedirecting">リセット</button>
        </div>
      </form>

      <div class="footer-link">
        <span @click="!isRedirecting && router.push('/login')" class="login-link" :class="{ 'disabled-link': isRedirecting }">
          既にアカウントをお持ちの方はこちら（ログイン画面へ）
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const userName = ref('');
const userId = ref('');
const password = ref('');
const passwordConfirm = ref('');
const errorMessage = ref('');

// ★新しく追加する状態変数
const successMessage = ref('');   // 成功メッセージ用
const isRedirecting = ref(false); // 3秒間のリダイレクト中かどうかを判定

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
      errorMessage.value = ""; // エラーを消す
      isRedirecting.value = true; // 連打や入力を防ぐため無効化モードオン
      
      // ★ カウントダウンタイマー（3秒）の作成
      let countdown = 3;
      successMessage.value = `登録が完了しました！${countdown}秒後にログイン画面へ移動します...`;
      
      const timer = setInterval(() => {
        countdown -= 1;
        if (countdown > 0) {
          successMessage.value = `登録が完了しました！${countdown}秒後にログイン画面へ移動します...`;
        } else {
          clearInterval(timer); // タイマーを止める
          
          // 既存のイベント通知を送りつつ、ルーターでログイン画面へジャンプ
          emit('switch-to-login'); 
          router.push('/login');
        }
      }, 1000); // 1秒ごとに実行

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
  successMessage.value = '';
};
</script>

<style scoped>
/* 既存のスタイルに以下を追加、または一部書き換え */

.register-container {
  padding-top: 40px;
  display: flex;
  justify-content: center;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 20px;
}

.form-title {
  text-align: center;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  text-align: left;
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

/* 入力不可状態（リダイレクト中）の入力欄の見た目 */
.form-group input:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.error-message {
  color: #ff4444;
  background-color: #fdf2f2; /* 成功メッセージに合わせて薄い背景色を追加して視認性を向上 */
  padding: 6px 10px;        /* 上下の余白を小さく（15px → 6px）してコンパクトに */
  border: 1px solid #fde8e8;  /* ほんのり枠線を付与 */
  border-radius: 4px;
  margin-bottom: 15px;
  text-align: center;
  font-size: 0.85rem;        /* 文字サイズを少し小さく（1rem → 0.85rem）して折り返しを防止 */
}

/* 登録成功メッセージのスタイル（安心感を与える緑系） */
.success-message {
  color: #27ae60;
  background-color: #e8f8f5;
  padding: 6px 10px;        /* 上下の余白を小さく（10px → 6px）してスマートに */
  border: 1px solid #2ecc71;
  border-radius: 4px;
  margin-bottom: 15px;
  text-align: center;
  font-weight: bold;
  font-size: 0.85rem;        /* 文字サイズを少し小さくして「○秒後に〜」が1行に収まりやすく調整 */
}

.button-group {
  margin-top: 30px;
  display: flex;
  gap: 15px;
  justify-content: center;
}

.submit-btn {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  flex: 1;
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

/* ボタンが無効化されているときのスタイル */
button:disabled {
  background-color: #cccccc !important;
  color: #888888 !important;
  cursor: not-allowed;
  filter: none !important;
}

.footer-link {
  margin-top: 25px;
  text-align: center;
}

.login-link {
  color: #007bff;
  text-decoration: underline;
  cursor: pointer;
  font-size: 0.9rem;
}

/* リダイレクト中にリンクをクリックできないようにする制御 */
.disabled-link {
  color: #999999 !important;
  text-decoration: none;
  cursor: not-allowed;
}

.submit-btn:hover, .login-link:hover {
  filter: brightness(1.2);
}
</style>