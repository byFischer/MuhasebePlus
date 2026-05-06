import React from 'react';
import { TRY } from '@/lib/format';

export default function AgingWidget({ D, mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');

  if (D.aging.every(x => x === 0)) {
    return <div className="empty">Açık alacak yok 🎉</div>;
  }

  const colors = ['var(--accent)', 'var(--warn)', 'oklch(0.60 0.13 50)', 'var(--neg)'];
  const rows = D.aging.map((val, i) => ({
    label: D.agingLabels[i] || `${i * 30 + 1}–${(i + 1) * 30} gün`,
    val,
    color: colors[i % colors.length],
  }));

  if (v === 's') {
    const top = [...rows].sort((a, b) => b.val - a.val)[0];
    const pct = (top.val / D.agingTotal * 100).toFixed(0);
    return (
      <div className="col gap-8" style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>En yüksek bucket</div>
        <div className="mono tnum" style={{ fontSize: 18, fontWeight: 600, color: top.color }}>{TRY(top.val)}</div>
        <div style={{ fontSize: 12, color: 'var(--ink-2)' }}>{top.label} · %{pct}</div>
        <div className="bar" style={{ marginTop: 4 }}>
          <span style={{ width: `${pct}%`, background: top.color }} />
        </div>
      </div>
    );
  }

  if (v === 'l') {
    return (
      <div className="col gap-12">
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
        <table className="table">
          <thead>
            <tr><th>Bucket</th><th className="num">Tutar</th><th className="num">% Pay</th></tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.label}>
                <td>{r.label}</td>
                <td className="num mono tnum"><b>{TRY(r.val)}</b></td>
                <td className="num mono tnum">%{(r.val / D.agingTotal * 100).toFixed(1)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

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
