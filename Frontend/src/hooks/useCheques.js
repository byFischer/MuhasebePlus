import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import chequeService from '@/services/chequeService';
import { toast } from '@/lib/toast';

export function useCheques(params) {
  return useQuery({ queryKey: ['cheques', params], queryFn: () => chequeService.list(params) });
}
export function useCheque(id) {
  return useQuery({ queryKey: ['cheques', id], queryFn: () => chequeService.get(id), enabled: !!id });
}
export function usePortfolioSummary() {
  return useQuery({ queryKey: ['cheques', 'portfolio-summary'], queryFn: () => chequeService.portfolioSummary() });
}
export function useUpcomingCheques(days) {
  return useQuery({ queryKey: ['cheques', 'upcoming', days], queryFn: () => chequeService.upcoming(days) });
}
export function useCreateCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => chequeService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek kaydedildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Çek kaydedilemedi'),
  });
}
export function useUpdateCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, dto }) => chequeService.update(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}
export function useDeleteCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => chequeService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
export function useDepositCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, bankAccountId }) => chequeService.deposit(id, bankAccountId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek tahsile verildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşlem başarısız'),
  });
}
export function useCollectCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, bankAccountId }) => chequeService.collect(id, bankAccountId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek tahsil edildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşlem başarısız'),
  });
}
export function useBounceCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }) => chequeService.bounce(id, reason),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek karşılıksız olarak işaretlendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşlem başarısız'),
  });
}
export function useEndorseCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, toCustomerId }) => chequeService.endorse(id, toCustomerId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek ciro edildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşlem başarısız'),
  });
}
export function useCancelCheque() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }) => chequeService.cancel(id, reason),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cheques'] }); toast.ok('Çek iptal edildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İptal başarısız'),
  });
}
