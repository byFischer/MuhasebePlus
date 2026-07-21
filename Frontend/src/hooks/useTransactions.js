import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import transactionService from '@/services/transactionService';
import { toast } from '@/lib/toast';

export function useTransactions(params) {
  return useQuery({ queryKey: ['transactions', params], queryFn: () => transactionService.list(params) });
}
export function useCreateTransaction() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (dto) => transactionService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['transactions'] }); toast.ok('İşlem kaydedildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşlem kaydedilemedi'),
  });
}
export function useDeleteTransaction() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => transactionService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['transactions'] }); toast.ok('İşlem silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
