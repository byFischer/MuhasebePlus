import { useMutation, useQueryClient } from '@tanstack/react-query';
import userService from '@/services/userService';
import { toast } from '@/lib/toast';

export function useUpdateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (dto) => userService.updateProfile(dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user'] });
      toast.ok('Profil güncellendi');
    },
    onError: (e) => toast.err(e?.message || 'Profil güncellenemedi'),
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (dto) => userService.changePassword(dto),
    onSuccess: () => toast.ok('Şifre değiştirildi'),
    onError: (e) => toast.err(e?.message || 'Şifre değiştirilemedi'),
  });
}

export function useChangeEmail() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (newEmail) => userService.changeEmail(newEmail),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user'] });
      toast.ok('E-posta değiştirildi');
    },
    onError: (e) => toast.err(e?.message || 'E-posta değiştirilemedi'),
  });
}
