import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useVatDeclarations } from '@/hooks/useDeclarations';
import { useBaBsDeclarations } from '@/hooks/useDeclarations';
import { useWithholdingDeclarations } from '@/hooks/useDeclarations';

const MONTHS_TR = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];

export default function BeyannameReminderWidget() {
  const nav = useNavigate();
  const now = new Date();
  // Beyan dönemi: geçen ay
  const prevMonth = now.getMonth() === 0 ? 12 : now.getMonth();
  const prevYear  = now.getMonth() === 0 ? now.getFullYear() - 1 : now.getFullYear();
  const prevLabel = `${prevYear} ${MONTHS_TR[prevMonth - 1]}`;

  const { data: vatList = [] }  = useVatDeclarations();
  const { data: babsList = [] } = useBaBsDeclarations();
  const { data: whList = [] }   = useWithholdingDeclarations();

  const checks = [
    {
      type: 'KDV1',
      tab: 'kdv1',
      label: `KDV1 Beyannamesi (${prevLabel})`,
      exists: vatList.some(d => d.year === prevYear && d.month === prevMonth),
    },
    {
      type: 'Muhtasar',
      tab: 'muhtasar',
      label: `Muhtasar Beyannamesi (${prevLabel})`,
      exists: whList.some(d => d.year === prevYear && d.month === prevMonth),
    },
    {
      type: 'Ba',
      tab: 'babs',
      label: `Ba Formu (${prevLabel})`,
      exists: babsList.some(d => d.babsType === 'BA' && d.year === prevYear && d.month === prevMonth),
    },
    {
      type: 'Bs',
      tab: 'babs',
      label: `Bs Formu (${prevLabel})`,
      exists: babsList.some(d => d.babsType === 'BS' && d.year === prevYear && d.month === prevMonth),
    },
  ];

  const missing = checks.filter(c => !c.exists);

  return (
    <div style={{ background: 'var(--surface)', borderRadius: 8, border: '1px solid var(--line)', padding: 16, minWidth: 240 }}>
      <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 12 }}>Bu Ay Hazırlanacak Beyannameler</div>

      {missing.length === 0 ? (
        <div style={{ color: 'var(--pos, green)', fontSize: 13 }}>
          ✓ Tüm beyannameler hazırlandı.
        </div>
      ) : (
        <div className="col gap-6">
          {missing.map(c => (
            <div
              key={c.type}
              onClick={() => nav(`/beyanname?tab=${c.tab}`)}
              style={{
                display: 'flex', alignItems: 'center', gap: 8,
                padding: '6px 10px',
                background: 'var(--warn-soft, #fffbe6)',
                border: '1px solid var(--warn, #d97706)',
                borderRadius: 5,
                cursor: 'pointer',
                fontSize: 13,
              }}
            >
              <span style={{ color: 'var(--warn, #d97706)' }}>⚠</span>
              <span>{c.label}</span>
              <span style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--text-3)' }}>→</span>
            </div>
          ))}
        </div>
      )}

      <div
        onClick={() => nav('/beyanname')}
        style={{ marginTop: 12, fontSize: 12, color: 'var(--accent)', cursor: 'pointer', textAlign: 'right' }}
      >
        Tüm beyannameleri görüntüle →
      </div>
    </div>
  );
}
