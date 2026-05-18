import React, { useState, useMemo } from 'react';
import Icon from '@/components/mp/Icon';
import Drawer from '@/components/mp/Drawer';
import { useChartOfAccounts, useCreateAccount, useUpdateAccount, useDeleteAccount, useSeedTdhp } from '@/hooks/useChartOfAccounts';
import { useAuth } from '@/context/AuthContext';

const TYPE_LABELS = {
  ASSET: 'Varlık', LIABILITY: 'Kaynak', EQUITY: 'Öz Kaynak',
  INCOME: 'Gelir', EXPENSE: 'Gider', COST: 'Maliyet', MEMO: 'Nazım',
};
const TYPE_BADGE = {
  ASSET: 'info', LIABILITY: 'warn', EQUITY: 'accent',
  INCOME: 'pos', EXPENSE: 'neg', COST: 'warn', MEMO: '',
};

const GROUP_NAMES = {
  '1': '1xx — Dönen Varlıklar', '2': '2xx — Duran Varlıklar',
  '3': '3xx — Kısa Vadeli Yabancı Kaynaklar', '4': '4xx — Uzun Vadeli Yabancı Kaynaklar',
  '5': '5xx — Öz Kaynaklar', '6': '6xx — Gelir Tablosu (Gelirler)', '7': '7xx — Maliyet Hesapları',
  '9': '9xx — Nazım Hesaplar',
};

