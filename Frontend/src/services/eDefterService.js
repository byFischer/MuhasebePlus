import api from '@/services/api';

const eDefterService = {
  list:            ()         => api.get('/api/edefter').then(r => r.data),
  listByYear:      (year)     => api.get(`/api/edefter/year/${year}`).then(r => r.data),
  get:             (id)       => api.get(`/api/edefter/${id}`).then(r => r.data),
  generate:        (year, mo) => api.post('/api/edefter/generate', null, { params: { year, month: mo } }).then(r => r.data),
  regenerate:      (id)       => api.post(`/api/edefter/${id}/regenerate`).then(r => r.data),
  downloadJournal: (id)       => api.get(`/api/edefter/${id}/journal`, { responseType: 'blob' }).then(r => r.data),
  downloadLedger:  (id)       => api.get(`/api/edefter/${id}/ledger`,  { responseType: 'blob' }).then(r => r.data),
  remove:          (id)       => api.delete(`/api/edefter/${id}`).then(r => r.data),
};

export default eDefterService;
