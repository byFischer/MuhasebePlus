import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import adminService from '@/services/adminService';
import { toast } from '@/lib/toast';

const errMsg = (err, fallback) => err?.response?.data?.message || fallback;

// Genel bakış

export function useAdminStats() {
  return useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: () => adminService.stats(),
  });
}

// Kullanıcılar

export function useAdminUsers(params) {
  return useQuery({
    queryKey: ['admin', 'users', params],
    queryFn: () => adminService.listUsers(params),
    placeholderData: (prev) => prev,
  });
}

function useUserMutation(mutationFn, okMsg, errFallback) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin'] });
      toast.ok(okMsg);
    },
    onError: (err) => toast.err(errMsg(err, errFallback)),
  });
}

export function useUpdateUserRole() {
  return useUserMutation(
    ({ id, role }) => adminService.updateUserRole(id, role),
    'Kullanıcı rolü güncellendi', 'Rol güncellenemedi');
}

export function useLockUser() {
  return useUserMutation((id) => adminService.lockUser(id), 'Kullanıcı kilitlendi', 'İşlem başarısız');
}

export function useUnlockUser() {
  return useUserMutation((id) => adminService.unlockUser(id), 'Kullanıcı kilidi açıldı', 'İşlem başarısız');
}

export function useActivateUser() {
  return useUserMutation((id) => adminService.activateUser(id), 'Kullanıcı aktifleştirildi', 'İşlem başarısız');
}

export function useDeactivateUser() {
  return useUserMutation((id) => adminService.deactivateUser(id), 'Kullanıcı pasifleştirildi', 'İşlem başarısız');
}

export function useResetUserPassword() {
  return useUserMutation(
    ({ id, newPassword }) => adminService.resetPassword(id, newPassword),
    'Şifre sıfırlandı', 'Şifre sıfırlanamadı');
}

export function useDeleteUser() {
  return useUserMutation((id) => adminService.deleteUser(id), 'Kullanıcı silindi', 'Kullanıcı silinemedi');
}

// Şirketler

export function useAdminCompanies(params) {
  return useQuery({
    queryKey: ['admin', 'companies', params],
    queryFn: () => adminService.listCompanies(params),
    placeholderData: (prev) => prev,
  });
}

export function useAdminCompanyDetail(companyId) {
  return useQuery({
    queryKey: ['admin', 'company', companyId],
    queryFn: () => adminService.getCompany(companyId),
    enabled: companyId != null,
  });
}

export function useSetCompanyActive() {
  return useUserMutation(
    ({ id, active }) => active ? adminService.activateCompany(id) : adminService.deactivateCompany(id),
    'Şirket durumu güncellendi', 'İşlem başarısız');
}

// Loglar

export function useAdminLogs(params) {
  return useQuery({
    queryKey: ['admin', 'logs', params],
    queryFn: () => adminService.listLogs(params),
    placeholderData: (prev) => prev,
  });
}

export function useExportAdminLogs() {
  return useMutation({
    mutationFn: (params) => adminService.exportLogs(params),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `admin_logs_${Date.now()}.csv`; a.click();
      URL.revokeObjectURL(url);
    },
    onError: (err) => toast.err(errMsg(err, 'Dışa aktarma başarısız')),
  });
}
