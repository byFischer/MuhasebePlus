import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useUpdateProfile, useChangePassword, useChangeEmail } from '@/hooks/useProfile';
import Icon from '@/components/mp/Icon';

export default function AccountTab() {
  const { user } = useAuth();
  const updateProfile = useUpdateProfile();
  const changePassword = useChangePassword();
  const changeEmail = useChangeEmail();

  const [profile, setProfile] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    phoneNumber: user?.phoneNumber || '',
    birthDate: user?.birthDate || '',
  });

  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [newEmail, setNewEmail] = useState('');
  const [emailSaving, setEmailSaving] = useState(false);

  const handleProfileSubmit = (e) => {
    e.preventDefault();
    updateProfile.mutate(profile);
  };

  const handlePasswordSubmit = (e) => {
    e.preventDefault();
    if (pw.newPassword !== pw.confirmPassword) return;
    changePassword.mutate({ currentPassword: pw.currentPassword, newPassword: pw.newPassword });
    setPw({ currentPassword: '', newPassword: '', confirmPassword: '' });
  };

  const handleEmailSubmit = (e) => {
    e.preventDefault();
    if (!newEmail) return;
    setEmailSaving(true);
    changeEmail.mutate(newEmail, {
      onSettled: () => setEmailSaving(false),
      onSuccess: () => setNewEmail(''),
    });
  };

  const pwMismatch = pw.newPassword && pw.confirmPassword && pw.newPassword !== pw.confirmPassword;

  return (
    <div className="settings-form-area">
      <div className="settings-section">
        <div className="settings-section-title"><Icon name="settings" size={14} /> Profil Bilgileri</div>
        <form onSubmit={handleProfileSubmit}>
          <div className="settings-fields">
            <div className="field">
              <label>Ad</label>
              <input className="input settings-input" value={profile.firstName} onChange={e => setProfile(p => ({ ...p, firstName: e.target.value }))} required />
            </div>
            <div className="field">
              <label>Soyad</label>
              <input className="input settings-input" value={profile.lastName} onChange={e => setProfile(p => ({ ...p, lastName: e.target.value }))} required />
            </div>
            <div className="field">
              <label>Telefon</label>
              <input className="input settings-input" value={profile.phoneNumber} onChange={e => setProfile(p => ({ ...p, phoneNumber: e.target.value }))} placeholder="05XX XXX XX XX" />
            </div>
            <div className="field">
              <label>Doğum Tarihi</label>
              <input className="input settings-input" type="date" value={profile.birthDate} onChange={e => setProfile(p => ({ ...p, birthDate: e.target.value }))} />
            </div>
            <div className="field">
              <label>E-posta</label>
              <input className="input settings-input" value={user?.email || ''} disabled />
            </div>
          </div>
          <button type="submit" className="btn primary settings-btn" disabled={updateProfile.isPending}>
            {updateProfile.isPending ? 'Kaydediliyor...' : 'Kaydet'}
          </button>
        </form>
      </div>

      <div className="divider" />

      <div className="settings-section">
        <div className="settings-section-title"><Icon name="lock" size={14} /> Şifre Değiştir</div>
        <form onSubmit={handlePasswordSubmit}>
          <div className="settings-fields">
            <div className="field">
              <label>Mevcut Şifre</label>
              <input className="input settings-input" type="password" value={pw.currentPassword} onChange={e => setPw(p => ({ ...p, currentPassword: e.target.value }))} required />
            </div>
            <div className="field">
              <label>Yeni Şifre</label>
              <input className="input settings-input" type="password" value={pw.newPassword} onChange={e => setPw(p => ({ ...p, newPassword: e.target.value }))} required minLength={6} />
            </div>
            <div className="field">
              <label>Yeni Şifre (Tekrar)</label>
              <input className="input settings-input" type="password" value={pw.confirmPassword} onChange={e => setPw(p => ({ ...p, confirmPassword: e.target.value }))} required />
            </div>
          </div>
          {pwMismatch && <div className="error-msg"><Icon name="alert" size={12} /> Şifreler eşleşmiyor</div>}
          <button type="submit" className="btn primary settings-btn" disabled={changePassword.isPending || pwMismatch}>
            {changePassword.isPending ? 'Değiştiriliyor...' : 'Şifreyi Güncelle'}
          </button>
        </form>
      </div>

      <div className="divider" />

      <div className="settings-section">
        <div className="settings-section-title"><Icon name="globe" size={14} /> E-posta Değiştir</div>
        <form onSubmit={handleEmailSubmit}>
          <div className="settings-fields">
            <div className="field">
              <label>Mevcut E-posta</label>
              <input className="input settings-input" value={user?.email || ''} disabled />
            </div>
            <div className="field">
              <label>Yeni E-posta</label>
              <input className="input settings-input" type="email" value={newEmail} onChange={e => setNewEmail(e.target.value)} required placeholder="yeni@email.com" />
            </div>
          </div>
          <button type="submit" className="btn primary settings-btn" disabled={emailSaving || !newEmail}>
            {emailSaving ? 'Değiştiriliyor...' : 'E-posta Güncelle'}
          </button>
        </form>
      </div>
    </div>
  );
}
