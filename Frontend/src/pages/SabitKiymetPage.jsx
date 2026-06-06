import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import {
  useFixedAssets, useCategories, useDepreciationSchedule,
  useCreateAsset, useUpdateAsset, useDeleteAsset,
  useCreateCategory, useRunDepreciation, useDisposeAsset,
} from '@/hooks/useFixedAssets';
import { useAuth } from '@/context/AuthContext';

const MONTH_NAMES = ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'];
const EMPTY_ASSET = { name: '', serialNumber: '', categoryId: '', acquisitionDate: '', acquisitionCost: '', salvageValue: '0' };
const EMPTY_CAT   = { name: '', depreciationMethod: 'STRAIGHT_LINE', usefulLifeMonths: 60, annualRate: '', assetAccountCode: '254', depreciationAccountCode: '257' };

export default function SabitKiymetPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const { data: assets = [], isLoading } = useFixedAssets();
  const { data: categories = [] } = useCategories();

  const [tab, setTab] = useState('assets');
  const [assetForm, setAssetForm]     = useState(null);   // null = closed, {} = new, {id,...} = edit
  const [catForm, setCatForm]         = useState(null);
  const [scheduleAssetId, setScheduleAssetId] = useState(null);
  const [depModal, setDepModal]       = useState(false);
  const [depYear, setDepYear]         = useState(new Date().getFullYear());
  const [depMonth, setDepMonth]       = useState(new Date().getMonth() + 1);
  const [disposeModal, setDisposeModal] = useState(null);
  const [disposeForm, setDisposeForm] = useState({ disposalDate: '', saleAmount: '', reason: '' });

  const createAsset   = useCreateAsset();
  const updateAsset   = useUpdateAsset();
  const deleteAsset   = useDeleteAsset();
  const createCat     = useCreateCategory();
  const runDep        = useRunDepreciation();
  const disposeAsset  = useDisposeAsset();

  const { data: schedule = [] } = useDepreciationSchedule(scheduleAssetId);

  const fmt = (v) => v != null ? Number(v).toLocaleString('tr-TR', { minimumFractionDigits: 2 }) : '—';

  const handleSaveAsset = () => {
    const payload = {
      ...assetForm,
      acquisitionCost: parseFloat(assetForm.acquisitionCost) || 0,
      salvageValue: parseFloat(assetForm.salvageValue) || 0,
      categoryId: assetForm.categoryId ? Number(assetForm.categoryId) : null,
    };
    if (assetForm.id) {
      updateAsset.mutate({ id: assetForm.id, data: payload }, { onSuccess: () => setAssetForm(null) });
    } else {
      createAsset.mutate(payload, { onSuccess: () => setAssetForm(null) });
    }
  };

  const handleSaveCat = () => {
    createCat.mutate({
      ...catForm,
      usefulLifeMonths: parseInt(catForm.usefulLifeMonths) || 60,
      annualRate: catForm.annualRate ? parseFloat(catForm.annualRate) : null,
    }, { onSuccess: () => setCatForm(null) });
  };

  const handleDispose = () => {
    disposeAsset.mutate({
      id: disposeModal.id,
      data: { ...disposeForm, saleAmount: disposeForm.saleAmount ? parseFloat(disposeForm.saleAmount) : null },
    }, { onSuccess: () => setDisposeModal(null) });
  };

  const statusBadge = (s) => {
    if (s === 'ACTIVE')   return <span className="badge pos">Aktif</span>;
    if (s === 'SOLD')     return <span className="badge">Satıldı</span>;
    if (s === 'DISPOSED') return <span className="badge neg">Hurdaya Ayrıldı</span>;
    return <span className="badge">{s}</span>;
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Sabit Kıymetler</h1>
          <p className="page-sub">Demirbaş sicil defteri ve amortisman takibi</p>
        </div>
        {isAdmin && (
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn ghost" onClick={() => setDepModal(true)}>
              <Icon name="flash" size={14} style={{ marginRight: 4 }} />
              Amortisman Çalıştır
            </button>
            <button className="btn primary" onClick={() => setAssetForm({ ...EMPTY_ASSET })}>
              <Icon name="plus" size={14} style={{ marginRight: 4 }} />
              Yeni Demirbaş
            </button>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 16, borderBottom: '1px solid var(--border)' }}>
        {[['assets', 'Demirbaşlar'], ['categories', 'Kategoriler'], ['schedule', 'Amortisman Tablosu']].map(([key, label]) => (
          <button key={key} className={`btn ghost${tab === key ? ' active' : ''}`}
            style={{ borderRadius: '4px 4px 0 0', borderBottom: tab === key ? '2px solid var(--accent)' : 'none' }}
            onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </div>

      {/* Assets Tab */}
      {tab === 'assets' && (
        isLoading ? <div className="empty">Yükleniyor…</div> : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Demirbaş Adı</th>
                  <th>Kategori</th>
                  <th>Alım Tarihi</th>
                  <th style={{ textAlign: 'right' }}>Maliyet</th>
                  <th style={{ textAlign: 'right' }}>Birikmiş Amor.</th>
                  <th style={{ textAlign: 'right' }}>Net Defter Değ.</th>
                  <th>Durum</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {assets.length === 0 && (
                  <tr><td colSpan={8} style={{ textAlign: 'center', color: 'var(--ink-3)' }}>Kayıt yok</td></tr>
                )}
                {assets.map(a => (
                  <tr key={a.id}>
                    <td>
                      <div style={{ fontWeight: 500 }}>{a.name}</div>
                      {a.serialNumber && <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>{a.serialNumber}</div>}
                    </td>
                    <td>{a.categoryName || '—'}</td>
                    <td>{a.acquisitionDate}</td>
                    <td style={{ textAlign: 'right' }}>{fmt(a.acquisitionCost)}</td>
                    <td style={{ textAlign: 'right' }}>{fmt(a.accumulatedDepreciation)}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(a.netBookValue)}</td>
                    <td>{statusBadge(a.status)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>
                        <button className="tb-icon-btn" title="Amortisman planı"
                          onClick={() => { setScheduleAssetId(a.id); setTab('schedule'); }}>
                          <Icon name="chart" size={14} />
                        </button>
                        {isAdmin && a.status === 'ACTIVE' && (
                          <>
                            <button className="tb-icon-btn" title="Düzenle"
                              onClick={() => setAssetForm({ ...a, categoryId: a.categoryId || '' })}>
                              <Icon name="edit" size={14} />
                            </button>
                            <button className="tb-icon-btn" title="Elden çıkar"
                              onClick={() => { setDisposeModal(a); setDisposeForm({ disposalDate: '', saleAmount: '', reason: '' }); }}>
                              <Icon name="x" size={14} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {/* Categories Tab */}
      {tab === 'categories' && (
        <div>
          {isAdmin && (
            <button className="btn primary" style={{ marginBottom: 12 }}
              onClick={() => setCatForm({ ...EMPTY_CAT })}>
              <Icon name="plus" size={13} style={{ marginRight: 4 }} />
              Yeni Kategori
            </button>
          )}
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Kategori Adı</th>
                  <th>Amortisman Yöntemi</th>
                  <th>Faydalı Ömür (Ay)</th>
                  <th>Yıllık Oran</th>
                  <th>Hesap Kodu</th>
                  <th>Amor. Hesabı</th>
                </tr>
              </thead>
              <tbody>
                {categories.length === 0 && (
                  <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--ink-3)' }}>Kategori yok</td></tr>
                )}
                {categories.map(c => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.depreciationMethod === 'DECLINING_BALANCE' ? 'Azalan Bakiyeler' : 'Normal (Doğrusal)'}</td>
                    <td>{c.usefulLifeMonths}</td>
                    <td>{c.annualRate ? `%${c.annualRate}` : '—'}</td>
                    <td>{c.assetAccountCode || '—'}</td>
                    <td>{c.depreciationAccountCode || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Schedule Tab */}
      {tab === 'schedule' && (
        <div>
          <div style={{ marginBottom: 12, display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ fontSize: 12.5, color: 'var(--ink-2)' }}>Demirbaş seçin:</span>
            <select className="input" style={{ width: 280 }}
              value={scheduleAssetId || ''}
              onChange={e => setScheduleAssetId(e.target.value ? Number(e.target.value) : null)}>
              <option value="">— Seçiniz —</option>
              {assets.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>
          </div>
          {scheduleAssetId && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Dönem</th>
                    <th style={{ textAlign: 'right' }}>Amortisman Tutarı</th>
                    <th style={{ textAlign: 'right' }}>Birikmiş Amortisman</th>
                    <th style={{ textAlign: 'right' }}>Net Defter Değeri</th>
                  </tr>
                </thead>
                <tbody>
                  {schedule.length === 0 && (
                    <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--ink-3)' }}>Henüz amortisman kaydı yok</td></tr>
                  )}
                  {schedule.map(l => (
                    <tr key={l.id}>
                      <td>{MONTH_NAMES[l.periodMonth - 1]} {l.periodYear}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(l.depreciationAmount)}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(l.accumulatedDepreciation)}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(l.netBookValue)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Asset Form Modal */}
      {assetForm && (
        <div className="scrim" onClick={() => setAssetForm(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="drawer-h">
              <h3>{assetForm.id ? 'Demirbaş Düzenle' : 'Yeni Demirbaş'}</h3>
              <button className="tb-icon-btn" onClick={() => setAssetForm(null)}><Icon name="x" size={14} /></button>
            </div>
            <div className="drawer-b" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label>Demirbaş Adı *</label>
                <input className="input" value={assetForm.name || ''} onChange={e => setAssetForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="field">
                <label>Seri No</label>
                <input className="input" value={assetForm.serialNumber || ''} onChange={e => setAssetForm(f => ({ ...f, serialNumber: e.target.value }))} />
              </div>
              <div className="field">
                <label>Kategori</label>
                <select className="input" value={assetForm.categoryId || ''} onChange={e => setAssetForm(f => ({ ...f, categoryId: e.target.value }))}>
                  <option value="">— Seçiniz —</option>
                  {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Alım Tarihi *</label>
                <input className="input" type="date" value={assetForm.acquisitionDate || ''} onChange={e => setAssetForm(f => ({ ...f, acquisitionDate: e.target.value }))} />
              </div>
              <div className="field">
                <label>Alım Maliyeti *</label>
                <input className="input" type="number" step="0.01" value={assetForm.acquisitionCost || ''} onChange={e => setAssetForm(f => ({ ...f, acquisitionCost: e.target.value }))} />
              </div>
              <div className="field">
                <label>Hurda Değeri</label>
                <input className="input" type="number" step="0.01" value={assetForm.salvageValue || '0'} onChange={e => setAssetForm(f => ({ ...f, salvageValue: e.target.value }))} />
              </div>
            </div>
            <div className="drawer-f">
              <button className="btn ghost" onClick={() => setAssetForm(null)}>İptal</button>
              <button className="btn primary" disabled={!assetForm.name || !assetForm.acquisitionDate || !assetForm.acquisitionCost}
                onClick={handleSaveAsset}>
                {createAsset.isPending || updateAsset.isPending ? 'Kaydediliyor…' : 'Kaydet'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Category Form Modal */}
      {catForm && (
        <div className="scrim" onClick={() => setCatForm(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="drawer-h">
              <h3>Yeni Kategori</h3>
              <button className="tb-icon-btn" onClick={() => setCatForm(null)}><Icon name="x" size={14} /></button>
            </div>
            <div className="drawer-b" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label>Kategori Adı *</label>
                <input className="input" value={catForm.name} onChange={e => setCatForm(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div className="field">
                <label>Amortisman Yöntemi</label>
                <select className="input" value={catForm.depreciationMethod} onChange={e => setCatForm(f => ({ ...f, depreciationMethod: e.target.value }))}>
                  <option value="STRAIGHT_LINE">Normal (Doğrusal)</option>
                  <option value="DECLINING_BALANCE">Azalan Bakiyeler</option>
                </select>
              </div>
              <div className="field">
                <label>Faydalı Ömür (Ay)</label>
                <input className="input" type="number" value={catForm.usefulLifeMonths} onChange={e => setCatForm(f => ({ ...f, usefulLifeMonths: e.target.value }))} />
              </div>
              <div className="field">
                <label>Yıllık Oran (%)</label>
                <input className="input" type="number" step="0.01" value={catForm.annualRate} onChange={e => setCatForm(f => ({ ...f, annualRate: e.target.value }))} placeholder="20" />
              </div>
              <div className="field">
                <label>Kıymet Hesap Kodu</label>
                <input className="input" value={catForm.assetAccountCode} onChange={e => setCatForm(f => ({ ...f, assetAccountCode: e.target.value }))} placeholder="254" />
              </div>
              <div className="field">
                <label>Amortisman Hesap Kodu</label>
                <input className="input" value={catForm.depreciationAccountCode} onChange={e => setCatForm(f => ({ ...f, depreciationAccountCode: e.target.value }))} placeholder="257" />
              </div>
            </div>
            <div className="drawer-f">
              <button className="btn ghost" onClick={() => setCatForm(null)}>İptal</button>
              <button className="btn primary" disabled={!catForm.name} onClick={handleSaveCat}>
                {createCat.isPending ? 'Kaydediliyor…' : 'Kaydet'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Run Depreciation Modal */}
      {depModal && (
        <div className="scrim" onClick={() => setDepModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 360 }}>
            <div className="drawer-h">
              <h3>Aylık Amortisman Çalıştır</h3>
              <button className="tb-icon-btn" onClick={() => setDepModal(false)}><Icon name="x" size={14} /></button>
            </div>
            <div className="drawer-b" style={{ display: 'flex', gap: 12 }}>
              <div className="field" style={{ flex: 1 }}>
                <label>Yıl</label>
                <input className="input" type="number" value={depYear} onChange={e => setDepYear(Number(e.target.value))} />
              </div>
              <div className="field" style={{ flex: 1 }}>
                <label>Ay</label>
                <select className="input" value={depMonth} onChange={e => setDepMonth(Number(e.target.value))}>
                  {MONTH_NAMES.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
                </select>
              </div>
            </div>
            <div className="drawer-f">
              <button className="btn ghost" onClick={() => setDepModal(false)}>İptal</button>
              <button className="btn primary" disabled={runDep.isPending}
                onClick={() => runDep.mutate({ year: depYear, month: depMonth }, { onSuccess: () => setDepModal(false) })}>
                {runDep.isPending ? 'Hesaplanıyor…' : 'Çalıştır'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dispose Modal */}
      {disposeModal && (
        <div className="scrim" onClick={() => setDisposeModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400 }}>
            <div className="drawer-h">
              <h3>Elden Çıkar: {disposeModal.name}</h3>
              <button className="tb-icon-btn" onClick={() => setDisposeModal(null)}><Icon name="x" size={14} /></button>
            </div>
            <div className="drawer-b" style={{ display: 'grid', gap: 12 }}>
              <div className="field">
                <label>Çıkış Tarihi *</label>
                <input className="input" type="date" value={disposeForm.disposalDate} onChange={e => setDisposeForm(f => ({ ...f, disposalDate: e.target.value }))} />
              </div>
              <div className="field">
                <label>Satış Tutarı (varsa)</label>
                <input className="input" type="number" step="0.01" value={disposeForm.saleAmount} onChange={e => setDisposeForm(f => ({ ...f, saleAmount: e.target.value }))} placeholder="0" />
              </div>
              <div className="field">
                <label>Açıklama</label>
                <input className="input" value={disposeForm.reason} onChange={e => setDisposeForm(f => ({ ...f, reason: e.target.value }))} />
              </div>
            </div>
            <div className="drawer-f">
              <button className="btn ghost" onClick={() => setDisposeModal(null)}>İptal</button>
              <button className="btn danger" disabled={!disposeForm.disposalDate || disposeAsset.isPending} onClick={handleDispose}>
                {disposeAsset.isPending ? 'İşleniyor…' : 'Elden Çıkar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
