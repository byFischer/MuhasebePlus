import { useQuery } from '@tanstack/react-query';
import currencyService from '@/services/currencyService';

export function useCurrencyRates() {
  return useQuery({
    queryKey: ['currency', 'sidebar-rates'],
    queryFn: currencyService.getRates,
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
    staleTime: 30_000,
    retry: 1,
  });
}
