import { useQuery, useMutation } from '@tanstack/react-query';
import systemLogService from '@/services/systemLogService';
import { toast } from '@/lib/toast';

export function useSystemLogs(params) {
  return useQuery({
    queryKey: ['logs', params],
    queryFn: () => systemLogService.list(params),
    placeholderData: (prev) => prev,
  });
}
export function useExportSystemLogs() {
  return useMutation({
    mutationFn: (params) => systemLogService.export(params),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `system_logs_${Date.now()}.xlsx`; a.click();
      URL.revokeObjectURL(url);
    },
    onError: () => toast.err('Dışa aktarma başarısız'),
  });
}
