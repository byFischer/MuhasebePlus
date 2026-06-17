import api from '@/services/api';
const BASE = '/api/admin';

const adminService = {
  // Genel bakış
  stats: () => api.get(`${BASE}/stats`).then(r => r.data),

  // Kullanıcı yönetimi
  listUsers:      (params)            => api.get(`${BASE}/users`, { params: { size: 20, ...params } }).then(r => r.data),
  getUser:        (id)                => api.get(`${BASE}/users/${id}`).then(r => r.data),
  updateUserRole: (id, role)          => api.put(`${BASE}/users/${id}/role`, { role }).then(r => r.data),
  lockUser:       (id)                => api.put(`${BASE}/users/${id}/lock`),
  unlockUser:     (id)                => api.put(`${BASE}/users/${id}/unlock`),
  activateUser:   (id)                => api.put(`${BASE}/users/${id}/activate`),
  deactivateUser: (id)                => api.put(`${BASE}/users/${id}/deactivate`),
  resetPassword:  (id, newPassword)   => api.put(`${BASE}/users/${id}/password`, { newPassword }),
  deleteUser:     (id)                => api.delete(`${BASE}/users/${id}`),

  // Şirket (tenant) yönetimi
  listCompanies:     (params) => api.get(`${BASE}/companies`, { params: { size: 20, ...params } }).then(r => r.data),
  getCompany:        (id)     => api.get(`${BASE}/companies/${id}`).then(r => r.data),
  activateCompany:   (id)     => api.put(`${BASE}/companies/${id}/activate`),
  deactivateCompany: (id)     => api.put(`${BASE}/companies/${id}/deactivate`),
};

export default adminService;
