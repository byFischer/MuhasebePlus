import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import journalEntryService from '@/services/journalEntryService';
import { toast } from '@/lib/toast';

export function useJournalEntries(params) {
  return useQuery({ queryKey: ['journal-entries', params], queryFn: () => journalEntryService.list(params) });
}
export function useJournalEntry(id) {
  return useQuery({ queryKey: ['journal-entries', id], queryFn: () => journalEntryService.get(id), enabled: !!id });
}
export function useTrialBalance(startDate, endDate) {
  return useQuery({
    queryKey: ['trial-balance', startDate, endDate],
    queryFn: () => journalEntryService.trialBalance(startDate, endDate),
    enabled: !!startDate && !!endDate,
  });
}
export function useGeneralLedger(accountId, startDate, endDate) {
  return useQuery({
    queryKey: ['general-ledger', accountId, startDate, endDate],
    queryFn: () => journalEntryService.generalLedger(accountId, startDate, endDate),
    enabled: !!accountId && !!startDate && !!endDate,
  });
}
export function useCreateJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => journalEntryService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['journal-entries'] }); toast.ok('Mahsup fişi kaydedildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Fiş kaydedilemedi'),
  });
}
export function useReverseJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }) => journalEntryService.reverse(id, reason),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['journal-entries'] }); toast.ok('Fiş ters çevrildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Ters çevirme başarısız'),
  });
}
export function useDeleteJournalEntry() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => journalEntryService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['journal-entries'] }); toast.ok('Fiş silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
