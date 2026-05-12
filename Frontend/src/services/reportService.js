import api from '@/services/api';
const BASE = '/api/reports';
const reportService = {
  list:     ()     => api.get(BASE).then(r => r.data),
  get:      (id)   => api.get(`${BASE}/${id}`).then(r => r.data),
  generate: (dto)  => api.post(`${BASE}/generate`, dto).then(r => r.data),
  preview:  (dto)  => api.post(`${BASE}/preview`, dto).then(r => r.data),
  download: (id)   => api.get(`${BASE}/${id}/download`, { responseType: 'blob' }).then(r => r.data),
  remove:   (id)   => api.delete(`${BASE}/${id}`).then(r => r.data),
  restore:  (id)   => api.put(`${BASE}/${id}/restore`).then(r => r.data),
  getStatement: (customerId, startDate, endDate) => api.get(`${BASE}/statement/${customerId}`, { params: { startDate, endDate } }).then(r => r.data),
  getReconciliation: (customerId, periodStart, periodEnd) => api.get(`${BASE}/reconciliation/${customerId}`, { params: { periodStart, periodEnd } }).then(r => r.data),
};
export default reportService;
