import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import Drawer from '@/components/mp/Drawer';
import { TRY } from '@/lib/format';
import { useInvoices, useCreateInvoice, useDeleteInvoice } from '@/hooks/useInvoices';
import { useCustomers } from '@/hooks/useCustomers';
import { useProducts } from '@/hooks/useProducts';

function InvoicePill({ status }) {
  const map = {
    draft:   { cls: 'warn', l: 'Taslak' },
    pending: { cls: 'info', l: 'Beklemede' },
    partially_paid: { cls: 'warn', l: 'Kısmi Ödeme' },
    paid:    { cls: 'pos',  l: 'Ödendi' },
    overdue: { cls: 'neg',  l: 'Gecikmiş' },
  };
  const s = map[status] || { cls: '', l: status };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

export default function FaturaPage() {
  const { data: list = [], isLoading, isError, refetch } = useInvoices();
  const { data: customers = [] } = useCustomers();
  const { data: products = [] } = useProducts();
  const deleteMut = useDeleteInvoice();
  const [q, setQ] = useState('');
  const [tab, setTab] = useState('hepsi');
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState(false);
  const PAGE_SIZE = 15;

  const filtered = useMemo(() => list.filter(i => {
    if (tab === 'paid' && i.paymentStatus !== 'paid') return false;
    if (tab === 'unpaid' && i.paymentStatus === 'paid') return false;
    if (tab === 'pending' && i.paymentStatus !== 'pending') return false;
    if (q && !(String(i.invoiceId) + (i.customerName || '')).toLowerCase().includes(q.toLowerCase())) return false;
    return true;
  }), [list, q, tab]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [totalPages]);
  useEffect(() => { setPage(1); }, [q, tab]);
  const pageStart = (page - 1) * PAGE_SIZE;
  const pageEnd = Math.min(pageStart + PAGE_SIZE, filtered.length);
  const paged = filtered.slice(pageStart, pageEnd);

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError) return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div><h1 className="page-title">Fatura Yönetimi</h1><p className="page-sub">{list.length} fatura</p></div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setDrawer(true)}><Icon name="plus" /> Yeni Fatura</button>
        </div>
      </div>
      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}>
            <Icon name="search" size={14} />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Fatura no, müşteri ara..." />
          </div>
          <div className="seg">
            {[['hepsi', 'Hepsi'], ['paid', 'Ödendi'], ['unpaid', 'Ödenmedi'], ['pending', 'Beklemede']].map(([k, l]) =>
              <button key={k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{l}</button>
            )}
          </div>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>ID</th><th>Müşteri</th><th>Vade</th><th className="num">Toplam</th><th>Durum</th><th></th></tr>
            </thead>
            <tbody>
              {paged.map(i => (
                <tr key={i.invoiceId}>
                  <td className="mono">{i.invoiceId}</td>
                  <td><b>{i.customerName}</b></td>
                  <td className="muted">{i.dueDate || i.invoiceDate}</td>
                  <td className="num mono tnum"><b>{TRY(i.totalAmount || 0)}</b></td>
                  <td><InvoicePill status={i.paymentStatus} /></td>
                  <td>
                    <div className="row gap-4">
                      <button className="tb-icon-btn" title="Sil" onClick={() => deleteMut.mutate(i.invoiceId)}><Icon name="trash" size={14} /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {paged.length === 0 && <tr><td colSpan="6" className="empty">Sonuç bulunamadı</td></tr>}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={totalPages} setPage={setPage} pageStart={pageStart} pageEnd={pageEnd} total={filtered.length} />
      </div>
      <InvoiceDrawer open={drawer} onClose={() => setDrawer(false)} customers={customers} products={products} />
    </div>
  );
}

const EMPTY_LINE = () => ({ productId: '', quantity: 1, newProduct: null, showNewProduct: false });

function InvoiceDrawer({ open, onClose, customers, products }) {
  const createMut = useCreateInvoice();
  const EMPTY = { invoiceNumber: '', customerId: '', invoiceType: 'sale', dueDate: '' };
  const [f, setF] = useState(EMPTY);
  const [lines, setLines] = useState([EMPTY_LINE()]);

  useEffect(() => {
    if (!open) { setF(EMPTY); setLines([EMPTY_LINE()]); }
  }, [open]);

  const isPurchase = f.invoiceType === 'purchase';

  const validLines = lines.filter(l => {
    if (l.newProduct) {
      return l.newProduct.barcode?.trim() && l.newProduct.name?.trim() && l.newProduct.unit?.trim()
        && Number(l.newProduct.salePrice) > 0 && Number(l.newProduct.costPrice) > 0
        && Number(l.quantity) >= 1;
    }
    return l.productId && Number(l.quantity) >= 1;
  });

  const valid = f.invoiceNumber.trim() && f.customerId && f.invoiceType && f.dueDate && validLines.length > 0;

  const addLine = () => setLines(prev => [...prev, EMPTY_LINE()]);
  const removeLine = (i) => setLines(prev => prev.length > 1 ? prev.filter((_, j) => j !== i) : prev);
  const updateLine = (i, field, val) => setLines(prev => prev.map((l, j) => j === i ? { ...l, [field]: val } : l));
  const updateNewProduct = (i, field, val) => setLines(prev => prev.map((l, j) => j === i ? { ...l, newProduct: { ...l.newProduct, [field]: val } } : l));

  const save = () => {
    if (!valid) return;
    createMut.mutate({
      invoiceNumber: f.invoiceNumber.trim(),
      customerId: Number(f.customerId),
      invoiceType: f.invoiceType,
      dueDate: f.dueDate,
      lineItems: validLines.map(l => {
        if (l.newProduct) {
          const np = l.newProduct;
          return {
            productId: null,
            quantity: Number(l.quantity),
            newProduct: {
              barcode: np.barcode.trim(),
              name: np.name.trim(),
              description: np.description?.trim() || undefined,
              unit: np.unit.trim(),
              salePrice: Number(np.salePrice),
              vatRate: Number(np.vatRate),
              costPrice: Number(np.costPrice),
              minQuantity: Number(np.minQuantity) || 0,
            },
          };
        }
        return { productId: Number(l.productId), quantity: Number(l.quantity), newProduct: null };
      }),
    }, { onSuccess: onClose });
  };

  return (
    <Drawer open={open} onClose={onClose} closeOnBackdrop={false} title="Yeni Fatura" width={isPurchase ? 640 : 560}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid || createMut.isPending} onClick={save}>Kaydet</button>
        </>
      }>
      <div className="col gap-12">
        <div className="grid-2">
          <div className="field">
            <label>Fatura No *</label>
            <input className="input mono" value={f.invoiceNumber} onChange={e => setF({ ...f, invoiceNumber: e.target.value.replace(/ /g, '-').slice(0, 25) })} placeholder="FTR-2024-001" maxLength={25} />
          </div>
          <div className="field">
            <label>Fatura Türü *</label>
            <select className="input" value={f.invoiceType} onChange={e => setF({ ...f, invoiceType: e.target.value })}>
              <option value="sale">Satış Faturası</option>
              <option value="purchase">Alış Faturası</option>
            </select>
          </div>
        </div>
        <div className="field">
          <label>Müşteri *</label>
          <select className="input" value={f.customerId} onChange={e => setF({ ...f, customerId: e.target.value })}>
            <option value="">Müşteri seçin...</option>
            {customers.map(c => (
              <option key={c.customerId} value={c.customerId}>{c.name}</option>
            ))}
          </select>
          {customers.length === 0 && <span style={{ fontSize: 11, color: 'var(--warn)', marginTop: 4, display: 'block' }}>Önce bir müşteri ekleyin</span>}
        </div>
        <div className="field">
          <label>Vade Tarihi *</label>
          <input className="input" type="date" value={f.dueDate} onChange={e => setF({ ...f, dueDate: e.target.value })} />
        </div>

        <div>
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
            <label style={{ fontWeight: 600, fontSize: 13 }}>Kalemler *</label>
            <button className="btn ghost sm" onClick={addLine}><Icon name="plus" size={12} /> Kalem Ekle</button>
          </div>
          <div className="col gap-8">
            {lines.map((l, i) => (
              <div key={i} style={{ border: '1px solid var(--line)', borderRadius: 6, padding: 10 }}>
                <div className="row gap-8" style={{ alignItems: 'flex-end' }}>
                  <div className="field" style={{ flex: 1, margin: 0 }}>
                    {i === 0 && <label style={{ fontSize: 11 }}>Ürün</label>}
                    {isPurchase && !l.newProduct && (
                      <button
                        className="btn ghost sm"
                        style={{ marginBottom: 4, fontSize: 11 }}
                        onClick={() => {
                          updateLine(i, 'showNewProduct', true);
                          if (!l.newProduct) updateLine(i, 'newProduct', { barcode: '', name: '', unit: 'adet', salePrice: '', costPrice: '', vatRate: '18', minQuantity: '0' });
                        }}
                      >
                        <Icon name="plus" size={10} /> Yeni ürün ekle
                      </button>
                    )}
                    {l.showNewProduct && isPurchase ? (
                      <div className="col gap-4">
                        <div className="grid-2">
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>Barkod *</label>
                            <input
                              className="input mono"
                              value={l.newProduct?.barcode || ''}
                              onChange={e => updateNewProduct(i, 'barcode', e.target.value.replace(/\D/g, '').slice(0, 13))}
                              placeholder="13 haneli barkod"
                              maxLength={13}
                            />
                          </div>
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>Ürün Adı *</label>
                            <input className="input" value={l.newProduct?.name || ''} onChange={e => updateNewProduct(i, 'name', e.target.value)} placeholder="Ürün adı" />
                          </div>
                        </div>
                        <div className="grid-3">
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>Birim *</label>
                            <input className="input" value={l.newProduct?.unit || 'adet'} onChange={e => updateNewProduct(i, 'unit', e.target.value)} />
                          </div>
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>Maliyet *</label>
                            <input className="input mono" type="number" min="0.01" step="0.01" value={l.newProduct?.costPrice || ''} onChange={e => updateNewProduct(i, 'costPrice', e.target.value)} placeholder="0.00" />
                          </div>
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>Satış *</label>
                            <input className="input mono" type="number" min="0.01" step="0.01" value={l.newProduct?.salePrice || ''} onChange={e => updateNewProduct(i, 'salePrice', e.target.value)} placeholder="0.00" />
                          </div>
                        </div>
                        <div className="grid-3">
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>KDV % *</label>
                            <input className="input mono" type="number" min="0" max="100" value={l.newProduct?.vatRate || '18'} onChange={e => updateNewProduct(i, 'vatRate', e.target.value)} />
                          </div>
                          <div className="field" style={{ margin: 0 }}>
                            <label style={{ fontSize: 11 }}>Min. Stok</label>
                            <input className="input mono" type="number" min="0" value={l.newProduct?.minQuantity || '0'} onChange={e => updateNewProduct(i, 'minQuantity', e.target.value)} />
                          </div>
                          <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                            <button className="btn ghost sm" onClick={() => { updateLine(i, 'showNewProduct', false); updateLine(i, 'newProduct', null); }}>İptal</button>
                          </div>
                        </div>
                      </div>
                    ) : (
                      <select className="input" value={l.productId} onChange={e => updateLine(i, 'productId', e.target.value)}>
                        <option value="">Ürün seçin...</option>
                        {products.map(p => (
                          <option key={p.productId} value={p.productId}>{p.name} — {TRY(p.salePrice)}</option>
                        ))}
                      </select>
                    )}
                  </div>
                  <div className="field" style={{ width: 80, margin: 0 }}>
                    {i === 0 && <label style={{ fontSize: 11 }}>Adet</label>}
                    <input
                      className="input mono"
                      type="number"
                      min="1"
                      value={l.quantity}
                      onChange={e => updateLine(i, 'quantity', e.target.value)}
                    />
                  </div>
                  <button
                    className="tb-icon-btn"
                    style={{ marginBottom: 1 }}
                    onClick={() => removeLine(i)}
                    disabled={lines.length === 1}
                  >
                    <Icon name="trash" size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
          {!isPurchase && products.length === 0 && (
            <span style={{ fontSize: 11, color: 'var(--warn)', marginTop: 4, display: 'block' }}>Önce stok sayfasından ürün ekleyin</span>
          )}
        </div>
      </div>
    </Drawer>
  );
}
