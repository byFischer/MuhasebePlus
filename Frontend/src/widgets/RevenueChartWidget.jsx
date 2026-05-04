import React from 'react';
import BarLineChart from '@/components/mp/charts/BarLineChart';
import { TRY } from '@/lib/format';
import { Kpi } from './KpisWidget';

export default function RevenueChartWidget({ D, mode, config }) {
  const months = config?.months || 12;
  const sliceFrom = Math.max(0, D.months.length - months);
  const m = D.months.slice(sliceFrom);
  const rev = D.revenue.slice(sliceFrom);
  const exp = D.expense.slice(sliceFrom);

  if (mode === 'detail') {
    const totalRev = rev.reduce((s, x) => s + x, 0);
    const totalExp = exp.reduce((s, x) => s + x, 0);
    return (
      <div className="col gap-12">
        <BarLineChart months={m} revenue={rev} expense={exp} />
        <div className="grid-3">
          <Kpi label={`${months} Ay Gelir`} value={totalRev} pos data={rev} />
          <Kpi label={`${months} Ay Gider`} value={totalExp} data={exp} color="var(--neg)" />
          <Kpi label={`${months} Ay Net`} value={totalRev - totalExp} pos data={rev.map((v, i) => v - exp[i])} />
        </div>
        <table className="table">
          <thead>
            <tr><th>Ay</th><th className="num">Gelir</th><th className="num">Gider</th><th className="num">Net</th></tr>
          </thead>
          <tbody>
            {m.map((mo, i) => (
              <tr key={mo}>
                <td>{mo}</td>
                <td className="num mono tnum" style={{ color: 'var(--pos)' }}>{TRY(rev[i])}</td>
                <td className="num mono tnum" style={{ color: 'var(--neg)' }}>{TRY(exp[i])}</td>
                <td className="num mono tnum"><b>{TRY(rev[i] - exp[i])}</b></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <>
      <div className="row gap-8" style={{ marginBottom: 8 }}>
        <span className="pill info"><span className="dot" />Gelir</span>
        <span className="pill"><span className="dot" />Gider</span>
      </div>
      <BarLineChart months={m} revenue={rev} expense={exp} />
    </>
  );
}
