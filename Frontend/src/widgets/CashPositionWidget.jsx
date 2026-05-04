import React from 'react';
import Donut from '@/components/mp/charts/Donut';
import { TRY } from '@/lib/format';

const currencySymbol = (c) => c === 'TRY' ? '₺' : c === 'USD' ? '$' : '€';

export default function CashPositionWidget({ D, mode }) {
  const segments = D.banks.map((b, i) => ({
    color: `oklch(${0.55 + i * 0.1} 0.06 ${240 - i * 20})`,
    value: b.balance || 0,
    label: b.name,
  }));

  if (mode === 'detail') {
    const detailSegments = D.banks.map((b, i) => ({
      color: `oklch(${0.50 + i * 0.05} 0.10 ${165 + i * 20})`,
      value: b.balance || 0,
      label: b.name,
    }));
    return (
      <div className="col gap-16">
        <div className="row" style={{ justifyContent: 'center' }}>
          <Donut size={280} segments={detailSegments} />
        </div>
        <table className="table">
          <thead>
            <tr><th>Hesap</th><th>IBAN</th><th>Para</th><th className="num">Bakiye</th><th>Son Hareket</th></tr>
          </thead>
          <tbody>
            {D.banks.map(b => (
              <tr key={b.id}>
                <td><b>{b.name}</b></td>
                <td className="mono muted" style={{ fontSize: 11 }}>{b.iban}</td>
                <td>{b.currency}</td>
                <td className="num mono tnum"><b>{TRY(b.balance || 0, { currency: currencySymbol(b.currency) })}</b></td>
                <td className="muted">{b.last}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div className="col gap-8">
      <div className="row" style={{ justifyContent: 'center', marginBottom: 4 }}>
        {D.banks.length > 0 && <Donut size={200} segments={segments} />}
      </div>
      {D.banks.map(b => (
        <div key={b.id} className="row" style={{ justifyContent: 'space-between', padding: '3px 0', fontSize: 12 }}>
          <span style={{ color: 'var(--ink-2)' }}>
            {b.name} <span className="muted">· {b.currency}</span>
          </span>
          <span className="mono tnum">{TRY(b.balance || 0, { currency: currencySymbol(b.currency) })}</span>
        </div>
      ))}
    </div>
  );
}
