import api from '@/services/api';
const BASE = '/api/vat-declarations';

export const vatService = {
  getAll: ()          => api.get(BASE).then(r => r.data),
  getById: (id)       => api.get(`${BASE}/${id}`).then(r => r.data),
  calculate: (dto)    => api.post(`${BASE}/calculate`, dto).then(r => r.data),
  lock: (id)          => api.post(`${BASE}/${id}/lock`).then(r => r.data),
  getBaBs: (id)       => api.get(`${BASE}/${id}/ba-bs`).then(r => r.data),
};
