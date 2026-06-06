import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import fixedAssetService from '@/services/fixedAssetService';
import { toast } from '@/lib/toast';

export function useCategories() {
  return useQuery({ queryKey: ['asset-categories'], queryFn: fixedAssetService.listCategories });
}
export function useCreateCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data) => fixedAssetService.createCategory(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['asset-categories'] }); toast.ok('Kategori oluşturuldu'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Kategori oluşturulamadı'),
  });
}
export function useUpdateCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => fixedAssetService.updateCategory(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['asset-categories'] }); toast.ok('Kategori güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Kategori güncellenemedi'),
  });
}
export function useDeleteCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => fixedAssetService.deleteCategory(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['asset-categories'] }); toast.ok('Kategori silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}

export function useFixedAssets() {
  return useQuery({ queryKey: ['fixed-assets'], queryFn: fixedAssetService.listAssets });
}
export function useFixedAsset(id) {
  return useQuery({ queryKey: ['fixed-assets', id], queryFn: () => fixedAssetService.getAsset(id), enabled: !!id });
}
export function useDepreciationSchedule(id) {
  return useQuery({ queryKey: ['depreciation-schedule', id], queryFn: () => fixedAssetService.getSchedule(id), enabled: !!id });
}
export function useCreateAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data) => fixedAssetService.createAsset(data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['fixed-assets'] }); toast.ok('Sabit kıymet eklendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Eklenemedi'),
  });
}
export function useUpdateAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => fixedAssetService.updateAsset(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['fixed-assets'] }); toast.ok('Güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncellenemedi'),
  });
}
export function useDeleteAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id) => fixedAssetService.deleteAsset(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['fixed-assets'] }); toast.ok('Silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silinemedi'),
  });
}
export function useDisposeAsset() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => fixedAssetService.disposeAsset(id, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['fixed-assets'] }); toast.ok('Sabit kıymet elden çıkarıldı'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'İşlem başarısız'),
  });
}
export function useRunDepreciation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ year, month }) => fixedAssetService.runDepreciation(year, month),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['fixed-assets'] });
      toast.ok(`Amortisman tamamlandı: ${data.processed} kıymet işlendi`);
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Amortisman çalıştırılamadı'),
  });
}
