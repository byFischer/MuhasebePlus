import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import templateService from '@/services/templateService';
import { toast } from '@/lib/toast';

export function useTemplates(params) {
  return useQuery({
    queryKey: ['templates', params],
    queryFn: () => templateService.list(params),
  });
}

export function useTemplate(id) {
  return useQuery({
    queryKey: ['templates', id],
    queryFn: () => templateService.get(id),
    enabled: !!id,
  });
}

export function useCreateTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => templateService.create(dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['templates'] });
      toast.ok('Şablon oluşturuldu');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Oluşturma başarısız'),
  });
}

export function useUpdateTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, dto }) => templateService.update(id, dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['templates'] });
      toast.ok('Şablon güncellendi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}

export function useDeleteTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => templateService.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['templates'] });
      toast.ok('Şablon silindi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}

export function useApplyTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => templateService.apply(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['templates'] });
      qc.invalidateQueries({ queryKey: ['bankAccounts'] });
      qc.invalidateQueries({ queryKey: ['transactions'] });
      toast.ok(data?.message || 'Şablon uygulandı');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Uygulama başarısız'),
  });
}
