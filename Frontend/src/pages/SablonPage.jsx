import { useEffect, useState } from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import TemplateDrawer from '@/components/template/TemplateDrawer';
import { useSearchParams } from 'react-router-dom';
import {
  useTemplates,
  useDeleteTemplate,
  useApplyTemplate,
} from '@/hooks/useTemplates';

const TYPE_CONFIG = {
  INCOME: { label: 'Gelir', pill: 'pos' },
  EXPENSE: { label: 'Gider', pill: 'neg' },
  INVOICE: { label: 'Fatura', pill: 'info' },
  STOCK_ADJUSTMENT: { label: 'Stok', pill: 'warn' },
  CUSTOMER_TRANSACTION: { label: 'Cari', pill: 'accent' },
  BANK_TRANSFER: { label: 'Banka', pill: 'info' },
};

function PayloadSummary({ type, payload }) {
  if (!payload) return <span className="muted">—</span>;

  if (type === 'INCOME' || type === 'EXPENSE') {
    return (
      <div className="grid-2 gap-8">
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Tutar</div>
          <div className="mono tnum" style={{ fontWeight: 600, fontSize: 15 }}>
            {payload.amount ? TRY(payload.amount) : '—'}
          </div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Hesap</div>
          <div style={{ fontSize: 13 }}>{payload.bankName || payload.accountName || payload.account || '—'}</div>
        </div>
      </div>
    );
  }

  if (type === 'INVOICE') {
    const items = payload.lineItems || [];
    return (
      <div>
        <div className="muted" style={{ fontSize: 11 }}>
          Müşteri ID: {payload.customerId || '—'} · Tip: {payload.invoiceType === 'purchase' ? 'Alış' : 'Satış'}
        </div>
        {items.length > 0 && (
          <div className="muted" style={{ fontSize: 11, marginTop: 2 }}>
            {items.length} kalem
          </div>
        )}
      </div>
    );
  }

  if (type === 'STOCK_ADJUSTMENT') {
    return (
      <div className="grid-2 gap-8">
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Ürün ID</div>
          <div style={{ fontSize: 13 }}>{payload.productId || '—'}</div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Yön / Miktar</div>
          <div style={{ fontSize: 13 }}>{payload.direction === 'OUT' ? 'Çıkış' : 'Giriş'} · {payload.quantity || '—'}</div>
        </div>
      </div>
    );
  }

  if (type === 'CUSTOMER_TRANSACTION') {
    return (
      <div className="grid-2 gap-8">
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Tutar</div>
          <div className="mono tnum" style={{ fontWeight: 600, fontSize: 15 }}>
            {payload.amount ? TRY(payload.amount) : '—'}
          </div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Yön</div>
          <div style={{ fontSize: 13 }}>{payload.direction === 'PAYMENT' ? 'Ödeme' : 'Tahsilat'}</div>
        </div>
      </div>
    );
  }

  if (type === 'BANK_TRANSFER') {
    return (
      <div className="grid-2 gap-8">
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Tutar</div>
          <div className="mono tnum" style={{ fontWeight: 600, fontSize: 15 }}>
            {payload.amount ? TRY(payload.amount) : '—'}
          </div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 11 }}>Açıklama</div>
          <div style={{ fontSize: 13 }}>{payload.description || '—'}</div>
        </div>
      </div>
    );
  }

  return <span className="muted">—</span>;
}

export default function SablonPage() {
  const { data: list = [], isLoading, isError, refetch } = useTemplates();
  const deleteMut = useDeleteTemplate();
  const applyMut = useApplyTemplate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState(null);

  useEffect(() => {
    if (searchParams.get('new') === '1' || searchParams.get('new') === 'true') {
      setEditingTemplate(null);
      setDrawerOpen(true);
    }
  }, [searchParams]);

  const typeLabel = (type) => TYPE_CONFIG[type]?.label || type;
  const typePillClass = (type) => TYPE_CONFIG[type]?.pill || 'accent';

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setEditingTemplate(null);
    if (searchParams.get('new')) {
      const next = new URLSearchParams(searchParams);
      next.delete('new');
      setSearchParams(next, { replace: true });
    }
  };

  const handleEdit = (tp) => {
    setEditingTemplate(tp);
    setDrawerOpen(true);
  };

  const handleDelete = (id) => {
    if (window.confirm('Bu şablonu silmek istediğinize emin misiniz?')) {
      deleteMut.mutate(id);
    }
  };

  if (isLoading) {
    return (
      <div className="page">
        <div className="page-head">
          <div>
            <h1 className="page-title">Şablonlar</h1>
            <p className="page-sub">Tekrarlayan işlemler için form şablonları — tek tıkla doldur</p>
          </div>
        </div>
        <div className="grid-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="card" style={{ height: 200, opacity: 0.5 }} />
          ))}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="page">
        <div className="card empty">
          Veri alınamadı{' '}
          <button className="btn sm" onClick={() => refetch()}>
            Tekrar Dene
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Şablonlar</h1>
          <p className="page-sub">
            Tekrarlayan işlemler için form şablonları — tek tıkla doldur
          </p>
        </div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setDrawerOpen(true)}>
            <Icon name="plus" /> Yeni Şablon
          </button>
        </div>
      </div>

      <div className="grid-3">
        {list.map((tp) => (
          <div key={tp.templateId} className="card">
            <div className="card-h" style={{ alignItems: 'flex-start' }}>
              <div>
                <h3>{tp.templateName}</h3>
                <div className="sub mono">
                  {tp.templateCode || `TPL-${String(tp.templateId).padStart(2, '0')}`}
                  {tp.period ? ` · ${tp.period}` : ''}
                </div>
              </div>
              <span className={`pill ${typePillClass(tp.templateType)}`}>
                {typeLabel(tp.templateType)}
              </span>
            </div>

            <div className="card-b">
              <PayloadSummary type={tp.templateType} payload={tp.payload} />

              <div className="divider" />

              <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <div className="muted" style={{ fontSize: 11 }}>
                  Kullanım: <b>{tp.usageCount || 0}×</b>
                </div>
                <div className="row gap-4">
                  <button
                    className="btn sm primary"
                    disabled={applyMut.isPending}
                    onClick={() => applyMut.mutate(tp.templateId)}
                  >
                    <Icon name="flash" size={12} /> Uygula
                  </button>
                  <button className="tb-icon-btn" title="Düzenle" onClick={() => handleEdit(tp)}>
                    <Icon name="edit" size={14} />
                  </button>
                  <button
                    className="tb-icon-btn"
                    title="Sil"
                    onClick={() => handleDelete(tp.templateId)}
                  >
                    <Icon name="trash" size={14} />
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}

        {list.length === 0 && (
          <div className="card empty" style={{ gridColumn: '1 / -1' }}>
            Henüz şablon yok
          </div>
        )}
      </div>

      <TemplateDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        template={editingTemplate}
      />
    </div>
  );
}
