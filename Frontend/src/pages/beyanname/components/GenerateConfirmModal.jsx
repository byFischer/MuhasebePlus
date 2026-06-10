import React from 'react';

const MONTHS_TR = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];

function periodLabel(periodType, year, period) {
  if (periodType === 'MONTH') return `${year} ${MONTHS_TR[period - 1]}`;
  if (periodType === 'QUARTER') return `${year} Q${period}`;
  return `${year}`;
}

export default function GenerateConfirmModal({ open, onClose, onConfirm, isPending, typeName, periodType, year, period }) {
  if (!open) return null;

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      <div style={{
        background: 'var(--surface)',
        borderRadius: 10,
        padding: 24,
        width: 420,
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
      }}>
        <div style={{ fontWeight: 700, fontSize: 16, marginBottom: 12 }}>Beyanname Üret</div>
        <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, marginBottom: 20 }}>
          <strong>{periodLabel(periodType, year, period)}</strong> dönemi için{' '}
          <strong>{typeName}</strong> beyannamesi üretilecek.{' '}
          Aynı dönem için mevcut <em>DRAFT</em> beyanname varsa üzerine yazılır.
          <br /><br />
          Devam etmek istiyor musunuz?
        </p>
        <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
          <button className="btn ghost" onClick={onClose} disabled={isPending}>Vazgeç</button>
          <button className="btn primary" onClick={onConfirm} disabled={isPending}>
            {isPending ? 'Üretiliyor...' : 'Üret'}
          </button>
        </div>
      </div>
    </div>
  );
}
