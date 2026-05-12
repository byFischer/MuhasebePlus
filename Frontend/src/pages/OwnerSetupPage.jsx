import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api from '@/services/api';

const schema = z.object({
  firstName: z.string().min(1, 'Ad zorunlu'),
  lastName: z.string().min(1, 'Soyad zorunlu'),
  email: z.string().email('Geçersiz e-posta'),
  password: z.string().min(8, 'En az 8 karakter'),
  phoneNumber: z.string().min(1, 'Telefon zorunlu'),
  birthDate: z.string().min(1, 'Doğum tarihi zorunlu'),
  companyName: z.string().min(1, 'Şirket adı zorunlu'),
  companyTaxNumber: z.string().min(1, 'Vergi numarası zorunlu'),
});

export default function OwnerSetupPage({ onComplete }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data) => {
    setLoading(true);
    setError('');
    try {
      await api.post('/api/system/initialize', data);
      onComplete();
    } catch (err) {
      setError(err.response?.data?.message || 'Kurulum başarısız. Tekrar deneyin.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--shell-1, #0b0e16)' }}>
      <div style={{ background: 'var(--surface, #1a2035)', border: '1px solid var(--line, rgba(232,241,255,0.15))', borderRadius: 16, padding: '2.5rem', width: '100%', maxWidth: 480 }}>
        <div style={{ marginBottom: '2rem', textAlign: 'center' }}>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--ink-0, #fff)', margin: 0 }}>Muhasebe+ Kurulum</h1>
          <p style={{ color: 'var(--muted, #a9b2c0)', marginTop: 8, fontSize: '0.9rem' }}>
            Yönetici hesabı ve şirket bilgilerini girin.
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <Field label="Ad" error={errors.firstName?.message}>
              <input {...register('firstName')} placeholder="Ad" className="mp-input" />
            </Field>
            <Field label="Soyad" error={errors.lastName?.message}>
              <input {...register('lastName')} placeholder="Soyad" className="mp-input" />
            </Field>
          </div>

          <Field label="E-posta" error={errors.email?.message}>
            <input {...register('email')} type="email" placeholder="ornek@sirket.com" className="mp-input" />
          </Field>

          <Field label="Parola" error={errors.password?.message}>
            <input {...register('password')} type="password" placeholder="En az 8 karakter" className="mp-input" />
          </Field>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <Field label="Telefon" error={errors.phoneNumber?.message}>
              <input {...register('phoneNumber')} placeholder="05XX XXX XX XX" className="mp-input" />
            </Field>
            <Field label="Doğum Tarihi" error={errors.birthDate?.message}>
              <input {...register('birthDate')} type="date" className="mp-input" />
            </Field>
          </div>

          <hr style={{ border: 'none', borderTop: '1px solid var(--line, rgba(232,241,255,0.1))', margin: '0.5rem 0' }} />

          <Field label="Şirket Adı" error={errors.companyName?.message}>
            <input {...register('companyName')} placeholder="Şirket Adı A.Ş." className="mp-input" />
          </Field>

          <Field label="Vergi Numarası" error={errors.companyTaxNumber?.message}>
            <input {...register('companyTaxNumber')} placeholder="1234567890" className="mp-input" />
          </Field>

          {error && (
            <div style={{ color: '#ff4b5f', fontSize: '0.85rem', textAlign: 'center' }}>{error}</div>
          )}

          <button type="submit" className="btn primary" disabled={loading} style={{ marginTop: '0.5rem', padding: '0.75rem', fontSize: '1rem' }}>
            {loading ? 'Kuruluyor...' : 'Kurulumu Tamamla'}
          </button>
        </form>
      </div>
    </div>
  );
}

function Field({ label, error, children }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <label style={{ fontSize: '0.8rem', color: 'var(--muted, #a9b2c0)', fontWeight: 500 }}>{label}</label>
      {children}
      {error && <span style={{ fontSize: '0.75rem', color: '#ff4b5f' }}>{error}</span>}
    </div>
  );
}
