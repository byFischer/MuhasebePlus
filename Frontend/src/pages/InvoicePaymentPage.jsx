import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import { TRY } from '@/lib/format';
import { useInvoices } from '@/hooks/useInvoices';
import { useInvoicePayments, useCreateInvoicePayment, useDeleteInvoicePayment, useInvoicePromises, useCreatePromise, useFulfillPromise } from '@/hooks/useInvoicePayments';
import { useLateFee } from '@/hooks/useInvoices';
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
  const { data: promises = [] } = useInvoicePromises(selectedInvoiceId);
  const { data: lateFeeData } = useLateFee(selectedInvoiceId);
  const deleteMut = useDeleteInvoicePayment(selectedInvoiceId);
  const fulfillPromiseMut = useFulfillPromise(selectedInvoiceId);

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

  // Form Vazgeç/X → formu kapat, seçili faturanın detayını göstermeye devam et
  const closeForm = () => setFormOpen(false);

  // Ödeme kaydedildikten sonra: kısmi ödemede detay açık kalır;
  // fatura tamamen ödendiyse seçim kapatılır (paid fatura listeden de düşer)
  const handlePaymentSuccess = (fullyPaid) => {
    setFormOpen(false);
    if (fullyPaid) setSelectedInvoiceId(null);
  };

  // Seçili faturanın detayı — satırın hemen altında inline gösterilir
  const detailPanel = (selectedInvoice && !formOpen) ? (
    <div style={{ background: 'var(--bg-2)', padding: '16px 20px', borderBottom: '1px solid var(--line)' }}>
      <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
        <div style={{ fontSize: 13, color: 'var(--ink-2)' }}>
          {payments.length} {tab === 'sale' ? 'tahsilat' : 'ödeme'} kaydı
          {' · '}{tab === 'sale' ? 'Tahsil edilen' : 'Ödenen'}: <span className="mono" style={{ color: 'var(--pos)' }}>{TRY(totalPaid)}</span>
          {' · '}Kalan: <span className="mono" style={{ color: remaining > 0 ? 'var(--neg)' : 'var(--pos)' }}>{TRY(remaining)}</span>
          {lateFeeData?.lateFee > 0 && (
            <span style={{ marginLeft: 8, color: 'var(--neg)' }}>{' · '}Gecikme faizi: <span className="mono">{TRY(lateFeeData.lateFee)}</span></span>
          )}
        </div>
        <div className="row gap-8">
          <button
            className="btn primary sm"
            onClick={() => setFormOpen(true)}
            disabled={selectedInvoice.paymentStatus === 'paid'}
          >
            <Icon name="plus" size={12} /> {tab === 'sale' ? 'Yeni Tahsilat' : 'Yeni Ödeme'}
          </button>
          <button className="btn ghost sm" onClick={() => setSelectedInvoiceId(null)}>
            <Icon name="x" size={12} /> Kapat
          </button>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>ID</th><th>Tarih</th><th>Tutar</th><th>Yöntem</th><th>Hesap</th><th>Notlar</th><th></th></tr>
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

      <div className="card">
        <div className="toolbar">
          <div style={{ fontSize: 13, fontWeight: 600 }}>Tahsilat Sözleri</div>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Söz Tarihi</th><th className="num">Tutar</th><th>Not</th><th>Durum</th><th></th></tr></thead>
            <tbody>
              {promises.map(p => (
                <tr key={p.promiseId}>
                  <td>{p.promisedDate}</td>
                  <td className="num mono tnum">{TRY(p.promisedAmount)}</td>
                  <td className="muted">{p.notes || '—'}</td>
                  <td>{p.fulfilled ? <span className="pill pos">✅ Gerçekleşti</span> : <span className="pill warn">⏳ Bekliyor</span>}</td>
                  <td>{!p.fulfilled && <button className="btn ghost sm" onClick={() => fulfillPromiseMut.mutate(p.promiseId)}>Gerçekleştir</button>}</td>
                </tr>
              ))}
              {promises.length === 0 && <tr><td colSpan="5" className="empty">Henüz tahsilat sözü yok</td></tr>}
            </tbody>
          </table>
        </div>
        <PromiseForm invoiceId={selectedInvoiceId} />
      </div>
    </div>
  ) : null;

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
                            onCancel={closeForm}
                            onSuccess={handlePaymentSuccess}
                          />
                        </td>
                      </tr>
                    )}
                    {isSelected && !formOpen && detailPanel && (
                      <tr>
                        <td colSpan="8" style={{ padding: 0 }}>{detailPanel}</td>
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
    </div>
  );
}

function formatCents(cents) {
  const c = Math.max(0, Math.floor(Number(cents) || 0));
  const lira = Math.floor(c / 100);
  const kurus = String(c % 100).padStart(2, '0');
  return `${lira.toLocaleString('tr-TR')},${kurus}`;
}

