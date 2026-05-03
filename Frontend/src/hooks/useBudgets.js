import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { budgetService } from '@/services/budgetService';
import { toast } from '@/lib/toast';

export function useBudgets(year) {
  return useQuery({
    queryKey: ['budgets', year],
    queryFn: () => budgetService.getAll(year),
  });
}

export function useCreateBudget() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: budgetService.create,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['budgets'] }); toast.ok('Bütçe eklendi'); },
    onError: () => toast.err('Bütçe eklenemedi'),
  });
}

export function useUpdateBudget() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, dto }) => budgetService.update(id, dto),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['budgets'] }); toast.ok('Bütçe güncellendi'); },
    onError: () => toast.err('Güncelleme başarısız'),
  });
}

export function useDeleteBudget() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: budgetService.remove,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['budgets'] }); toast.ok('Bütçe silindi'); },
    onError: () => toast.err('Silme başarısız'),
  });
}
