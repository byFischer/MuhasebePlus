import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import bankAccountService from '@/services/bankAccountService';
import { toast } from '@/lib/toast';

export function useBankAccounts() {
  return useQuery({ queryKey: ['bankAccounts'], queryFn: () => bankAccountService.list() });
}
export function useBankList() {
  return useQuery({ queryKey: ['bankList'], queryFn: () => bankAccountService.banks(), staleTime: Infinity });
}
export function useCreateBankAccount() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (dto) => bankAccountService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bankAccounts'] }); toast.ok('Hesap kaydedildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Hesap kaydedilemedi'),
  });
}
export function useUpdateBankAccount() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ id, dto }) => bankAccountService.update(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bankAccounts'] }); toast.ok('Hesap güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}
export function useDeleteBankAccount() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => bankAccountService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bankAccounts'] }); toast.ok('Hesap silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
