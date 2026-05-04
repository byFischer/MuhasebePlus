import api from './api';

export const dashboardService = {
  getLayouts: async () => {
    const response = await api.get('/api/dashboard/layouts');
    return response.data;
  },

  getDefaultLayout: async () => {
    const response = await api.get('/api/dashboard/layouts/default');
    return response.data;
  },

  updateLayout: async (layoutId, dto) => {
    const response = await api.put(`/api/dashboard/layouts/${layoutId}`, dto);
    return response.data;
  },

  addWidget: async (layoutId, dto) => {
    const response = await api.post(`/api/dashboard/layouts/${layoutId}/widgets`, dto);
    return response.data;
  },

  updateWidget: async (layoutId, widgetId, dto) => {
    const response = await api.put(`/api/dashboard/layouts/${layoutId}/widgets/${widgetId}`, dto);
    return response.data;
  },

  deleteWidget: async (layoutId, widgetId) => {
    await api.delete(`/api/dashboard/layouts/${layoutId}/widgets/${widgetId}`);
  },

  reorderWidgets: async (layoutId, orderedIds) => {
    await api.put(`/api/dashboard/layouts/${layoutId}/widgets/reorder`, { orderedWidgetIds: orderedIds });
  }
};

export default dashboardService;
