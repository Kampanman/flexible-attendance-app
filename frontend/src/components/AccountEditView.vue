<template>
  <div class="account-edit-container">
    <h2>アカウント情報編集</h2>

    <div v-if="countdown > 0" class="alert alert-success">
      <p v-for="parts in successMessageParts">{{ parts }}</p>
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

      <div class="button-group">
        <button type="submit" :disabled="isSubmitting" class="btn-submit">
          {{ isSubmitting ? '更新中...' : '変更を保存する' }}
        </button>
        
        <div v-if="quitDemand === 1" class="quit-pending-message">
          現在、アカウント削除申請中です。管理者の承認をお待ちください。
        </div>

        <div v-else-if="userId === 'admin@example.com'" class="admin-notice-message">
          統括管理者アカウントは削除申請をすることができません。
        </div>

        <button v-else type="button" :disabled="isSubmitting" @click="handleQuitDemand" class="btn-quit">
          アカウント削除申請をする
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api'; // 共通APIクライアント（/api がベースURLに設定されている前提）

const router = useRouter();

// フォーム用の状態管理
const accountId = ref('');
const userId = ref('');
const userName = ref('');
const password = ref('');
const passwordConfirm = ref('');
const quitDemand = ref(0); // 退会申請フラグの状態を管理する変数

const errorMessage = ref('');
const successMessage = ref(''); // 通知ボックス用の動的メッセージ
const isSubmitting = ref(false);
const countdown = ref(0); // 自動ログアウトまでのカウントダウン秒数

let successMessageParts = "";

// 画面表示時にローカルストレージから現在の情報を復元
onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    const user = JSON.parse(userData);
    accountId.value = user.accountId;
    userId.value = user.userId;
    userName.value = user.userName;
    quitDemand.value = user.quitDemand || 0; // 初期状態のフラグをセット
  } else {
    router.push('/login');
  }
});

// タイマーと自動ログアウトの共通処理
const startLogoutTimer = (message) => {
  isSubmitting.value = false;
  const dotSplit = message.split('。').filter(text => {
    return text != ""
  }).map((text) => {
    return `${text}。`
  });

  successMessageParts = dotSplit;
  countdown.value = 3; // 3秒のカウントダウンを開始

  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value === 0) {
      clearInterval(timer);
      localStorage.removeItem('user'); // ローカルストレージ消去
      router.push('/login'); // ログイン画面へリダイレクト
    }
  }, 1000);
};

// 情報更新処理
const handleUpdate = async () => {
  errorMessage.value = '';

  if (password.value) {
    if (password.value !== passwordConfirm.value) {
      errorMessage.value = 'パスワード（確認）が一致しません。';
      return;
    }
    if (password.value.length < 4) {
      errorMessage.value = 'パスワードは4文字以上で入力してください。';
      return;
    }
  }

  isSubmitting.value = true;

  try {
    // 前回、二重パス対策で解決した正しいエンドポイント（/api を除いたパス）
    await apiClient.put('/users/update', {
      accountId: accountId.value,
      userName: userName.value,
      password: password.value || null
    });

    startLogoutTimer('アカウント情報を更新しました。');

  } catch (error) {
    isSubmitting.value = false;
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data;
    } else {
      errorMessage.value = '通信エラーが発生しました。';
    }
  }
};

// ★【核心】2段階の確認ダイアログを経て実行する退会申請ロジック
const handleQuitDemand = async () => {
  errorMessage.value = '';

  // 1段階目の確認
  const firstConfirm = confirm('本当にアカウント削除申請（退会申請）を行いますか？');
  if (!firstConfirm) return;

  // 2段階目の確認
  const secondConfirm = confirm('【最終確認】申請を行うと、管理者の承認後にアカウントが削除されます。よろしいですか？');
  if (!secondConfirm) return;

  isSubmitting.value = true;

  try {
    // 先ほどJava側で作成した PUT /api/users/quit を叩く（ベースURLを考慮して /users/quit）
    await apiClient.put('/users/quit', {
      accountId: accountId.value
    });

    // 削除申請成功後、数秒のインターバルを経て自動ログアウト
    startLogoutTimer('アカウント削除申請を受け付けました。ご利用ありがとうございました。');

  } catch (error) {
    isSubmitting.value = false;
    if (error.response && error.response.data) {
      errorMessage.value = error.response.data;
    } else {
      errorMessage.value = '退会申請の通信中にエラーが発生しました。';
    }
  }
};
</script>

<style scoped>
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
  text-align: center;
}

/* ボタン配置エリアのスタイル */
.button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 25px;
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

/* 退会用の警告カラー（赤系）のボタンスタイル */
.btn-quit {
  width: 100%;
  padding: 12px;
  background-color: #fff3f6;
  color: #dc3545;
  border: 1px solid #dc3545;
  border-radius: 4px;
  font-size: 0.95rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-quit:hover {
  background-color: #fff5f5;
}

button:disabled {
  background-color: #6c757d !important;
  border-color: #6c757d !important;
  color: white !important;
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

/* アカウント削除申請中のメッセージスタイル */
.quit-pending-message {
  padding: 12px;
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeeba;
  border-radius: 4px;
  font-size: 0.9rem;
  font-weight: bold;
  text-align: center;
}

/* 管理者専用の案内メッセージスタイル */
.admin-notice-message {
  padding: 12px;
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: center;
}
</style>