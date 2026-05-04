import React, { useMemo } from 'react';

const DAYS = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'];

export default function HeatmapWidget({ D }) {
  const txns = D._transactions || [];

  const cells = useMemo(() => {
    const now = new Date();
    const weeks = 12;
    const days = weeks * 7;
    const start = new Date(now);
    start.setDate(start.getDate() - days + 1);
    start.setHours(0, 0, 0, 0);

    const counts = {};
    txns.forEach(t => {
      const d = new Date(t.transactionDate || t.createdAt);
      if (isNaN(d.getTime())) return;
      d.setHours(0, 0, 0, 0);
      if (d >= start && d <= now) {
        const k = d.toISOString().split('T')[0];
        counts[k] = (counts[k] || 0) + Math.abs(Number(t.amount || 1));
      }
    });

    const max = Math.max(1, ...Object.values(counts));
    const rows = Array.from({ length: 7 }, () => []);

    for (let i = 0; i < days; i++) {
      const d = new Date(start);
      d.setDate(d.getDate() + i);
      const k = d.toISOString().split('T')[0];
      const dayIndex = (d.getDay() + 6) % 7; // Pazartesi=0
      const intensity = (counts[k] || 0) / max;
      rows[dayIndex].push({ date: k, intensity });
    }

    return rows;
  }, [txns]);

  return (
    <div className="heatmap">
      <div className="heatmap-grid">
        {cells.map((row, ri) => (
          <div key={ri} className="heatmap-row">
            <span className="heatmap-label">{DAYS[ri]}</span>
            {row.map((cell, ci) => (
              <div
                key={ci}
                className="heatmap-cell"
                title={cell.date}
                style={{ opacity: 0.1 + cell.intensity * 0.9 }}
              />
            ))}
          </div>
        ))}
      </div>
      <div className="heatmap-legend">
        <span className="muted" style={{ fontSize: 10 }}>Düşük</span>
        <div className="heatmap-legend-bar" />
        <span className="muted" style={{ fontSize: 10 }}>Yüksek</span>
      </div>
    </div>
  );
}
