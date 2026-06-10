import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { bankReconciliationService } from '@/services/bankReconciliationService';
import { toast } from '@/lib/toast';

export function useBankStatements() {
  return useQuery({ queryKey: ['bank-statements'], queryFn: bankReconciliationService.getAll });
}

export function useBankStatement(id) {
  return useQuery({
    queryKey: ['bank-statement', id],
    queryFn: () => bankReconciliationService.getById(id),
    enabled: !!id,
  });
}

export function useImportStatement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ file, bankAccountId, statementDate, openingBalance, closingBalance }) =>
      bankReconciliationService.import(file, bankAccountId, statementDate, openingBalance, closingBalance),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['bank-statements'] });
      qc.setQueryData(['bank-statement', data.id], data);
      toast.ok('Ekstre yüklendi');
    },
    onError: () => toast.err('Yükleme başarısız'),
  });
}

export function useAutoMatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: bankReconciliationService.autoMatch,
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['bank-statements'] });
      qc.setQueryData(['bank-statement', data.id], data);
      toast.ok('Otomatik eşleştirme tamamlandı');
    },
    onError: () => toast.err('Eşleştirme başarısız'),
  });
}

export function useManualMatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: bankReconciliationService.manualMatch,
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['bank-statement', data.id] });
      toast.ok('Eşleştirme kaydedildi');
    },
    onError: () => toast.err('Eşleştirme başarısız'),
  });
}

export function useUnmatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: bankReconciliationService.unmatch,
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['bank-statement', data.id] });
      toast.ok('Eşleştirme kaldırıldı');
    },
    onError: () => toast.err('İşlem başarısız'),
  });
}
