import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { stockService, stockMovementService } from '@/services/stockService';
import { toast } from '@/lib/toast';

export function useStocks() {
  return useQuery({ queryKey: ['stocks'], queryFn: () => stockService.list() });
}

export function useLowStock() {
  return useQuery({ queryKey: ['stocks', 'low'], queryFn: () => stockService.lowStock() });
}

export function useStockMovements(productId) {
  return useQuery({
    queryKey: ['stock-movements', productId],
    queryFn: () => stockMovementService.list(productId),
    enabled: productId != null,
  });
}

export function useCreateStockMovement() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => stockMovementService.create(dto),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['stock-movements', vars.productId] });
      qc.invalidateQueries({ queryKey: ['stocks'] });
      qc.invalidateQueries({ queryKey: ['products'] });
      toast.ok('Stok hareketi kaydedildi');
    },
    onError: (e) => toast.err(e?.response?.data?.message || 'Hareket kaydedilemedi'),
  });
}
