import { useQuery } from '@tanstack/react-query';
import taxCodeService from '@/services/taxCodeService';

const HOUR = 60 * 60 * 1000;

export function useWithholdingCodes() {
  return useQuery({
    queryKey: ['tax-codes', 'withholding'],
    queryFn: taxCodeService.getWithholdingCodes,
    staleTime: HOUR,
  });
}

export function useExemptionCodes() {
  return useQuery({
    queryKey: ['tax-codes', 'exemption'],
    queryFn: taxCodeService.getExemptionCodes,
    staleTime: HOUR,
  });
}
