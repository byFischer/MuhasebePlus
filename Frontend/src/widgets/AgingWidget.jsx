import React from 'react';
import { TRY } from '@/lib/format';

export default function AgingWidget({ D }) {
  if (D.aging.every(v => v === 0)) {
    return <div className="empty">Açık alacak yok 🎉</div>;
  }

  const colors = ['var(--accent)', 'var(--warn)', 'oklch(0.60 0.13 50)', 'var(--neg)'];
  const rows = D.aging.map((val, i) => ({
    label: D.agingLabels[i] || `${i * 30 + 1}–${(i + 1) * 30} gün`,
    val,
    color: colors[i % colors.length],
  }));

  return (
    <div>
      {rows.map(r => (
        <div key={r.label} style={{ margin: '8px 0' }}>
          <div className="row" style={{ justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
            <span>{r.label}</span>
            <span className="mono tnum">{TRY(r.val)}</span>
          </div>
          <div className="bar">
            <span style={{ width: `${r.val / D.agingTotal * 100}%`, background: r.color }} />
          </div>
        </div>
      ))}
    </div>
  );
}
