import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import reportService from '@/services/reportService';
import { toast } from '@/lib/toast';

export function useReports() {
  return useQuery({ queryKey: ['reports'], queryFn: () => reportService.list() });
}

export function useReportPreview(dto, enabled = true) {
  return useQuery({
    queryKey: ['report-preview', dto],
    queryFn: () => reportService.preview(dto),
    enabled: enabled && !!dto?.reportType && !!dto?.startDate && !!dto?.endDate,
    keepPreviousData: true,
  });
}

export function useGenerateReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => reportService.generate(dto),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      const id = res?.reportId != null ? `R-${String(res.reportId).padStart(4, '0')}` : '';
      toast.ok(id ? `Rapor hazır — ${id}` : 'Rapor oluşturuldu');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Rapor oluşturulamadı'),
  });
}

export function useDownloadReport() {
  return useMutation({
    mutationFn: (id) => reportService.download(id),
    onSuccess: (blob, id) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `rapor_${id}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    },
    onError: () => toast.err('İndirme başarısız'),
  });
}

export function useDeleteReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => reportService.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['reports'] });
      toast.ok('Rapor silindi');
    },
    onError: () => toast.err('Rapor silinemedi'),
  });
}

export function useStatement(customerId, startDate, endDate, enabled = true) {
  return useQuery({
    queryKey: ['statement', customerId, startDate, endDate],
    queryFn: () => reportService.getStatement(customerId, startDate, endDate),
    enabled: enabled && !!customerId && !!startDate && !!endDate,
  });
}

export function useReconciliation(customerId, periodStart, periodEnd, enabled = true) {
  return useQuery({
    queryKey: ['reconciliation', customerId, periodStart, periodEnd],
    queryFn: () => reportService.getReconciliation(customerId, periodStart, periodEnd),
    enabled: enabled && !!customerId && !!periodStart && !!periodEnd,
  });
}
