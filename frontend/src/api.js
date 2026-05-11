// frontend/src/api.js
import axios from 'axios';

// バックエンドのURLを指定（CodespacesのURLに合わせて変更が必要です）
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api', 
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;