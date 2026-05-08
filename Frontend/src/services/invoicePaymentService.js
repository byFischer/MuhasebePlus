import api from '@/services/api';
const BASE = '/api/invoices';

const invoicePaymentService = {
  list:       (invoiceId) => api.get(`${BASE}/${invoiceId}/payments`).then(r => r.data),
  create:     (invoiceId, dto) => api.post(`${BASE}/${invoiceId}/payments`, dto).then(r => r.data),
  remove:     (invoiceId, paymentId) => api.delete(`${BASE}/${invoiceId}/payments/${paymentId}`).then(r => r.data),
};

export default invoicePaymentService;
