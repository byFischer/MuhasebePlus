import api from '@/services/api';
const BASE = '/api/bank-reconciliation';

export const bankReconciliationService = {
  getAll: ()        => api.get(BASE).then(r => r.data),
  getById: (id)     => api.get(`${BASE}/${id}`).then(r => r.data),
  autoMatch: (id)   => api.post(`${BASE}/${id}/auto-match`).then(r => r.data),
  manualMatch: (dto) => api.post(`${BASE}/match`, dto).then(r => r.data),
  unmatch: (lineId)  => api.delete(`${BASE}/match/${lineId}`).then(r => r.data),
  import: (file, bankAccountId, statementDate, openingBalance, closingBalance) => {
    const form = new FormData();
    form.append('file', file);
    form.append('bankAccountId', bankAccountId);
    if (statementDate) form.append('statementDate', statementDate);
    if (openingBalance != null) form.append('openingBalance', openingBalance);
    if (closingBalance != null) form.append('closingBalance', closingBalance);
    return api.post(`${BASE}/import`, form, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data);
  },
};
