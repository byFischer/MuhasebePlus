import React from 'react';
import { TRY } from '@/lib/format';

function InvoicePill({ status }) {
  const map = {
    'onaylı':    { cls: 'pos',  l: 'Onaylı' },
    'beklemede': { cls: 'info', l: 'Beklemede' },
    'taslak':    { cls: 'warn', l: 'Taslak' },
    'gecikmiş':  { cls: 'neg',  l: 'Gecikmiş' },
  };
  const s = map[status] || { cls: '', l: status };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

export default function RecentInvoicesWidget({ D }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr><th>Fatura</th><th>Müşteri</th><th>Tarih</th><th>Durum</th><th className="num">Tutar</th></tr>
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
