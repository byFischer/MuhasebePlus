import React from 'react';
import { TRY } from '@/lib/format';

function InvoicePill({ status }) {
  const map = {
    'onaylı':    { cls: 'pos',  l: 'Onaylı' },
    'beklemede': { cls: 'info', l: 'Beklemede' },
    'taslak':    { cls: 'warn', l: 'Taslak' },
    'gecikmiş':  { cls: 'neg',  l: 'Gecikmiş' },
    paid:    { cls: 'pos',  l: 'Ödendi' },
    pending: { cls: 'info', l: 'Beklemede' },
    draft:   { cls: 'warn', l: 'Taslak' },
    overdue: { cls: 'neg',  l: 'Gecikmiş' },
  };
  const s = map[status] || { cls: '', l: status };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

export default function RecentInvoicesWidget({ D, mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');

  if (v === 's') {
    const top = D.invoices.slice(0, 3);
    return (
      <div className="col gap-4" style={{ padding: 12 }}>
        {top.length === 0 && <div className="empty">Fatura yok</div>}
        {top.map(i => (
          <div key={i.id} className="row" style={{ justifyContent: 'space-between', fontSize: 12, padding: '4px 0' }}>
            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{i.customer}</span>
            <span className="mono tnum" style={{ fontWeight: 600 }}>{TRY(i.total)}</span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Fatura</th>
            <th>Müşteri</th>
            <th>Tarih</th>
            <th>Durum</th>
            <th className="num">Tutar</th>
          </tr>
        </thead>
        <tbody>
          {D.invoices.map(i => (
            <tr key={i.id}>
              <td className="mono">{i.id}</td>
              <td>{i.customer}</td>
              <td className="muted">{i.date}</td>
              <td><InvoicePill status={i.status} /></td>
              <td className="num mono tnum">{TRY(i.total)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
