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

const RATE_LIMIT_MAX_RETRIES = 3;

// Sunucu rate limit'e takıldığında (429) isteği üstel bekleme ile birkaç kez
// yeniden dener. Önizleme gibi toplu istekler limiti geçici doldurduğunda,
// asıl kullanıcı işlemleri (ör. "Dashboard'a Ekle") hata vermek yerine kendini
// toparlar.
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem("token");
      const isHash = import.meta.env.VITE_ROUTER === 'hash';
      const current = isHash ? window.location.hash : window.location.pathname;
      if (!current.includes('/login')) {
        window.location.href = isHash ? (window.location.pathname + '#/login') : '/login';
      }
      return Promise.reject(error);
    }

    const config = error.config;
    if (error.response?.status === 429 && config) {
      config.__retryCount = (config.__retryCount || 0) + 1;
      if (config.__retryCount <= RATE_LIMIT_MAX_RETRIES) {
        const retryAfter = Number(error.response.headers?.['retry-after']);
        const wait = Number.isFinite(retryAfter) && retryAfter > 0
          ? retryAfter * 1000
          : 500 * 2 ** (config.__retryCount - 1) + Math.random() * 250;
        await new Promise((resolve) => setTimeout(resolve, wait));
        return api(config);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
