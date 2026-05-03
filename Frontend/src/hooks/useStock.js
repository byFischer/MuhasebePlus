import { useQuery } from '@tanstack/react-query';
import stockService from '@/services/stockService';

export function useStocks() {
  return useQuery({ queryKey: ['stocks'], queryFn: () => stockService.list() });
}
export function useLowStock() {
  return useQuery({ queryKey: ['stocks', 'low'], queryFn: () => stockService.lowStock() });
}
