import api from '@/services/api';
const BASE = '/api/fixed-assets';

const fixedAssetService = {
  // Categories
  listCategories:   ()        => api.get(`${BASE}/categories`).then(r => r.data),
  createCategory:   (data)    => api.post(`${BASE}/categories`, data).then(r => r.data),
  updateCategory:   (id, data) => api.put(`${BASE}/categories/${id}`, data).then(r => r.data),
  deleteCategory:   (id)      => api.delete(`${BASE}/categories/${id}`),

  // Assets
  listAssets:   ()        => api.get(BASE).then(r => r.data),
  getAsset:     (id)      => api.get(`${BASE}/${id}`).then(r => r.data),
  createAsset:  (data)    => api.post(BASE, data).then(r => r.data),
  updateAsset:  (id, data) => api.put(`${BASE}/${id}`, data).then(r => r.data),
  deleteAsset:  (id)      => api.delete(`${BASE}/${id}`),
  getSchedule:  (id)      => api.get(`${BASE}/${id}/schedule`).then(r => r.data),
  disposeAsset: (id, data) => api.post(`${BASE}/${id}/dispose`, data).then(r => r.data),

  // Depreciation
  runDepreciation: (year, month) => api.post(`${BASE}/depreciation/run`, null, { params: { year, month } }).then(r => r.data),
};

export default fixedAssetService;
