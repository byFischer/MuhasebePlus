import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { dashboardService } from "../services/dashboardService";
import { toaster } from "../components/ui/toaster";

export function useDashboardLayout() {
  return useQuery({
    queryKey: ["dashboard", "default"],
    queryFn: dashboardService.getDefaultLayout,
  });
}

export function useDashboardLayouts() {
  return useQuery({
    queryKey: ["dashboard", "layouts"],
    queryFn: dashboardService.getLayouts,
  });
}

export function useSaveLayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ layoutId, widgets }) =>
      dashboardService.reorderWidgets(layoutId, widgets),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["dashboard"] }),
  });
}

export function useAddWidget() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ layoutId, dto }) => dashboardService.addWidget(layoutId, dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      toaster.create({ title: "Widget eklendi", type: "success", duration: 2000 });
    },
    onError: (err) => {
      const msg = err?.response?.data?.message ?? err?.response?.data?.error ?? err?.message ?? "Widget eklenemedi";
      toaster.create({ title: "Hata", description: msg, type: "error", duration: 4000 });
    },
  });
}

export function useDeleteWidget() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ layoutId, widgetId }) => dashboardService.deleteWidget(layoutId, widgetId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["dashboard"] });
      toaster.create({ title: "Widget kaldırıldı", type: "success", duration: 2000 });
    },
    onError: (err) => {
      const msg = err?.response?.data?.message ?? err?.message ?? "Widget kaldırılamadı";
      toaster.create({ title: "Hata", description: msg, type: "error", duration: 4000 });
    },
  });
}

export function useCloneLayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (layoutId) => dashboardService.cloneLayout(layoutId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["dashboard"] }),
  });
}

export function useCreateLayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => dashboardService.createLayout(dto),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["dashboard"] }),
  });
}
