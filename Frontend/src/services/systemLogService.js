import api from '@/services/api';
const BASE = '/api/logs';
const systemLogService = {
  list:   (params) => api.get(BASE, { params }).then(r => r.data),
  export: (params) => api.get(`${BASE}/export`, { params, responseType: 'blob' }).then(r => r.data),
};
export default systemLogService;
