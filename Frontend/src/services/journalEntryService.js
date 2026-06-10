import api from '@/services/api';
const BASE = '/api/journal-entries';
const journalEntryService = {
  list:          (params)          => api.get(BASE, { params }).then(r => r.data.content || r.data),
  get:           (id)              => api.get(`${BASE}/${id}`).then(r => r.data),
  create:        (dto)             => api.post(BASE, dto).then(r => r.data),
  reverse:       (id, reason)      => api.post(`${BASE}/${id}/reverse`, null, { params: { reason } }).then(r => r.data),
  remove:        (id)              => api.delete(`${BASE}/${id}`).then(r => r.data),
  trialBalance:  (startDate, endDate) => api.get(`${BASE}/trial-balance`, { params: { startDate, endDate } }).then(r => r.data),
  incomeStatement: (startDate, endDate) => api.get(`${BASE}/income-statement`, { params: { startDate, endDate } }).then(r => r.data),
  balanceSheet: (asOfDate) => api.get(`${BASE}/balance-sheet`, { params: { asOfDate } }).then(r => r.data),
  generalLedger: (accountId, startDate, endDate) => api.get(`${BASE}/general-ledger/${accountId}`, { params: { startDate, endDate } }).then(r => r.data),
};
export default journalEntryService;
