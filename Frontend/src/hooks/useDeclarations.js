import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import declarationService from '@/services/declarationService';
import { toast } from '@/lib/toast';

// ─── VAT (KDV1) ───────────────────────────────────────────────────────────────

export function useVatDeclarations() {
  return useQuery({ queryKey: ['declarations', 'vat'], queryFn: declarationService.vat.list });
}

export function useVatDeclaration(id) {
  return useQuery({
    queryKey: ['declarations', 'vat', id],
    queryFn: () => declarationService.vat.get(id),
    enabled: !!id,
  });
}

export function useGenerateVatDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body) => declarationService.vat.generate(body),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'vat'] }); toast.ok('KDV1 beyannamesi üretildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Beyanname üretilemedi'),
  });
}

export function useFinalizeVatDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.vat.finalize(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'vat'] }); toast.ok('Beyanname onaylandı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Onaylanamadı'),
  });
}

export function useSubmitVatDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.vat.submit(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'vat'] }); toast.ok('Beyanname gönderilmiş olarak işaretlendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşaretlenemedi'),
  });
}

export function useDeleteVatDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.vat.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'vat'] }); toast.ok('Beyanname silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}

// ─── BA-BS ────────────────────────────────────────────────────────────────────

export function useBaBsDeclarations() {
  return useQuery({ queryKey: ['declarations', 'babs'], queryFn: declarationService.babs.list });
}

export function useGenerateBaBsDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ year, month, type }) => declarationService.babs.generate(year, month, type),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'babs'] }); toast.ok('Ba-Bs formu üretildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Form üretilemedi'),
  });
}

export function useFinalizeBaBsDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.babs.finalize(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'babs'] }); toast.ok('Form onaylandı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Onaylanamadı'),
  });
}

export function useDeleteBaBsDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.babs.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'babs'] }); toast.ok('Form silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}

// ─── MUHTASAR ─────────────────────────────────────────────────────────────────

export function useWithholdingDeclarations() {
  return useQuery({ queryKey: ['declarations', 'withholding'], queryFn: declarationService.withholding.list });
}

export function useGenerateWithholdingDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ year, month }) => declarationService.withholding.generate(year, month),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'withholding'] }); toast.ok('Muhtasar beyannamesi üretildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Beyanname üretilemedi'),
  });
}

export function useFinalizeWithholdingDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.withholding.finalize(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'withholding'] }); toast.ok('Beyanname onaylandı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Onaylanamadı'),
  });
}

export function useDeleteWithholdingDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.withholding.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'withholding'] }); toast.ok('Beyanname silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}

// ─── GENERIC (KDV2, Geçici Vergi, Kurumlar Vergisi) ──────────────────────────

export function useGenericDeclarations(type) {
  return useQuery({
    queryKey: ['declarations', 'generic', type],
    queryFn: () => declarationService.generic.list(type),
    enabled: !!type,
  });
}

export function useGenerateGenericDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ type, year, period }) => declarationService.generic.generate(type, year, period),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['declarations', 'generic', vars.type] });
      toast.ok('Beyanname üretildi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Beyanname üretilemedi'),
  });
}

export function useFinalizeGenericDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.generic.finalize(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'generic'] }); toast.ok('Beyanname onaylandı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Onaylanamadı'),
  });
}

export function useSubmitGenericDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.generic.submit(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'generic'] }); toast.ok('Beyanname gönderilmiş olarak işaretlendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşaretlenemedi'),
  });
}

export function useDeleteGenericDeclaration() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => declarationService.generic.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['declarations', 'generic'] }); toast.ok('Beyanname silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}
