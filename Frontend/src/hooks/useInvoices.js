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
export function useCancelInvoice() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ id, reason }) => invoiceService.cancel(id, reason),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoices'] }); toast.ok('Fatura iptal edildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İptal başarısız'),
  });
}
export function useCreateReturnInvoice() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ id, dto }) => invoiceService.createReturn(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoices'] }); toast.ok('İade faturası oluşturuldu'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İade başarısız'),
  });
}
export function useInvoiceSeries() {
  return useQuery({ queryKey: ['invoice-series'], queryFn: () => invoiceService.getSeries() });
}
export function useCreateInvoiceSeries() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (dto) => invoiceService.createSeries(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['invoice-series'] }); toast.ok('Seri oluşturuldu'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Seri oluşturulamadı'),
  });
}
export function useNextInvoiceNumber() {
  return useMutation({ mutationFn: ({ invoiceType, seriesCode }) => invoiceService.nextNumber(invoiceType, seriesCode) });
}
export function useLateFee(invoiceId) {
  return useQuery({ queryKey: ['late-fee', invoiceId], queryFn: () => invoiceService.getLateFee(invoiceId), enabled: !!invoiceId });
}
export function useLateFeeConfig() {
  return useQuery({ queryKey: ['late-fee-config'], queryFn: () => invoiceService.getLateFeeConfig() });
}
export function useUpdateLateFeeConfig() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ monthlyRate, gracePeriodDays }) => invoiceService.updateLateFeeConfig(monthlyRate, gracePeriodDays),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['late-fee-config'] }); toast.ok('Faiz ayarı güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}
