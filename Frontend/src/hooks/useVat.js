import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vatService } from '@/services/vatService';
import { toast } from '@/lib/toast';

export function useVatPeriods() {
  return useQuery({ queryKey: ['vat-periods'], queryFn: vatService.getAll });
}

export function useVatBaBs(id, enabled) {
  return useQuery({
    queryKey: ['vat-ba-bs', id],
    queryFn: () => vatService.getBaBs(id),
    enabled: !!id && enabled,
  });
}

export function useCalculateVat() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: vatService.calculate,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['vat-periods'] }); toast.ok('KDV dönemi hesaplandı'); },
    onError: () => toast.err('Hesaplama başarısız'),
  });
}

export function useLockVat() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: vatService.lock,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['vat-periods'] }); toast.ok('Dönem kilitlendi'); },
    onError: () => toast.err('Kilitleme başarısız'),
  });
}
