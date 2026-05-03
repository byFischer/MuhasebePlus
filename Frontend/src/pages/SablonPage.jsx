import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import TemplateDrawer from '@/components/template/TemplateDrawer';
import {
  useTemplates,
  useDeleteTemplate,
  useApplyTemplate,
} from '@/hooks/useTemplates';

export default function SablonPage() {
  const { data: list = [], isLoading, isError, refetch } = useTemplates();
  const deleteMut = useDeleteTemplate();
  const applyMut = useApplyTemplate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState(null);

  const typeLabel = (type) => {
    switch (type) {
      case 'INCOME': return 'Gelir';
      case 'EXPENSE': return 'Gider';
      case 'INVOICE': return 'Fatura';
      case 'STOCK_ADJUSTMENT': return 'Stok';
      case 'CUSTOMER_TRANSACTION': return 'Cari';
      case 'BANK_TRANSFER': return 'Banka';
      default: return type;
    }
  };

  const typePillClass = (type) => {
    switch (type) {
      case 'INCOME': return 'pos';
      case 'EXPENSE': return 'neg';
      default: return 'info';
    }
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setEditingTemplate(null);
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
            <p className="page-sub">Tekrarlayan gelir/gider işlemleri için form şablonları — tek tıkla doldur</p>
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
      {/* PAGE HEADER */}
      <div className="page-head">
        <div>
          <h1 className="page-title">Şablonlar</h1>
          <p className="page-sub">
            Tekrarlayan gelir/gider işlemleri için form şablonları — tek tıkla doldur
          </p>
        </div>
        <div className="page-actions">
          <button
            className="btn primary"
            style={{
              background: '#1a7f5a',
              borderColor: '#1a7f5a',
            }}
            onClick={() => setDrawerOpen(true)}
          >
            <Icon name="plus" /> Yeni Şablon
          </button>
        </div>
      </div>

      {/* TEMPLATE GRID */}
      <div className="grid-3">
        {list.map((tp) => (
          <div key={tp.templateId} className="card">
            {/* CARD HEADER */}
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

            {/* CARD BODY */}
            <div className="card-b">
              <div className="grid-2 gap-8">
                <div>
                  <div className="muted" style={{ fontSize: 11 }}>Tutar</div>
                  <div className="mono tnum" style={{ fontWeight: 600, fontSize: 15 }}>
                    {tp.payload?.amount ? TRY(tp.payload.amount) : (tp.payload?.defaultAmount ? TRY(tp.payload.defaultAmount) : '—')}
                  </div>
                </div>
                <div>
                  <div className="muted" style={{ fontSize: 11 }}>Hesap</div>
                  <div style={{ fontSize: 13 }}>
                    {tp.payload?.bankName || tp.payload?.accountName || tp.payload?.account || '—'}
                  </div>
                </div>
              </div>

              <div className="divider" />

              {/* CARD FOOTER */}
              <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <div className="muted" style={{ fontSize: 11 }}>
                  Kullanım: <b>{tp.usageCount || 0}×</b>
                </div>
                <div className="row gap-4">
                  <button
                    className="btn sm primary"
                    style={{
                      background: '#1a7f5a',
                      borderColor: '#1a7f5a',
                    }}
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

      {/* DRAWER */}
      <TemplateDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        template={editingTemplate}
      />
    </div>
  );
}
