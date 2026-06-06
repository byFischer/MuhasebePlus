import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import Drawer from '@/components/mp/Drawer';
import { TRY, fmtDate, toIsoDate } from '@/lib/format';
import {
  useJournalEntries, useJournalEntry, useTrialBalance,
  useIncomeStatement, useBalanceSheet,
  useCreateJournalEntry, useReverseJournalEntry, useDeleteJournalEntry,
} from '@/hooks/useJournalEntries';
import { useChartOfAccounts } from '@/hooks/useChartOfAccounts';
import { useAuth } from '@/context/AuthContext';

const TABS = [
  { id: 'journal', label: 'Yevmiye Defteri' },
  { id: 'trial',   label: 'Mizan' },
  { id: 'income',  label: 'Gelir Tablosu' },
  { id: 'balance', label: 'Bilanço' },
  { id: 'manual',  label: 'Manuel Fiş' },
];

const SOURCE_LABELS = {
  INVOICE: 'Fatura', PAYMENT: 'Ödeme', TRANSACTION: 'İşlem',
  CHEQUE: 'Çek', MANUAL: 'Manuel', REVERSAL: 'İptal',
};

const TYPE_LABELS_TR = {
  ASSET: 'Varlık', LIABILITY: 'Kaynak', EQUITY: 'Öz Kaynak',
  INCOME: 'Gelir', EXPENSE: 'Gider', COST: 'Maliyet', MEMO: 'Nazım',
};

export default function YevmiyePage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [tab, setTab] = useState('journal');

  const today = toIsoDate(new Date());
  const firstOfYear = `${new Date().getFullYear()}-01-01`;
  const [startDate, setStartDate] = useState(firstOfYear);
  const [endDate, setEndDate]     = useState(today);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Muhasebe Defteri</h1>
          <p className="page-sub">Yevmiye, mizan, mali tablolar ve manuel fiş</p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input type="date" className="input" style={{ width: 'auto' }}
            value={startDate} onChange={e => setStartDate(e.target.value)} />
          <span style={{ color: 'var(--ink-3)' }}>—</span>
          <input type="date" className="input" style={{ width: 'auto' }}
            value={endDate} onChange={e => setEndDate(e.target.value)} />
        </div>
      </div>

      <div className="tabs">
        {TABS.map(t => (
          <button key={t.id} className={tab === t.id ? 'on' : ''} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'journal' && <JournalTab startDate={startDate} endDate={endDate} isAdmin={isAdmin} />}
      {tab === 'trial'   && <TrialBalanceTab startDate={startDate} endDate={endDate} />}
      {tab === 'income'  && <IncomeStatementTab startDate={startDate} endDate={endDate} />}
      {tab === 'balance' && <BalanceSheetTab asOfDate={endDate} />}
      {tab === 'manual'  && isAdmin && <ManualEntryTab />}
      {tab === 'manual'  && !isAdmin && <div className="empty">Bu sekmeye erişim için yönetici yetkisi gereklidir.</div>}
    </div>
  );
}

