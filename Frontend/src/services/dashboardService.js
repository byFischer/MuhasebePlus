import api from "./api";

export const dashboardService = {
  getDefaultLayout: () => api.get("/api/dashboard/layouts/default").then((r) => r.data),
  getLayouts: () => api.get("/api/dashboard/layouts").then((r) => r.data),
  getLayout: (id) => api.get(`/api/dashboard/layouts/${id}`).then((r) => r.data),
  createLayout: (dto) => api.post("/api/dashboard/layouts", dto).then((r) => r.data),
  updateLayout: (id, dto) => api.put(`/api/dashboard/layouts/${id}`, dto).then((r) => r.data),
  deleteLayout: (id) => api.delete(`/api/dashboard/layouts/${id}`),
  cloneLayout: (id) => api.post(`/api/dashboard/layouts/${id}/clone`).then((r) => r.data),

  getWidgets: (layoutId) =>
    api.get(`/api/dashboard/layouts/${layoutId}/widgets`).then((r) => r.data),
  addWidget: (layoutId, dto) =>
    api.post(`/api/dashboard/layouts/${layoutId}/widgets`, dto).then((r) => r.data),
  updateWidget: (layoutId, widgetId, dto) =>
    api.put(`/api/dashboard/layouts/${layoutId}/widgets/${widgetId}`, dto).then((r) => r.data),
  deleteWidget: (layoutId, widgetId) =>
    api.delete(`/api/dashboard/layouts/${layoutId}/widgets/${widgetId}`),
  reorderWidgets: (layoutId, positions) =>
    api.put(`/api/dashboard/layouts/${layoutId}/widgets/reorder`, { positions }),

  getWidgetData: (layoutId, widgetId) =>
    api.get(`/api/dashboard/layouts/${layoutId}/widgets/${widgetId}/data`).then((r) => r.data),

  getWidgetCatalog: () => api.get("/api/dashboard/widget-catalog").then((r) => r.data),

  generateAiWidget: (prompt) =>
    api.post("/api/ai/widgets/generate", { prompt }).then((r) => r.data),
};
