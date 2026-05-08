import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import invoiceService from '@/services/invoiceService';
import { toast } from '@/lib/toast';

export function useInvoices(params) {
  return useQuery({ queryKey: ['invoices', params], queryFn: () => invoiceService.list(params) });
}
export function useInvoice(id) {
  return useQuery({
    queryKey: ['invoices', id],
    queryFn: () => invoiceService.get(id),
    enabled: id != null,
  });
}
export function useCreateInvoice() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (dto) => invoiceService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoices'] }); toast.ok('Fatura oluşturuldu'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Fatura oluşturulamadı'),
  });
}
export function useDeleteInvoice() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => invoiceService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoices'] }); toast.ok('Fatura silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
export function useConfirmInvoice() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => invoiceService.confirm(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoices'] }); toast.ok('Fatura onaylandı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Onay başarısız'),
  });
}
