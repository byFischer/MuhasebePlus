import api from '@/services/api';

const taxCodeService = {
  getWithholdingCodes: () => api.get('/api/tax-codes/withholding').then(r => r.data),
  getExemptionCodes:   () => api.get('/api/tax-codes/exemption').then(r => r.data),
};

export default taxCodeService;
