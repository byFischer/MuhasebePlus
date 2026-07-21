import api from '@/services/api';
const BASE = '/api/chart-of-accounts';
const chartOfAccountService = {
  list:   ()         => api.get(BASE).then(r => r.data),
  get:    (id)       => api.get(`${BASE}/${id}`).then(r => r.data),
  create: (dto)      => api.post(BASE, dto).then(r => r.data),
  update: (id, dto)  => api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove:    (id)  => api.delete(`${BASE}/${id}`).then(r => r.data),
  seedTdhp:  ()    => api.post(`${BASE}/seed-tdhp`).then(r => r.data),
};
export default chartOfAccountService;
