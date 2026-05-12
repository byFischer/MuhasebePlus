import api from '@/services/api';
const BASE = '/api/invoices';
const invoiceService = {
  list:       (params) => api.get(BASE, { params }).then(r => r.data.content || r.data),
  get:        (id)     => api.get(`${BASE}/${id}`).then(r => r.data),
  create:     (dto)    => api.post(BASE, dto).then(r => r.data),
  update:     (id, dto)=> api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove:     (id)     => api.delete(`${BASE}/${id}`).then(r => r.data),
  restore:    (id)     => api.put(`${BASE}/${id}/restore`).then(r => r.data),
  confirm:    (id)     => api.put(`${BASE}/${id}/confirm`).then(r => r.data),
  cancel:     (id, reason) => api.put(`${BASE}/${id}/cancel`, null, { params: { reason } }).then(r => r.data),
  createReturn: (id, dto) => api.post(`${BASE}/${id}/return`, dto).then(r => r.data),
  setPayment: (id, status) => api.put(`${BASE}/${id}/payment-status`, null, { params: { status } }).then(r => r.data),
  byCustomer: (customerId) => api.get(`${BASE}/customer/${customerId}`).then(r => r.data),
  getSeries:  () => api.get(`${BASE}/series`).then(r => r.data),
  createSeries: (dto) => api.post(`${BASE}/series`, dto).then(r => r.data),
  nextNumber: (invoiceType, seriesCode) => api.get(`${BASE}/series/next-number`, { params: { invoiceType, seriesCode } }).then(r => r.data),
  getLateFee: (id) => api.get(`${BASE}/${id}/late-fee`).then(r => r.data),
  getLateFeeConfig: () => api.get(`${BASE}/late-fee-config`).then(r => r.data),
  updateLateFeeConfig: (monthlyRate, gracePeriodDays) => api.put(`${BASE}/late-fee-config`, null, { params: { monthlyRate, gracePeriodDays } }).then(r => r.data),
  downloadPdf: (id) => api.get(`${BASE}/${id}/pdf`, { responseType: 'blob' }).then(r => r.data),
};
export default invoiceService;
