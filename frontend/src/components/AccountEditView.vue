<template>
  <div class="account-edit-container">
    <h2>アカウント情報編集</h2>

    <div v-if="countdown > 0" class="alert alert-success">
      <p>アカウント情報を更新しました。</p>
      <p>安全のため、<strong>{{ countdown }}秒後</strong>に自動ログアウトします...</p>
    </div>

    <form v-else @submit.prevent="handleUpdate" class="edit-form">
      <div class="form-group">
        <label>ユーザーID（メールアドレス）</label>
        <input type="text" :value="userId" disabled class="input-disabled" />
        <small class="form-help">※ユーザーIDは変更できません。</small>
      </div>

      <div class="form-group">
        <label for="userName">新しいユーザー名</label>
        <input type="text" id="userName" v-model="userName" required placeholder="ユーザー名を入力" />
      </div>

      <div class="form-group">
        <label for="password">新しいパスワード</label>
        <input type="password" id="password" v-model="password" placeholder="変更する場合のみ入力" />
      </div>

      <div class="form-group">
        <label for="passwordConfirm">新しいパスワード（確認）</label>
        <input type="password" id="passwordConfirm" v-model="passwordConfirm" placeholder="もう一度入力" />
      </div>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <button type="submit" :disabled="isSubmitting" class="btn-submit">
        {{ isSubmitting ? '更新中...' : '変更を保存する' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api'; // ※お使いの共通APIクライアントのパスに合わせて調整してください

const router = useRouter();

// フォーム用の状態管理
const accountId = ref('');
const userId = ref('');
const userName = ref('');
const password = ref('');
const passwordConfirm = ref('');

const errorMessage = ref('');
const isSubmitting = ref(false);
const countdown = ref(0); // 自動ログアウトまでのカウントダウン秒数

// 画面表示時にローカルストレージから現在の情報を復元
onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    accountId.value = user.accountId;
    userId.value = user.userId;       // エンティティ定義の userId（メールアドレス）
    userName.value = user.userName;   // 現在の名前を初期値としてセット
  } else {
    // ログイン情報がなければログイン画面へ弾く
    router.push('/login');
  }
});

// 更新処理
const handleUpdate = async () => {
  errorMessage.value = '';

  // パスワードのバリデーション（入力されている場合のみチェック）
  if (password.value) {
    if (password.value !== passwordConfirm.value) {
      errorMessage.value = 'パスワード（確認）が一致しません。';
      return;
    }
    if (password.value.length < 4) { // 必要に応じて桁数は調整してください
      errorMessage.value = 'パスワードは4文字以上で入力してください。';
      return;
    }
  }

  isSubmitting.value = true;

  try {
    // 先ほどJava側で作成した PUT /users/update を叩く
    await apiClient.put('/users/update', {
      accountId: accountId.value,
      userName: userName.value,
      password: password.value || null // 空白ならnullを送り、Java側で維持させる
    });

    // 【成功時の処理】
    isSubmitting.value = false;
    countdown.value = 3; // 3秒のカウントダウンを開始

    // タイマーを回す（1秒ごとにカウントを減らす）
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value === 0) {
        clearInterval(timer);
        
        // ローカルストレージを綺麗に消去（ログアウト処理）
        localStorage.removeItem('user');
        
        // ログイン画面へ強制リダイレクト
        router.push('/login');
      }
    }, 1000);

  } catch (error) {
    isSubmitting.value = false;
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data;
    } else {
      errorMessage.value = '通信エラーが発生しました。時間をおいて再度お試しください。';
    }
  }
};
</script>

<style scoped>
/* シンプルでモダンなフォームデザイン（必要に応じて既存の共通スタイルと合わせてください） */
.account-edit-container {
  max-width: 500px;
  margin: 40px auto;
  padding: 30px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
h2 {
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}
.form-group {
  margin-bottom: 20px;
}
label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: #495057;
}
input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  box-sizing: border-box;
}
.input-disabled {
  background-color: #e9ecef;
  color: #6c757d;
  cursor: not-allowed;
}
.form-help {
  color: #6c757d;
  font-size: 0.85rem;
}
.error-message {
  color: #dc3545;
  font-weight: 500;
  margin-bottom: 15px;
}
.btn-submit {
  width: 100%;
  padding: 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-submit:hover {
  background-color: #0056b3;
}
.btn-submit:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}
.alert-success {
  background-color: #d4edda;
  color: #155724;
  padding: 20px;
  border-radius: 4px;
  border-left: 5px solid #28a745;
  text-align: center;
}
</style>