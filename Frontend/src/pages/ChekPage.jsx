import { useState } from 'react';
import Icon from '@/components/mp/Icon';
import Drawer from '@/components/mp/Drawer';
import { TRY, fmtDate, toIsoDate } from '@/lib/format';
import {
  useCheques, useCreateCheque, useDeleteCheque,
  useDepositCheque, useCollectCheque, useBounceCheque, useCancelCheque,
  usePortfolioSummary,
} from '@/hooks/useCheques';
import { useBankAccounts } from '@/hooks/useBankAccounts';
import { useCustomers } from '@/hooks/useCustomers';

const STATUS_LABELS = {
  IN_PORTFOLIO: 'Portföyde',
  DEPOSITED: 'Tahsilde',
  COLLECTED: 'Tahsil Edildi',
  BOUNCED: 'Karşılıksız',
  ENDORSED: 'Ciro Edildi',
  CANCELLED: 'İptal',
};
const STATUS_BADGE = {
  IN_PORTFOLIO: 'info',
  DEPOSITED: 'warn',
  COLLECTED: 'pos',
  BOUNCED: 'neg',
  ENDORSED: 'accent',
  CANCELLED: '',
};
const KIND_LABELS = { CHEQUE: 'Çek', PROMISSORY_NOTE: 'Senet' };
const TYPE_LABELS = { RECEIVABLE: 'Alacak', PAYABLE: 'Borç' };

