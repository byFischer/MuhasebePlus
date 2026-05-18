import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import accountingPeriodService from '@/services/accountingPeriodService';
import { toast } from '@/lib/toast';

export function useAccountingPeriods(year) {
  return useQuery({ queryKey: ['accounting-periods', year], queryFn: () => accountingPeriodService.list(year) });
}
export function usePeriodStatus() {
  return useQuery({ queryKey: ['accounting-periods', 'status'], queryFn: () => accountingPeriodService.status(), staleTime: 60_000 });
}
export function useClosePeriod() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ year, month }) => accountingPeriodService.close(year, month),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['accounting-periods'] }); toast.ok('Dönem kapatıldı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Dönem kapatılamadı'),
  });
}
export function useReopenPeriod() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ year, month, reason }) => accountingPeriodService.reopen(year, month, reason),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['accounting-periods'] }); toast.ok('Dönem yeniden açıldı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Dönem açılamadı'),
  });
}
