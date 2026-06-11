import api from '@/services/api';
const BASE = '/api/share-links';
const shareLinkService = {
  getCurrent: () => api.get(`${BASE}/current`).then(r => r.data),
  regenerate: () => api.post(`${BASE}/regenerate`).then(r => r.data),
  revoke:     () => api.delete(`${BASE}/current`).then(r => r.data),
};
export default shareLinkService;
