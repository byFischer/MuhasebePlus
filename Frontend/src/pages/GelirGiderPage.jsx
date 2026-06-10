import React, { useState, useMemo, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import Drawer from '@/components/mp/Drawer';
import { TRY } from '@/lib/format';
import { useTransactions, useCreateTransaction } from '@/hooks/useTransactions';
import { useBankAccounts } from '@/hooks/useBankAccounts';

const TODAY = new Date().toISOString().slice(0, 10);
const TX_CATEGORY_OPTIONS = [
  ['GENERAL', 'Genel'],
  ['BANK_FEE', 'Banka Masrafi'],
  ['POS_COLLECTION', 'POS Tahsilati'],
  ['CREDIT_CARD_PAYMENT', 'Kredi Karti Odemesi'],
  ['TRANSFER', 'Virman'],
];
const TX_CATEGORY_LABELS = Object.fromEntries(TX_CATEGORY_OPTIONS);

export default function GelirGiderPage() {
  const { data: list = [], isLoading, isError, refetch } = useTransactions();
  const { data: banks = [] } = useBankAccounts();
  const [tab, setTab] = useState('hepsi');
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState(false);
  const PAGE_SIZE = 15;

  const totals = useMemo(() => {
    const operating = list.filter(x => x.transactionCategory !== 'TRANSFER');
    const g = operating.filter(x => x.transactionType === 'INCOME').reduce((s, x) => s + Number(x.amount || 0), 0);
    const e = operating.filter(x => x.transactionType === 'EXPENSE').reduce((s, x) => s + Number(x.amount || 0), 0);
    return { g, e, net: g - e };
  }, [list]);

  const filtered = list.filter(x => {
    if (tab === 'gelir') return x.transactionType === 'INCOME' && x.transactionCategory !== 'TRANSFER';
    if (tab === 'gider') return x.transactionType === 'EXPENSE' && x.transactionCategory !== 'TRANSFER';
    return true;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  useEffect(() => { if (page > totalPages) setPage(totalPages); }, [totalPages]);
  useEffect(() => { setPage(1); }, [tab]);
  const pageStart = (page - 1) * PAGE_SIZE;
  const pageEnd = Math.min(pageStart + PAGE_SIZE, filtered.length);
  const paged = filtered.slice(pageStart, pageEnd);

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError) return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Gelir / Gider İşlemleri</h1>
          <p className="page-sub">
            Gelir <b style={{ color: 'var(--pos)' }}>{TRY(totals.g)}</b> · Gider <b style={{ color: 'var(--neg)' }}>{TRY(totals.e)}</b> · Net <b className="mono tnum">{TRY(totals.net)}</b>
          </p>
        </div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setDrawer(true)}><Icon name="plus" /> Yeni İşlem</button>
        </div>
      </div>
      <div className="card">
        <div className="tabs">
          <button className={tab === 'hepsi' ? 'on' : ''} onClick={() => setTab('hepsi')}>Hepsi</button>
          <button className={tab === 'gelir' ? 'on' : ''} onClick={() => setTab('gelir')}>Gelirler</button>
          <button className={tab === 'gider' ? 'on' : ''} onClick={() => setTab('gider')}>Giderler</button>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>ID</th><th>Tarih</th><th>Tür</th><th>Kategori</th><th>Açıklama</th><th className="num">Tutar</th></tr>
            </thead>
            <tbody>
              {paged.map(x => (
                <tr key={x.transactionId}>
                  <td className="mono">{x.transactionId}</td>
                  <td className="muted">{x.transactionDate}</td>
                  <td>
                    <span className={`pill ${x.transactionCategory === 'TRANSFER' ? '' : x.transactionType === 'INCOME' ? 'pos' : 'neg'}`}>
                      <span className="dot" />{x.transactionCategory === 'TRANSFER' ? 'Virman' : x.transactionType === 'INCOME' ? 'Gelir' : 'Gider'}
                    </span>
                  </td>
                  <td>{x.category || TX_CATEGORY_LABELS[x.transactionCategory] || '-'}</td>
                  <td>{x.description}</td>
                  <td className="num mono tnum" style={{ color: x.transactionCategory === 'TRANSFER' ? 'inherit' : x.transactionType === 'INCOME' ? 'var(--pos)' : 'var(--neg)', fontWeight: 600 }}>
                    {TRY(x.amount || 0)}
                  </td>
                </tr>
              ))}
              {paged.length === 0 && <tr><td colSpan="6" className="empty">Kayıt bulunamadı</td></tr>}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={totalPages} setPage={setPage} pageStart={pageStart} pageEnd={pageEnd} total={filtered.length} />
      </div>
      <TransactionDrawer open={drawer} onClose={() => setDrawer(false)} banks={banks} />
    </div>
  );
}

