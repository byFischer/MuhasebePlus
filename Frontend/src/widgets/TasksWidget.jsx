import React from 'react';
import Icon from '@/components/mp/Icon';

export const getHeaderExtra = () => <span className="pill info">Scheduler</span>;

export default function TasksWidget({ mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');

  if (v === 's') {
    return (
      <div className="empty" style={{ paddingTop: 16, textAlign: 'center' }}>
        <Icon name="clock" size={20} style={{ opacity: 0.25 }} />
        <div style={{ color: 'var(--ink-3)', fontSize: 11, marginTop: 4 }}>Yakında</div>
      </div>
    );
  }

  return (
    <div className="empty" style={{ paddingTop: v === 'l' ? 48 : 32, textAlign: 'center' }}>
      <Icon name="clock" size={v === 'l' ? 36 : 24} style={{ opacity: 0.25, marginBottom: 8 }} />
      <div style={{ color: 'var(--ink-3)', fontSize: 13 }}>Görev modülü yakında aktif olacak</div>
      {v === 'l' && (
        <div style={{ color: 'var(--ink-3)', fontSize: 11, marginTop: 8 }}>
          Planlanmış işlemleri ve hatırlatmaları bu alanda göreceksin.
        </div>
      )}
    </div>
  );
}
