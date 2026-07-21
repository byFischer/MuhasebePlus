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

export function useInvoicePromises(invoiceId) {
  return useQuery({
    queryKey: ['invoice-promises', invoiceId],
    queryFn: () => invoicePaymentService.getPromises(invoiceId),
    enabled: !!invoiceId,
  });
}

export function useCreatePromise(invoiceId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => invoicePaymentService.createPromise(invoiceId, dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoice-promises', invoiceId] });
      toast.ok('Tahsilat sözü kaydedildi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Söz kaydedilemedi'),
  });
}

export function useFulfillPromise(invoiceId) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (promiseId) => invoicePaymentService.fulfillPromise(invoiceId, promiseId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['invoice-promises', invoiceId] });
      qc.invalidateQueries({ queryKey: ['invoices'] });
      toast.ok('Tahsilat sözü gerçekleştirildi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Gerçekleştirme başarısız'),
  });
}
