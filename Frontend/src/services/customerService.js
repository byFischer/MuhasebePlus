import api from '@/services/api';
const BASE = '/api/customers';
const customerService = {
  list:    (params) => api.get(BASE, { params }).then(r => r.data.content || r.data),
  get:     (id)     => api.get(`${BASE}/${id}`).then(r => r.data),
  create:  (dto)    => api.post(BASE, dto).then(r => r.data),
  update:  (id, dto)=> api.put(`${BASE}/${id}`, dto).then(r => r.data),
  remove:  (id)     => api.delete(`${BASE}/${id}`).then(r => r.data),
  restore: (id)     => api.put(`${BASE}/${id}/restore`).then(r => r.data),
  importFile: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`${BASE}/import`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => r.data);
  },
};
export default customerService;
