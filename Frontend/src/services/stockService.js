import api from '@/services/api';
const BASE = '/api/stocks';
const stockService = {
  list:       ()          => api.get(BASE).then(r => r.data),
  getByProduct: (pid)     => api.get(`${BASE}/product/${pid}`).then(r => r.data),
  lowStock:   ()          => api.get(`${BASE}/low`).then(r => r.data),
  create:     (dto)       => api.post(BASE, dto).then(r => r.data),
  update:     (pid, dto)  => api.put(`${BASE}/${pid}`, dto).then(r => r.data),
  addStock:   (pid, dto)  => api.post(`${BASE}/${pid}/add`, dto).then(r => r.data),
  removeStock:(pid, dto)  => api.post(`${BASE}/${pid}/remove`, dto).then(r => r.data),
  countAdj:   (pid, qty)  => api.put(`${BASE}/${pid}/count`, null, { params: { quantity: qty } }).then(r => r.data),
  remove:     (pid)       => api.delete(`${BASE}/${pid}`).then(r => r.data),
  restore:    (pid)       => api.put(`${BASE}/${pid}/restore`).then(r => r.data),
};
export default stockService;
