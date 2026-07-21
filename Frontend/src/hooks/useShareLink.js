import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import shareLinkService from '@/services/shareLinkService';
import { toast } from '@/lib/toast';

export function useShareLink() {
  return useQuery({ queryKey: ['share-link'], queryFn: () => shareLinkService.getCurrent() });
}
export function useRegenerateShareLink() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: () => shareLinkService.regenerate(),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['share-link'] }); toast.ok('Paylaşım linki oluşturuldu'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Link oluşturulamadı'),
  });
}
export function useRevokeShareLink() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: () => shareLinkService.revoke(),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['share-link'] }); toast.ok('Paylaşım linki iptal edildi'); },
    onError: (e) => toast.err(e?.response?.data?.message || 'Link iptal edilemedi'),
  });
}
