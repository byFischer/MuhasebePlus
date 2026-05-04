import React from 'react';
import Spark from '@/components/mp/charts/Spark';
import { TRY } from '@/lib/format';

function linearRegression(y) {
  const n = y.length;
  if (n < 3) return null;
  let sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
  for (let i = 0; i < n; i++) {
    sumX += i;
    sumY += y[i];
    sumXY += i * y[i];
    sumXX += i * i;
  }
  const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
  const intercept = (sumY - slope * sumX) / n;
  return { slope, intercept };
}

export default function ForecastWidget({ D, mode }) {
  const net = D.revenue.map((v, i) => v - D.expense[i]);
  const reg = linearRegression(net);

  if (!reg) {
    return <div className="empty">Tahmin için yeterli veri yok (en az 3 ay gerekli)</div>;
  }

  const future = [];
  for (let i = 1; i <= 3; i++) {
    future.push(Math.round(reg.intercept + reg.slope * (net.length - 1 + i)));
  }

  const all = [...net, ...future];
  const labels = [...D.months, ...future.map((_, i) => `T+${i + 1}`)];

  if (mode === 'detail') {
    return (
      <div className="col gap-12">
        <div className="row gap-8" style={{ marginBottom: 8 }}>
          <span className="pill info"><span className="dot" />Geçmiş</span>
          <span className="pill warn"><span className="dot" />Tahmin</span>
        </div>
        <Spark data={all} color="var(--accent)" />
        <table className="table">
          <thead>
            <tr><th>Ay</th><th className="num">Net Kar (₺)</th></tr>
          </thead>
          <tbody>
            {labels.map((m, i) => (
              <tr key={m} style={{ opacity: i >= net.length ? 0.75 : 1 }}>
                <td>{m}</td>
                <td className={`num mono tnum ${all[i] >= 0 ? '' : 'neg'}`}>{TRY(all[i])}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div className="col gap-8">
      <div className="row gap-8" style={{ marginBottom: 4 }}>
        <span className="pill info"><span className="dot" />Geçmiş</span>
        <span className="pill warn"><span className="dot" />Tahmin</span>
      </div>
      <Spark data={all} color="var(--accent)" />
      <div className="row" style={{ justifyContent: 'space-between', fontSize: 12, marginTop: 4 }}>
        <span className="muted">Eğim</span>
        <span className={`mono tnum ${reg.slope >= 0 ? 'pos' : 'neg'}`}>
          {reg.slope >= 0 ? '+' : ''}{TRY(Math.round(reg.slope))}/ay
        </span>
      </div>
    </div>
  );
}
