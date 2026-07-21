import { useQuery } from '@tanstack/react-query';
import publicShareService from '@/services/publicShareService';

export function usePublicShareInfo(token) {
  return useQuery({
    queryKey: ['public-share', token],
    queryFn: () => publicShareService.getInfo(token),
    enabled: !!token,
    retry: false,
  });
}
export function usePublicShareInvoices(token, params) {
  return useQuery({
    queryKey: ['public-share', token, 'invoices', params],
    queryFn: () => publicShareService.listInvoices(token, params),
    enabled: !!token,
    retry: false,
    // Müşavir sayfayı açık tutar; "canlı" davranış için periyodik ve odakta yenileme
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  });
}
export function usePublicShareSummary(token, params) {
  return useQuery({
    queryKey: ['public-share', token, 'summary', params],
    queryFn: () => publicShareService.getSummary(token, params),
    enabled: !!token,
    retry: false,
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  });
}
