import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.method === "post") {
    config.headers["Idempotency-Key"] = config.idempotencyKey ?? crypto.randomUUID();
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem("token");
      const isHash = import.meta.env.VITE_ROUTER === 'hash';
      const current = isHash ? window.location.hash : window.location.pathname;
      if (!current.includes('/login')) {
        window.location.href = isHash ? (window.location.pathname + '#/login') : '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
