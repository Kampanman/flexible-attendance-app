import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router' // 1. 作成したルーター設定（router/index.jsなど）を読み込む

createApp(App).mount('#app')

const app = createApp(App)

// 2. アプリ全体にルーターを登録する（有効化する）
// これを挟むことで、App.vue の中で <router-view> や <router-link> が使えるようになります。
app.use(router) 

// 3. 最後に画面にマウントする
app.mount('#app')