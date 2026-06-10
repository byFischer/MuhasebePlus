import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import Drawer from '@/components/mp/Drawer';
import { TRY } from '@/lib/format';
import { useInvoices, useInvoice, useCreateInvoice, useDeleteInvoice, useConfirmInvoice, useCancelInvoice, useCreateReturnInvoice, useInvoiceSeries, useCreateInvoiceSeries, useNextInvoiceNumber } from '@/hooks/useInvoices';
import { useCustomers } from '@/hooks/useCustomers';
import { useProducts } from '@/hooks/useProducts';
import { useStockMovements } from '@/hooks/useStock';
import { MOVEMENT_LABELS, MOVEMENT_TYPE_COLORS } from '@/lib/movementLabels';
import { useSearchParams } from 'react-router-dom';
import { toast } from '@/lib/toast';
import invoiceService from '@/services/invoiceService';
import { useWithholdingCodes, useExemptionCodes } from '@/hooks/useTaxCodes';

function InvoicePill({ status, cancelled }) {
  if (cancelled) return <span className="pill neg"><span className="dot" />İptal</span>;
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
  const confirmMut = useConfirmInvoice();
  const cancelMut = useCancelInvoice();
  const { data: seriesList = [] } = useInvoiceSeries();
  const createSeriesMut = useCreateInvoiceSeries();
  const [q, setQ] = useState('');
  const [tab, setTab] = useState('hepsi');
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState(false);
  const [returnDrawer, setReturnDrawer] = useState(null);
  const [gibOpen, setGibOpen] = useState(false);
  const [seriesOpen, setSeriesOpen] = useState(false);
  const [detailInvoiceId, setDetailInvoiceId] = useState(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const PAGE_SIZE = 15;

  useEffect(() => {
    const invId = searchParams.get('invoiceId');
    if (invId) {
      setDetailInvoiceId(Number(invId));
    }
  }, [searchParams]);

  const closeDetail = () => {
    setDetailInvoiceId(null);
    if (searchParams.get('invoiceId')) {
      const next = new URLSearchParams(searchParams);
      next.delete('invoiceId');
      setSearchParams(next, { replace: true });
    }
  };

  const handleCancel = (inv) => {
    const reason = prompt('İptal sebebi:');
    if (!reason) return;
    cancelMut.mutate({ id: inv.invoiceId, reason });
  };

  const handleDownloadPdf = async (invoiceId) => {
    try {
      const blob = await invoiceService.downloadPdf(invoiceId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'fatura-' + invoiceId + '.pdf';
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      toast.err('PDF indirilemedi');
    }
  };

  const handlePrintPdf = async (invoiceId) => {
    try {
      const blob = await invoiceService.downloadPdf(invoiceId);
      const url = URL.createObjectURL(blob);
      const w = window.open(url, '_blank');
      if (w) {
        w.onload = () => { w.print(); };
      }
    } catch (e) {
      toast.err('PDF yazdırılamadı');
    }
  };

  const handleSendToGib = async (invoiceId) => {
    try {
      const log = await invoiceService.sendToGib(invoiceId);
      toast.ok('E-Arşiv gönderildi' + (log.envelopeId ? ' ETTN: ' + log.envelopeId : ''));
    } catch (e) {
      toast.err(e?.response?.data?.message || 'E-Arşiv gönderilemedi');
    }
  };

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
          <button className="btn ghost" onClick={() => setSeriesOpen(true)}><Icon name="grip" size={14} /> Seriler</button>
          <button className="btn ghost" onClick={() => setGibOpen(true)}><Icon name="globe" size={14} /> GİB</button>
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
              <tr><th>Fatura No</th><th>Müşteri</th><th>Tarih</th><th>Vade</th><th className="num">Toplam</th><th>Durum</th><th></th></tr>
            </thead>
            <tbody>
              {paged.map(i => (
                <tr key={i.invoiceId} style={{ cursor: 'pointer', opacity: i.cancelled ? 0.5 : 1 }} onClick={() => setDetailInvoiceId(i.invoiceId)}>
                  <td className="mono">{i.invoiceNumber}{i.cancelled ? <span className="pill neg" style={{ marginLeft: 4, fontSize: 10 }}>İPTAL</span> : ''}</td>
                  <td><b>{i.customerName}</b></td>
                  <td className="muted">{i.invoiceDate || i.dueDate}</td>
                  <td className="muted">{i.dueDate}</td>
                  <td className="num mono tnum"><b>{TRY(i.totalAmount || 0)}</b></td>
                  <td><InvoicePill status={i.paymentStatus} cancelled={i.cancelled} /></td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <div className="row gap-4">
                      {i.paymentStatus === 'draft' && (
                        <button className="tb-icon-btn" title="Onayla" onClick={() => confirmMut.mutate(i.invoiceId)}>
                          <Icon name="check" size={14} />
                        </button>
                      )}
                      {!i.cancelled && (i.paymentStatus === 'pending' || i.paymentStatus === 'overdue') && (
                        <button className="tb-icon-btn" title="İptal" onClick={() => handleCancel(i)}>
                          <Icon name="x" size={14} />
                        </button>
                      )}
                      {!i.cancelled && (
                        <button className="tb-icon-btn" title="İade" onClick={() => setReturnDrawer(i)}>
                          <Icon name="refresh" size={14} />
                        </button>
                      )}
                      <button className="tb-icon-btn" title="Detay" onClick={() => setDetailInvoiceId(i.invoiceId)}>
                        <Icon name="eye" size={14} />
                      </button>
                      <button className="tb-icon-btn" title="PDF İndir" onClick={(e) => { e.stopPropagation(); handleDownloadPdf(i.invoiceId); }}>
                        <Icon name="download" size={14} />
                      </button>
                      <button className="tb-icon-btn" title="Yazdır" onClick={(e) => { e.stopPropagation(); handlePrintPdf(i.invoiceId); }}>
                        <Icon name="print" size={14} />
                      </button>
                      {!i.cancelled && (
                        <button className="tb-icon-btn" title="E-Arşiv Gönder" onClick={(e) => { e.stopPropagation(); handleSendToGib(i.invoiceId); }}>
                          <Icon name="upload" size={14} />
                        </button>
                      )}
                      <button className="tb-icon-btn" title="Sil" onClick={() => deleteMut.mutate(i.invoiceId)}><Icon name="trash" size={14} /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {paged.length === 0 && <tr><td colSpan="7" className="empty">Sonuç bulunamadı</td></tr>}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={totalPages} setPage={setPage} pageStart={pageStart} pageEnd={pageEnd} total={filtered.length} />
      </div>
      <InvoiceDrawer open={drawer} onClose={() => setDrawer(false)} customers={customers} products={products} seriesList={seriesList} />
      <InvoiceDetailDrawer open={detailInvoiceId != null} onClose={closeDetail} invoiceId={detailInvoiceId} />
      {returnDrawer && (
        <ReturnInvoiceDrawer original={returnDrawer} customers={customers} onClose={() => setReturnDrawer(null)} />
      )}
      {gibOpen && <GibProfileModal onClose={() => setGibOpen(false)} />}
      {seriesOpen && <SeriesModal seriesList={seriesList} createMut={createSeriesMut} onClose={() => setSeriesOpen(false)} />}
    </div>
  );
}

function InvoiceDetailDrawer({ open, onClose, invoiceId }) {
  const { data: invoice, isLoading } = useInvoice(invoiceId);
  const lineItems = invoice?.lineItems || [];

  return (
    <Drawer open={open} onClose={onClose} title={`Fatura #${invoiceId || ''}`} width={720}>
      {isLoading && <div className="muted">Yükleniyor...</div>}
      {!isLoading && invoice && (
        <div className="col gap-12">
          <div className="card" style={{ padding: 12, background: 'var(--bg-2)' }}>
            <div className="grid-3">
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Fatura No</span><b className="mono">{invoice.invoiceNumber || '—'}</b></div>
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Müşteri</span><b>{invoice.customerName || '—'}</b></div>
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Tür</span><b>{invoice.invoiceType === 'purchase' ? 'Alış' : invoice.invoiceType === 'sale' ? 'Satış' : invoice.invoiceType || '—'}</b></div>
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Fatura Tarihi</span><b className="mono">{invoice.invoiceDate || '—'}</b></div>
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Vade</span><b className="mono">{invoice.dueDate || '—'}</b></div>
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Vade Tutarı</span><b className="mono tnum">{TRY(invoice.totalAmount || 0)}</b></div>
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Durum</span><div><InvoicePill status={invoice.paymentStatus} cancelled={invoice.cancelled} /></div></div>
              {invoice.currency && invoice.currency !== 'TRY' && (
                <><div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Döviz</span><b>{invoice.currency}</b></div>
                <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Kur</span><b className="mono">{invoice.exchangeRate || '1.00'}</b></div>
                <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>TRY</span><b className="mono tnum">{TRY(invoice.totalAmountTry || invoice.totalAmount)}</b></div></>
              )}
              <div className="col gap-4"><span className="muted" style={{ fontSize: 11 }}>Toplam</span><b className="mono tnum" style={{ fontSize: 16 }}>{TRY(invoice.totalAmount || 0)}</b></div>
            </div>
            {(invoice.discountAmount > 0 || invoice.withholdingTaxAmount > 0) && (
              <div className="row gap-12" style={{ marginTop: 8 }}>
                {invoice.discountAmount > 0 && <span className="pill warn">İskonto: {TRY(invoice.discountAmount)}</span>}
                {invoice.withholdingTaxAmount > 0 && <span className="pill warn">Stopaj: {TRY(invoice.withholdingTaxAmount)}</span>}
              </div>
            )}
            {invoice.description && <div style={{ marginTop: 8, fontSize: 12, color: 'var(--ink-2)' }}>{invoice.description}</div>}
            {invoice.cancelled && <div style={{ marginTop: 8, fontSize: 12, color: 'var(--neg)' }}>İptal Sebebi: {invoice.cancellationReason || 'Belirtilmedi'}</div>}
            {invoice.referenceInvoiceId && <div style={{ marginTop: 4, fontSize: 12, color: 'var(--info)' }}>İade Faturası — Orijinal: #{invoice.referenceInvoiceId}</div>}
          </div>

          <div>
            <h4 style={{ marginBottom: 8 }}>Kalemler</h4>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Ürün</th><th className="num">Adet</th><th className="num">Birim Fiyat</th><th className="num">İsk.%</th><th className="num">KDV%</th><th className="num">Stopaj%</th><th className="num">Tutar</th></tr>
                </thead>
                <tbody>
                  {lineItems.map(li => (
                    <tr key={li.lineItemId}>
                      <td><b>{li.productName || `#${li.productId}`}</b>{li.barcode && <span className="muted mono" style={{ fontSize: 11, display: 'block' }}>{li.barcode}</span>}</td>
                      <td className="num mono tnum">{li.quantity}</td>
                      <td className="num mono tnum">{TRY(li.unitPrice || 0)}</td>
                      <td className="num mono tnum muted">{li.discountRate > 0 ? `%${li.discountRate}` : '—'}</td>
                      <td className="num mono tnum muted">%{li.vatRate ?? 0}</td>
                      <td className="num mono tnum muted">{li.withholdingTaxRate > 0 ? `%${li.withholdingTaxRate}` : '—'}</td>
                      <td className="num mono tnum"><b>{TRY(li.lineTotal || 0)}</b></td>
                    </tr>
                  ))}
                  {lineItems.length === 0 && <tr><td colSpan="7" className="empty">Kalem yok</td></tr>}
                </tbody>
              </table>
            </div>
            <div className="row" style={{ justifyContent: 'flex-end', marginTop: 8, gap: 24, fontSize: 13 }}>
              <span className="muted">Ara Toplam: <span className="mono tnum">{TRY(invoice.subtotal || 0)}</span></span>
              {invoice.discountAmount > 0 && <span className="muted">İskonto: <span className="mono tnum" style={{ color: 'var(--neg)' }}>-{TRY(invoice.discountAmount)}</span></span>}
              <span className="muted">KDV: <span className="mono tnum">{TRY(invoice.vatAmount || 0)}</span></span>
              {invoice.withholdingTaxAmount > 0 && <span className="muted">Stopaj: <span className="mono tnum" style={{ color: 'var(--neg)' }}>-{TRY(invoice.withholdingTaxAmount)}</span></span>}
              <span><b>Toplam: <span className="mono tnum">{TRY(invoice.totalAmount || 0)}</span></b></span>
            </div>
          </div>

          <RelatedStockMovements invoiceId={invoiceId} lineItems={lineItems} />
        </div>
      )}
    </Drawer>
  );
}

function RelatedStockMovements({ invoiceId, lineItems }) {
  // Fatura kalem'lerinden ürün ID listesi - her birinin hareketlerini çekip filtreliyoruz.
  // Pratikte bir fatura için stok hareketi az olduğundan ilk ürünün hareketlerini yüklemek yeterli;
  // ama tüm kalemler için ayrı sorgu yapmak istemiyorsak basit bir özet gösterelim.
  const productIds = (lineItems || []).map(li => li.productId).filter(Boolean);
  const firstId = productIds[0] || null;
  const { data: movements = [] } = useStockMovements(firstId);
  const related = movements.filter(m => m.sourceType === 'INVOICE' && m.sourceId === invoiceId);

  return (
    <div>
      <h4 style={{ marginBottom: 8 }}>İlgili Stok Hareketleri</h4>
      {productIds.length > 1 && (
        <div className="muted" style={{ fontSize: 11, marginBottom: 6 }}>
          Bu listede sadece ilk kalemle ilişkili hareketler gösterilir. Tüm hareketler için stok sayfasındaki ürün geçmişlerine bakın.
        </div>
      )}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Tarih</th>
              <th>Tür</th>
              <th className="num">Miktar</th>
              <th>Ürün</th>
            </tr>
          </thead>
          <tbody>
            {related.map(m => {
              const label = MOVEMENT_LABELS[m.movementType] || m.movementType;
              const color = MOVEMENT_TYPE_COLORS[m.movementType] || '';
              const qty = m.quantity;
              const qtyStr = qty > 0 ? `+${qty}` : `${qty}`;
              const li = (lineItems || []).find(x => x.productId === m.productId);
              return (
                <tr key={m.movementId}>
                  <td className="muted" style={{ fontSize: 12 }}>{m.createdAt ? m.createdAt.slice(0, 10) : '—'}</td>
                  <td><span className={`pill ${color}`}><span className="dot" />{label}</span></td>
                  <td className="num mono tnum" style={{ color: qty > 0 ? 'var(--pos)' : 'var(--neg)', fontWeight: 600 }}>{qtyStr}</td>
                  <td className="muted" style={{ fontSize: 12 }}>{li?.productName || `#${m.productId}`}</td>
                </tr>
              );
            })}
            {related.length === 0 && <tr><td colSpan="4" className="empty">Bu fatura için stok hareketi bulunamadı</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const EMPTY_LINE = () => ({
  productId: '',
  quantity: 1,
  newProduct: null,
  showNewProduct: false,
  discountRate: '',
  withholdingTaxRate: '',
  withholdingTaxCodeId: null,
  vatExemptionCodeId: null,
  vatExemptionReason: '',
});

function InvoiceDrawer({ open, onClose, customers, products, seriesList }) {
  const createMut = useCreateInvoice();
  const nextNumMut = useNextInvoiceNumber();
  const { data: withholdingCodes = [] } = useWithholdingCodes();
  const { data: exemptionCodes = [] } = useExemptionCodes();
  const EMPTY = { invoiceNumber: '', customerId: '', invoiceType: 'sale', invoiceDate: new Date().toISOString().slice(0, 10), dueDate: '', currency: 'TRY', exchangeRate: '', discountType: '', discountAmount: '', description: '', deliveryAddress: '', seriesCode: '' };
  const [f, setF] = useState(EMPTY);
  const [lines, setLines] = useState([EMPTY_LINE()]);

  useEffect(() => {
    if (!open) { setF(EMPTY); setLines([EMPTY_LINE()]); }
  }, [open]);

  const isPurchase = f.invoiceType === 'purchase';
  const activeSeries = (seriesList || []).filter(s => s.invoiceType === f.invoiceType && s.isActive);

  const handleNextNumber = async () => {
    if (!f.seriesCode) return;
    try {
      const num = await nextNumMut.mutateAsync({ invoiceType: f.invoiceType, seriesCode: f.seriesCode });
      setF(prev => ({ ...prev, invoiceNumber: num }));
    } catch (e) { toast.err(e?.response?.data?.message || 'Numara alınamadı'); }
  };

  const validLines = lines.filter(l => {
    if (l.newProduct) {
      return l.newProduct.barcode?.trim() && l.newProduct.name?.trim() && l.newProduct.unit?.trim()
        && Number(l.newProduct.salePrice) > 0 && Number(l.newProduct.costPrice) > 0
        && Number(l.quantity) >= 1;
    }
    return l.productId && Number(l.quantity) >= 1;
  });

  const valid = f.invoiceNumber.trim() && f.customerId && f.invoiceType && f.invoiceDate && f.dueDate && validLines.length > 0;

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
      invoiceDate: f.invoiceDate,
      dueDate: f.dueDate,
      currency: f.currency !== 'TRY' ? f.currency : null,
      exchangeRate: f.currency !== 'TRY' && f.exchangeRate ? Number(f.exchangeRate) : null,
      discountType: f.discountType || null,
      discountAmount: f.discountAmount ? Number(f.discountAmount) : null,
      description: f.description?.trim() || null,
      deliveryAddress: f.deliveryAddress?.trim() || null,
      lineItems: validLines.map(l => {
        const taxCodeObj = withholdingCodes.find(c => c.codeId === l.withholdingTaxCodeId);
        const computedRate = taxCodeObj ? Number(taxCodeObj.withholdingRate) * 100 : (l.withholdingTaxRate ? Number(l.withholdingTaxRate) : null);
        if (l.newProduct) {
          const np = l.newProduct;
          return {
            productId: null, quantity: Number(l.quantity),
            newProduct: { barcode: np.barcode.trim(), name: np.name.trim(), description: np.description?.trim() || undefined, unit: np.unit.trim(), salePrice: Number(np.salePrice), vatRate: Number(np.vatRate), costPrice: Number(np.costPrice), minQuantity: Number(np.minQuantity) || 0 },
            discountRate: l.discountRate ? Number(l.discountRate) : null,
            withholdingTaxRate: computedRate,
          };
        }
        return {
          productId: Number(l.productId), quantity: Number(l.quantity), newProduct: null,
          discountRate: l.discountRate ? Number(l.discountRate) : null,
          withholdingTaxRate: computedRate,
          withholdingTaxCodeId: l.withholdingTaxCodeId || null,
          vatExemptionCodeId: l.vatExemptionCodeId || null,
          vatExemptionReason: l.vatExemptionReason || null,
        };
      }),
    }, { onSuccess: onClose });
  };

  return (
    <Drawer open={open} onClose={onClose} closeOnBackdrop={false} title="Yeni Fatura" width={680}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid || createMut.isPending} onClick={save}>Kaydet</button>
        </>
      }>
      <div className="col gap-12">
        <div className="grid-3">
          <div className="field">
            <label>Seri</label>
            <div className="row gap-4">
              <select className="input" value={f.seriesCode || ''} onChange={e => setF({ ...f, seriesCode: e.target.value })}>
                <option value="">Seri seç...</option>
                {activeSeries.map(s => <option key={s.seriesId} value={s.seriesCode}>{s.seriesCode} ({s.lastSequenceNumber})</option>)}
              </select>
              {f.seriesCode && <button className="btn ghost sm" onClick={handleNextNumber} disabled={nextNumMut.isPending}><Icon name="arrowRight" size={12} /></button>}
            </div>
          </div>
          <div className="field">
            <label>Fatura No *</label>
            <input className="input mono" value={f.invoiceNumber} onChange={e => setF({ ...f, invoiceNumber: e.target.value.replace(/ /g, '-').slice(0, 25) })} placeholder="FTR-2024-001" maxLength={25} />
          </div>
          <div className="field">
            <label>Fatura Türü *</label>
            <select className="input" value={f.invoiceType} onChange={e => setF({ ...f, invoiceType: e.target.value })}>
              <option value="sale">Satış</option>
              <option value="purchase">Alış</option>
            </select>
          </div>
        </div>
        <div className="grid-3">
          <div className="field">
            <label>Müşteri *</label>
            <select className="input" value={f.customerId} onChange={e => setF({ ...f, customerId: e.target.value })}>
              <option value="">Müşteri seçin...</option>
              {customers.map(c => <option key={c.customerId} value={c.customerId}>{c.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Fatura Tarihi *</label>
            <input className="input" type="date" value={f.invoiceDate} onChange={e => setF({ ...f, invoiceDate: e.target.value })} />
          </div>
          <div className="field">
            <label>Vade Tarihi *</label>
            <input className="input" type="date" value={f.dueDate} onChange={e => setF({ ...f, dueDate: e.target.value })} />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Döviz</label>
            <select className="input" value={f.currency || 'TRY'} onChange={e => setF({ ...f, currency: e.target.value })}>
              <option value="TRY">TRY</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
            </select>
          </div>
          {f.currency !== 'TRY' && (
            <div className="field">
              <label>Kur</label>
              <input className="input mono" type="number" step="0.0001" value={f.exchangeRate} onChange={e => setF({ ...f, exchangeRate: e.target.value })} placeholder="örn: 32.50" />
            </div>
          )}
        </div>
        <div className="grid-2">
          <div className="field">
            <label>İskonto Türü</label>
            <select className="input" value={f.discountType || ''} onChange={e => setF({ ...f, discountType: e.target.value })}>
              <option value="">Yok</option>
              <option value="PERCENTAGE">Yüzde (%)</option>
              <option value="AMOUNT">Tutar (₺)</option>
            </select>
          </div>
          {f.discountType && (
            <div className="field">
              <label>İskonto {f.discountType === 'PERCENTAGE' ? '(%)' : '(₺)'}</label>
              <input className="input mono" type="number" min="0" value={f.discountAmount} onChange={e => setF({ ...f, discountAmount: e.target.value })} />
            </div>
          )}
        </div>
        <div className="field">
          <label>Açıklama</label>
          <input className="input" value={f.description || ''} onChange={e => setF({ ...f, description: e.target.value })} placeholder="Fatura açıklaması..." />
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
                  <div className="field" style={{ width: 70, margin: 0 }}>
                    {i === 0 && <label style={{ fontSize: 11 }}>Adet</label>}
                    <input className="input mono" type="number" min="1" value={l.quantity} onChange={e => updateLine(i, 'quantity', e.target.value)} />
                  </div>
                  <div className="field" style={{ width: 60, margin: 0 }}>
                    {i === 0 && <label style={{ fontSize: 11 }}>İsk.%</label>}
                    <input className="input mono" type="number" min="0" max="100" value={l.discountRate || ''} onChange={e => updateLine(i, 'discountRate', e.target.value)} placeholder="0" />
                  </div>
                  <div className="field" style={{ width: 130, margin: 0 }}>
                    {i === 0 && <label style={{ fontSize: 11 }}>Tevkifat Kodu</label>}
                    <select
                      className="input"
                      style={{ fontSize: 11 }}
                      value={l.withholdingTaxCodeId || ''}
                      onChange={e => {
                        const codeId = e.target.value ? Number(e.target.value) : null;
                        const code = withholdingCodes.find(c => c.codeId === codeId);
                        updateLine(i, 'withholdingTaxCodeId', codeId);
                        updateLine(i, 'withholdingTaxRate', code ? String(Number(code.withholdingRate) * 100) : '');
                      }}
                    >
                      <option value="">Tevkifat yok</option>
                      {withholdingCodes.map(c => (
                        <option key={c.codeId} value={c.codeId}>{c.code} — {c.numerator}/{c.denominator}</option>
                      ))}
                    </select>
                  </div>
                  <div className="field" style={{ width: 130, margin: 0 }}>
                    {i === 0 && <label style={{ fontSize: 11 }}>İstisna Kodu</label>}
                    <select
                      className="input"
                      style={{ fontSize: 11 }}
                      value={l.vatExemptionCodeId || ''}
                      onChange={e => updateLine(i, 'vatExemptionCodeId', e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">İstisna yok</option>
                      {exemptionCodes.map(c => (
                        <option key={c.codeId} value={c.codeId}>{c.code} — {c.description?.slice(0, 20)}</option>
                      ))}
                    </select>
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

function ReturnInvoiceDrawer({ original, customers, onClose }) {
  const returnMut = useCreateReturnInvoice();
  const [invoiceNumber, setInvoiceNumber] = useState('IADE-' + (original.invoiceNumber || original.invoiceId));

  const save = () => {
    returnMut.mutate({
      id: original.invoiceId,
      dto: {
        invoiceNumber: invoiceNumber.trim(),
        customerId: original.customerId,
        invoiceType: original.invoiceType,
        invoiceDate: new Date().toISOString().slice(0, 10),
        dueDate: new Date().toISOString().slice(0, 10),
        lineItems: [],
        deliveryAddress: null,
      },
    }, { onSuccess: onClose });
  };

  return (
    <Drawer open={true} onClose={onClose} closeOnBackdrop={false} title="İade Faturası" width={480}
      footer={<><button className="btn ghost" onClick={onClose}>Vazgeç</button><button className="btn primary" disabled={!invoiceNumber.trim() || returnMut.isPending} onClick={save}>İade Faturası Oluştur</button></>}>
      <div className="col gap-12">
        <div className="card" style={{ padding: 12, background: 'var(--bg-2)' }}>
          <p className="muted" style={{ fontSize: 12 }}>Orijinal Fatura: <b>{original.invoiceNumber}</b> ({original.invoiceType === 'sale' ? 'Satış' : 'Alış'})</p>
          <p className="muted" style={{ fontSize: 12 }}>Müşteri: <b>{original.customerName}</b></p>
          <p className="muted" style={{ fontSize: 12 }}>Toplam: <b className="tnum">{TRY(original.totalAmount || 0)}</b></p>
        </div>
        <div className="field">
          <label>İade Fatura No *</label>
          <input className="input mono" value={invoiceNumber} onChange={e => setInvoiceNumber(e.target.value.replace(/ /g, '-').slice(0, 50))} />
        </div>
        <div className="muted" style={{ fontSize: 12 }}>İade faturası, orijinal faturadaki tüm kalemleri ters yönde içerecektir.</div>
      </div>
    </Drawer>
  );
}

function GibProfileModal({ onClose }) {
  const [alias, setAlias] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const save = async () => {
    if (!alias.trim() || !password.trim()) return;
    setLoading(true);
    try {
      await invoiceService.saveGibProfile(alias.trim(), password.trim(), null);
      toast.ok('GİB profili kaydedildi');
      onClose();
    } catch (e) {
      toast.err(e?.response?.data?.message || 'Kaydedilemedi');
    }
    setLoading(false);
  };

  return (
    <div className="drawer-scrim" onClick={onClose}>
      <div className="drawer" style={{ maxWidth: 420, height: 'auto' }} onClick={e => e.stopPropagation()}>
        <div className="drawer-head">
          <h2>GİB E-Arşiv Profili</h2>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="drawer-body col gap-12">
          <div className="field">
            <label>GİB Kullanıcı Kodu (Alias)</label>
            <input className="input mono" value={alias} onChange={e => setAlias(e.target.value)} placeholder="örn: 3333333300" />
          </div>
          <div className="field">
            <label>Şifre</label>
            <input className="input mono" type="password" value={password} onChange={e => setPassword(e.target.value)} />
          </div>
          <p className="muted" style={{ fontSize: 11 }}>GİB test ortamı: efaturatest.efatura.gov.tr</p>
          <button className="btn primary" disabled={loading || !alias.trim() || !password.trim()} onClick={save}>
            {loading ? 'Kaydediliyor...' : 'Kaydet'}
          </button>
        </div>
      </div>
    </div>
  );
}

function SeriesModal({ seriesList, createMut, onClose }) {
  const [code, setCode] = useState('');
  const [type, setType] = useState('sale');
  const [prefix, setPrefix] = useState('');

  const save = () => {
    if (!code.trim()) return;
    createMut.mutate({
      seriesCode: code.trim().toUpperCase(),
      invoiceType: type,
      prefix: prefix.trim() || null,
      year: new Date().getFullYear(),
      isActive: true,
    }, { onSuccess: onClose });
  };

  return (
    <div className="drawer-scrim" onClick={onClose}>
      <div className="drawer" style={{ maxWidth: 480, height: 'auto' }} onClick={e => e.stopPropagation()}>
        <div className="drawer-head">
          <h2>Fatura Serileri</h2>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="drawer-body col gap-12">
          {(seriesList || []).length > 0 && (
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Kod</th><th>Tür</th><th>Son Sıra</th><th>Durum</th></tr></thead>
                <tbody>
                  {seriesList.map(s => (
                    <tr key={s.seriesId}>
                      <td className="mono"><b>{s.seriesCode}</b>{s.prefix && <span className="muted" style={{ fontSize: 10, marginLeft: 4 }}>({s.prefix})</span>}</td>
                      <td><span className="pill">{s.invoiceType === 'sale' ? 'Satış' : 'Alış'}</span></td>
                      <td className="mono">{s.lastSequenceNumber}</td>
                      <td><span className={`pill ${s.isActive ? 'pos' : ''}`}>{s.isActive ? 'Aktif' : 'Pasif'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div style={{ borderTop: '1px solid var(--line)', paddingTop: 12 }}>
            <label style={{ fontWeight: 600, fontSize: 13, marginBottom: 8, display: 'block' }}>Yeni Seri Ekle</label>
            <div className="row gap-8" style={{ alignItems: 'flex-end' }}>
              <div className="field" style={{ width: 100 }}>
                <label style={{ fontSize: 11 }}>Kod *</label>
                <input className="input mono" value={code} onChange={e => setCode(e.target.value.toUpperCase())} placeholder="A" maxLength={10} />
              </div>
              <div className="field" style={{ width: 100 }}>
                <label style={{ fontSize: 11 }}>Tür</label>
                <select className="input" value={type} onChange={e => setType(e.target.value)}>
                  <option value="sale">Satış</option>
                  <option value="purchase">Alış</option>
                </select>
              </div>
              <div className="field" style={{ width: 100 }}>
                <label style={{ fontSize: 11 }}>Önek</label>
                <input className="input mono" value={prefix} onChange={e => setPrefix(e.target.value)} placeholder="ops." maxLength={20} />
              </div>
              <button className="btn primary sm" disabled={!code.trim() || createMut.isPending} onClick={save}>
                {createMut.isPending ? '...' : 'Ekle'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
