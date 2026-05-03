import api from '@/services/api';
const BASE = '/api/invoices';
const invoiceService = {
  list:       (params) => api.get(BASE, { params }).then(r => r.data),
  get:        (id)     => api.get(`${BASE}/${id}`).then(r => r.data),
  create:     (dto)    => api.post(BASE, dto).then(r => r.data),
  update:     (id, dto)=> api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove:     (id)     => api.delete(`${BASE}/${id}`).then(r => r.data),
  restore:    (id)     => api.put(`${BASE}/${id}/restore`).then(r => r.data),
  confirm:    (id)     => api.put(`${BASE}/${id}/confirm`).then(r => r.data),
  setPayment: (id, status) => api.put(`${BASE}/${id}/payment-status`, null, { params: { status } }).then(r => r.data),
  byCustomer: (customerId) => api.get(`${BASE}/customer/${customerId}`).then(r => r.data),
};
export default invoiceService;
