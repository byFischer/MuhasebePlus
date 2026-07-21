import api from '@/services/api';
const BASE = '/api/bank-accounts';
const bankAccountService = {
  banks:   ()       => api.get(`${BASE}/banks`).then(r => r.data),
  list:    ()       => api.get(BASE).then(r => r.data),
  get:     (id)     => api.get(`${BASE}/${id}`).then(r => r.data),
  create:  (dto)    => api.post(BASE, dto).then(r => r.data),
  update:  (id, dto)=> api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove:  (id)     => api.delete(`${BASE}/${id}`).then(r => r.data),
  restore: (id)     => api.put(`${BASE}/${id}/restore`).then(r => r.data),
};
export default bankAccountService;
