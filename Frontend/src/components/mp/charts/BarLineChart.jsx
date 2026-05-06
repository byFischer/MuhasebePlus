import React from 'react';

export default function BarLineChart({ months = [], revenue = [], expense = [] }) {
  const w = 720, h = 200, pad = 28;

  const safeMonths = Array.isArray(months) ? months : [];
  const safeRevenue = (Array.isArray(revenue) ? revenue : []).map(v =>
    Number.isFinite(Number(v)) ? Number(v) : 0
  );
  const safeExpense = (Array.isArray(expense) ? expense : []).map(v =>
    Number.isFinite(Number(v)) ? Number(v) : 0
  );

  const len = Math.min(safeMonths.length, safeRevenue.length, safeExpense.length);

  if (len === 0) {
    return <div className="empty">Grafik verisi yok</div>;
  }

  const labels = safeMonths.slice(0, len);
  const rev = safeRevenue.slice(0, len);
  const exp = safeExpense.slice(0, len);

  const rawMax = Math.max(0, ...rev, ...exp);
  const maxV = rawMax > 0 ? rawMax * 1.1 : 1;

  const bw = len > 0 ? (w - pad * 2) / len / 2.6 : 0;
  const xFor = (i) =>
    len === 1
      ? w / 2
      : pad + i * ((w - pad * 2) / (len - 1));

  const yFor = (v) => {
    const n = Number(v);
    const safe = Number.isFinite(n) ? n : 0;
    return h - pad - (safe / maxV) * (h - pad * 2);
  };

  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 220 }}>
      {[0.25, 0.5, 0.75, 1].map((t) => (
        <line
          key={t}
          x1={pad}
          x2={w - pad}
          y1={h - pad - (h - pad * 2) * t}
          y2={h - pad - (h - pad * 2) * t}
          stroke="var(--line)"
          strokeDasharray="2 3"
        />
      ))}

      {labels.map((m, i) => {
        const ry = yFor(rev[i]);
        const ey = yFor(exp[i]);
        const rh = Math.max(0, h - pad - ry);
        const eh = Math.max(0, h - pad - ey);

        return (
          <g key={`${m}-${i}`}>
            <rect
              x={xFor(i) - bw - 1}
              y={ry}
              width={bw}
              height={rh}
              fill="var(--accent)"
              opacity={0.85}
              rx={1}
            />
            <rect
              x={xFor(i) + 1}
              y={ey}
              width={bw}
              height={eh}
              fill="var(--ink-3)"
              opacity={0.5}
              rx={1}
            />
            <text
              x={xFor(i)}
              y={h - 8}
              fontSize="10"
              fill="var(--ink-2)"
              textAnchor="middle"
            >
              {m}
            </text>
          </g>
        );
      })}
    </svg>
  );
}