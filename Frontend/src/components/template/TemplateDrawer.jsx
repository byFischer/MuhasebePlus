import React, { useState, useEffect } from 'react';
import Drawer from '@/components/mp/Drawer';
import Icon from '@/components/mp/Icon';
import { useCreateTemplate, useUpdateTemplate } from '@/hooks/useTemplates';
import { useBankAccounts } from '@/hooks/useBankAccounts';

const TYPE_OPTIONS = [
  { value: 'EXPENSE', label: 'Gider' },
  { value: 'INCOME', label: 'Gelir' },
  { value: 'INVOICE', label: 'Fatura' },
  { value: 'STOCK_ADJUSTMENT', label: 'Stok Hareketi' },
  { value: 'CUSTOMER_TRANSACTION', label: 'Cari İşlem' },
  { value: 'BANK_TRANSFER', label: 'Banka Transferi' },
];

const PERIOD_OPTIONS = ['aylık', 'haftalık', 'yıllık', 'tek_seferlik'];

function getInitialState(template) {
  if (template) {
    return {
      templateCode: template.templateCode || '',
      templateName: template.templateName || '',
      templateType: template.templateType || 'EXPENSE',
      description: template.description || '',
      period: template.period || 'aylık',
      payload: {
        amount: template.payload?.amount || template.payload?.defaultAmount || '',
        accountId: template.payload?.accountId || '',
        bankName: template.payload?.bankName || template.payload?.accountName || template.payload?.account || '',
        category: template.payload?.category || '',
      },
    };
  }
  return {
    templateCode: '',
    templateName: '',
    templateType: 'EXPENSE',
    description: '',
    period: 'aylık',
    payload: { amount: '', accountId: '', bankName: '', category: '' },
  };
}

export default function TemplateDrawer({ open, onClose, template }) {
  const isEdit = !!template;
  const [form, setForm] = useState(getInitialState(template));
  const createMut = useCreateTemplate();
  const updateMut = useUpdateTemplate();
  const { data: bankAccounts = [] } = useBankAccounts();

  useEffect(() => {
    if (open) setForm(getInitialState(template));
  }, [open, template]);

  const updateField = (key, value) => {
    setForm((f) => ({ ...f, [key]: value }));
  };

  const updatePayload = (key, value) => {
    setForm((f) => ({ ...f, payload: { ...f.payload, [key]: value } }));
  };

  const handleAccountChange = (accountId) => {
    const selected = bankAccounts.find((b) => String(b.accountId) === accountId);
    setForm((f) => ({
      ...f,
      payload: {
        ...f.payload,
        accountId: accountId,
        bankName: selected ? selected.bankName : '',
      },
    }));
  };

  const isFinancial = form.templateType === 'INCOME' || form.templateType === 'EXPENSE';

  const handleSubmit = () => {
    const payload = {};
    if (isFinancial) {
      if (form.payload.amount) payload.amount = Number(form.payload.amount);
      if (form.payload.accountId) payload.accountId = Number(form.payload.accountId);
      if (form.payload.bankName) payload.bankName = form.payload.bankName;
      if (form.payload.category) payload.category = form.payload.category;
    }

    const dto = {
      templateCode: form.templateCode,
      templateName: form.templateName,
      templateType: form.templateType,
      description: form.description,
      period: form.period,
      payload: Object.keys(payload).length > 0 ? payload : null,
    };

    if (isEdit) {
      updateMut.mutate({ id: template.templateId, dto }, { onSuccess: onClose });
    } else {
      createMut.mutate(dto, { onSuccess: onClose });
    }
  };

  const canSubmit =
    form.templateCode.trim() && form.templateName.trim() && !createMut.isPending && !updateMut.isPending;

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={isEdit ? 'Şablon Düzenle' : 'Yeni Şablon'}
      width="520px"
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>
            Vazgeç
          </button>
          <button className="btn primary" disabled={!canSubmit} onClick={handleSubmit}>
            {isEdit ? 'Güncelle' : 'Kaydet'}
          </button>
        </>
      }
    >
      <div className="col gap-12">
        {/* Kod + Ad */}
        <div className="grid-2">
          <div className="field">
            <label>Şablon Kodu</label>
            <input
              className="input mono"
              value={form.templateCode}
              onChange={(e) => updateField('templateCode', e.target.value)}
              placeholder="TPL-01"
            />
          </div>
          <div className="field">
            <label>Şablon Adı</label>
            <input
              className="input"
              value={form.templateName}
              onChange={(e) => updateField('templateName', e.target.value)}
              placeholder="Örn: Ofis Kirası"
            />
          </div>
        </div>

        {/* Tip + Periyot */}
        <div className="grid-2">
          <div className="field">
            <label>Şablon Tipi</label>
            <select
              className="select"
              value={form.templateType}
              onChange={(e) => updateField('templateType', e.target.value)}
            >
              {TYPE_OPTIONS.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Periyot</label>
            <select
              className="select"
              value={form.period}
              onChange={(e) => updateField('period', e.target.value)}
            >
              {PERIOD_OPTIONS.map((p) => (
                <option key={p} value={p}>
                  {p}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Açıklama */}
        <div className="field">
          <label>Açıklama</label>
          <textarea
            className="input"
            rows={2}
            value={form.description}
            onChange={(e) => updateField('description', e.target.value)}
            placeholder="Şablon hakkında kısa not..."
          />
        </div>

        {/* Dinamik Payload Alanları */}
        {isFinancial && (
          <div className="card" style={{ background: 'var(--bg-elev)' }}>
            <div className="card-h">
              <h3>İşlem Detayları</h3>
            </div>
            <div className="card-b">
              <div className="grid-2 gap-8">
                <div className="field">
                  <label>Tutar (₺)</label>
                  <input
                    type="number"
                    className="input mono"
                    value={form.payload.amount}
                    onChange={(e) => updatePayload('amount', e.target.value)}
                    placeholder="42000"
                  />
                </div>
                <div className="field">
                  <label>Banka Hesabı</label>
                  <select
                    className="select"
                    value={form.payload.accountId}
                    onChange={(e) => handleAccountChange(e.target.value)}
                  >
                    <option value="">Hesap seçin...</option>
                    {bankAccounts.map((acc) => (
                      <option key={acc.accountId} value={acc.accountId}>
                        {acc.bankName}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="field" style={{ marginTop: 12 }}>
                <label>Kategori</label>
                <input
                  className="input"
                  value={form.payload.category}
                  onChange={(e) => updatePayload('category', e.target.value)}
                  placeholder="Kira, Elektrik, Maaş..."
                />
              </div>
            </div>
          </div>
        )}

        {!isFinancial && (
          <div className="hint">
            <Icon name="info" size={14} />
            Bu şablon tipi için detaylar şu an sadece backend tarafında oluşturuluyor.
          </div>
        )}
      </div>
    </Drawer>
  );
}