export default function HesapPlaniPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selected, setSelected] = useState(null);

  const { data: accounts = [], isLoading } = useChartOfAccounts();
  const createAccount = useCreateAccount();
  const updateAccount = useUpdateAccount();
  const deleteAccount = useDeleteAccount();
  const seedTdhp = useSeedTdhp();

  const filtered = useMemo(() => {
    let list = accounts;
    if (typeFilter) list = list.filter(a => a.accountType === typeFilter);
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(a => a.accountCode?.toLowerCase().includes(q) || a.accountName?.toLowerCase().includes(q));
    }
    return list;
  }, [accounts, typeFilter, search]);

  const openCreate = () => { setSelected(null); setDrawerOpen(true); };
  const openEdit = (a) => { setSelected(a); setDrawerOpen(true); };

  const handleSave = (dto) => {
    if (selected) {
      updateAccount.mutate({ id: selected.accountId, dto }, { onSuccess: () => setDrawerOpen(false) });
    } else {
      createAccount.mutate(dto, { onSuccess: () => setDrawerOpen(false) });
    }
  };

  const grouped = useMemo(() => {
    const groups = {};
    filtered.forEach(a => {
      const prefix = a.accountCode?.[0] || '?';
      if (!groups[prefix]) groups[prefix] = [];
      groups[prefix].push(a);
    });
    return groups;
  }, [filtered]);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Tek Düzen Hesap Planı</h1>
          <p className="page-sub">{accounts.length} hesap</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {isAdmin && (
            <>
              <button className="btn ghost" disabled={seedTdhp.isPending} onClick={() => seedTdhp.mutate()}
                title="Tek Düzen Hesap Planı standart hesaplarını yükle">
                <Icon name="accounting" size={14} />
                {seedTdhp.isPending ? 'Yükleniyor…' : 'TDHP Yükle'}
              </button>
              <button className="btn primary" onClick={openCreate}>
                <Icon name="plus" size={14} /> Yeni Hesap
              </button>
            </>
          )}
        </div>
      </div>

      <div className="toolbar">
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, maxWidth: 280,
          background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 'var(--r-md)', padding: '5px 10px' }}>
          <Icon name="search" size={13} style={{ color: 'var(--ink-3)', flexShrink: 0 }} />
          <input value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Hesap kodu veya adı…"
            style={{ border: 0, outline: 0, background: 'transparent', flex: 1, fontSize: 12.5, color: 'var(--ink)' }} />
        </div>
        <select className="select" style={{ width: 'auto', minWidth: 140 }}
          value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
          <option value="">Tüm Tipler</option>
          {Object.entries(TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        {(search || typeFilter) && (
          <button className="btn ghost" onClick={() => { setSearch(''); setTypeFilter(''); }}>
            <Icon name="x" size={13} /> Temizle
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="empty">Yükleniyor…</div>
      ) : Object.keys(grouped).length === 0 ? (
        <div className="empty" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, padding: 40 }}>
          <Icon name="accounting" size={40} style={{ color: 'var(--ink-3)' }} />
          <p style={{ margin: 0 }}>Hesap planı henüz oluşturulmamış.</p>
          {isAdmin && (
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn primary" disabled={seedTdhp.isPending} onClick={() => seedTdhp.mutate()}>
                <Icon name="accounting" size={14} />
                {seedTdhp.isPending ? 'Yükleniyor…' : 'TDHP Yükle (80+ Hesap)'}
              </button>
              <button className="btn ghost" onClick={openCreate}>Manuel Ekle</button>
            </div>
          )}
        </div>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Hesap Kodu</th>
                <th>Hesap Adı</th>
                <th>Tür</th>
                <th>Üst Hesap</th>
                <th>Yaprak</th>
                <th>Sistem</th>
                {isAdmin && <th></th>}
              </tr>
            </thead>
            <tbody>
              {Object.keys(grouped).sort().map(prefix => (
                <React.Fragment key={prefix}>
                  <tr className="table-group-row">
                    <td colSpan={isAdmin ? 7 : 6}>
                      {GROUP_NAMES[prefix] || `${prefix}xx`}
                    </td>
                  </tr>
                  {grouped[prefix].map(a => (
                    <tr key={a.accountId}
                      style={{ cursor: isAdmin && !a.isSystem ? 'pointer' : 'default' }}
                      onClick={() => isAdmin && !a.isSystem && openEdit(a)}>
                      <td className="mono" style={{ fontWeight: 600 }}>{a.accountCode}</td>
                      <td style={{ paddingLeft: a.parentId ? 28 : 12 }}>{a.accountName}</td>
                      <td>
                        <span className={`badge${TYPE_BADGE[a.accountType] ? ' ' + TYPE_BADGE[a.accountType] : ''}`}>
                          {TYPE_LABELS[a.accountType] || a.accountType}
                        </span>
                      </td>
                      <td style={{ color: 'var(--ink-3)', fontSize: 12 }}>{a.parentCode || '—'}</td>
                      <td>
                        {a.isLeaf
                          ? <Icon name="check" size={14} style={{ color: 'var(--pos)' }} />
                          : <span style={{ color: 'var(--ink-3)' }}>—</span>}
                      </td>
                      <td>
                        {a.isSystem ? <span className="badge">Sistem</span> : '—'}
                      </td>
                      {isAdmin && (
                        <td onClick={e => e.stopPropagation()}>
                          {!a.isSystem && (
                            <div style={{ display: 'flex', gap: 4 }}>
                              <button className="tb-icon-btn" onClick={() => openEdit(a)} title="Düzenle">
                                <Icon name="edit" size={13} />
                              </button>
                              <button className="tb-icon-btn danger" onClick={() => deleteAccount.mutate(a.accountId)} title="Sil">
                                <Icon name="trash" size={13} />
                              </button>
                            </div>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <AccountFormDrawer
        open={drawerOpen}
        account={selected}
        accounts={accounts}
        onClose={() => setDrawerOpen(false)}
        onSave={handleSave}
        loading={createAccount.isPending || updateAccount.isPending}
      />
    </div>
  );
}

function AccountFormDrawer({ open, account, accounts, onClose, onSave, loading }) {
  const [form, setForm] = useState({
    accountCode: account?.accountCode || '',
    accountName: account?.accountName || '',
    accountType: account?.accountType || 'ASSET',
    parentId: account?.parentId || '',
    isLeaf: account?.isLeaf ?? true,
    description: account?.description || '',
  });

  React.useEffect(() => {
    if (open) {
      setForm({
        accountCode: account?.accountCode || '',
        accountName: account?.accountName || '',
        accountType: account?.accountType || 'ASSET',
        parentId: account?.parentId || '',
        isLeaf: account?.isLeaf ?? true,
        description: account?.description || '',
      });
    }
  }, [open, account]);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const TYPE_LABELS_FORM = {
    ASSET: 'Varlık', LIABILITY: 'Kaynak', EQUITY: 'Öz Kaynak',
    INCOME: 'Gelir', EXPENSE: 'Gider', COST: 'Maliyet', MEMO: 'Nazım',
  };

  return (
    <Drawer
      open={open}
      title={account ? 'Hesap Düzenle' : 'Yeni Hesap'}
      onClose={onClose}
      width={440}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>İptal</button>
          <button className="btn primary"
            disabled={loading || !form.accountCode || !form.accountName}
            onClick={() => onSave({ ...form, parentId: form.parentId || null })}>
            {loading ? 'Kaydediliyor…' : 'Kaydet'}
          </button>
        </>
      }
    >
      <div className="col gap-12">
        <div className="grid-2">
          <div className="field">
            <label>Hesap Kodu *</label>
            <input className="input mono" value={form.accountCode}
              onChange={e => set('accountCode', e.target.value)} placeholder="120.00.001" />
          </div>
          <div className="field">
            <label>Hesap Adı *</label>
            <input className="input" value={form.accountName}
              onChange={e => set('accountName', e.target.value)} placeholder="Alıcılar" />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Hesap Tipi</label>
            <select className="select" value={form.accountType} onChange={e => set('accountType', e.target.value)}>
              {Object.entries(TYPE_LABELS_FORM).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Üst Hesap</label>
            <select className="select" value={form.parentId}
              onChange={e => set('parentId', e.target.value ? Number(e.target.value) : '')}>
              <option value="">Yok (Ana Hesap)</option>
              {accounts.filter(a => !a.isLeaf && a.accountId !== account?.accountId)
                .map(a => <option key={a.accountId} value={a.accountId}>{a.accountCode} — {a.accountName}</option>)}
            </select>
          </div>
        </div>
        <div className="field">
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
            <input type="checkbox" checked={form.isLeaf} onChange={e => set('isLeaf', e.target.checked)} />
            Yaprak hesap (fiş kesilebilir)
          </label>
        </div>
        <div className="field">
          <label>Açıklama</label>
          <textarea className="input" value={form.description}
            onChange={e => set('description', e.target.value)} rows={2} />
        </div>
      </div>
    </Drawer>
  );
}
