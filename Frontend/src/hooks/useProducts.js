import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import productService from '@/services/productService';
import { toast } from '@/lib/toast';

export function useProducts() {
  return useQuery({ queryKey: ['products'], queryFn: () => productService.list() });
}
export function useCreateProduct() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (dto) => productService.create(dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); toast.ok('Ürün kaydedildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Ürün kaydedilemedi'),
  });
}
export function useUpdateProduct() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ id, dto }) => productService.update(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); toast.ok('Ürün güncellendi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Güncelleme başarısız'),
  });
}
export function useDeleteProduct() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id) => productService.remove(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['products'] }); toast.ok('Ürün silindi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Silme başarısız'),
  });
}
