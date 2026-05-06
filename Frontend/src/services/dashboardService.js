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
  },

  getWidgetData: async (layoutId, widgetId) => {
    const response = await api.get(`/api/dashboard/layouts/${layoutId}/widgets/${widgetId}/data`);
    return response.data;
  },

  getDataSources: async () => {
    const response = await api.get('/api/data-sources');
    return response.data;
  },

  getDataSourceFields: async (source) => {
    const response = await api.get(`/api/data-sources/${source}/fields`);
    return response.data;
  },

  createWidgetDefinition: async (dto) => {
    const response = await api.post('/api/widget-definitions', dto);
    return response.data;
  },

  getWidgetDefinitions: async () => {
    const response = await api.get('/api/widget-definitions');
    return response.data;
  },

  getWidgetDefinition: async (id) => {
    const response = await api.get(`/api/widget-definitions/${id}`);
    return response.data;
  },

  updateWidgetDefinition: async (id, dto) => {
    const response = await api.put(`/api/widget-definitions/${id}`, dto);
    return response.data;
  },

  deleteWidgetDefinition: async (id) => {
    await api.delete(`/api/widget-definitions/${id}`);
  },

  cloneWidgetDefinition: async (id) => {
    const response = await api.post(`/api/widget-definitions/${id}/clone`);
    return response.data;
  },

  previewQuery: async (queryConfig) => {
    const response = await api.post('/api/widget-definitions/preview', { queryConfig });
    return response.data;
  }
};

export default dashboardService;
