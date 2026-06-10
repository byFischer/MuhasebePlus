import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import companyService from '@/services/companyService';
import { toast } from '@/lib/toast';

export function useCurrentCompany() {
  return useQuery({
    queryKey: ['company', 'current'],
    queryFn: companyService.getCurrent,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUpdateCurrentCompany() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => companyService.updateCurrent(dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['company', 'current'] });
      toast.ok('Şirket bilgileri güncellendi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncellenemedi'),
  });
}
