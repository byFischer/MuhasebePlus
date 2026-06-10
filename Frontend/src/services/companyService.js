import api from '@/services/api';

const companyService = {
  getCurrent:    ()    => api.get('/api/companies/me').then(r => r.data),
  updateCurrent: (dto) => api.put('/api/companies/me', dto).then(r => r.data),
};

export default companyService;
