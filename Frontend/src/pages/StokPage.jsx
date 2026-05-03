import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import Drawer from '@/components/mp/Drawer';
import { TRY } from '@/lib/format';
import { useProducts, useCreateProduct, useDeleteProduct } from '@/hooks/useProducts';

export default function StokPage() {
  const { data: list = [], isLoading, isError, refetch } = useProducts();
  const deleteMut = useDeleteProduct();
  const [q, setQ] = useState('');
  const [filter, setFilter] = useState('hepsi');
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState(false);
  const PAGE_SIZE = 15;

  const filtered = useMemo(() => list.filter(p => {
    if (filter === 'dusuk' && !((p.stockQuantity || p.quantity || 0) > 0 && (p.stockQuantity || p.quantity || 0) < (p.minStockLevel || p.minQuantity || 0))) return false;
    if (filter === 'tukenmis' && (p.stockQuantity || p.quantity || 0) !== 0) return false;
    if (filter === 'saglikli' && (p.stockQuantity || p.quantity || 0) < (p.minStockLevel || p.minQuantity || 0)) return false;
    return (p.name + (p.productId || '') + (p.barcode || '')).toLowerCase().includes(q.toLowerCase());
  }), [list, q, filter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [totalPages]);
  useEffect(() => { setPage(1); }, [q, filter]);
  const pageStart = (page - 1) * PAGE_SIZE;
  const pageEnd = Math.min(pageStart + PAGE_SIZE, filtered.length);
  const paged = filtered.slice(pageStart, pageEnd);

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError) return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div><h1 className="page-title">Stok Yönetimi</h1><p className="page-sub">{list.length} ürün</p></div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setDrawer(true)}><Icon name="plus" /> Yeni Ürün</button>
        </div>
      </div>
      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}>
            <Icon name="search" size={14} />
            <input placeholder="Ürün adı, barkod..." value={q} onChange={e => setQ(e.target.value)} />
          </div>
          <div className="seg">
            {[['hepsi', 'Hepsi'], ['saglikli', 'Sağlıklı'], ['dusuk', 'Düşük'], ['tukenmis', 'Tükenmiş']].map(([k, l]) =>
              <button key={k} className={filter === k ? 'on' : ''} onClick={() => setFilter(k)}>{l}</button>
            )}
          </div>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>ID</th><th>Ürün</th><th>Barkod</th><th className="num">Maliyet</th><th className="num">Satış</th><th>Stok</th><th></th></tr>
            </thead>
            <tbody>
              {paged.map(p => {
                const qty = p.stockQuantity ?? p.quantity ?? 0;
                const min = p.minStockLevel ?? p.minQuantity ?? 0;
                return (
                  <tr key={p.productId}>
                    <td className="mono">{p.productId}</td>
                    <td><b>{p.name}</b></td>
                    <td className="mono muted" style={{ fontSize: 12 }}>{p.barcode}</td>
                    <td className="num mono tnum muted">{TRY(p.costPrice || 0)}</td>
                    <td className="num mono tnum">{TRY(p.salePrice || 0)}</td>
                    <td>
                      <span style={{ color: qty === 0 ? 'var(--neg)' : qty < min ? 'var(--warn)' : 'var(--ink-2)', fontWeight: 600 }}>
                        {qty} {p.unit || 'adet'}
                      </span>
                    </td>
                    <td>
                      <button className="tb-icon-btn" onClick={() => deleteMut.mutate(p.productId)}><Icon name="trash" size={14} /></button>
                    </td>
                  </tr>
                );
              })}
              {paged.length === 0 && <tr><td colSpan="7" className="empty">Sonuç bulunamadı</td></tr>}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={totalPages} setPage={setPage} pageStart={pageStart} pageEnd={pageEnd} total={filtered.length} />
      </div>
      <ProductDrawer open={drawer} onClose={() => setDrawer(false)} />
    </div>
  );
}

function ProductDrawer({ open, onClose }) {
  const createMut = useCreateProduct();
  const EMPTY = { name: '', barcode: '', unit: 'adet', salePrice: '', costPrice: '', vatRate: '18', description: '', initialQuantity: '0', minQuantity: '0' };
  const [f, setF] = useState(EMPTY);

  const valid = f.name.trim() && f.barcode.trim() && f.unit.trim()
    && Number(f.salePrice) > 0 && Number(f.costPrice) > 0 && Number(f.vatRate) >= 0;

  useEffect(() => { if (!open) setF(EMPTY); }, [open]);

  const save = () => {
    if (!valid) return;
    createMut.mutate({
      name: f.name.trim(),
      barcode: f.barcode.trim(),
      unit: f.unit.trim(),
      salePrice: Number(f.salePrice),
      costPrice: Number(f.costPrice),
      vatRate: Number(f.vatRate),
      description: f.description.trim() || undefined,
      initialQuantity: Number(f.initialQuantity) || 0,
      minQuantity: Number(f.minQuantity) || 0,
    }, { onSuccess: onClose });
  };

  return (
    <Drawer open={open} onClose={onClose} title="Yeni Ürün"
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid || createMut.isPending} onClick={save}>Kaydet</button>
        </>
      }>
      <div className="col gap-12">
        <div className="grid-2">
          <div className="field">
            <label>Ürün Adı *</label>
            <input className="input" value={f.name} onChange={e => setF({ ...f, name: e.target.value })} placeholder="Ürün adı" />
          </div>
          <div className="field">
            <label>Barkod *</label>
            <input className="input mono" value={f.barcode} onChange={e => setF({ ...f, barcode: e.target.value })} placeholder="8690000000000" />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Birim *</label>
            <input className="input" value={f.unit} onChange={e => setF({ ...f, unit: e.target.value })} placeholder="adet, kg, lt..." />
          </div>
          <div className="field">
            <label>KDV Oranı (%) *</label>
            <input className="input mono" type="number" min="0" max="100" value={f.vatRate} onChange={e => setF({ ...f, vatRate: e.target.value })} />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Maliyet Fiyatı (₺) *</label>
            <input className="input mono" type="number" min="0.01" step="0.01" value={f.costPrice} onChange={e => setF({ ...f, costPrice: e.target.value })} placeholder="0.00" />
          </div>
          <div className="field">
            <label>Satış Fiyatı (₺) *</label>
            <input className="input mono" type="number" min="0.01" step="0.01" value={f.salePrice} onChange={e => setF({ ...f, salePrice: e.target.value })} placeholder="0.00" />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Başlangıç Stoku</label>
            <input className="input mono" type="number" min="0" value={f.initialQuantity} onChange={e => setF({ ...f, initialQuantity: e.target.value })} />
          </div>
          <div className="field">
            <label>Min. Stok Eşiği</label>
            <input className="input mono" type="number" min="0" value={f.minQuantity} onChange={e => setF({ ...f, minQuantity: e.target.value })} />
          </div>
        </div>
        <div className="field">
          <label>Açıklama</label>
          <textarea className="input" rows={2} value={f.description} onChange={e => setF({ ...f, description: e.target.value })} />
        </div>
      </div>
    </Drawer>
  );
}
