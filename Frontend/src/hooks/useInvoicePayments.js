import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import invoicePaymentService from '@/services/invoicePaymentService';
import { toast } from '@/lib/toast';

export function useInvoicePayments(invoiceId) {
  return useQuery({
    queryKey: ['invoice-payments', invoiceId],
    queryFn: () => invoicePaymentService.list(invoiceId),
    enabled: !!invoiceId,
  });
}

export function useCreateInvoicePayment(invoiceId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => invoicePaymentService.create(invoiceId, dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoice-payments', invoiceId] });
      qc.invalidateQueries({ queryKey: ['invoices'] });
      toast.ok('Ödeme kaydedildi');
    },
    onError: (e) => toast.err(e?.message || e?.response?.data?.message || 'Ödeme kaydedilemedi'),
  });
}

export function useDeleteInvoicePayment(invoiceId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (paymentId) => invoicePaymentService.remove(invoiceId, paymentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoice-payments', invoiceId] });
      qc.invalidateQueries({ queryKey: ['invoices'] });
      toast.ok('Ödeme silindi');
    },
    onError: (e) => toast.err(e?.message || e?.response?.data?.message || 'Silme başarısız'),
  });
}
