<template>
  <div class="admin-announcement-panel">
    <div class="panel-header">
      <h3>お知らせ（インフォメーション）管理一覧</h3>
      <button 
        @click="prepareNewAnnouncement" 
        class="btn-primary"
        v-if="!isEditing"
      >
        新しいお知らせを作成
      </button>
    </div>

    <div v-if="isEditing" class="form-container-box">
      <h4>{{ form.announcementId ? 'お知らせの編集' : '新しいお知らせの登録' }}</h4>
      
      <div class="form-group">
        <label>タイトル <span class="required">※必須</span></label>
        <input 
          v-model="form.announcementTitle" 
          type="text" 
          placeholder="例: 年末年始の稼働日に関するお知らせ"
          maxlength="100"
        />
      </div>

      <div class="form-group">
        <label>本文・詳細内容 <span class="required">※必須</span></label>
        <textarea 
          v-model="form.announcementAbout" 
          rows="5" 
          placeholder="一般ユーザーのダッシュボード最上部に掲示される文章を入力してください。"
        ></textarea>
      </div>

      <div class="form-actions">
        <button @click="handleSave" class="btn-save">内容を保存する</button>
        <button @click="cancelEdit" class="btn-cancel">キャンセル</button>
      </div>
    </div>

    <div class="announcement-list" v-if="announcements.length > 0">
      <div 
        v-for="item in announcements" 
        :key="item.announcementId" 
        class="announcement-card"
      >
        <div class="card-title-bar">
          <h5>{{ item.announcementTitle }}</h5>
        </div>
        <p class="card-content-text">{{ item.announcementAbout }}</p>
        
        <div class="card-footer">
          <small class="id-text">ID: {{ item.announcementId }}</small>
          <div class="card-actions-gap">
            <button @click="startEdit(item)" class="btn-edit-trigger">編集する</button>
            <button @click="handleDelete(item.announcementId, item.announcementTitle)" class="btn-delete-trigger">
              削除する
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!isEditing" class="no-data-alert">
      現在登録されているお知らせはありません。右上のボタンから作成してください。
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import apiClient from '../api'; // ご利用の共通APIクライアント

// 状態管理変数
const announcements = ref([]); // お知らせリスト
const isEditing = ref(false);    // フォームを開いているかどうかのフラグ

// フォームの入力データを束ねるオブジェクト（既存のJavaフィールド名に準拠）
const form = ref({
  announcementId: '',
  announcementTitle: '',
  announcementAbout: ''
});

// 1. サーバーから最新のお知らせ一覧を全件引っ張ってくる関数
const fetchAnnouncements = async () => {
  try {
    const response = await apiClient.get('/admin/announcements');
    announcements.value = response.data;
  } catch (error) {
    console.error('お知らせ一覧の取得に失敗しました:', error);
    alert('お知らせ一覧の読み込みに失敗しました。');
  }
};

// 新規作成フォームの準備
const prepareNewAnnouncement = () => {
  form.value = {
    announcementId: '',
    announcementTitle: '',
    announcementAbout: ''
  };
  isEditing.value = true;
};

