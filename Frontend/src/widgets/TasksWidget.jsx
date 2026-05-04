import React from 'react';
import Icon from '@/components/mp/Icon';

export const getHeaderExtra = () => <span className="pill info">Scheduler</span>;

export default function TasksWidget() {
  return (
    <div className="empty" style={{ paddingTop: 32, textAlign: 'center' }}>
      <Icon name="clock" size={24} style={{ opacity: 0.25, marginBottom: 8 }} />
      <div style={{ color: 'var(--ink-3)', fontSize: 13 }}>Görev modülü yakında aktif olacak</div>
    </div>
  );
}
