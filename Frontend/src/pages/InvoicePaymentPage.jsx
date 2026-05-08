import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import { TRY } from '@/lib/format';
import { useInvoices } from '@/hooks/useInvoices';
import { useInvoicePayments, useCreateInvoicePayment, useDeleteInvoicePayment } from '@/hooks/useInvoicePayments';
import { useBankAccounts } from '@/hooks/useBankAccounts';

function PaymentMethodPill({ method }) {
  const map = {
    cash: { cls: 'pos', l: 'Nakit' },
    credit_card: { cls: 'info', l: 'Kredi Kartı' },
    bank_transfer: { cls: '', l: 'Havale/EFT' },
    check: { cls: 'warn', l: 'Çek' },
    other: { cls: '', l: 'Diğer' },
  };
  const s = map[method] || { cls: '', l: method };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

function StatusPill({ status }) {
  const map = {
    pending: { cls: 'info', l: 'Beklemede' },
    partially_paid: { cls: 'warn', l: 'Kısmi Ödeme' },
  };
  const s = map[status] || { cls: '', l: status || '—' };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

export default function InvoicePaymentPage() {
  const { data: invoices = [] } = useInvoices();
  const [tab, setTab] = useState('sale');
  const [selectedInvoiceId, setSelectedInvoiceId] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [page, setPage] = useState(1);
  const [invoicePage, setInvoicePage] = useState(1);
  const PAGE_SIZE = 15;
  const INVOICE_PAGE_SIZE = 10;

  const { data: payments = [] } = useInvoicePayments(selectedInvoiceId);
  const createMut = useCreateInvoicePayment(selectedInvoiceId);
  const deleteMut = useDeleteInvoicePayment(selectedInvoiceId);

  const filteredInvoices = useMemo(
    () => invoices.filter((i) => i.invoiceType === tab && i.paymentStatus !== 'draft' && i.paymentStatus !== 'paid'),
    [invoices, tab]
  );

  const selectedInvoice = useMemo(
    () => invoices.find((i) => i.invoiceId === selectedInvoiceId),
    [invoices, selectedInvoiceId]
  );

  const totalPaid = useMemo(
    () => payments.reduce((sum, p) => sum + Number(p.amount || 0), 0),
    [payments]
  );

  const remaining = useMemo(
    () => selectedInvoice ? Number(selectedInvoice.totalAmount || 0) - totalPaid : 0,
    [selectedInvoice, totalPaid]
  );

  const invoiceTotalPages = Math.max(1, Math.ceil(filteredInvoices.length / INVOICE_PAGE_SIZE));
  useEffect(() => { if (invoicePage > invoiceTotalPages) setInvoicePage(invoiceTotalPages); }, [invoiceTotalPages]);
  const invoicePageStart = (invoicePage - 1) * INVOICE_PAGE_SIZE;
  const invoicePageEnd = Math.min(invoicePageStart + INVOICE_PAGE_SIZE, filteredInvoices.length);
  const pagedInvoices = filteredInvoices.slice(invoicePageStart, invoicePageEnd);

  const paymentTotalPages = Math.max(1, Math.ceil(payments.length / PAGE_SIZE));
  useEffect(() => { if (page > paymentTotalPages) setPage(paymentTotalPages); }, [paymentTotalPages]);
  useEffect(() => { setPage(1); setSelectedInvoiceId(null); setInvoicePage(1); setFormOpen(false); }, [tab]);
  const paymentPageStart = (page - 1) * PAGE_SIZE;
  const paymentPageEnd = Math.min(paymentPageStart + PAGE_SIZE, payments.length);
  const pagedPayments = payments.slice(paymentPageStart, paymentPageEnd);

  const openPaymentForm = (invoiceId) => {
    setSelectedInvoiceId(invoiceId);
    setFormOpen(true);
  };

  const closePaymentForm = () => {
    setFormOpen(false);
  };

  return (
    <div className="page">
      <div className="page-head">
        <div><h1 className="page-title">Fatura Ödemeleri</h1><p className="page-sub">Fatura tahsilat ve ödemelerini yönetin</p></div>
      </div>

      <div className="seg" style={{ marginBottom: 16 }}>
        <button className={tab === 'sale' ? 'on' : ''} onClick={() => setTab('sale')}>Satış Faturaları (Tahsilat)</button>
        <button className={tab === 'purchase' ? 'on' : ''} onClick={() => setTab('purchase')}>Alış Faturaları (Ödeme)</button>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="toolbar">
          <div style={{ fontSize: 13, color: 'var(--ink-2)' }}>{filteredInvoices.length} aktif fatura</div>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Fatura No</th>
                <th>Müşteri</th>
                <th>Vade</th>
                <th className="num">Toplam</th>
                <th className="num">Ödenen</th>
                <th className="num">Kalan</th>
                <th>Durum</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {pagedInvoices.map((inv) => {
                const invPayments = payments.filter((p) => p.invoiceId === inv.invoiceId);
                const invPaid = invPayments.reduce((s, p) => s + Number(p.amount || 0), 0);
                const invRemaining = Number(inv.totalAmount || 0) - invPaid;
                const isSelected = selectedInvoiceId === inv.invoiceId;
                return (
                  <React.Fragment key={inv.invoiceId}>
                    <tr
                      style={{ cursor: 'pointer', background: isSelected ? 'var(--bg-2)' : undefined }}
                      onClick={() => { setSelectedInvoiceId(inv.invoiceId); setFormOpen(false); }}
                    >
                      <td className="mono"><b>{inv.invoiceNumber}</b></td>
                      <td>{inv.customerName}</td>
                      <td className="muted">{inv.dueDate || '—'}</td>
                      <td className="num mono tnum">{TRY(inv.totalAmount)}</td>
                      <td className="num mono tnum" style={{ color: 'var(--pos)' }}>{TRY(invPaid)}</td>
                      <td className="num mono tnum" style={{ color: invRemaining > 0 ? 'var(--neg)' : 'var(--pos)' }}>{TRY(invRemaining)}</td>
                      <td><StatusPill status={inv.paymentStatus} /></td>
                      <td>
                        <button
                          className="btn primary sm"
                          onClick={(e) => { e.stopPropagation(); openPaymentForm(inv.invoiceId); }}
                          disabled={inv.paymentStatus === 'paid'}
                        >
                          <Icon name="plus" size={12} /> {tab === 'sale' ? 'Tahsilat' : 'Ödeme'}
                        </button>
                      </td>
                    </tr>
                    {isSelected && formOpen && (
                      <tr>
                        <td colSpan="8" style={{ padding: 0 }}>
                          <InlinePaymentForm
                            invoice={inv}
                            remaining={invRemaining}
                            invoiceType={tab}
                            onClose={closePaymentForm}
                            onSuccess={() => { closePaymentForm(); }}
                          />
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
              {pagedInvoices.length === 0 && <tr><td colSpan="8" className="empty">Aktif fatura bulunamadı</td></tr>}
            </tbody>
          </table>
        </div>
        <Pagination page={invoicePage} totalPages={invoiceTotalPages} setPage={setInvoicePage} pageStart={invoicePageStart} pageEnd={invoicePageEnd} total={filteredInvoices.length} />
      </div>

      {selectedInvoice && !formOpen && (
        <>
          <div className="row gap-12" style={{ marginBottom: 16, flexWrap: 'wrap' }}>
            <div className="card" style={{ flex: 1, minWidth: 180 }}>
              <div className="muted" style={{ fontSize: 12 }}>Fatura No</div>
              <div className="mono" style={{ fontWeight: 600 }}>{selectedInvoice.invoiceNumber}</div>
            </div>
            <div className="card" style={{ flex: 1, minWidth: 180 }}>
              <div className="muted" style={{ fontSize: 12 }}>Toplam Tutar</div>
              <div className="mono" style={{ fontWeight: 600 }}>{TRY(selectedInvoice.totalAmount)}</div>
            </div>
            <div className="card" style={{ flex: 1, minWidth: 180 }}>
              <div className="muted" style={{ fontSize: 12 }}>{tab === 'sale' ? 'Tahsil Edilen' : 'Ödenen'}</div>
              <div className="mono" style={{ fontWeight: 600, color: 'var(--pos)' }}>{TRY(totalPaid)}</div>
            </div>
            <div className="card" style={{ flex: 1, minWidth: 180 }}>
              <div className="muted" style={{ fontSize: 12 }}>Kalan</div>
              <div className="mono" style={{ fontWeight: 600, color: remaining > 0 ? 'var(--neg)' : 'var(--pos)' }}>{TRY(remaining)}</div>
            </div>
            <div className="card" style={{ flex: 1, minWidth: 140 }}>
              <div className="muted" style={{ fontSize: 12 }}>Durum</div>
              <StatusPill status={selectedInvoice.paymentStatus} />
            </div>
          </div>

          <div className="card">
            <div className="toolbar">
              <div style={{ fontSize: 13, color: 'var(--ink-2)' }}>
                {payments.length} {tab === 'sale' ? 'tahsilat' : 'ödeme'} kaydı
                <button className="btn ghost sm" style={{ marginLeft: 12 }} onClick={() => setSelectedInvoiceId(null)}>
                  <Icon name="x" size={12} /> Kapat
                </button>
              </div>
              <div className="page-actions">
                <button
                  className="btn primary"
                  onClick={() => setFormOpen(true)}
                  disabled={selectedInvoice.paymentStatus === 'paid'}
                >
                  <Icon name="plus" /> {tab === 'sale' ? 'Yeni Tahsilat' : 'Yeni Ödeme'}
                </button>
              </div>
            </div>

            {selectedInvoice.paymentStatus === 'paid' && (
              <div style={{ padding: '8px 12px', background: 'var(--pos)', color: '#fff', borderRadius: 'var(--r-md)', fontSize: 13, marginBottom: 12 }}>
                Bu fatura tamamen {tab === 'sale' ? 'tahsil edilmiştir' : 'ödenmiştir'}.
              </div>
            )}

            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Tarih</th>
                    <th>Tutar</th>
                    <th>Yöntem</th>
                    <th>Hesap</th>
                    <th>Notlar</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {pagedPayments.map((p) => (
                    <tr key={p.paymentId}>
                      <td className="mono">{p.paymentId}</td>
                      <td>{p.paymentDate}</td>
                      <td className="num mono tnum"><b>{TRY(p.amount)}</b></td>
                      <td><PaymentMethodPill method={p.paymentMethod} /></td>
                      <td className="muted">{p.bankAccountName || '—'}</td>
                      <td className="muted" style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.notes || '—'}</td>
                      <td>
                        <button className="tb-icon-btn" title="Sil" onClick={() => { if (window.confirm('Bu kaydı silmek istediğinize emin misiniz?')) deleteMut.mutate(p.paymentId); }}>
                          <Icon name="trash" size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {pagedPayments.length === 0 && <tr><td colSpan="7" className="empty">Henüz {tab === 'sale' ? 'tahsilat' : 'ödeme'} kaydı yok</td></tr>}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={paymentTotalPages} setPage={setPage} pageStart={paymentPageStart} pageEnd={paymentPageEnd} total={payments.length} />
          </div>
        </>
      )}
    </div>
  );
}

function InlinePaymentForm({ invoice, remaining, invoiceType, onClose, onSuccess }) {
  const createMut = useCreateInvoicePayment(invoice?.invoiceId);
  const { data: bankAccounts = [] } = useBankAccounts();
  const EMPTY = { amount: '', paymentDate: new Date().toISOString().split('T')[0], paymentMethod: 'cash', bankAccountId: '', notes: '' };
  const [f, setF] = useState(EMPTY);

  const valid = f.amount && Number(f.amount) > 0 && f.paymentDate && f.bankAccountId && Number(f.amount) <= remaining;

  const save = () => {
    if (!valid || !invoice) return;
    createMut.mutate({
      amount: Number(f.amount),
      paymentDate: f.paymentDate,
      paymentMethod: f.paymentMethod,
      bankAccountId: Number(f.bankAccountId),
      notes: f.notes.trim() || undefined,
    }, { onSuccess });
  };

  const isSale = invoiceType === 'sale';

  return (
    <div style={{ background: 'var(--bg-2)', padding: '16px 20px', borderBottom: '1px solid var(--line)' }}>
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
        <div>
          <span style={{ fontWeight: 600, fontSize: 13 }}>{isSale ? 'Yeni Tahsilat' : 'Yeni Ödeme'} — </span>
          <span className="mono" style={{ fontSize: 13 }}>{invoice.invoiceNumber}</span>
        </div>
        <button className="tb-icon-btn" onClick={onClose}><Icon name="x" size={14} /></button>
      </div>

      <div className="row gap-12" style={{ flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div className="field" style={{ flex: 1, minWidth: 120, margin: 0 }}>
          <label>Tutar *</label>
          <input
            className="input mono"
            type="number"
            step="0.01"
            min="0"
            max={remaining}
            value={f.amount}
            onChange={(e) => setF({ ...f, amount: e.target.value })}
            placeholder="0.00"
          />
        </div>

        <div className="field" style={{ flex: 1, minWidth: 140, margin: 0 }}>
          <label>Tarih *</label>
          <input
            className="input"
            type="date"
            value={f.paymentDate}
            onChange={(e) => setF({ ...f, paymentDate: e.target.value })}
          />
        </div>

        <div className="field" style={{ flex: 1, minWidth: 130, margin: 0 }}>
          <label>Yöntem *</label>
          <select
            className="input"
            value={f.paymentMethod}
            onChange={(e) => setF({ ...f, paymentMethod: e.target.value })}
          >
            <option value="cash">Nakit</option>
            <option value="credit_card">Kredi Kartı</option>
            <option value="bank_transfer">Havale/EFT</option>
            <option value="check">Çek</option>
            <option value="other">Diğer</option>
          </select>
        </div>

        <div className="field" style={{ flex: 1.5, minWidth: 160, margin: 0 }}>
          <label>Hesap *</label>
          <select
            className="input"
            value={f.bankAccountId}
            onChange={(e) => setF({ ...f, bankAccountId: e.target.value })}
          >
            <option value="">Seçin...</option>
            {bankAccounts.map((b) => (
              <option key={b.accountId} value={b.accountId}>{b.bankName || 'Bilinmeyen'}</option>
            ))}
          </select>
        </div>

        <div className="field" style={{ flex: 1.5, minWidth: 160, margin: 0 }}>
          <label>Notlar</label>
          <input
            className="input"
            value={f.notes}
            onChange={(e) => setF({ ...f, notes: e.target.value })}
            placeholder="Opsiyonel..."
            maxLength={500}
          />
        </div>

        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
          <button className="btn ghost sm" onClick={onClose}>Vazgeç</button>
          <button className="btn primary sm" disabled={!valid || createMut.isPending} onClick={save}>
            {createMut.isPending ? 'Kaydediliyor...' : 'Kaydet'}
          </button>
        </div>
      </div>

      <div style={{ marginTop: 8, fontSize: 12, color: 'var(--ink-3)' }}>
        Kalan: <span className="mono" style={{ color: 'var(--neg)' }}>{TRY(remaining)}</span>
      </div>
    </div>
  );
}