// 2. 「編集する」ボタンが押されたとき、フォームに既存の値をロードする関数
const startEdit = (item) => {
  form.value = { ...item };
  isEditing.value = true;
  // 編集しやすいように画面上部へスクロール
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// キャンセル処理
const cancelEdit = () => {
  isEditing.value = false;
};

// 3. 「内容を保存する」ボタンが押されたときの処理（新規・上書き共通）
const handleSave = async () => {
  // フロントエンド防壁（簡易バリデーション）
  if (!form.value.announcementTitle.trim()) {
    alert('タイトルを入力してください。');
    return;
  }
  if (!form.value.announcementAbout.trim()) {
    alert('本文・詳細内容を入力してください。');
    return;
  }

  try {
    // バックエンドの POST /api/admin/announcements/save へ送信
    const response = await apiClient.post('/admin/announcements/save', form.value);
    alert(response.data); // 「お知らせの内容を正常に反映・保存しました」を表示
    isEditing.value = false;
    fetchAnnouncements(); // 一覧をパッと再ロード
  } catch (error) {
    console.error('お知らせ保存エラー:', error);
    alert(error.response?.data || 'お知らせの保存に失敗しました。');
  }
};

// お知らせの削除リクエストを処理する
const handleDelete = async (announcementId, announcementTitle) => {
  // 管理者への最終確認（誤操作ガード）
  if (!confirm(`【確認】\nお知らせ「${announcementTitle}」を完全に削除してもよろしいですか？\nこの操作は取り消せません。`)) {
    return;
  }

  try {
    // バックエンドの @DeleteMapping("/delete/{id}") へ接続
    // パスパラメータとしてURLの末尾に直接IDを結合して送信します
    const response = await apiClient.delete(`/admin/announcements/delete/${announcementId}`);
    
    alert(response.data); // 「お知らせを正常に削除しました。」を表示
    // 現在編集中のデータが、たった今削除したデータだった場合はフォームを閉じる安全策
    if (form.value.announcementId === announcementId) {
      cancelEdit();
    }
    
    fetchAnnouncements(); // 削除後の最新リストを再取得して画面をパッと更新！
  } catch (error) {
    console.error('お知らせ削除エラー:', error);
    alert(error.response?.data || 'お知らせの削除に失敗しました。');
  }
};

// 画面表示時に自動ロード
onMounted(() => {
  fetchAnnouncements();
});
</script>

<style scoped>
.admin-announcement-panel { padding: 5px; text-align: left; font-family: sans-serif; }
.panel-header { display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid #eceff1;
  padding-bottom: 10px;
}
.panel-header h3 { margin: 0; color: #2c3e50; font-size: 1.2rem; }

/* ボタン系のスタイル */
.btn-primary {
  background-color: #0288d1;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}
.btn-primary:hover { background-color: #01579b; }

/* フォームのスタイリング */
.form-container-box { background: #f8f9fa;
  border: 1px solid #b0bec5;
  border-radius: 6px;
  padding: 20px;
  margin-bottom: 25px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
}
.form-container-box h4 { margin-top: 0; margin-bottom: 15px; color: #37474f; border-left: 4px solid #0288d1; padding-left: 10px; }
.form-group { margin-bottom: 15px; }
.form-group label { display: block; font-weight: bold; font-size: 0.9rem; color: #455a64; margin-bottom: 6px; }
.form-group input[type="text"], .form-group textarea { width: 100%;
  padding: 10px;
  border: 1px solid #cfd8dc;
  border-radius: 4px;
  box-sizing: border-box;
  font-size: 0.95rem;
}
.required { color: #d32f2f; }

.form-actions { display: flex; gap: 10px; }
.btn-save {
  background-color: #2e7d32;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-weight: bold;
  cursor: pointer;
}
.btn-save:hover { background-color: #1b5e20; }
.btn-cancel { background-color: #cfd8dc; color: #37474f; border: none; padding: 10px 16px; border-radius: 4px; cursor: pointer; }
.btn-cancel:hover { background-color: #b0bec5; }

/* お知らせカードのスタイリング */
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 15px;
}
.announcement-card {
  background: #ffffff;
  border: 1px solid #cfd8dc;
  border-radius: 6px;
  padding: 15px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.04);
}
.card-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-title-bar h5 {
  margin: 0;
  font-size: 1.05rem;
  color: #263238;
  font-weight: bold;
}

.card-content-text { font-size: 0.95rem; color: #455a64; margin: 0 0 12px 0; line-height: 1.5; white-space: pre-wrap; }
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #eceff1;
  padding-top: 10px;
}
.id-text { color: #90a4ae; font-family: monospace; }
.btn-edit-trigger { background: transparent;
  border: 1px solid #0288d1;
  color: #0288d1;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-edit-trigger:hover { background: #e1f5fe; }

/* ボタンを綺麗に横並びにする */
.card-actions-gap {
  display: flex;
  gap: 8px;
}

/* 削除ボタンのスタイリング */
.btn-delete-trigger {
  background: transparent;
  border: 1px solid #e53935;
  color: #e53935;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-delete-trigger:hover {
  background-color: #ffebee;
}

.no-data-alert {
  background: #eceff1;
  border: 1px solid #b0bec5;
  text-align: center;
  padding: 25px;
  border-radius: 6px;
  color: #546e7a;
  font-weight: bold;
}
</style>