function TransactionDrawer({ open, onClose, banks }) {
  const createMut = useCreateTransaction();
  const EMPTY = {
    accountId: '',
    transferAccountId: '',
    transactionType: 'INCOME',
    transactionCategory: 'GENERAL',
    amount: '',
    transactionDate: TODAY,
    description: '',
    category: ''
  };
  const [f, setF] = useState(EMPTY);

  const valid = f.accountId
    && f.transactionType
    && Number(f.amount) > 0
    && f.transactionDate
    && (f.transactionCategory !== 'TRANSFER' || (f.transferAccountId && f.transferAccountId !== f.accountId));

  useEffect(() => { if (!open) setF({ ...EMPTY, transactionDate: new Date().toISOString().slice(0, 10) }); }, [open]);

  const save = () => {
    if (!valid) return;
    createMut.mutate({
      accountId: Number(f.accountId),
      transferAccountId: f.transactionCategory === 'TRANSFER' ? Number(f.transferAccountId) : null,
      transactionType: f.transactionCategory === 'TRANSFER' ? 'EXPENSE' : f.transactionType,
      transactionCategory: f.transactionCategory,
      amount: Number(f.amount),
      transactionDate: f.transactionDate,
      description: f.description.trim() || undefined,
      category: f.category.trim() || undefined,
    }, { onSuccess: onClose });
  };

  return (
    <Drawer open={open} onClose={onClose} closeOnBackdrop={false} title="Yeni İşlem"
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid || createMut.isPending} onClick={save}>Kaydet</button>
        </>
      }>
      <div className="col gap-12">
        <div className="field">
          <label>Hesap *</label>
          <select className="input" value={f.accountId} onChange={e => setF({ ...f, accountId: e.target.value })}>
            <option value="">Hesap seçin...</option>
            {banks.map(b => (
              <option key={b.bankAccountId || b.accountId} value={b.bankAccountId || b.accountId}>
                {b.bankName || b.accountName} — {b.currency}
              </option>
            ))}
          </select>
          {banks.length === 0 && <span style={{ fontSize: 11, color: 'var(--warn)', marginTop: 4, display: 'block' }}>Önce bir banka hesabı ekleyin</span>}
        </div>
        <div className="grid-2">
          <div className="field">
            <label>İşlem Kategorisi *</label>
            <select
              className="input"
              value={f.transactionCategory}
              onChange={e => {
                const next = e.target.value;
                setF({
                  ...f,
                  transactionCategory: next,
                  transactionType: ['BANK_FEE', 'CREDIT_CARD_PAYMENT', 'TRANSFER'].includes(next)
                    ? 'EXPENSE'
                    : next === 'POS_COLLECTION' ? 'INCOME' : f.transactionType,
                  transferAccountId: next === 'TRANSFER' ? f.transferAccountId : ''
                });
              }}
            >
              {TX_CATEGORY_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
          {f.transactionCategory === 'TRANSFER' && (
            <div className="field">
              <label>Hedef Hesap *</label>
              <select className="input" value={f.transferAccountId} onChange={e => setF({ ...f, transferAccountId: e.target.value })}>
                <option value="">Hesap seçin...</option>
                {banks.filter(b => String(b.bankAccountId || b.accountId) !== String(f.accountId)).map(b => (
                  <option key={b.bankAccountId || b.accountId} value={b.bankAccountId || b.accountId}>
                    {b.bankName || b.accountName} - {b.currency}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Tür *</label>
            <select
              className="input"
              value={f.transactionType}
              disabled={f.transactionCategory !== 'GENERAL'}
              onChange={e => setF({ ...f, transactionType: e.target.value })}
            >
              <option value="INCOME">Gelir</option>
              <option value="EXPENSE">Gider</option>
            </select>
          </div>
          <div className="field">
            <label>Tarih *</label>
            <input className="input" type="date" max={TODAY} value={f.transactionDate} onChange={e => setF({ ...f, transactionDate: e.target.value })} />
          </div>
        </div>
        <div className="field">
          <label>Tutar (₺) *</label>
          <input className="input mono" type="number" min="0.01" step="0.01" value={f.amount} onChange={e => setF({ ...f, amount: e.target.value })} placeholder="0.00" />
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Kategori</label>
            <input className="input" value={f.category} onChange={e => setF({ ...f, category: e.target.value })} placeholder="Satış, kira, fatura..." />
          </div>
          <div className="field">
            <label>Açıklama</label>
            <input className="input" value={f.description} onChange={e => setF({ ...f, description: e.target.value })} />
          </div>
        </div>
      </div>
    </Drawer>
  );
}
