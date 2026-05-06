import React, { useState, useEffect } from 'react';
import Drawer from '@/components/mp/Drawer';
import Icon from '@/components/mp/Icon';
import { useCreateTemplate, useUpdateTemplate } from '@/hooks/useTemplates';
import { useBankAccounts } from '@/hooks/useBankAccounts';
import { useCustomers } from '@/hooks/useCustomers';
import { useProducts } from '@/hooks/useProducts';

const TYPE_OPTIONS = [
  { value: 'EXPENSE', label: 'Gider' },
  { value: 'INCOME', label: 'Gelir' },
  { value: 'INVOICE', label: 'Fatura' },
  { value: 'STOCK_ADJUSTMENT', label: 'Stok Hareketi' },
  { value: 'CUSTOMER_TRANSACTION', label: 'Cari İşlem' },
  { value: 'BANK_TRANSFER', label: 'Banka Transferi' },
];

const PERIOD_OPTIONS = ['aylık', 'haftalık', 'yıllık', 'tek_seferlik'];

const EMPTY_PAYLOADS = {
  INCOME: { amount: '', accountId: '', bankName: '', category: '' },
  EXPENSE: { amount: '', accountId: '', bankName: '', category: '' },
  INVOICE: { customerId: '', invoiceType: 'sale', dueDate: '', lineItems: [] },
  STOCK_ADJUSTMENT: { productId: '', quantity: '', direction: 'IN' },
  CUSTOMER_TRANSACTION: { customerId: '', amount: '', accountId: '', bankName: '', direction: 'COLLECTION', category: '' },
  BANK_TRANSFER: { fromAccountId: '', toAccountId: '', amount: '', description: '' },
};

function getInitialState(template) {
  if (template) {
    const type = template.templateType || 'EXPENSE';
    const saved = template.payload || {};
    const def = EMPTY_PAYLOADS[type] || {};
    const payload = {};
    for (const key of Object.keys(def)) {
      if (key === 'lineItems') {
        payload.lineItems = saved.lineItems || [];
      } else {
        payload[key] = saved[key] !== undefined && saved[key] !== null ? saved[key] : def[key];
      }
    }
    if ((type === 'INCOME' || type === 'EXPENSE') && saved.defaultAmount && !payload.amount) {
      payload.amount = saved.defaultAmount;
    }
    if ((type === 'INCOME' || type === 'EXPENSE') && (saved.accountName || saved.account) && !payload.bankName) {
      payload.bankName = saved.accountName || saved.account;
    }
    return {
      templateCode: template.templateCode || '',
      templateName: template.templateName || '',
      templateType: type,
      description: template.description || '',
      period: template.period || 'aylık',
      payload,
    };
  }
  return {
    templateCode: '',
    templateName: '',
    templateType: 'EXPENSE',
    description: '',
    period: 'aylık',
    payload: { ...EMPTY_PAYLOADS.INCOME },
  };
}

