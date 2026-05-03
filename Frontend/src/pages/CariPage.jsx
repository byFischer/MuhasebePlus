import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import Drawer from '@/components/mp/Drawer';
import { DropdownFilter, CityFilter } from '@/components/mp/DropdownFilter';
import { TRY } from '@/lib/format';
import { validateEmail, validateTaxNumberByType } from '@/lib/validators';
import { useCustomers, useCreateCustomer, useUpdateCustomer, useDeleteCustomer } from '@/hooks/useCustomers';

export default function CariPage() {
  const emptyForm = {
    name: '',
    email: '',
    phoneNumber: '',
    taxNumber: '',
    city: '',
    address: '',
    type: 'INDIVIDUAL',
  };

  const { data: list = [], isLoading, isError, refetch } = useCustomers();
  const createMut = useCreateCustomer();
  const updateMut = useUpdateCustomer();
  const deleteMut = useDeleteCustomer();

  const [form, setForm] = useState(emptyForm);
  const [q, setQ] = useState('');
  const [filter, setFilter] = useState('hepsi');
  const [city, setCity] = useState('hepsi');
  const [cityOpen, setCityOpen] = useState(false);
  const [drawer, setDrawer] = useState(null);
  const [sel, setSel] = useState(null);
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 15;

  const cities = useMemo(() => ['hepsi', ...Array.from(new Set(list.map(c => c.city).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'tr'))], [list]);

  const filtered = useMemo(() => list.filter(c => {
    if (filter === 'borclu' && (c.balance || 0) <= 0) return false;
    if (filter === 'alacakli' && (c.balance || 0) >= 0) return false;
    if (filter === 'risk' && c.status !== 'risk') return false;
    if (city !== 'hepsi' && c.city !== city) return false;
    if (q && !(c.name + (c.customerId || '') + (c.vkn || '') + (c.tckn || '')).toLowerCase().includes(q.toLowerCase())) return false;
    return true;
  }), [list, q, filter, city]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [totalPages]);
  useEffect(() => { setPage(1); }, [q, filter, city]);
  const pageStart = (page - 1) * PAGE_SIZE;
  const pageEnd = Math.min(pageStart + PAGE_SIZE, filtered.length);
  const paged = filtered.slice(pageStart, pageEnd);

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
          <button className="btn primary" onClick={() => setDrawer({ mode: 'new' })}><Icon name="plus" /> Yeni Müşteri</button>
        </div>
      </div>
      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}><Icon name="search" size={14} /><input placeholder="Müşteri, VKN, TCKN ara..." value={q} onChange={e => setQ(e.target.value)} /></div>
          <div className="seg">
            {[['hepsi', 'Hepsi'], ['borclu', 'Borçlu'], ['alacakli', 'Alacaklı'], ['risk', 'Riskli']].map(([k, l]) =>
              <button key={k} className={filter === k ? 'on' : ''} onClick={() => setFilter(k)}>{l}</button>
            )}
          </div>
          <CityFilter cities={cities} city={city} setCity={setCity} open={cityOpen} setOpen={setCityOpen} />
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>ID</th><th>Müşteri</th><th>Tip</th><th>Şehir</th><th>Durum</th><th></th></tr></thead>
            <tbody>
              {paged.map(c => (
                <tr key={c.customerId} className={sel === c.customerId ? 'sel' : ''} onClick={() => setSel(c.customerId)}>
                  <td className="mono">{c.customerId}</td>
                  <td><b>{c.name}</b>{c.email && <div className="muted" style={{ fontSize: 11 }}>{c.email}</div>}</td>
                  <td><span className="pill">{c.type || 'kurumsal'}</span></td>
                  <td className="muted">{c.city}</td>
                  <td><span className={`pill ${c.status === 'risk' ? 'neg' : 'pos'}`}><span className="dot" />{c.status === 'risk' ? 'Riskli' : 'Aktif'}</span></td>
                  <td>
                    <div className="row gap-4">
                      <button className="tb-icon-btn" onClick={e => { e.stopPropagation(); setDrawer({ mode: 'edit', c }); }}><Icon name="edit" size={14} /></button>
                      <button className="tb-icon-btn" onClick={e => { e.stopPropagation(); deleteMut.mutate(c.customerId); }}><Icon name="trash" size={14} /></button>
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
      <CustomerDrawer open={!!drawer} mode={drawer?.mode} customer={drawer?.c} onClose={() => setDrawer(null)}
        onSave={(dto) => {
          if (drawer?.mode === 'edit') updateMut.mutate({ id: drawer.c.customerId, dto });
          else createMut.mutate(dto);
          setDrawer(null);
        }} />
    </div>
  );
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
  });

  useEffect(() => {
    setC(
      customer
        ? { ...customer, phoneNumber: customer.phoneNumber || customer.phone || '', taxNumber: customer.taxNumber || '', type: customer.type || 'INDIVIDUAL' }
        : { name: '', email: '', phoneNumber: '', taxNumber: '', city: '', address: '', type: 'INDIVIDUAL' }
    );
  }, [customer, open]);

  const taxLabel    = c.type === 'CORPORATE' ? 'VKN' : 'TCKN';
  const taxMaxLen   = c.type === 'CORPORATE' ? 10 : 11;
  const taxCheck    = validateTaxNumberByType(c.type, c.taxNumber || '');
  const emailCheck  = !c.email ? { ok: true } : validateEmail(c.email);
  const phoneCheck  = !c.phoneNumber ? { ok: true } : /^[1-9][0-9]{9}$/.test(c.phoneNumber.replace(/\s/g, '')) ? { ok: true } : { ok: false, msg: '10 haneli numara girin (örn: 5551234567)' };

  const valid = !!c.name?.trim() && taxCheck.ok && emailCheck.ok && phoneCheck.ok;

  const typeBtn = (label, val) => (
    <button
      type="button"
      onClick={() => setC({ ...c, type: val, taxNumber: '' })}
      style={{
        padding: '6px 18px',
        borderRadius: 6,
        border: c.type === val ? '1.5px solid var(--accent)' : '1.5px solid var(--border)',
        background: c.type === val ? 'var(--accent)' : 'transparent',
        color: c.type === val ? '#fff' : 'var(--text-2)',
        fontWeight: c.type === val ? 600 : 400,
        fontSize: 13,
        cursor: 'pointer',
        transition: 'all .15s',
      }}
    >{label}</button>
  );

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={mode === 'edit' ? `Düzenle — ${customer?.name}` : 'Yeni Müşteri'}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button
            className="btn primary"
            disabled={!valid}
            onClick={() => onSave({
              name: c.name?.trim(),
              email: c.email?.trim() || null,
              phoneNumber: c.phoneNumber?.replace(/\s/g, '').trim() || null,
              taxNumber: c.taxNumber?.trim(),
              city: c.city || null,
              address: c.address?.trim() || null,
              type: c.type,
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

        <div className="field">
          <label>Firma/Ad *</label>
          <input className="input" value={c.name || ''} onChange={e => setC({ ...c, name: e.target.value })} />
        </div>

        <div className="grid-2">
          <div className="field">
            <label>E-posta</label>
            <input className="input" type="email" value={c.email || ''} onChange={e => setC({ ...c, email: e.target.value })} />
            {!!c.email && !emailCheck.ok && <div style={{ color: 'var(--neg)', fontSize: 12, marginTop: 4 }}>{emailCheck.msg}</div>}
          </div>

          <div className="field">
            <label>Telefon</label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <span style={{
                position: 'absolute', left: 10, fontSize: 13, color: 'var(--text-3)',
                pointerEvents: 'none', userSelect: 'none', fontVariantNumeric: 'tabular-nums',
              }}>+90</span>
              <input
                className="input mono"
                type="tel"
                inputMode="numeric"
                maxLength={13}
                placeholder="5XX XXX XX XX"
                value={c.phoneNumber || ''}
                style={{ paddingLeft: 38 }}
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
            <label>{taxLabel}</label>
            <input
              className="input mono"
              type="text"
              inputMode="numeric"
              maxLength={taxMaxLen}
              placeholder={c.type === 'CORPORATE' ? '10 haneli VKN' : '11 haneli TCKN'}
              value={c.taxNumber || ''}
              onChange={e => setC({ ...c, taxNumber: e.target.value.replace(/\D/g, '').slice(0, taxMaxLen) })}
            />
            {!!c.taxNumber && !taxCheck.ok && <div style={{ color: 'var(--neg)', fontSize: 12, marginTop: 4 }}>{taxCheck.msg}</div>}
          </div>

          <div className="field">
            <label>Şehir</label>
            <select
              className="input"
              value={c.city || ''}
              onChange={e => setC({ ...c, city: e.target.value })}
              style={{ cursor: 'pointer' }}
            >
              <option value="">Seçiniz...</option>
              {TR_CITIES.map(city => <option key={city} value={city}>{city}</option>)}
            </select>
          </div>
        </div>

        <div className="field">
          <label>Adres</label>
          <textarea
            className="input"
            rows={3}
            placeholder="Mahalle, cadde, sokak, bina no..."
            value={c.address || ''}
            onChange={e => setC({ ...c, address: e.target.value })}
            style={{ resize: 'vertical', minHeight: 72, fontFamily: 'inherit', fontSize: 13 }}
          />
        </div>

      </div>
    </Drawer>
  );
}
