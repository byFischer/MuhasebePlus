import { useState, useRef } from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import {
  useBankStatements, useBankStatement,
  useImportStatement, useAutoMatch, useManualMatch, useUnmatch,
} from '@/hooks/useBankReconciliation';
import { useBankAccounts } from '@/hooks/useBankAccounts';

export default function BankaMutabakatPage() {
  const [selectedStmtId, setSelectedStmtId] = useState(null);
  const [showImport, setShowImport] = useState(false);
  const [selectedLineId, setSelectedLineId] = useState(null);
  const [matchTxId, setMatchTxId] = useState('');

  const { data: statements = [], isLoading } = useBankStatements();
  const { data: stmt, isLoading: stmtLoading } = useBankStatement(selectedStmtId);
  const { data: accounts = [] } = useBankAccounts();

  const importMut   = useImportStatement();
  const autoMut     = useAutoMatch();
  const manualMut   = useManualMatch();
  const unmatchMut  = useUnmatch();

  const handleManualMatch = () => {
    if (!selectedLineId || !matchTxId) return;
    manualMut.mutate({ statementLineId: selectedLineId, transactionId: Number(matchTxId) }, {
      onSuccess: () => { setSelectedLineId(null); setMatchTxId(''); },
    });
  };

  const statusLabel = (s) => s === 'COMPLETED' ? 'Tamamlandı' : 'Devam Ediyor';
  const statusClass = (s) => s === 'COMPLETED' ? 'info' : 'warn';

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Banka Mutabakatı</h1>
          <p className="page-sub">Banka ekstresini içe aktarın ve işlemlerle eşleştirin</p>
        </div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setShowImport(true)}>
            <Icon name="upload" size={14} /> Ekstre Yükle
          </button>
        </div>
      </div>

      {showImport && (
        <ImportPanel
          accounts={accounts}
          isPending={importMut.isPending}
          onImport={(params) => importMut.mutate(params, {
            onSuccess: (data) => { setSelectedStmtId(data.id); setShowImport(false); },
          })}
          onCancel={() => setShowImport(false)}
        />
      )}

      <div className="grid-2" style={{ gap: 16, alignItems: 'start' }}>
        {/* Ekstre Listesi */}
        <div className="card">
          <div className="card-h"><h3>Yüklenen Ekstreler</h3></div>
          {isLoading && <div className="empty">Yükleniyor…</div>}
          {!isLoading && statements.length === 0 && (
            <div className="empty">Henüz ekstre yüklenmedi</div>
          )}
          {!isLoading && statements.length > 0 && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Hesap</th>
                    <th>Tarih</th>
                    <th>Eşleşme</th>
                    <th>Durum</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {statements.map(s => (
                    <tr
                      key={s.id}
                      style={{ cursor: 'pointer', background: selectedStmtId === s.id ? 'var(--surface-2)' : '' }}
                      onClick={() => setSelectedStmtId(s.id)}
                    >
                      <td>{s.bankAccountName}</td>
                      <td className="mono muted">{s.statementDate || '—'}</td>
                      <td className="mono">{s.matchedLines} / {s.totalLines}</td>
                      <td><span className={`pill ${statusClass(s.status)}`}>{statusLabel(s.status)}</span></td>
                      <td>
                        <button className="tb-icon-btn" onClick={e => { e.stopPropagation(); setSelectedStmtId(s.id); }}>
                          <Icon name="eye" size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Seçili Ekstre Detayı */}
        {selectedStmtId && (
          <div className="card">
            <div className="card-h">
              <h3>{stmt?.bankAccountName} — Satırlar</h3>
              <button
                className="btn sm"
                onClick={() => autoMut.mutate(selectedStmtId)}
                disabled={autoMut.isPending}
              >
                <Icon name="refresh" size={14} />
                {autoMut.isPending ? 'Eşleştiriliyor…' : 'Otomatik Eşleştir'}
              </button>
            </div>

            {stmtLoading && <div className="empty">Yükleniyor…</div>}
            {!stmtLoading && stmt && (
              <>
                {/* Özet */}
                <div className="row gap-16" style={{ padding: '8px 16px', borderBottom: '1px solid var(--border)' }}>
                  <span className="muted" style={{ fontSize: 12 }}>
                    Eşleşen: <strong>{stmt.matchedLines}/{stmt.totalLines}</strong>
                  </span>
                  <span className="muted" style={{ fontSize: 12 }}>
                    Açılış: <strong className="mono">{TRY(stmt.openingBalance)}</strong>
                  </span>
                  <span className="muted" style={{ fontSize: 12 }}>
                    Kapanış: <strong className="mono">{TRY(stmt.closingBalance)}</strong>
                  </span>
                </div>

                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Tarih</th>
                        <th>Açıklama</th>
                        <th>Tutar</th>
                        <th>Bakiye</th>
                        <th>Durum</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {(stmt.lines || []).map(line => (
                        <tr key={line.id} style={{ background: selectedLineId === line.id ? 'var(--surface-2)' : '' }}>
                          <td className="mono muted">{line.transactionDate}</td>
                          <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {line.description || '—'}
                          </td>
                          <td className="mono tnum" style={{ color: Number(line.amount) >= 0 ? 'var(--pos)' : 'var(--neg)' }}>
                            {TRY(line.amount)}
                          </td>
                          <td className="mono tnum muted">{line.balance != null ? TRY(line.balance) : '—'}</td>
                          <td>
                            {line.isMatched
                              ? <span className="pill info">Eşleşti</span>
                              : <span className="pill warn">Bekliyor</span>
                            }
                          </td>
                          <td>
                            {line.isMatched ? (
                              <button
                                className="tb-icon-btn"
                                title="Eşleştirmeyi Kaldır"
                                onClick={() => unmatchMut.mutate(line.id)}
                                disabled={unmatchMut.isPending}
                              >
                                <Icon name="x" size={14} />
                              </button>
                            ) : (
                              <button
                                className="tb-icon-btn"
                                title="Manuel Eşleştir"
                                onClick={() => setSelectedLineId(line.id)}
                              >
                                <Icon name="check" size={14} />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Manuel Eşleştirme Paneli */}
                {selectedLineId && (
                  <div className="card-b col gap-8" style={{ borderTop: '1px solid var(--border)', paddingTop: 12 }}>
                    <div className="muted" style={{ fontSize: 12 }}>
                      Satır #{selectedLineId} için işlem ID girin:
                    </div>
                    <div className="row gap-8">
                      <input
                        className="input mono"
                        style={{ width: 140 }}
                        placeholder="İşlem ID"
                        value={matchTxId}
                        onChange={e => setMatchTxId(e.target.value)}
                      />
                      <button
                        className="btn primary sm"
                        onClick={handleManualMatch}
                        disabled={!matchTxId || manualMut.isPending}
                      >
                        Eşleştir
                      </button>
                      <button className="btn sm" onClick={() => { setSelectedLineId(null); setMatchTxId(''); }}>
                        İptal
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function ImportPanel({ accounts, isPending, onImport, onCancel }) {
  const fileRef = useRef();
  const [form, setForm] = useState({
    bankAccountId: '',
    statementDate: '',
    openingBalance: '',
    closingBalance: '',
  });
  const [file, setFile] = useState(null);

  const setField = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = () => {
    if (!file || !form.bankAccountId) return;
    onImport({
      file,
      bankAccountId: Number(form.bankAccountId),
      statementDate: form.statementDate || null,
      openingBalance: form.openingBalance ? parseFloat(form.openingBalance) : null,
      closingBalance: form.closingBalance ? parseFloat(form.closingBalance) : null,
    });
  };

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div className="card-h">
        <h3>Ekstre Yükle (CSV / XLSX)</h3>
        <button className="tb-icon-btn" onClick={onCancel}><Icon name="x" size={14} /></button>
      </div>
      <div className="card-b col gap-12">
        <div className="muted" style={{ fontSize: 12 }}>
          Beklenen sütun sırası: <code>Tarih, Açıklama, Tutar, Bakiye</code>
        </div>
        <div className="grid-2">
          <div className="field">
            <label>Banka Hesabı</label>
            <select className="select" value={form.bankAccountId} onChange={e => setField('bankAccountId', e.target.value)}>
              <option value="">Seçin…</option>
              {accounts.map(a => <option key={a.accountId} value={a.accountId}>{a.bankName} — {a.accountNumber || a.iban}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Ekstre Tarihi</label>
            <input className="input" type="date" value={form.statementDate} onChange={e => setField('statementDate', e.target.value)} />
          </div>
          <div className="field">
            <label>Açılış Bakiyesi</label>
            <input className="input mono" type="text" placeholder="0.00" value={form.openingBalance} onChange={e => setField('openingBalance', e.target.value)} />
          </div>
          <div className="field">
            <label>Kapanış Bakiyesi</label>
            <input className="input mono" type="text" placeholder="0.00" value={form.closingBalance} onChange={e => setField('closingBalance', e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label>Dosya (CSV veya XLSX)</label>
          <input ref={fileRef} type="file" accept=".csv,.xlsx,.xls" onChange={e => setFile(e.target.files[0])} className="input" />
        </div>
        <div className="row gap-8">
          <button className="btn primary" onClick={handleSubmit} disabled={!file || !form.bankAccountId || isPending}>
            <Icon name="upload" size={14} />
            {isPending ? 'Yükleniyor…' : 'Yükle ve Analiz Et'}
          </button>
          <button className="btn" onClick={onCancel}>İptal</button>
        </div>
      </div>
    </div>
  );
}
