import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import chartOfAccountService from '@/services/chartOfAccountService';
import { toast } from '@/lib/toast';

export function useChartOfAccounts() {
  return useQuery({ queryKey: ['chart-of-accounts'], queryFn: () => chartOfAccountService.list() });
}
export function useChartOfAccount(id) {
  return useQuery({ queryKey: ['chart-of-accounts', id], queryFn: () => chartOfAccountService.get(id), enabled: !!id });
}
export function useCreateAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => chartOfAccountService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chart-of-accounts'] }); toast.ok('Hesap oluşturuldu'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Hesap oluşturulamadı'),
  });
}
export function useUpdateAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, dto }) => chartOfAccountService.update(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chart-of-accounts'] }); toast.ok('Hesap güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}
export function useDeleteAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => chartOfAccountService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chart-of-accounts'] }); toast.ok('Hesap silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
export function useSeedTdhp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => chartOfAccountService.seedTdhp(),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chart-of-accounts'] }); toast.ok('TDHP başarıyla yüklendi — 80+ hesap eklendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'TDHP yüklenemedi'),
  });
}
