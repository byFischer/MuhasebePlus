import api from '@/services/api';
const BASE = '/api/budgets';

export const budgetService = {
  getAll: (year) => api.get(BASE, { params: year ? { year } : {} }).then(r => r.data),
  create: (dto)  => api.post(BASE, dto).then(r => r.data),
  update: (id, dto) => api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove: (id)   => api.delete(`${BASE}/${id}`),
};
