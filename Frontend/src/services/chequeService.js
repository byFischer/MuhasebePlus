import api from '@/services/api';
const BASE = '/api/cheques';
const chequeService = {
  list:            (params)       => api.get(BASE, { params }).then(r => r.data.content || r.data),
  get:             (id)           => api.get(`${BASE}/${id}`).then(r => r.data),
  create:          (dto)          => api.post(BASE, dto).then(r => r.data),
  update:          (id, dto)      => api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove:          (id)           => api.delete(`${BASE}/${id}`).then(r => r.data),
  deposit:         (id, bankAccountId) => api.post(`${BASE}/${id}/deposit`, null, { params: { bankAccountId } }).then(r => r.data),
  collect:         (id, bankAccountId) => api.post(`${BASE}/${id}/collect`, null, { params: { bankAccountId } }).then(r => r.data),
  bounce:          (id, reason)   => api.post(`${BASE}/${id}/bounce`, null, { params: { reason } }).then(r => r.data),
  endorse:         (id, toCustomerId) => api.post(`${BASE}/${id}/endorse`, null, { params: { toCustomerId } }).then(r => r.data),
  cancel:          (id, reason)   => api.post(`${BASE}/${id}/cancel`, null, { params: { reason } }).then(r => r.data),
  portfolioSummary: ()            => api.get(`${BASE}/portfolio/summary`).then(r => r.data),
  upcoming:        (days = 30)    => api.get(`${BASE}/upcoming`, { params: { days } }).then(r => r.data),
};
export default chequeService;
