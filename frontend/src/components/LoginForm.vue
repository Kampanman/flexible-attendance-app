<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="form-title">ログイン</h2>
      
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="userId">ユーザーID</label>
          <input id="userId" v-model="userId" type="text" required placeholder="example@mail.com" />
        </div>

        <div class="form-group">
          <label for="password">パスワード</label>
          <input id="password" v-model="password" type="password" required />
        </div>

        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

        <div class="button-group">
          <button type="submit" class="submit-btn">ログイン</button>
        </div>
      </form>

      <div class="footer-link">
        <span @click="router.push('/register')" class="register-link">
          アカウントをお持ちでないですか？（新規ユーザー登録へ）
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import apiClient from '../api';
import { useRouter } from 'vue-router'; // ★重要: ルーターをインポート

const router = useRouter(); // ★重要: ルーターオブジェクトを取得

const userId = ref('');
const password = ref('');
const errorMessage = ref('');

const emit = defineEmits(['login-success']);

// ログイン処理（router導入前）
// const handleLogin = async () => {
//   try {
//     const response = await apiClient.post('/users/login', {
//       userId: userId.value,
//       password: password.value
//     });
    
//     // 親コンポーネントにイベントを送る
//     emit('login-success', response.data); 
//   } catch (error) {
//     console.error('ログイン失敗:', error);
//     errorMessage.value = 'ユーザーIDまたはパスワードが正しくありません。';
//   }
// };

// ログイン処理（router導入後）
const handleLogin = async () => {
  try {
    const response = await apiClient.post('/users/login', {
      userId: userId.value,
      password: password.value
    });
    
    // 1. ローカルストレージ等に、バックエンドから返ってきたユーザー情報を保存する（後で認証ガードに使うため）
    // ※ response.data の構造（accountId や userName が入っているか）に合わせて調整してください
    localStorage.setItem('user', JSON.stringify(response.data));
    
    // 2. 親へのemit（もし動かなくても保険として残す、不要なら消してもOK）
    emit('login-success', response.data); 
    
    // 3. ★ここで直接、新設したダッシュボード画面へジャンプさせる！
    router.push('/dashboard'); 
    
  } catch (error) {
    console.error('ログイン失敗:', error);
    errorMessage.value = 'ユーザーIDまたはパスワードが正しくありません。';
  }
};
</script>

<style scoped>
/* RegisterForm.vue の洗練されたCSS設計をベースに完全同期 */

/* コンテナ全体の余白 */
.login-container {
  padding-top: 40px; /* 見出し上部のスペース */
  display: flex;
  justify-content: center;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 20px;
}

.form-title {
  text-align: center;
  margin-bottom: 30px;
}

/* フォームグループ（ラベルと入力欄のセット）の間隔をRegisterFormと完全一致 */
.form-group {
  margin-bottom: 20px; /* 上下の間隔を確保 */
  display: flex;
  flex-direction: column; /* ラベルを上に、入力欄を下に配置 */
  text-align: left; /* 左寄せにしてフォームらしさを強調 */
}

/* ラベルのスタイルをRegisterFormの太文字・余白に統一 */
.form-group label {
  margin-bottom: 8px;
  font-weight: bold;
}

/* 入力インプットのサイズ、パディング、境界線を統一 */
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

/* ボタンエリアの設定（横幅100%ではなく、横に並んでも自然な幅に制限） */
.button-group {
  margin-top: 30px; /* フォームとの間隔 */
  display: flex;
  justify-content: center;
}

/* ログインボタン：RegisterFormの「これで登録する」と同色にしつつ、サイズを「小さく自然な大きさ」に調整 */
.submit-btn {
  background-color: #007bff; /* RegisterFormのボタンと同色の鮮やかな青 */
  color: white;
  border: none;
  padding: 10px 40px; /* 横のパディングを広げて存在感を適正化 */
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: bold;
  min-width: 150px; /* 小さすぎず、大きすぎない自然な横幅 */
  transition: filter 0.2s;
}

/* 下部リンクの調整：RegisterFormのfooter-linkとクラス、カラー、ホバーエフェクトを完全統一 */
.footer-link {
  margin-top: 25px;
  text-align: center;
}

.register-link {
  color: #007bff; /* 視認性を高めるため、登録画面と同色の青系に設定 */
  text-decoration: underline;
  cursor: pointer;
  font-size: 0.9rem;
}

.submit-btn:hover, .register-link:hover {
  filter: brightness(1.2); /* ホバー時に少し明るくして反応を示す */
}
</style>