export default function TemplateDrawer({ open, onClose, template }) {
  const isEdit = !!template;
  const [form, setForm] = useState(getInitialState(template));
  const createMut = useCreateTemplate();
  const updateMut = useUpdateTemplate();
  const { data: bankAccounts = [] } = useBankAccounts();
  const { data: customersData } = useCustomers();
  const customers = customersData || [];
  const { data: productsData } = useProducts();
  const products = productsData || [];

  useEffect(() => {
    if (open) setForm(getInitialState(template));
  }, [open, template]);

  const updateField = (key, value) => {
    setForm((f) => {
      if (key === 'templateType') {
        return { ...f, templateType: value, payload: { ...EMPTY_PAYLOADS[value] } };
      }
      return { ...f, [key]: value };
    });
  };

  const updatePayload = (key, value) => {
    setForm((f) => ({ ...f, payload: { ...f.payload, [key]: value } }));
  };

  const handleAccountChange = (accountId, field = 'accountId', nameField = 'bankName') => {
    const selected = bankAccounts.find((b) => String(b.accountId) === accountId);
    setForm((f) => ({
      ...f,
      payload: {
        ...f.payload,
        [field]: accountId,
        [nameField]: selected ? selected.bankName : '',
      },
    }));
  };

  const addLineItem = () => {
    setForm((f) => ({
      ...f,
      payload: {
        ...f.payload,
        lineItems: [...(f.payload.lineItems || []), { productId: '', quantity: 1 }],
      },
    }));
  };

  const updateLineItem = (index, key, value) => {
    setForm((f) => {
      const items = [...(f.payload.lineItems || [])];
      items[index] = { ...items[index], [key]: key === 'quantity' ? Number(value) || 1 : value };
      return { ...f, payload: { ...f.payload, lineItems: items } };
    });
  };

  const removeLineItem = (index) => {
    setForm((f) => {
      const items = (f.payload.lineItems || []).filter((_, i) => i !== index);
      return { ...f, payload: { ...f.payload, lineItems: items } };
    });
  };

  const buildPayload = () => {
    const t = form.templateType;
    const p = form.payload;
    const payload = {};

    if (t === 'INCOME' || t === 'EXPENSE') {
      if (p.amount) payload.amount = Number(p.amount);
      if (p.accountId) payload.accountId = Number(p.accountId);
      if (p.bankName) payload.bankName = p.bankName;
      if (p.category) payload.category = p.category;
    } else if (t === 'INVOICE') {
      if (p.customerId) payload.customerId = Number(p.customerId);
      payload.invoiceType = p.invoiceType || 'sale';
      if (p.dueDate) payload.dueDate = p.dueDate;
      payload.lineItems = (p.lineItems || [])
        .filter((li) => li.productId && li.quantity > 0)
        .map((li) => ({ productId: Number(li.productId), quantity: Number(li.quantity) }));
    } else if (t === 'STOCK_ADJUSTMENT') {
      if (p.productId) payload.productId = Number(p.productId);
      if (p.quantity) payload.quantity = Number(p.quantity);
      payload.direction = p.direction || 'IN';
    } else if (t === 'CUSTOMER_TRANSACTION') {
      if (p.customerId) payload.customerId = Number(p.customerId);
      if (p.amount) payload.amount = Number(p.amount);
      if (p.accountId) payload.accountId = Number(p.accountId);
      if (p.bankName) payload.bankName = p.bankName;
      payload.direction = p.direction || 'COLLECTION';
      if (p.category) payload.category = p.category;
    } else if (t === 'BANK_TRANSFER') {
      if (p.fromAccountId) payload.fromAccountId = Number(p.fromAccountId);
      if (p.toAccountId) payload.toAccountId = Number(p.toAccountId);
      if (p.amount) payload.amount = Number(p.amount);
      if (p.description) payload.description = p.description;
    }

    return Object.keys(payload).length > 0 ? payload : null;
  };

  const handleSubmit = () => {
    const payload = buildPayload();
    const dto = {
      templateCode: form.templateCode,
      templateName: form.templateName,
      templateType: form.templateType,
      description: form.description,
      period: form.period,
      payload,
    };

    if (isEdit) {
      updateMut.mutate({ id: template.templateId, dto }, { onSuccess: onClose });
    } else {
      createMut.mutate(dto, { onSuccess: onClose });
    }
  };

  const canSubmit =
    form.templateCode.trim() && form.templateName.trim() && !createMut.isPending && !updateMut.isPending;

  const t = form.templateType;
  const p = form.payload;

  const renderPayloadFields = () => {
    if (t === 'INCOME' || t === 'EXPENSE') {
      return (
        <div className="card" style={{ background: 'var(--bg-elev)' }}>
          <div className="card-h"><h3>İşlem Detayları</h3></div>
          <div className="card-b">
            <div className="grid-2 gap-8">
              <div className="field">
                <label>Tutar (₺)</label>
                <input type="number" className="input mono" value={p.amount} onChange={(e) => updatePayload('amount', e.target.value)} placeholder="42000" />
              </div>
              <div className="field">
                <label>Banka Hesabı</label>
                <select className="select" value={p.accountId} onChange={(e) => handleAccountChange(e.target.value)}>
                  <option value="">Hesap seçin...</option>
                  {bankAccounts.map((acc) => (
                    <option key={acc.accountId} value={acc.accountId}>{acc.bankName}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="field" style={{ marginTop: 12 }}>
              <label>Kategori</label>
              <input className="input" value={p.category} onChange={(e) => updatePayload('category', e.target.value)} placeholder="Kira, Elektrik, Maaş..." />
            </div>
          </div>
        </div>
      );
    }

    if (t === 'INVOICE') {
      return (
        <div className="card" style={{ background: 'var(--bg-elev)' }}>
          <div className="card-h"><h3>Fatura Detayları</h3></div>
          <div className="card-b">
            <div className="grid-2 gap-8">
              <div className="field">
                <label>Müşteri</label>
                <select className="select" value={p.customerId} onChange={(e) => updatePayload('customerId', e.target.value)}>
                  <option value="">Müşteri seçin...</option>
                  {customers.map((c) => (
                    <option key={c.customerId} value={c.customerId}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Fatura Tipi</label>
                <select className="select" value={p.invoiceType} onChange={(e) => updatePayload('invoiceType', e.target.value)}>
                  <option value="sale">Satış</option>
                  <option value="purchase">Alış</option>
                </select>
              </div>
            </div>
            <div className="field" style={{ marginTop: 12 }}>
              <label>Vade Tarihi</label>
              <input type="date" className="input" value={p.dueDate} onChange={(e) => updatePayload('dueDate', e.target.value)} />
            </div>
            <div style={{ marginTop: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <label style={{ fontWeight: 600, fontSize: 13 }}>Kalemler</label>
                <button className="btn sm" type="button" onClick={addLineItem}><Icon name="plus" size={12} /> Kalem Ekle</button>
              </div>
              {(p.lineItems || []).map((li, i) => (
                <div key={i} className="grid-3 gap-8" style={{ marginBottom: 6 }}>
                  <div className="field">
                    <select className="select" value={li.productId} onChange={(e) => updateLineItem(i, 'productId', e.target.value)}>
                      <option value="">Ürün seçin...</option>
                      {products.map((pr) => (
                        <option key={pr.productId} value={pr.productId}>{pr.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field">
                    <input type="number" className="input mono" min={1} value={li.quantity} onChange={(e) => updateLineItem(i, 'quantity', e.target.value)} placeholder="Adet" />
                  </div>
                  <button className="tb-icon-btn" type="button" onClick={() => removeLineItem(i)} title="Kaldır"><Icon name="trash" size={14} /></button>
                </div>
              ))}
              {(p.lineItems || []).length === 0 && <div className="muted" style={{ fontSize: 12 }}>En az bir kalem ekleyin</div>}
            </div>
          </div>
        </div>
      );
    }

    if (t === 'STOCK_ADJUSTMENT') {
      return (
        <div className="card" style={{ background: 'var(--bg-elev)' }}>
          <div className="card-h"><h3>Stok Detayları</h3></div>
          <div className="card-b">
            <div className="field">
              <label>Ürün</label>
              <select className="select" value={p.productId} onChange={(e) => updatePayload('productId', e.target.value)}>
                <option value="">Ürün seçin...</option>
                {products.map((pr) => (
                  <option key={pr.productId} value={pr.productId}>{pr.name}</option>
                ))}
              </select>
            </div>
            <div className="grid-2 gap-8" style={{ marginTop: 12 }}>
              <div className="field">
                <label>Miktar</label>
                <input type="number" className="input mono" min={1} value={p.quantity} onChange={(e) => updatePayload('quantity', e.target.value)} placeholder="Adet" />
              </div>
              <div className="field">
                <label>Yön</label>
                <select className="select" value={p.direction} onChange={(e) => updatePayload('direction', e.target.value)}>
                  <option value="IN">Giriş (Stok Ekle)</option>
                  <option value="OUT">Çıkış (Stok Düş)</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      );
    }

    if (t === 'CUSTOMER_TRANSACTION') {
      return (
        <div className="card" style={{ background: 'var(--bg-elev)' }}>
          <div className="card-h"><h3>Cari İşlem Detayları</h3></div>
          <div className="card-b">
            <div className="grid-2 gap-8">
              <div className="field">
                <label>Müşteri</label>
                <select className="select" value={p.customerId} onChange={(e) => updatePayload('customerId', e.target.value)}>
                  <option value="">Müşteri seçin...</option>
                  {customers.map((c) => (
                    <option key={c.customerId} value={c.customerId}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>İşlem Yönü</label>
                <select className="select" value={p.direction} onChange={(e) => updatePayload('direction', e.target.value)}>
                  <option value="COLLECTION">Tahsilat (Gelir)</option>
                  <option value="PAYMENT">Ödeme (Gider)</option>
                </select>
              </div>
            </div>
            <div className="grid-2 gap-8" style={{ marginTop: 12 }}>
              <div className="field">
                <label>Tutar (₺)</label>
                <input type="number" className="input mono" value={p.amount} onChange={(e) => updatePayload('amount', e.target.value)} placeholder="0.00" />
              </div>
              <div className="field">
                <label>Banka Hesabı</label>
                <select className="select" value={p.accountId} onChange={(e) => handleAccountChange(e.target.value)}>
                  <option value="">Hesap seçin...</option>
                  {bankAccounts.map((acc) => (
                    <option key={acc.accountId} value={acc.accountId}>{acc.bankName}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="field" style={{ marginTop: 12 }}>
              <label>Kategori</label>
              <input className="input" value={p.category} onChange={(e) => updatePayload('category', e.target.value)} placeholder="Tahsilat, Ödeme..." />
            </div>
          </div>
        </div>
      );
    }

    if (t === 'BANK_TRANSFER') {
      return (
        <div className="card" style={{ background: 'var(--bg-elev)' }}>
          <div className="card-h"><h3>Transfer Detayları</h3></div>
          <div className="card-b">
            <div className="grid-2 gap-8">
              <div className="field">
                <label>Kaynak Hesap (Çıkış)</label>
                <select className="select" value={p.fromAccountId} onChange={(e) => updatePayload('fromAccountId', e.target.value)}>
                  <option value="">Hesap seçin...</option>
                  {bankAccounts.map((acc) => (
                    <option key={acc.accountId} value={acc.accountId}>{acc.bankName}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Hedef Hesap (Giriş)</label>
                <select className="select" value={p.toAccountId} onChange={(e) => updatePayload('toAccountId', e.target.value)}>
                  <option value="">Hesap seçin...</option>
                  {bankAccounts.map((acc) => (
                    <option key={acc.accountId} value={acc.accountId}>{acc.bankName}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="grid-2 gap-8" style={{ marginTop: 12 }}>
              <div className="field">
                <label>Tutar (₺)</label>
                <input type="number" className="input mono" value={p.amount} onChange={(e) => updatePayload('amount', e.target.value)} placeholder="0.00" />
              </div>
              <div className="field">
                <label>Açıklama</label>
                <input className="input" value={p.description} onChange={(e) => updatePayload('description', e.target.value)} placeholder="Transfer açıklaması..." />
              </div>
            </div>
          </div>
        </div>
      );
    }

    return null;
  };

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
        <div className="grid-2">
          <div className="field">
            <label>Şablon Kodu</label>
            <input className="input mono" value={form.templateCode} onChange={(e) => updateField('templateCode', e.target.value)} placeholder="TPL-01" />
          </div>
          <div className="field">
            <label>Şablon Adı</label>
            <input className="input" value={form.templateName} onChange={(e) => updateField('templateName', e.target.value)} placeholder="Örn: Ofis Kirası" />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Şablon Tipi</label>
            <select className="select" value={form.templateType} onChange={(e) => updateField('templateType', e.target.value)}>
              {TYPE_OPTIONS.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Periyot</label>
            <select className="select" value={form.period} onChange={(e) => updateField('period', e.target.value)}>
              {PERIOD_OPTIONS.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="field">
          <label>Açıklama</label>
          <textarea className="input" rows={2} value={form.description} onChange={(e) => updateField('description', e.target.value)} placeholder="Şablon hakkında kısa not..." />
        </div>

        {renderPayloadFields()}
      </div>
    </Drawer>
  );
}