/* ─── Yevmiye Defteri ─── */
function JournalTab({ startDate, endDate, isAdmin }) {
  const [detailId, setDetailId] = useState(null);
  const { data: entries = [], isLoading } = useJournalEntries({ startDate, endDate, size: 500 });
  const reverseEntry = useReverseJournalEntry();
  const deleteEntry  = useDeleteJournalEntry();

  return (
    <>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Fiş No</th>
              <th>Tarih</th>
              <th>Kaynak</th>
              <th>Açıklama</th>
              <th className="num">Toplam Borç</th>
              <th className="num">Toplam Alacak</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr><td colSpan={7} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Yükleniyor…</td></tr>
            )}
            {!isLoading && entries.length === 0 && (
              <tr><td colSpan={7} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Bu tarih aralığında fiş bulunamadı.</td></tr>
            )}
            {entries.map(e => {
              const totalDebit  = (e.lines || []).reduce((s, l) => s + (l.debitAmount || 0), 0);
              const totalCredit = (e.lines || []).reduce((s, l) => s + (l.creditAmount || 0), 0);
              return (
                <tr key={e.entryId} className={e.isReversed ? 'row-dimmed' : ''}>
                  <td>
                    <button className="btn ghost sm" style={{ fontFamily: 'var(--mono)', fontWeight: 600, color: 'var(--accent-ink)' }}
                      onClick={() => setDetailId(e.entryId)}>
                      {e.entryNumber}
                    </button>
                  </td>
                  <td>{fmtDate(e.entryDate)}</td>
                  <td><span className="badge">{SOURCE_LABELS[e.sourceType] || e.sourceType}</span></td>
                  <td style={{ color: 'var(--ink-2)', maxWidth: 280, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {e.description}
                  </td>
                  <td className="num mono">{TRY(totalDebit)}</td>
                  <td className="num mono">{TRY(totalCredit)}</td>
                  <td>
                    {isAdmin && !e.isReversed && e.sourceType === 'MANUAL' && (
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button className="tb-icon-btn" title="Ters Çevir"
                          onClick={() => reverseEntry.mutate({ id: e.entryId, reason: 'Manuel iptal' })}>
                          <Icon name="undo" size={13} />
                        </button>
                        <button className="tb-icon-btn danger" title="Sil"
                          onClick={() => deleteEntry.mutate(e.entryId)}>
                          <Icon name="trash" size={13} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {detailId && (
        <EntryDetailDrawer entryId={detailId} onClose={() => setDetailId(null)} />
      )}
    </>
  );
}

function EntryDetailDrawer({ entryId, onClose }) {
  const { data: entry, isLoading } = useJournalEntry(entryId);

  if (isLoading || !entry) return null;

  const totalDebit  = (entry.lines || []).reduce((s, l) => s + (l.debitAmount || 0), 0);
  const totalCredit = (entry.lines || []).reduce((s, l) => s + (l.creditAmount || 0), 0);
  const balanced = Math.abs(totalDebit - totalCredit) < 0.01;

  return (
    <Drawer open title={`Mahsup Fişi — ${entry.entryNumber}`} onClose={onClose} width={560}
      footer={<button className="btn ghost" onClick={onClose}>Kapat</button>}>
      <div className="col gap-12">
        <dl className="detail-grid">
          <dt>Tarih</dt><dd>{fmtDate(entry.entryDate)}</dd>
          <dt>Kaynak</dt><dd>{SOURCE_LABELS[entry.sourceType] || entry.sourceType}</dd>
          <dt>Durum</dt>
          <dd>
            <span className={`badge${entry.isReversed ? ' neg' : ' pos'}`}>
              {entry.isReversed ? 'İptal Edildi' : 'Aktif'}
            </span>
          </dd>
          {entry.description && <><dt>Açıklama</dt><dd>{entry.description}</dd></>}
        </dl>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Hesap Kodu</th>
                <th>Hesap Adı</th>
                <th className="num">Borç</th>
                <th className="num">Alacak</th>
              </tr>
            </thead>
            <tbody>
              {(entry.lines || []).map(l => (
                <tr key={l.lineId}>
                  <td className="mono">{l.accountCode}</td>
                  <td>{l.accountName}</td>
                  <td className="num mono">{l.debitAmount > 0 ? TRY(l.debitAmount) : '—'}</td>
                  <td className="num mono">{l.creditAmount > 0 ? TRY(l.creditAmount) : '—'}</td>
                </tr>
              ))}
              <tr className="table-total-row">
                <td colSpan={2} style={{ fontWeight: 600 }}>TOPLAM</td>
                <td className="num mono" style={{ fontWeight: 600 }}>{TRY(totalDebit)}</td>
                <td className="num mono" style={{ fontWeight: 600 }}>{TRY(totalCredit)}</td>
              </tr>
            </tbody>
          </table>
        </div>

        {!balanced && (
          <div className="hint" style={{ background: 'var(--warn-soft)', color: 'var(--warn)' }}>
            <Icon name="alert" size={14} />
            Borç ≠ Alacak farkı: {TRY(Math.abs(totalDebit - totalCredit))}
          </div>
        )}
      </div>
    </Drawer>
  );
}

/* ─── Mizan ─── */
function TrialBalanceTab({ startDate, endDate }) {
  const { data: rows = [], isLoading } = useTrialBalance(startDate, endDate);
  const totalDebit  = rows.reduce((s, r) => s + (r.totalDebit  || 0), 0);
  const totalCredit = rows.reduce((s, r) => s + (r.totalCredit || 0), 0);
  const balanced = Math.abs(totalDebit - totalCredit) < 0.01;

  return (
    <div>
      {rows.length > 0 && (
        <div className="kpis" style={{ marginTop: 16, marginBottom: 16 }}>
          <div className="kpi">
            <div className="kpi-label">Toplam Borç</div>
            <div className="kpi-val">{TRY(totalDebit)}</div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Toplam Alacak</div>
            <div className="kpi-val">{TRY(totalCredit)}</div>
          </div>
          <div className="kpi" style={!balanced ? { borderColor: 'var(--neg)', background: 'var(--neg-soft)' } : {}}>
            <div className="kpi-label">Denge Kontrolü</div>
            <div className="kpi-val" style={{ color: balanced ? 'var(--pos)' : 'var(--neg)', fontSize: 18 }}>
              {balanced ? 'Dengeli ✓' : `Fark: ${TRY(Math.abs(totalDebit - totalCredit))}`}
            </div>
          </div>
        </div>
      )}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Hesap Kodu</th>
              <th>Hesap Adı</th>
              <th>Tür</th>
              <th className="num">Borç Toplamı</th>
              <th className="num">Alacak Toplamı</th>
              <th className="num">Bakiye</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Yükleniyor…</td></tr>
            )}
            {!isLoading && rows.length === 0 && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Bu tarih aralığında kayıt bulunamadı.</td></tr>
            )}
            {rows.map(r => (
              <tr key={r.accountId}>
                <td className="mono" style={{ fontWeight: 600 }}>{r.accountCode}</td>
                <td>{r.accountName}</td>
                <td><span className="badge">{TYPE_LABELS_TR[r.accountType] || r.accountType}</span></td>
                <td className="num mono">{TRY(r.totalDebit)}</td>
                <td className="num mono">{TRY(r.totalCredit)}</td>
                <td className="num mono" style={{ fontWeight: 600, color: r.balance < 0 ? 'var(--neg)' : undefined }}>
                  {TRY(r.balance)}
                </td>
              </tr>
            ))}
            {rows.length > 0 && (
              <tr className="table-total-row">
                <td colSpan={3} style={{ fontWeight: 600 }}>TOPLAM</td>
                <td className="num mono" style={{ fontWeight: 600 }}>{TRY(totalDebit)}</td>
                <td className="num mono" style={{ fontWeight: 600 }}>{TRY(totalCredit)}</td>
                <td className="num mono" style={{ fontWeight: 600, color: !balanced ? 'var(--neg)' : undefined }}>
                  {TRY(totalDebit - totalCredit)}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* ─── Manuel Fiş ─── */
function IncomeStatementTab({ startDate, endDate }) {
  const { data, isLoading } = useIncomeStatement(startDate, endDate);
  const sections = data?.sections || [];
  const netProfit = Number(data?.netProfit || 0);

  return (
    <div>
      {data && (
        <div className="kpis" style={{ marginTop: 16, marginBottom: 16 }}>
          <div className="kpi">
            <div className="kpi-label">Toplam Gelir</div>
            <div className="kpi-val">{TRY(data.totalIncome)}</div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Toplam Gider</div>
            <div className="kpi-val">{TRY(data.totalExpense)}</div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Net Kar/Zarar</div>
            <div className="kpi-val" style={{ color: netProfit >= 0 ? 'var(--pos)' : 'var(--neg)' }}>
              {TRY(data.netProfit)}
            </div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Kar Marji</div>
            <div className="kpi-val">{Number(data.profitMarginPercent || 0).toFixed(2)}%</div>
          </div>
        </div>
      )}
      <FinancialStatementTable
        isLoading={isLoading}
        sections={sections}
        emptyText="Bu tarih aralığında gelir tablosu kaydı bulunamadı."
      />
    </div>
  );
}

function BalanceSheetTab({ asOfDate }) {
  const { data, isLoading } = useBalanceSheet(asOfDate);
  const difference = Number(data?.difference || 0);
  const balanced = Math.abs(difference) < 0.01;
  const sections = [
    ...(data?.assetSections || []),
    ...(data?.liabilitySections || []),
    ...(data?.equitySections || []),
  ];

  return (
    <div>
      {data && (
        <div className="kpis" style={{ marginTop: 16, marginBottom: 16 }}>
          <div className="kpi">
            <div className="kpi-label">Toplam Aktif</div>
            <div className="kpi-val">{TRY(data.totalAssets)}</div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Toplam Pasif</div>
            <div className="kpi-val">{TRY(data.totalLiabilitiesAndEquity)}</div>
          </div>
          <div className="kpi">
            <div className="kpi-label">Oz Kaynak</div>
            <div className="kpi-val">{TRY(data.totalEquity)}</div>
          </div>
          <div className="kpi" style={!balanced ? { borderColor: 'var(--neg)', background: 'var(--neg-soft)' } : {}}>
            <div className="kpi-label">Denge Kontrolu</div>
            <div className="kpi-val" style={{ color: balanced ? 'var(--pos)' : 'var(--neg)', fontSize: 18 }}>
              {balanced ? 'Dengeli' : `Fark: ${TRY(Math.abs(difference))}`}
            </div>
          </div>
        </div>
      )}
      <FinancialStatementTable
        isLoading={isLoading}
        sections={sections}
        emptyText="Bu tarih itibarıyla bilanço kaydı bulunamadı."
      />
    </div>
  );
}

function FinancialStatementTable({ isLoading, sections, emptyText }) {
  const hasRows = sections.some(section => (section.lines || []).length > 0);

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Grup</th>
            <th>Hesap Kodu</th>
            <th>Hesap Adı</th>
            <th className="num">Tutar</th>
          </tr>
        </thead>
        <tbody>
          {isLoading && (
            <tr><td colSpan={4} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>Yükleniyor…</td></tr>
          )}
          {!isLoading && !hasRows && (
            <tr><td colSpan={4} style={{ textAlign: 'center', padding: 24, color: 'var(--ink-3)' }}>{emptyText}</td></tr>
          )}
          {!isLoading && hasRows && sections.map(section => (
            <React.Fragment key={section.sectionCode}>
              <tr className="table-total-row">
                <td colSpan={3} style={{ fontWeight: 600 }}>{section.sectionName}</td>
                <td className="num mono" style={{ fontWeight: 600 }}>{TRY(section.totalAmount)}</td>
              </tr>
              {(section.lines || []).map(line => (
                <tr key={`${section.sectionCode}-${line.accountId ?? line.accountCode}`}>
                  <td className="muted">{section.sectionName}</td>
                  <td className="mono" style={{ fontWeight: 600 }}>{line.accountCode}</td>
                  <td>{line.accountName}</td>
                  <td className="num mono">{TRY(line.amount)}</td>
                </tr>
              ))}
            </React.Fragment>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ManualEntryTab() {
  const today = toIsoDate(new Date());
  const { data: accounts = [] } = useChartOfAccounts();
  const leafAccounts = accounts.filter(a => a.isLeaf);
  const createEntry = useCreateJournalEntry();

  const [entryDate, setEntryDate] = useState(today);
  const [description, setDescription] = useState('');
  const [lines, setLines] = useState([
    { accountId: '', debitAmount: '', creditAmount: '', description: '' },
    { accountId: '', debitAmount: '', creditAmount: '', description: '' },
  ]);

  const setLine = (i, k, v) => setLines(ls => ls.map((l, idx) => idx === i ? { ...l, [k]: v } : l));
  const addLine = () => setLines(ls => [...ls, { accountId: '', debitAmount: '', creditAmount: '', description: '' }]);
  const removeLine = (i) => setLines(ls => ls.filter((_, idx) => idx !== i));

  const totalDebit  = lines.reduce((s, l) => s + (Number(l.debitAmount)  || 0), 0);
  const totalCredit = lines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0);
  const balanced = Math.abs(totalDebit - totalCredit) < 0.01;

  const handleSave = () => {
    if (!balanced) return;
    createEntry.mutate({
      entryDate,
      description,
      lines: lines
        .filter(l => l.accountId)
        .map(l => ({
          accountId: Number(l.accountId),
          debitAmount: Number(l.debitAmount) || 0,
          creditAmount: Number(l.creditAmount) || 0,
          description: l.description,
        })),
    }, {
      onSuccess: () => {
        setDescription('');
        setLines([
          { accountId: '', debitAmount: '', creditAmount: '', description: '' },
          { accountId: '', debitAmount: '', creditAmount: '', description: '' },
        ]);
      },
    });
  };

  return (
    <div style={{ maxWidth: 860, marginTop: 16 }}>
      <div className="card">
        <div className="card-h">
          <h3>Mahsup Fişi Girişi</h3>
        </div>
        <div className="card-b col gap-12">
          <div className="grid-2">
            <div className="field">
              <label>Fiş Tarihi</label>
              <input type="date" className="input" value={entryDate} onChange={e => setEntryDate(e.target.value)} />
            </div>
            <div className="field">
              <label>Açıklama</label>
              <input className="input" value={description} onChange={e => setDescription(e.target.value)} placeholder="İşlem açıklaması…" />
            </div>
          </div>

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th style={{ width: '35%' }}>Hesap</th>
                  <th className="num" style={{ width: '18%' }}>Borç</th>
                  <th className="num" style={{ width: '18%' }}>Alacak</th>
                  <th>Açıklama</th>
                  <th style={{ width: 36 }}></th>
                </tr>
              </thead>
              <tbody>
                {lines.map((l, i) => (
                  <tr key={i}>
                    <td>
                      <select className="select" value={l.accountId} onChange={e => setLine(i, 'accountId', e.target.value)}>
                        <option value="">Hesap seçiniz…</option>
                        {leafAccounts.map(a => (
                          <option key={a.accountId} value={a.accountId}>{a.accountCode} — {a.accountName}</option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <input type="number" className="input" value={l.debitAmount}
                        onChange={e => setLine(i, 'debitAmount', e.target.value)}
                        style={{ textAlign: 'right' }} placeholder="0.00"
                        onFocus={() => l.creditAmount && setLine(i, 'creditAmount', '')} />
                    </td>
                    <td>
                      <input type="number" className="input" value={l.creditAmount}
                        onChange={e => setLine(i, 'creditAmount', e.target.value)}
                        style={{ textAlign: 'right' }} placeholder="0.00"
                        onFocus={() => l.debitAmount && setLine(i, 'debitAmount', '')} />
                    </td>
                    <td>
                      <input className="input" value={l.description} onChange={e => setLine(i, 'description', e.target.value)} />
                    </td>
                    <td>
                      {lines.length > 2 && (
                        <button className="tb-icon-btn danger" onClick={() => removeLine(i)}>
                          <Icon name="x" size={13} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                <tr className="table-total-row">
                  <td style={{ fontWeight: 600 }}>TOPLAM</td>
                  <td className="num mono" style={{ fontWeight: 600, color: !balanced && totalDebit > 0 ? 'var(--neg)' : undefined }}>
                    {TRY(totalDebit)}
                  </td>
                  <td className="num mono" style={{ fontWeight: 600, color: !balanced && totalCredit > 0 ? 'var(--neg)' : undefined }}>
                    {TRY(totalCredit)}
                  </td>
                  <td colSpan={2}></td>
                </tr>
              </tbody>
            </table>
          </div>

          {!balanced && (totalDebit > 0 || totalCredit > 0) && (
            <div className="hint" style={{ background: 'var(--warn-soft)', color: 'var(--warn)' }}>
              <Icon name="alert" size={14} />
              Fiş dengesiz: Borç - Alacak farkı {TRY(Math.abs(totalDebit - totalCredit))}
            </div>
          )}

          <div style={{ display: 'flex', gap: 8, justifyContent: 'space-between', alignItems: 'center' }}>
            <button className="btn ghost" onClick={addLine}>
              <Icon name="plus" size={13} /> Satır Ekle
            </button>
            <button className="btn primary"
              disabled={!balanced || totalDebit === 0 || createEntry.isPending}
              onClick={handleSave}>
              {createEntry.isPending ? 'Kaydediliyor…' : 'Fişi Kaydet'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
