import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import Drawer from '@/components/mp/Drawer';
import { DropdownFilter, CityFilter } from '@/components/mp/DropdownFilter';
import { TRY } from '@/lib/format';
import { validateEmail, validateTaxNumberByType } from '@/lib/validators';
import { useCustomers, useCreateCustomer, useUpdateCustomer, useDeleteCustomer, useImportCustomers } from '@/hooks/useCustomers';
import { toast } from '@/lib/toast';
import customerService from '@/services/customerService';

export default function CariPage() {
  const emptyForm = {
    name: '', email: '', phoneNumber: '', taxNumber: '', city: '', address: '', type: 'INDIVIDUAL',
    accountCode: '', openingBalance: '', openingBalanceDate: '', taxOffice: '', identityNumber: '',
    iban: '', currency: 'TRY', creditLimit: '', customerRole: 'BOTH', status: 'ACTIVE', customerGroup: '',
  };

  const { data: list = [], isLoading, isError, refetch } = useCustomers();
  const createMut = useCreateCustomer();
  const updateMut = useUpdateCustomer();
  const deleteMut = useDeleteCustomer();
  const importMut = useImportCustomers();

  const [form, setForm] = useState(emptyForm);
  const [q, setQ] = useState('');
  const [filter, setFilter] = useState('hepsi');
  const [roleFilter, setRoleFilter] = useState('hepsi');
  const [statusFilter, setStatusFilter] = useState('hepsi');
  const [groupFilter, setGroupFilter] = useState('hepsi');
  const [city, setCity] = useState('hepsi');
  const [cityOpen, setCityOpen] = useState(false);
  const [drawer, setDrawer] = useState(null);
  const [sel, setSel] = useState(null);
  const [page, setPage] = useState(1);
  const [importOpen, setImportOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const PAGE_SIZE = 15;

  const cities = useMemo(() => ['hepsi', ...Array.from(new Set(list.map(c => c.city).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'tr'))], [list]);

  const filtered = useMemo(() => list.filter(c => {
    if (filter === 'borclu' && (c.currentBalance || 0) <= 0) return false;
    if (filter === 'alacakli' && (c.currentBalance || 0) >= 0) return false;
    if (filter === 'risk' && !c.hasOverdueInvoices) return false;
    if (roleFilter === 'BUYER' && c.customerRole !== 'BUYER') return false;
    if (roleFilter === 'SELLER' && c.customerRole !== 'SELLER') return false;
    if (statusFilter !== 'hepsi' && c.status !== statusFilter) return false;
    if (groupFilter !== 'hepsi' && c.customerGroup !== groupFilter) return false;
    if (city !== 'hepsi' && c.city !== city) return false;
    if (q && !(c.name + (c.customerId || '') + (c.taxNumber || '') + (c.accountCode || '')).toLowerCase().includes(q.toLowerCase())) return false;
    return true;
  }), [list, q, filter, roleFilter, statusFilter, groupFilter, city]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [totalPages]);
  useEffect(() => { setPage(1); }, [q, filter, roleFilter, statusFilter, groupFilter, city]);
  const pageStart = (page - 1) * PAGE_SIZE;
  const pageEnd = Math.min(pageStart + PAGE_SIZE, filtered.length);
  const paged = filtered.slice(pageStart, pageEnd);

  const doImport = async () => {
    if (!importFile) return;
    try {
      const res = await importMut.mutateAsync(importFile);
      setImportResult(res);
    } catch (e) {
      toast.err(e?.response?.data?.message || 'İçe aktarma başarısız');
    }
  };

  const handleExportExcel = async () => {
    try {
      const blob = await customerService.exportExcel();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'musteri-listesi.xlsx';
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      toast.err('Excel aktarılamadı');
    }
  };

  const handleExportPdf = async () => {
    try {
      const blob = await customerService.exportPdf();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
    } catch (e) {
      toast.err('PDF aktarılamadı');
    }
  };

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError) return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Cari Yönetimi</h1>
          <p className="page-sub">{list.length} müşteri/tedarikçi</p>
        </div>
        <div className="page-actions">
          <button className="btn ghost" onClick={handleExportExcel}><Icon name="download" size={14} /> Excel</button>
          <button className="btn ghost" onClick={handleExportPdf}><Icon name="print" size={14} /> PDF</button>
          <button className="btn ghost" onClick={() => { setImportOpen(true); setImportFile(null); setImportResult(null); }}><Icon name="upload" size={14} /> İçe Aktar</button>
          <button className="btn primary" onClick={() => setDrawer({ mode: 'new' })}><Icon name="plus" /> Yeni Müşteri</button>
        </div>
      </div>
      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}><Icon name="search" size={14} /><input placeholder="Müşteri, VKN, Hesap Kodu ara..." value={q} onChange={e => setQ(e.target.value)} /></div>
          <div className="seg">
            {[['hepsi', 'Hepsi'], ['borclu', 'Borçlu'], ['alacakli', 'Alacaklı'], ['risk', 'Riskli']].map(([k, l]) =>
              <button key={k} className={filter === k ? 'on' : ''} onClick={() => setFilter(k)}>{l}</button>
            )}
          </div>
          <div className="seg">
            {[['hepsi', 'Tümü'], ['BUYER', 'Alıcı'], ['SELLER', 'Satıcı']].map(([k, l]) =>
              <button key={k} className={roleFilter === k ? 'on' : ''} onClick={() => setRoleFilter(k)}>{l}</button>
            )}
          </div>
          <div className="seg">
            {[['hepsi', 'Tümü'], ['ACTIVE', 'Aktif'], ['PASSIVE', 'Pasif'], ['BLOCKED', 'Bloke']].map(([k, l]) =>
              <button key={k} className={statusFilter === k ? 'on' : ''} onClick={() => setStatusFilter(k)}>{l}</button>
            )}
          </div>
          <CityFilter cities={cities} city={city} setCity={setCity} open={cityOpen} setOpen={setCityOpen} />
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Hesap Kodu</th><th>Müşteri</th><th>VKN/TCKN</th><th>Şehir</th><th>Bakiye</th><th>Grup</th><th>Durum</th><th></th></tr></thead>
            <tbody>
              {paged.map(c => (
                <tr key={c.customerId} className={sel === c.customerId ? 'sel' : ''} onClick={() => setSel(c.customerId)}>
                  <td className="mono">{c.accountCode || '-'}</td>
                  <td><b>{c.name}</b>{c.email && <div className="muted" style={{ fontSize: 11 }}>{c.email}</div>}</td>
                  <td className="mono">{c.taxNumber}</td>
                  <td className="muted">{c.city}</td>
                  <td className="mono"><span style={{ color: (c.currentBalance || 0) < 0 ? 'var(--neg)' : (c.currentBalance || 0) > 0 ? 'var(--pos)' : 'var(--ink-2)' }}>{TRY(c.currentBalance)}</span></td>
                  <td>{c.customerGroup ? <span className="pill">{c.customerGroup}</span> : '-'}</td>
                  <td><StatusPill status={c.status} /></td>
                  <td>
                    <div className="row gap-4">
                      <button className="tb-icon-btn" onClick={e => { e.stopPropagation(); setDrawer({ mode: 'edit', c }); }}><Icon name="edit" size={14} /></button>
                      <button className="tb-icon-btn" onClick={e => { e.stopPropagation(); deleteMut.mutate(c.customerId); }}><Icon name="trash" size={14} /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {paged.length === 0 && <tr><td colSpan="8" className="empty">Sonuç bulunamadı</td></tr>}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={totalPages} setPage={setPage} pageStart={pageStart} pageEnd={pageEnd} total={filtered.length} />
      </div>
      <CustomerDrawer open={!!drawer} mode={drawer?.mode} customer={drawer?.c} onClose={() => setDrawer(null)}
        onSave={(dto) => {
          if (drawer?.mode === 'edit') updateMut.mutate({ id: drawer.c.customerId, dto });
          else createMut.mutate(dto);
          setDrawer(null);
        }} />

      {/* Toplu İçe Aktarma Modal */}
      {importOpen && (
        <div className="drawer-scrim" onClick={() => setImportOpen(false)}>
          <div className="drawer" style={{ maxWidth: 520, height: 'auto', maxHeight: '80vh', overflow: 'auto' }} onClick={e => e.stopPropagation()}>
            <div className="drawer-head">
              <h2>Toplu Müşteri İçe Aktar</h2>
              <button className="tb-icon-btn" onClick={() => setImportOpen(false)}><Icon name="x" /></button>
            </div>
            <div className="drawer-body col gap-12">
              <p className="muted" style={{ fontSize: 13 }}>.xlsx veya .csv dosyası yükleyin. Sütunlar: Ad, VKN, Adres, Şehir, Telefon, Tip(INDIVIDUAL/CORPORATE), Email, HesapKodu, AçılışBakiyesi, AçılışTarihi, VergiDairesi, TCKimlikNo, IBAN, Döviz(TRY/USD/EUR), KrediLimiti, Rol(BUYER/SELLER/BOTH)</p>
              <div className="field">
                <input type="file" accept=".xlsx,.csv" onChange={e => setImportFile(e.target.files[0])} />
              </div>
              {importFile && (
                <div className="col gap-8">
                  <div className="row gap-8" style={{ alignItems: 'center' }}>
                    <Icon name="file" size={14} style={{ color: 'var(--ink-3)' }} />
                    <span style={{ fontSize: 13 }}>{importFile.name}</span>
                  </div>
                  <button className="btn primary" onClick={doImport} disabled={importMut.isPending}>
                    {importMut.isPending ? 'Aktarılıyor...' : 'Aktar'}
                  </button>
                </div>
              )}
              {importResult && (
                <div className="col gap-8" style={{ marginTop: 8 }}>
                  <div className="row gap-12">
                    <span className="pill pos">✅ {importResult.successCount} başarılı</span>
                    <span className="pill neg">❌ {importResult.errorCount} hata</span>
                  </div>
                  {importResult.errors?.length > 0 && (
                    <div className="card" style={{ background: 'var(--surface)', maxHeight: 200, overflow: 'auto' }}>
                      {importResult.errors.map((err, i) => (
                        <div key={i} style={{ fontSize: 12, color: 'var(--neg)', padding: '4px 0', borderBottom: '1px solid var(--border)' }}>{err}</div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function StatusPill({ status }) {
  const map = {
    ACTIVE: { cls: 'pos', l: 'Aktif' },
    PASSIVE: { cls: '', l: 'Pasif' },
    BLOCKED: { cls: 'neg', l: 'Bloke' },
  };
  const s = map[status] || { cls: '', l: status || 'Bilinmiyor' };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

const TR_CITIES = [
  'Adana','Adıyaman','Afyonkarahisar','Ağrı','Aksaray','Amasya','Ankara','Antalya','Ardahan',
  'Artvin','Aydın','Balıkesir','Bartın','Batman','Bayburt','Bilecik','Bingöl','Bitlis','Bolu',
  'Burdur','Bursa','Çanakkale','Çankırı','Çorum','Denizli','Diyarbakır','Düzce','Edirne',
  'Elazığ','Erzincan','Erzurum','Eskişehir','Gaziantep','Giresun','Gümüşhane','Hakkari','Hatay',
  'Iğdır','Isparta','İstanbul','İzmir','Kahramanmaraş','Karabük','Karaman','Kars','Kastamonu',
  'Kayseri','Kilis','Kırıkkale','Kırklareli','Kırşehir','Kocaeli','Konya','Kütahya','Malatya',
  'Manisa','Mardin','Mersin','Muğla','Muş','Nevşehir','Niğde','Ordu','Osmaniye','Rize',
  'Sakarya','Samsun','Siirt','Sinop','Sivas','Şanlıurfa','Şırnak','Tekirdağ','Tokat','Trabzon',
  'Tunceli','Uşak','Van','Yalova','Yozgat','Zonguldak',
];

function CustomerDrawer({ open, mode, customer, onClose, onSave }) {
  const [c, setC] = useState({
    name: '', email: '', phoneNumber: '', taxNumber: '', city: '', address: '', type: 'INDIVIDUAL',
    accountCode: '', openingBalance: '', openingBalanceDate: '', taxOffice: '', identityNumber: '',
    iban: '', currency: 'TRY', creditLimit: '', customerRole: 'BOTH', status: 'ACTIVE', customerGroup: '',
  });

  useEffect(() => {
    setC(
      customer
        ? {
            ...customer, phoneNumber: customer.phoneNumber || customer.phone || '', taxNumber: customer.taxNumber || '', type: customer.type || 'INDIVIDUAL',
            accountCode: customer.accountCode || '', openingBalance: customer.openingBalance || '',
            openingBalanceDate: customer.openingBalanceDate || '', taxOffice: customer.taxOffice || '',
            identityNumber: customer.identityNumber || '', iban: customer.iban || '',
            currency: customer.currency || 'TRY', creditLimit: customer.creditLimit || '',
            customerRole: customer.customerRole || 'BOTH', status: customer.status || 'ACTIVE',
            customerGroup: customer.customerGroup || '',
          }
        : { name: '', email: '', phoneNumber: '', taxNumber: '', city: '', address: '', type: 'INDIVIDUAL', accountCode: '', openingBalance: '', openingBalanceDate: '', taxOffice: '', identityNumber: '', iban: '', currency: 'TRY', creditLimit: '', customerRole: 'BOTH', status: 'ACTIVE', customerGroup: '' }
    );
  }, [customer, open]);

  const taxLabel    = c.type === 'CORPORATE' ? 'VKN' : 'TCKN';
  const taxMaxLen   = c.type === 'CORPORATE' ? 10 : 11;
  const taxCheck    = validateTaxNumberByType(c.type, c.taxNumber || '');
  const emailCheck  = !c.email ? { ok: true } : validateEmail(c.email);
  const phoneCheck  = !c.phoneNumber ? { ok: true } : /^[1-9][0-9]{9}$/.test(c.phoneNumber.replace(/\s/g, '')) ? { ok: true } : { ok: false, msg: '10 haneli numara girin (örn: 5551234567)' };
  const tcCheck     = !c.identityNumber ? { ok: true } : /^\d{11}$/.test(c.identityNumber) ? { ok: true } : { ok: false, msg: '11 haneli TC Kimlik No girin' };

  const valid = !!c.name?.trim() && !!c.accountCode?.trim() && taxCheck.ok && emailCheck.ok && phoneCheck.ok && tcCheck.ok;

  const typeBtn = (label, val) => (
    <button
      type="button"
      onClick={() => setC({ ...c, type: val, taxNumber: '' })}
      style={{
        padding: '6px 18px', borderRadius: 6,
        border: c.type === val ? '1.5px solid var(--accent)' : '1.5px solid var(--border)',
        background: c.type === val ? 'var(--accent)' : 'transparent',
        color: c.type === val ? '#fff' : 'var(--text-2)',
        fontWeight: c.type === val ? 600 : 400, fontSize: 13, cursor: 'pointer', transition: 'all .15s',
      }}
    >{label}</button>
  );

  const roleBtn = (label, val) => (
    <button
      type="button"
      onClick={() => setC({ ...c, customerRole: val })}
      style={{
        padding: '4px 14px', borderRadius: 6,
        border: c.customerRole === val ? '1.5px solid var(--accent)' : '1.5px solid var(--border)',
        background: c.customerRole === val ? 'var(--accent)' : 'transparent',
        color: c.customerRole === val ? '#fff' : 'var(--text-2)',
        fontWeight: c.customerRole === val ? 600 : 400, fontSize: 12, cursor: 'pointer', transition: 'all .15s',
      }}
    >{label}</button>
  );

  return (
    <Drawer
      open={open} onClose={onClose} closeOnBackdrop={false}
      title={mode === 'edit' ? `Düzenle — ${customer?.name}` : 'Yeni Müşteri'}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid}
            onClick={() => onSave({
              name: c.name?.trim(), email: c.email?.trim() || null,
              phoneNumber: c.phoneNumber?.replace(/\s/g, '').trim() || null,
              taxNumber: c.taxNumber?.trim(), city: c.city || null, address: c.address?.trim() || null,
              type: c.type,
              accountCode: c.accountCode?.trim(), openingBalance: c.openingBalance ? Number(c.openingBalance) : 0,
              openingBalanceDate: c.openingBalanceDate || null, taxOffice: c.taxOffice?.trim() || null,
              identityNumber: c.identityNumber?.trim() || null, iban: c.iban?.trim() || null,
              currency: c.currency || 'TRY', creditLimit: c.creditLimit ? Number(c.creditLimit) : null,
              customerRole: c.customerRole || 'BOTH', status: c.status || 'ACTIVE',
              customerGroup: c.customerGroup?.trim() || null,
            })}
          >
            {mode === 'edit' ? 'Güncelle' : 'Kaydet'}
          </button>
        </>
      }
    >
      <div className="col gap-12">

        <div style={{ display: 'flex', gap: 8 }}>
          {typeBtn('Bireysel', 'INDIVIDUAL')}
          {typeBtn('Şirket', 'CORPORATE')}
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Hesap Kodu *</label>
            <input className="input mono" value={c.accountCode || ''} onChange={e => setC({ ...c, accountCode: e.target.value })} placeholder="örn: 320.01" />
          </div>
          <div className="field">
            <label>Firma/Ad *</label>
            <input className="input" value={c.name || ''} onChange={e => setC({ ...c, name: e.target.value })} />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Vergi Dairesi</label>
            <input className="input" value={c.taxOffice || ''} onChange={e => setC({ ...c, taxOffice: e.target.value })} />
          </div>
          <div className="field">
            <label>{taxLabel}</label>
            <input className="input mono" type="text" inputMode="numeric" maxLength={taxMaxLen}
              placeholder={c.type === 'CORPORATE' ? '10 haneli VKN' : '11 haneli TCKN'}
              value={c.taxNumber || ''}
              onChange={e => setC({ ...c, taxNumber: e.target.value.replace(/\D/g, '').slice(0, taxMaxLen) })}
            />
            {!!c.taxNumber && !taxCheck.ok && <div style={{ color: 'var(--neg)', fontSize: 12, marginTop: 4 }}>{taxCheck.msg}</div>}
          </div>
        </div>

        {c.type === 'INDIVIDUAL' && (
          <div className="field">
            <label>TC Kimlik No</label>
            <input className="input mono" type="text" inputMode="numeric" maxLength={11} placeholder="11 haneli TCKN"
              value={c.identityNumber || ''}
              onChange={e => setC({ ...c, identityNumber: e.target.value.replace(/\D/g, '').slice(0, 11) })}
            />
            {!!c.identityNumber && !tcCheck.ok && <div style={{ color: 'var(--neg)', fontSize: 12, marginTop: 4 }}>{tcCheck.msg}</div>}
          </div>
        )}

        <div className="grid-2">
          <div className="field">
            <label>E-posta</label>
            <input className="input" type="email" value={c.email || ''} onChange={e => setC({ ...c, email: e.target.value })} />
            {!!c.email && !emailCheck.ok && <div style={{ color: 'var(--neg)', fontSize: 12, marginTop: 4 }}>{emailCheck.msg}</div>}
          </div>
          <div className="field">
            <label>Telefon</label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <span style={{ position: 'absolute', left: 10, fontSize: 13, color: 'var(--text-3)', pointerEvents: 'none', userSelect: 'none', fontVariantNumeric: 'tabular-nums' }}>+90</span>
              <input className="input mono" type="tel" inputMode="numeric" maxLength={13} placeholder="5XX XXX XX XX"
                value={c.phoneNumber || ''} style={{ paddingLeft: 38 }}
                onChange={e => {
                  const digits = e.target.value.replace(/\D/g, '').slice(0, 10);
                  const fmt = digits.replace(/^(\d{3})(\d{3})(\d{2})(\d{0,2})$/, '$1 $2 $3 $4').replace(/^(\d{3})(\d{3})(\d{0,2})$/, '$1 $2 $3').replace(/^(\d{3})(\d{0,3})$/, '$1 $2').trim();
                  setC({ ...c, phoneNumber: fmt });
                }}
              />
            </div>
            {!!c.phoneNumber && !phoneCheck.ok && <div style={{ color: 'var(--neg)', fontSize: 12, marginTop: 4 }}>{phoneCheck.msg}</div>}
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>IBAN</label>
            <input className="input mono" value={c.iban || ''} onChange={e => setC({ ...c, iban: e.target.value })} placeholder="TR..." />
          </div>
          <div className="field">
            <label>Döviz</label>
            <select className="input" value={c.currency || 'TRY'} onChange={e => setC({ ...c, currency: e.target.value })}>
              <option value="TRY">TRY</option>
              <option value="USD">USD</option>
              <option value="EUR">EUR</option>
            </select>
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Kredi Limiti (₺)</label>
            <input className="input mono" type="number" min="0" value={c.creditLimit || ''} onChange={e => setC({ ...c, creditLimit: e.target.value })} placeholder="0.00" />
          </div>
          <div className="field">
            <label>Açılış Bakiyesi (₺)</label>
            <input className="input mono" type="number" value={c.openingBalance || ''} onChange={e => setC({ ...c, openingBalance: e.target.value })} placeholder="0.00" />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Açılış Bakiye Tarihi</label>
            <input className="input" type="date" value={c.openingBalanceDate || ''} onChange={e => setC({ ...c, openingBalanceDate: e.target.value })} />
          </div>
          <div className="field">
            <label>Kart Türü *</label>
            <div style={{ display: 'flex', gap: 6 }}>
              {roleBtn('Alıcı', 'BUYER')}
              {roleBtn('Satıcı', 'SELLER')}
              {roleBtn('Her İkisi', 'BOTH')}
            </div>
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Kart Durumu</label>
            <select className="input" value={c.status || 'ACTIVE'} onChange={e => setC({ ...c, status: e.target.value })}>
              <option value="ACTIVE">Aktif</option>
              <option value="PASSIVE">Pasif</option>
              <option value="BLOCKED">Bloke</option>
            </select>
          </div>
          <div className="field">
            <label>Müşteri Grubu</label>
            <input className="input" value={c.customerGroup || ''} onChange={e => setC({ ...c, customerGroup: e.target.value })} placeholder="örn: VIP, Riskli, Tedarikçi..." />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Şehir</label>
            <select className="input" value={c.city || ''} onChange={e => setC({ ...c, city: e.target.value })} style={{ cursor: 'pointer' }}>
              <option value="">Seçiniz...</option>
              {TR_CITIES.map(city => <option key={city} value={city}>{city}</option>)}
            </select>
          </div>
        </div>

        <div className="field">
          <label>Adres</label>
          <textarea className="input" rows={3} placeholder="Mahalle, cadde, sokak, bina no..."
            value={c.address || ''} onChange={e => setC({ ...c, address: e.target.value })}
            style={{ resize: 'vertical', minHeight: 72, fontFamily: 'inherit', fontSize: 13 }}
          />
        </div>

      </div>
    </Drawer>
  );
}
