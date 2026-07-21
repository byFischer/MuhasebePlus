import api from '@/services/api';

const declarationService = {
  vat: {
    generate:    (body)  => api.post('/api/declarations/vat/generate', body).then(r => r.data),
    list:        ()      => api.get('/api/declarations/vat').then(r => r.data),
    get:         (id)    => api.get(`/api/declarations/vat/${id}`).then(r => r.data),
    finalize:    (id)    => api.post(`/api/declarations/vat/${id}/finalize`).then(r => r.data),
    submit:      (id)    => api.post(`/api/declarations/vat/${id}/submit`).then(r => r.data),
    downloadXml: (id)    => api.get(`/api/declarations/vat/${id}/xml`, { responseType: 'blob' }).then(r => r.data),
    downloadExcel:(id)   => api.get(`/api/declarations/vat/${id}/excel`, { responseType: 'blob' }).then(r => r.data),
    remove:      (id)    => api.delete(`/api/declarations/vat/${id}`).then(r => r.data),
  },

  babs: {
    generate: (year, month, type) =>
      api.post('/api/declarations/babs/generate', null, { params: { year, month, type } }).then(r => r.data),
    list:     ()   => api.get('/api/declarations/babs').then(r => r.data),
    get:      (id) => api.get(`/api/declarations/babs/${id}`).then(r => r.data),
    finalize: (id) => api.post(`/api/declarations/babs/${id}/finalize`).then(r => r.data),
    downloadExcel: (id) => api.get(`/api/declarations/babs/${id}/excel`, { responseType: 'blob' }).then(r => r.data),
    remove:   (id) => api.delete(`/api/declarations/babs/${id}`).then(r => r.data),
  },

  withholding: {
    generate: (year, month) =>
      api.post('/api/declarations/withholding/generate', null, { params: { year, month } }).then(r => r.data),
    list:     ()   => api.get('/api/declarations/withholding').then(r => r.data),
    get:      (id) => api.get(`/api/declarations/withholding/${id}`).then(r => r.data),
    finalize: (id) => api.post(`/api/declarations/withholding/${id}/finalize`).then(r => r.data),
    downloadExcel: (id) => api.get(`/api/declarations/withholding/${id}/excel`, { responseType: 'blob' }).then(r => r.data),
    remove:   (id) => api.delete(`/api/declarations/withholding/${id}`).then(r => r.data),
  },

  generic: {
    generate: (type, year, period) =>
      api.post('/api/declarations/generic/generate', null, { params: { type, year, period } }).then(r => r.data),
    list:     (type) => api.get('/api/declarations/generic', { params: { type } }).then(r => r.data),
    get:      (id)   => api.get(`/api/declarations/generic/${id}`).then(r => r.data),
    finalize: (id)   => api.post(`/api/declarations/generic/${id}/finalize`).then(r => r.data),
    submit:   (id)   => api.post(`/api/declarations/generic/${id}/submit`).then(r => r.data),
    downloadExcel: (id) => api.get(`/api/declarations/generic/${id}/excel`, { responseType: 'blob' }).then(r => r.data),
    remove:   (id)   => api.delete(`/api/declarations/generic/${id}`).then(r => r.data),
  },
};

export default declarationService;
