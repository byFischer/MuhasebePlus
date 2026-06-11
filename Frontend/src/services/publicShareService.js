import publicApi from '@/services/publicApi';
const BASE = '/api/public/share';
const publicShareService = {
  getInfo:      (token) => publicApi.get(`${BASE}/${token}`).then(r => r.data),
  listInvoices: (token, params) => publicApi.get(`${BASE}/${token}/invoices`, { params }).then(r => r.data),
  getSummary:   (token, params) => publicApi.get(`${BASE}/${token}/summary`, { params }).then(r => r.data),
  downloadPdf:  (token, invoiceId) => publicApi.get(`${BASE}/${token}/invoices/${invoiceId}/pdf`, { responseType: 'blob' }).then(r => r.data),
};
export default publicShareService;
