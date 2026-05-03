import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import customerService from '@/services/customerService';
import { toast } from '@/lib/toast';

export function useCustomers(params) {
  return useQuery({ queryKey: ['customers', params], queryFn: () => customerService.list(params) });
}
export function useCustomer(id) {
  return useQuery({ queryKey: ['customers', id], queryFn: () => customerService.get(id), enabled: !!id });
}
export function useCreateCustomer() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (dto) => customerService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['customers'] }); toast.ok('Müşteri kaydedildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Müşteri kaydedilemedi'),
  });
}
export function useUpdateCustomer() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ id, dto }) => customerService.update(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['customers'] }); toast.ok('Müşteri güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}
export function useDeleteCustomer() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => customerService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['customers'] }); toast.ok('Müşteri silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
