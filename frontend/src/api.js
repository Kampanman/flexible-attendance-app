// frontend/src/api.js
import axios from 'axios';

// バックエンドのURLを指定（CodespacesのURLに合わせて変更が必要です）
const apiClient = axios.create({
  // baseURL: 'http://localhost:8080/api', 
  baseURL: 'https://ubiquitous-spork-4vq65g5rr79c5j6q-8080.app.github.dev/api', 
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;