function InlinePaymentForm({ invoice, remaining, invoiceType, onCancel, onSuccess }) {
  const createMut = useCreateInvoicePayment(invoice?.invoiceId);
  const { data: bankAccounts = [] } = useBankAccounts();
  const remainingCents = Math.round(Number(remaining || 0) * 100);
  const EMPTY = {
    amountCents: 0,
    paymentDate: new Date().toISOString().split('T')[0],
    paymentMethod: 'cash',
    bankAccountId: '',
    notes: '',
  };
  const [f, setF] = useState(EMPTY);

  const valid = f.amountCents > 0 && f.paymentDate && f.bankAccountId && f.amountCents <= remainingCents;

  const handleAmountChange = (raw) => {
    const digits = String(raw).replace(/\D/g, '');
    let cents = digits === '' ? 0 : parseInt(digits, 10);
    if (cents > remainingCents) cents = remainingCents;
    setF((prev) => ({ ...prev, amountCents: cents }));
  };

  const fillAll = () => setF((prev) => ({ ...prev, amountCents: remainingCents }));

  const save = () => {
    if (!valid || !invoice) return;
    const fullyPaid = f.amountCents >= remainingCents;
    createMut.mutate({
      amount: f.amountCents / 100,
      paymentDate: f.paymentDate,
      paymentMethod: f.paymentMethod,
      bankAccountId: Number(f.bankAccountId),
      notes: f.notes.trim() || undefined,
    }, { onSuccess: () => onSuccess?.(fullyPaid) });
  };

  const isSale = invoiceType === 'sale';

  return (
    <div style={{ background: 'var(--bg-2)', padding: '16px 20px', borderBottom: '1px solid var(--line)' }}>
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
        <div>
          <span style={{ fontWeight: 600, fontSize: 13 }}>{isSale ? 'Yeni Tahsilat' : 'Yeni Ödeme'} — </span>
          <span className="mono" style={{ fontSize: 13 }}>{invoice.invoiceNumber}</span>
        </div>
        <button className="tb-icon-btn" onClick={onCancel}><Icon name="x" size={14} /></button>
      </div>

      <div className="row gap-12" style={{ flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div className="field" style={{ flex: 1.2, minWidth: 160, margin: 0 }}>
          <label>Tutar *</label>
          <div className="row gap-4">
            <input
              className="input mono"
              type="text"
              inputMode="numeric"
              value={formatCents(f.amountCents)}
              onChange={(e) => handleAmountChange(e.target.value)}
              placeholder="0,00"
              style={{ flex: 1, textAlign: 'right' }}
            />
            <button
              type="button"
              className="btn ghost sm"
              onClick={fillAll}
              title="Kalan tutarın tamamını gir"
              disabled={remainingCents <= 0}
            >
              Tümü
            </button>
          </div>
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
          <button className="btn ghost sm" onClick={onCancel}>Vazgeç</button>
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

function PromiseForm({ invoiceId }) {
  const createMut = useCreatePromise(invoiceId);
  const [show, setShow] = useState(false);
  const [promisedDate, setPromisedDate] = useState('');
  const [promisedAmount, setPromisedAmount] = useState('');
  const [notes, setNotes] = useState('');

  const save = () => {
    if (!promisedDate || !promisedAmount) return;
    createMut.mutate({
      promisedDate,
      promisedAmount: Number(promisedAmount),
      notes: notes?.trim() || null,
    }, { onSuccess: () => { setShow(false); setPromisedDate(''); setPromisedAmount(''); setNotes(''); } });
  };

  if (!show) return <button className="btn ghost sm" style={{ marginTop: 8 }} onClick={() => setShow(true)}><Icon name="plus" size={12} /> Yeni Tahsilat Sözü</button>;

  return (
    <div className="col gap-8" style={{ marginTop: 8, padding: '8px 0' }}>
      <div className="row gap-8" style={{ alignItems: 'flex-end' }}>
        <div className="field" style={{ minWidth: 140 }}>
          <label style={{ fontSize: 11 }}>Söz Tarihi</label>
          <input className="input" type="date" value={promisedDate} onChange={e => setPromisedDate(e.target.value)} />
        </div>
        <div className="field" style={{ minWidth: 120 }}>
          <label style={{ fontSize: 11 }}>Tutar</label>
          <input className="input mono" type="number" min="0" value={promisedAmount} onChange={e => setPromisedAmount(e.target.value)} placeholder="0.00" />
        </div>
        <div className="field" style={{ flex: 1 }}>
          <label style={{ fontSize: 11 }}>Not</label>
          <input className="input" value={notes} onChange={e => setNotes(e.target.value)} placeholder="Opsiyonel..." />
        </div>
        <button className="btn ghost sm" onClick={() => setShow(false)}>Vazgeç</button>
        <button className="btn primary sm" disabled={!promisedDate || !promisedAmount || createMut.isPending} onClick={save}>Kaydet</button>
      </div>
    </div>
  );
}
