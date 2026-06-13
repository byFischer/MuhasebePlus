import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import { useApplyTemplate } from '@/hooks/useTemplates';

export default function DailyBriefWidget({ D, onNav, mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');
  const applyMut = useApplyTemplate();
  const now = new Date();
  const todayStr = now.toISOString().split('T')[0];
  const yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1);

  const dueToday = D._invoices?.filter(i => {
    if (!i.dueDate) return false;
    const d = new Date(i.dueDate);
    return d.toISOString().split('T')[0] === todayStr && i.paymentStatus !== 'paid';
  }) || [];

  const overdue = D._invoices?.filter(i => i.paymentStatus === 'overdue' && !i.isDeleted) || [];

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

  if (v === 's') {
    const top = items[0];
    return (
      <div className="dbrief">
        <button className={`dbrief-row ${top.tone}`} onClick={() => top.route && onNav?.(top.route)}>
          <Icon name={top.icon} size={14} />
          <span>{top.label}</span>
          {top.route && <Icon name="chevRight" size={12} style={{ marginLeft: 'auto', opacity: 0.5 }} />}
        </button>
        {items.length > 1 && (
          <div className="muted" style={{ fontSize: 11, textAlign: 'center', marginTop: 6 }}>
            +{items.length - 1} ek uyarı
          </div>
        )}
      </div>
    );
  }

  const TYPE_META = {
    INCOME: { icon: 'swap', label: 'Gelir', cls: 'pos' },
    EXPENSE: { icon: 'swap', label: 'Gider', cls: 'neg' },
    INVOICE: { icon: 'invoice', label: 'Fatura', cls: 'info' },
    STOCK_ADJUSTMENT: { icon: 'box', label: 'Stok', cls: 'warn' },
    CUSTOMER_TRANSACTION: { icon: 'users', label: 'Cari', cls: 'accent' },
    BANK_TRANSFER: { icon: 'bank', label: 'Banka', cls: 'info' },
  };

  const templateSuggestions = (D._templates || [])
    .filter(tp => tp.period === 'aylık' || tp.period === 'haftalık')
    .slice(0, v === 's' ? 1 : 3);

  const handleApply = (id) => {
    applyMut.mutate(id);
  };

  return (
    <div className="dbrief">
      {items.map((it, i) => (
        <button key={i} className={`dbrief-row ${it.tone}`} onClick={() => it.route && onNav?.(it.route)}>
          <Icon name={it.icon} size={14} />
          <span>{it.label}</span>
          {it.route && <Icon name="chevRight" size={12} style={{ marginLeft: 'auto', opacity: 0.5 }} />}
        </button>
      ))}
      {templateSuggestions.length > 0 && (
        <div style={{ marginTop: 10, borderTop: '1px solid var(--line)', paddingTop: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--ink-3)', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            <Icon name="template" size={11} style={{ marginRight: 4 }} /> Bugün Uygula
          </div>
          {templateSuggestions.map((tp) => {
            const meta = TYPE_META[tp.templateType] || TYPE_META.EXPENSE;
            return (
              <button key={tp.templateId} className="dbrief-row neutral" onClick={() => handleApply(tp.templateId)} disabled={applyMut.isPending}>
                <span className={`pill ${meta.cls}`} style={{ fontSize: 9 }}>{meta.label}</span>
                <span style={{ flex: 1, textAlign: 'left', fontSize: 12 }}>{tp.templateName}</span>
                <Icon name="flash" size={11} style={{ color: 'var(--accent)' }} />
              </button>
            );
          })}
        </div>
      )}
      {v === 'l' && (
        <div className="muted" style={{ fontSize: 11, marginTop: 12, padding: '0 4px' }}>
          Vadesi bugün olan {dueToday.length} fatura · {overdue.length} gecikmiş · dünkü net {TRY(yNet)}.
        </div>
      )}
    </div>
  );
}
