import React from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';

export default function DailyBriefWidget({ D, onNav }) {
  const now = new Date();
  const todayStr = now.toISOString().split('T')[0];
  const yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1);

  // Vadesi bugün olan faturalar
  const dueToday = D._invoices?.filter(i => {
    if (!i.dueDate) return false;
    const d = new Date(i.dueDate);
    return d.toISOString().split('T')[0] === todayStr && i.paymentStatus !== 'PAID';
  }) || [];

  // Gecikmiş faturalar
  const overdue = D._invoices?.filter(i => i.paymentStatus === 'OVERDUE' && !i.isDeleted) || [];

  // Dün net kar (transactions'tan hesapla)
  const yDayStr = yesterday.toISOString().split('T')[0];
  const yTxns = D._transactions?.filter(t => {
    const td = new Date(t.transactionDate || t.createdAt);
    return td.toISOString().split('T')[0] === yDayStr;
  }) || [];
  const yIncome = yTxns.filter(t => t.transactionType === 'INCOME').reduce((s, t) => s + Number(t.amount || 0), 0);
  const yExpense = yTxns.filter(t => t.transactionType === 'EXPENSE').reduce((s, t) => s + Math.abs(Number(t.amount || 0)), 0);
  const yNet = yIncome - yExpense;

  const items = [];
  if (dueToday.length) items.push({ icon: 'invoice', label: `${dueToday.length} fatura vadesi bugün`, route: '/fatura', tone: 'warn' });
  if (overdue.length) items.push({ icon: 'alert', label: `${overdue.length} fatura gecikmiş`, route: '/fatura', tone: 'neg' });
  items.push({ icon: 'chart', label: `Dün net ${yNet >= 0 ? '+' : ''}${TRY(yNet)}`, route: '/rapor', tone: yNet >= 0 ? 'pos' : 'neg' });
  if (!items.length) items.push({ icon: 'check', label: 'Bugün için kritik bir durum yok', tone: 'pos' });

  return (
    <div className="dbrief">
      {items.map((it, i) => (
        <button key={i} className={`dbrief-row ${it.tone}`} onClick={() => it.route && onNav?.(it.route)}>
          <Icon name={it.icon} size={14} />
          <span>{it.label}</span>
          {it.route && <Icon name="chevRight" size={12} style={{ marginLeft: 'auto', opacity: 0.5 }} />}
        </button>
      ))}
    </div>
  );
}
