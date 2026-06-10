import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import eDefterService from '@/services/eDefterService';
import { toast } from '@/lib/toast';

export function useEDefterList() {
  return useQuery({ queryKey: ['edefter'], queryFn: eDefterService.list });
}

export function useEDefterByYear(year) {
  return useQuery({
    queryKey: ['edefter', 'year', year],
    queryFn: () => eDefterService.listByYear(year),
    enabled: !!year,
  });
}

export function useGenerateEDefter() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ year, month }) => eDefterService.generate(year, month),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['edefter', 'year', vars.year] });
      qc.invalidateQueries({ queryKey: ['edefter'] });
      toast.ok('e-Defter üretildi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'e-Defter üretilemedi'),
  });
}

export function useRegenerateEDefter() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => eDefterService.regenerate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['edefter'] });
      toast.ok('e-Defter yeniden üretildi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Yeniden üretilemedi'),
  });
}

export function useDeleteEDefter() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => eDefterService.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['edefter'] });
      toast.ok('e-Defter silindi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}
