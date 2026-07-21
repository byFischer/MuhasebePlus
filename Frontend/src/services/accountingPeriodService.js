import api from '@/services/api';
const BASE = '/api/accounting-periods';
const accountingPeriodService = {
  list:   (year)         => api.get(BASE, { params: { year } }).then(r => r.data),
  get:    (year, month)  => api.get(`${BASE}/${year}/${month}`).then(r => r.data),
  status: ()             => api.get(`${BASE}/status`).then(r => r.data),
  close:     (year, month)         => api.post(`${BASE}/close`, { year, month }).then(r => r.data),
  reopen:    (year, month, reason) => api.post(`${BASE}/reopen`, { year, month, reason }).then(r => r.data),
  closeYear: (year)                => api.post(`${BASE}/close-year`, null, { params: { year } }).then(r => r.data),
};
export default accountingPeriodService;