export default function ChekPage() {
  const [filter, setFilter] = useState({ status: '', chequeType: '', chequeKind: '' });
  const [drawerMode, setDrawerMode] = useState(null);
  const [selected, setSelected] = useState(null);
  const [actionType, setActionType] = useState(null);
  const [actionData, setActionData] = useState({});

  const { data: cheques = [], isLoading } = useCheques(
    Object.fromEntries(Object.entries(filter).filter(([, v]) => v))
  );
  const { data: summary } = usePortfolioSummary();
  const { data: bankAccounts = [] } = useBankAccounts();
  const { data: customers = [] } = useCustomers();

  const createCheque = useCreateCheque();
  const deleteCheque = useDeleteCheque();
  const depositCheque = useDepositCheque();
  const collectCheque = useCollectCheque();
  const bounceCheque = useBounceCheque();
  const cancelCheque = useCancelCheque();

  const openCreate = () => { setSelected(null); setDrawerMode('create'); };
  const openDetail = (c) => { setSelected(c); setDrawerMode('detail'); };
  const openAction = (c, type) => { setSelected(c); setActionType(type); setActionData({}); setDrawerMode('action'); };

  const handleAction = () => {
    const id = selected.chequeId;
    const close = () => setDrawerMode(null);
    switch (actionType) {
      case 'deposit':  depositCheque.mutate({ id, bankAccountId: actionData.bankAccountId }, { onSuccess: close }); break;
      case 'collect':  collectCheque.mutate({ id, bankAccountId: actionData.bankAccountId }, { onSuccess: close }); break;
      case 'bounce':   bounceCheque.mutate({ id, reason: actionData.reason || 'Karşılıksız' }, { onSuccess: close }); break;
      case 'cancel':   cancelCheque.mutate({ id, reason: actionData.reason || 'İptal' }, { onSuccess: close }); break;
      default: break;
    }
  };

  const today = toIsoDate(new Date());
  const upcoming = cheques.filter(c => c.status === 'IN_PORTFOLIO' && c.dueDate && c.dueDate <= toIsoDate(new Date(Date.now() + 30 * 86400000)));
  const bounced  = cheques.filter(c => c.status === 'BOUNCED');

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Çek / Senet Portföyü</h1>
          <p className="page-sub">{cheques.length} kayıt</p>
        </div>
        <button className="btn primary" onClick={openCreate}>
          <Icon name="plus" size={14} /> Yeni Çek / Senet
        </button>
      </div>

      {summary && (
        <div className="kpis" style={{ marginBottom: 16 }}>
          <div className="kpi">
            <div className="kpi-label">Toplam Alacak Çeki</div>
            <div className="kpi-val">{TRY(summary.totalReceivable ?? 0)}</div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Toplam Borç Çeki</div>
            <div className="kpi-val">{TRY(summary.totalPayable ?? 0)}</div>
          </div>
          <div className="kpi" style={upcoming.length > 0 ? { borderColor: 'var(--warn)', background: 'var(--warn-soft)' } : {}}>
            <div className="kpi-label">Vadesi Yaklaşan (30 gün)</div>
            <div className="kpi-val" style={{ color: upcoming.length > 0 ? 'var(--warn)' : undefined }}>
              {upcoming.length} adet
            </div>
          </div>
          <div className="kpi" style={bounced.length > 0 ? { borderColor: 'var(--neg)', background: 'var(--neg-soft)' } : {}}>
            <div className="kpi-label">Karşılıksız</div>
            <div className="kpi-val" style={{ color: bounced.length > 0 ? 'var(--neg)' : undefined }}>
              {bounced.length} adet
            </div>
          </div>
        </div>
      )}

      <div className="toolbar">
        <select className="select" style={{ width: 'auto' }}
          value={filter.status} onChange={e => setFilter(f => ({ ...f, status: e.target.value }))}>
          <option value="">Tüm Durumlar</option>
          {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="select" style={{ width: 'auto' }}
          value={filter.chequeType} onChange={e => setFilter(f => ({ ...f, chequeType: e.target.value }))}>
          <option value="">Alacak / Borç</option>
          <option value="RECEIVABLE">Alacak Çekleri</option>
          <option value="PAYABLE">Borç Çekleri</option>
        </select>
        <select className="select" style={{ width: 'auto' }}
          value={filter.chequeKind} onChange={e => setFilter(f => ({ ...f, chequeKind: e.target.value }))}>
          <option value="">Çek / Senet</option>
          <option value="CHEQUE">Çek</option>
          <option value="PROMISSORY_NOTE">Senet</option>
        </select>
        {(filter.status || filter.chequeType || filter.chequeKind) && (
          <button className="btn ghost" onClick={() => setFilter({ status: '', chequeType: '', chequeKind: '' })}>
            <Icon name="x" size={13} /> Temizle
          </button>
        )}
      </div>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Çek No</th>
              <th>Tür</th>
              <th>Cari</th>
              <th>Banka</th>
              <th className="num">Tutar</th>
              <th>Vade</th>
              <th>Durum</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Yükleniyor…</td></tr>
            )}
            {!isLoading && cheques.length === 0 && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Kayıt bulunamadı</td></tr>
            )}
            {cheques.map(c => {
              const overdue = c.status === 'IN_PORTFOLIO' && c.dueDate && c.dueDate < today;
              return (
                <tr key={c.chequeId} className={overdue ? 'row-warn' : ''} onClick={() => openDetail(c)} style={{ cursor: 'pointer' }}>
                  <td className="mono">{c.chequeNumber}</td>
                  <td>
                    <span style={{ color: 'var(--ink-3)', fontSize: 11 }}>{KIND_LABELS[c.chequeKind] || c.chequeKind}</span>
                    {' · '}
                    {TYPE_LABELS[c.chequeType] || c.chequeType}
                  </td>
                  <td>{c.customerName || `#${c.customerId}`}</td>
                  <td style={{ color: 'var(--ink-2)' }}>{c.drawerBank || '—'}</td>
                  <td className="num" style={{ fontWeight: 600 }}>
                    {TRY(c.amount)}{c.currency !== 'TRY' ? ` ${c.currency}` : ''}
                  </td>
                  <td style={{ color: overdue ? 'var(--neg)' : undefined, fontWeight: overdue ? 600 : undefined }}>
                    {fmtDate(c.dueDate)}
                  </td>
                  <td>
                    <span className={`badge${STATUS_BADGE[c.status] ? ' ' + STATUS_BADGE[c.status] : ''}`}>
                      {STATUS_LABELS[c.status] || c.status}
                    </span>
                  </td>
                  <td onClick={e => e.stopPropagation()}>
                    <ActionMenu cheque={c} onAction={(type) => openAction(c, type)} onDelete={() => deleteCheque.mutate(c.chequeId)} />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <ChequeFormDrawer
        open={drawerMode === 'create'}
        onClose={() => setDrawerMode(null)}
        customers={customers}
        onSave={(dto) => createCheque.mutate(dto, { onSuccess: () => setDrawerMode(null) })}
        loading={createCheque.isPending}
      />

      <ChequeDetailDrawer
        open={drawerMode === 'detail' && !!selected}
        cheque={selected}
        onClose={() => setDrawerMode(null)}
        onAction={(type) => openAction(selected, type)}
      />

      <ActionDrawer
        open={drawerMode === 'action' && !!selected}
        cheque={selected}
        actionType={actionType}
        actionData={actionData}
        setActionData={setActionData}
        bankAccounts={bankAccounts}
        onClose={() => setDrawerMode(null)}
        onConfirm={handleAction}
        loading={depositCheque.isPending || collectCheque.isPending || bounceCheque.isPending || cancelCheque.isPending}
      />
    </div>
  );
}

function ActionMenu({ cheque, onAction, onDelete }) {
  const [open, setOpen] = useState(false);
  const canDeposit = cheque.status === 'IN_PORTFOLIO';
  const canCollect = cheque.status === 'DEPOSITED' || cheque.status === 'IN_PORTFOLIO';
  const canBounce  = cheque.status === 'DEPOSITED' || cheque.status === 'IN_PORTFOLIO';
  const canCancel  = cheque.status === 'IN_PORTFOLIO';

  return (
    <div style={{ position: 'relative' }}>
      <button className="tb-icon-btn" onClick={() => setOpen(o => !o)}>
        <Icon name="chevDown" size={13} />
      </button>
      {open && (
        <div className="dropdown" onMouseLeave={() => setOpen(false)}>
          {canDeposit && <div className="dd-item" onClick={() => { setOpen(false); onAction('deposit'); }}>Bankaya Ver</div>}
          {canCollect && <div className="dd-item" onClick={() => { setOpen(false); onAction('collect'); }}>Tahsil Edildi</div>}
          {canBounce  && <div className="dd-item" onClick={() => { setOpen(false); onAction('bounce'); }}>Karşılıksız</div>}
          {canCancel  && <div className="dd-item" onClick={() => { setOpen(false); onAction('cancel'); }}>İptal Et</div>}
          <div className="dd-item danger" onClick={() => { setOpen(false); onDelete(); }}>Sil</div>
        </div>
      )}
    </div>
  );
}

function ChequeFormDrawer({ open, onClose, customers, onSave, loading }) {
  const today = toIsoDate(new Date());
  const [form, setForm] = useState({
    chequeType: 'RECEIVABLE', chequeKind: 'CHEQUE', customerId: '',
    drawerBank: '', drawerBranch: '', drawerAccountNumber: '',
    chequeNumber: '', amount: '', currency: 'TRY', issueDate: today, dueDate: '',
    notes: '',
  });
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  return (
    <Drawer open={open} title="Yeni Çek / Senet" onClose={onClose} width={480}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>İptal</button>
          <button className="btn primary"
            disabled={loading || !form.customerId || !form.amount || !form.dueDate}
            onClick={() => onSave({ ...form, customerId: Number(form.customerId), amount: Number(form.amount) })}>
            {loading ? 'Kaydediliyor…' : 'Kaydet'}
          </button>
        </>
      }>
      <div className="col gap-12">
        <div className="grid-2">
          <div className="field">
            <label>Tür</label>
            <select className="select" value={form.chequeType} onChange={e => set('chequeType', e.target.value)}>
              <option value="RECEIVABLE">Alacak (Müşteri Çeki)</option>
              <option value="PAYABLE">Borç (Firma Çeki)</option>
            </select>
          </div>
          <div className="field">
            <label>Cinsi</label>
            <select className="select" value={form.chequeKind} onChange={e => set('chequeKind', e.target.value)}>
              <option value="CHEQUE">Çek</option>
              <option value="PROMISSORY_NOTE">Senet</option>
            </select>
          </div>
        </div>
        <div className="field">
          <label>Cari *</label>
          <select className="select" value={form.customerId} onChange={e => set('customerId', e.target.value)}>
            <option value="">Seçiniz…</option>
            {customers.map(c => <option key={c.customerId} value={c.customerId}>{c.name}</option>)}
          </select>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Çek / Senet No</label>
            <input className="input mono" value={form.chequeNumber} onChange={e => set('chequeNumber', e.target.value)} placeholder="123456789" />
          </div>
          <div className="field">
            <label>Tutar *</label>
            <input type="number" className="input" value={form.amount} onChange={e => set('amount', e.target.value)} placeholder="0.00" />
          </div>
        </div>
        <div className="field">
          <label>Döviz</label>
          <select className="select" value={form.currency} onChange={e => set('currency', e.target.value)}>
            <option>TRY</option><option>USD</option><option>EUR</option>
          </select>
        </div>
        <div className="field">
          <label>Banka Adı</label>
          <input className="input" value={form.drawerBank} onChange={e => set('drawerBank', e.target.value)} placeholder="Garanti BBVA" />
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Şube</label>
            <input className="input" value={form.drawerBranch} onChange={e => set('drawerBranch', e.target.value)} />
          </div>
          <div className="field">
            <label>Hesap No</label>
            <input className="input" value={form.drawerAccountNumber} onChange={e => set('drawerAccountNumber', e.target.value)} />
          </div>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Düzenleme Tarihi</label>
            <input type="date" className="input" value={form.issueDate} onChange={e => set('issueDate', e.target.value)} />
          </div>
          <div className="field">
            <label>Vade Tarihi *</label>
            <input type="date" className="input" value={form.dueDate} onChange={e => set('dueDate', e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>Not</label>
          <textarea className="input" value={form.notes} onChange={e => set('notes', e.target.value)} rows={2} />
        </div>
      </div>
    </Drawer>
  );
}

function ChequeDetailDrawer({ open, cheque: c, onClose, onAction }) {
  if (!c) return null;
  const canDeposit = c.status === 'IN_PORTFOLIO';
  const canCollect = c.status === 'DEPOSITED' || c.status === 'IN_PORTFOLIO';
  const canBounce  = c.status === 'DEPOSITED' || c.status === 'IN_PORTFOLIO';
  const canCancel  = c.status === 'IN_PORTFOLIO';

  return (
    <Drawer open={open} title={`Çek Detayı — ${c.chequeNumber}`} onClose={onClose} width={440}
      footer={<button className="btn ghost" onClick={onClose}>Kapat</button>}>
      <div className="col gap-12">
        <dl className="detail-grid">
          <dt>Durum</dt>
          <dd>
            <span className={`badge${STATUS_BADGE[c.status] ? ' ' + STATUS_BADGE[c.status] : ''}`}>
              {STATUS_LABELS[c.status] || c.status}
            </span>
          </dd>
          <dt>Tür / Cinsi</dt><dd>{TYPE_LABELS[c.chequeType]} {KIND_LABELS[c.chequeKind]}</dd>
          <dt>Cari</dt><dd>{c.customerName || `#${c.customerId}`}</dd>
          <dt>Tutar</dt><dd style={{ fontWeight: 600 }}>{TRY(c.amount)}{c.currency !== 'TRY' ? ` ${c.currency}` : ''}</dd>
          <dt>Banka</dt><dd>{c.drawerBank || '—'}</dd>
          <dt>Şube</dt><dd>{c.drawerBranch || '—'}</dd>
          <dt>Hesap No</dt><dd className="mono">{c.drawerAccountNumber || '—'}</dd>
          <dt>Düzenleme</dt><dd>{fmtDate(c.issueDate)}</dd>
          <dt>Vade</dt><dd>{fmtDate(c.dueDate)}</dd>
          {c.notes && <><dt>Not</dt><dd>{c.notes}</dd></>}
        </dl>
        {(canDeposit || canCollect || canBounce || canCancel) && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {canDeposit && <button className="btn sm primary" onClick={() => onAction('deposit')}>Bankaya Ver</button>}
            {canCollect && <button className="btn sm primary" onClick={() => onAction('collect')}>Tahsil Edildi</button>}
            {canBounce  && <button className="btn sm danger"  onClick={() => onAction('bounce')}>Karşılıksız</button>}
            {canCancel  && <button className="btn sm ghost"   onClick={() => onAction('cancel')}>İptal Et</button>}
          </div>
        )}
      </div>
    </Drawer>
  );
}

function ActionDrawer({ open, cheque, actionType, actionData, setActionData, bankAccounts, onClose, onConfirm, loading }) {
  const TITLES = {
    deposit: 'Bankaya Tahsile Ver',
    collect: 'Tahsil Edildi İşaretle',
    bounce: 'Karşılıksız İşaretle',
    cancel: 'Çeki İptal Et',
  };
  const needsBank   = actionType === 'deposit' || actionType === 'collect';
  const needsReason = actionType === 'bounce'  || actionType === 'cancel';

  return (
    <Drawer open={open} title={TITLES[actionType] || 'İşlem'} onClose={onClose} width={400}
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>İptal</button>
          <button className="btn primary"
            disabled={loading || (needsBank && !actionData.bankAccountId)}
            onClick={onConfirm}>
            {loading ? 'İşleniyor…' : 'Onayla'}
          </button>
        </>
      }>
      <div className="col gap-12">
        {cheque && (
          <p style={{ margin: 0, fontSize: 12.5, color: 'var(--ink-2)' }}>
            <strong className="mono">{cheque.chequeNumber}</strong> · {TRY(cheque.amount)}
          </p>
        )}
        {needsBank && (
          <div className="field">
            <label>Banka Hesabı</label>
            <select className="select" value={actionData.bankAccountId || ''}
              onChange={e => setActionData(d => ({ ...d, bankAccountId: Number(e.target.value) }))}>
              <option value="">Seçiniz…</option>
              {bankAccounts.map(b => (
                <option key={b.accountId} value={b.accountId}>
                  {b.bankName} — {b.iban || b.accountNumber}
                </option>
              ))}
            </select>
          </div>
        )}
        {needsReason && (
          <div className="field">
            <label>Sebep</label>
            <input className="input" value={actionData.reason || ''}
              onChange={e => setActionData(d => ({ ...d, reason: e.target.value }))}
              placeholder="Açıklama giriniz…" />
          </div>
        )}
      </div>
    </Drawer>
  );
}
