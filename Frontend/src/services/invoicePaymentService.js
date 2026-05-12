import api from '@/services/api';
const BASE = '/api/invoices';

const invoicePaymentService = {
  list:       (invoiceId) => api.get(`${BASE}/${invoiceId}/payments`).then(r => r.data),
  create:     (invoiceId, dto) => api.post(`${BASE}/${invoiceId}/payments`, dto).then(r => r.data),
  remove:     (invoiceId, paymentId) => api.delete(`${BASE}/${invoiceId}/payments/${paymentId}`).then(r => r.data),
  getPromises: (invoiceId) => api.get(`${BASE}/${invoiceId}/promises`).then(r => r.data),
  createPromise: (invoiceId, dto) => api.post(`${BASE}/${invoiceId}/promises`, dto).then(r => r.data),
  fulfillPromise: (invoiceId, promiseId) => api.put(`${BASE}/${invoiceId}/promises/${promiseId}/fulfill`).then(r => r.data),
};

export default invoicePaymentService;
