import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import transactionTemplateService from '@/services/transactionTemplateService';
import { toast } from '@/lib/toast';

export function useTransactionTemplates(params) {
  return useQuery({ queryKey: ['templates', params], queryFn: () => transactionTemplateService.list(params) });
}
export function useTransactionTemplate(id) {
  return useQuery({ queryKey: ['templates', id], queryFn: () => transactionTemplateService.get(id), enabled: !!id });
}
export function useDeleteTransactionTemplate() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => transactionTemplateService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['templates'] }); toast.ok('Şablon silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
