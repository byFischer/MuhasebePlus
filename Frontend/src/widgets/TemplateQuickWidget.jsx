import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import { useApplyTemplate } from '@/hooks/useTemplates';

const TYPE_META = {
  INCOME: { icon: 'swap', label: 'Gelir', cls: 'pos' },
  EXPENSE: { icon: 'swap', label: 'Gider', cls: 'neg' },
  INVOICE: { icon: 'invoice', label: 'Fatura', cls: 'info' },
  STOCK_ADJUSTMENT: { icon: 'box', label: 'Stok', cls: 'warn' },
  CUSTOMER_TRANSACTION: { icon: 'users', label: 'Cari', cls: 'accent' },
  BANK_TRANSFER: { icon: 'bank', label: 'Banka', cls: 'info' },
};

function PayloadPreview({ type, payload }) {
  if (!payload) return null;
  if (type === 'INCOME' || type === 'EXPENSE') {
    return <span className="mono tnum">{payload.amount ? TRY(payload.amount) : '—'}</span>;
  }
  if (type === 'INVOICE') {
    const items = payload.lineItems || [];
    return <span>{items.length} kalem</span>;
  }
  if (type === 'STOCK_ADJUSTMENT') {
    return <span>{payload.direction === 'OUT' ? 'Çıkış' : 'Giriş'} · {payload.quantity || '—'} adet</span>;
  }
  if (type === 'CUSTOMER_TRANSACTION') {
    return <span className="mono tnum">{payload.amount ? TRY(payload.amount) : '—'} · {payload.direction === 'PAYMENT' ? 'Ödeme' : 'Tahsilat'}</span>;
  }
  if (type === 'BANK_TRANSFER') {
    return <span className="mono tnum">{payload.amount ? TRY(payload.amount) : '—'}</span>;
  }
  return null;
}

export default function TemplateQuickWidget({ D, onNav, mode, variant, config }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');
  const applyMut = useApplyTemplate();
  const [appliedId, setAppliedId] = useState(null);
  const templates = D?._templates || [];

  if (!templates.length) {
    return (
      <div className="tpl-quick-empty">
        <Icon name="template" size={20} />
        <p>Henüz şablon yok</p>
        <button className="btn sm" onClick={() => onNav?.('/sablon')}>Şablon Oluştur</button>
      </div>
    );
  }

  const handleApply = (id) => {
    setAppliedId(id);
    applyMut.mutate(id, {
      onSettled: () => {
        setTimeout(() => setAppliedId(null), 1500);
      },
    });
  };

  const displayed = v === 's' ? templates.slice(0, 3) : templates;

  if (v === 's') {
    return (
      <div className="tpl-quick-s">
        {displayed.map((tp) => {
          const meta = TYPE_META[tp.templateType] || { icon: 'template', label: tp.templateType, cls: 'accent' };
          const isApplied = appliedId === tp.templateId;
          return (
            <button key={tp.templateId} className={`tpl-quick-row ${isApplied ? 'applied' : ''}`} onClick={() => handleApply(tp.templateId)} disabled={applyMut.isPending}>
              <span className={`pill ${meta.cls}`}>{meta.label}</span>
              <span className="tpl-name">{tp.templateName}</span>
              {isApplied && <Icon name="check" size={12} style={{ color: 'var(--pos)' }} />}
            </button>
          );
        })}
      </div>
    );
  }

  return (
    <div className="tpl-quick">
      {displayed.map((tp) => {
        const meta = TYPE_META[tp.templateType] || { icon: 'template', label: tp.templateType, cls: 'accent' };
        const isApplied = appliedId === tp.templateId;
        return (
          <div key={tp.templateId} className="tpl-quick-item">
            <div className="tpl-quick-item-icon">
              <Icon name={meta.icon} size={14} />
            </div>
            <div className="tpl-quick-item-body">
              <div className="tpl-quick-item-top">
                <span className="tpl-quick-item-name">{tp.templateName}</span>
                <span className={`pill ${meta.cls}`}>{meta.label}</span>
              </div>
              <div className="tpl-quick-item-sub">
                <PayloadPreview type={tp.templateType} payload={tp.payload} />
                {tp.period && <span className="muted" style={{ marginLeft: 6 }}>· {tp.period}</span>}
              </div>
            </div>
            <button
              className={`btn sm ${isApplied ? 'pos' : 'primary'}`}
              onClick={() => handleApply(tp.templateId)}
              disabled={applyMut.isPending}
            >
              {isApplied ? <><Icon name="check" size={12} /> Uygulandı</> : <><Icon name="flash" size={12} /> Uygula</>}
            </button>
          </div>
        );
      })}
    </div>
  );
}