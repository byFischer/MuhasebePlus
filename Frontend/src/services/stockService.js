import api from '@/services/api';

const STOCK_BASE = '/api/stocks';
const MOVEMENT_BASE = '/api/stock-movements';

export const stockService = {
  list:        ()          => api.get(STOCK_BASE).then(r => r.data),
  getByProduct:(pid)       => api.get(`${STOCK_BASE}/product/${pid}`).then(r => r.data),
  lowStock:    ()          => api.get(`${STOCK_BASE}/low`).then(r => r.data),
  create:      (dto)       => api.post(STOCK_BASE, dto).then(r => r.data),
  update:      (pid, dto)  => api.put(`${STOCK_BASE}/${pid}`, dto).then(r => r.data),
  remove:      (pid)       => api.delete(`${STOCK_BASE}/${pid}`).then(r => r.data),
  restore:     (pid)       => api.put(`${STOCK_BASE}/${pid}/restore`).then(r => r.data),
};

export const stockMovementService = {
  create:      (dto)       => api.post(MOVEMENT_BASE, dto).then(r => r.data),
  list:        (productId) => api.get(MOVEMENT_BASE, { params: productId != null ? { productId } : {} }).then(r => r.data),
};
