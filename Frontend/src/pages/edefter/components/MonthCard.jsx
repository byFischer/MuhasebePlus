import React, { useState } from 'react';

const MONTHS_TR = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];

function ConfirmModal({ open, onClose, onConfirm, isPending, title, message }) {
  if (!open) return null;
  return (
    <div style={{ position: 'fixed', inset: 0, zIndex: 1000, background: 'rgba(0,0,0,0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: 'var(--surface)', borderRadius: 10, padding: 24, width: 380, boxShadow: '0 8px 32px rgba(0,0,0,0.18)' }}>
        <div style={{ fontWeight: 700, fontSize: 15, marginBottom: 10 }}>{title}</div>
        <p style={{ fontSize: 13, color: 'var(--text-2)', marginBottom: 20, lineHeight: 1.6 }}>{message}</p>
        <div className="row gap-8" style={{ justifyContent: 'flex-end' }}>
          <button className="btn ghost" onClick={onClose} disabled={isPending}>Vazgeç</button>
          <button className="btn primary" onClick={onConfirm} disabled={isPending}>
            {isPending ? '...' : 'Devam Et'}
          </button>
        </div>
      </div>
    </div>
  );
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

export default function MonthCard({ year, month, run, onGenerate, onRegenerate, onDelete, isGeneratePending, isRegeneratePending, isDeletePending, downloadJournal, downloadLedger }) {
  const [generateOpen, setGenerateOpen] = useState(false);
  const [regenOpen, setRegenOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const monthName = MONTHS_TR[month - 1];

  const handleGenerate = () => { onGenerate(); setGenerateOpen(false); };
  const handleRegen    = () => { onRegenerate(); setRegenOpen(false); };
  const handleDelete   = () => { onDelete(); setDeleteOpen(false); };

  const handleDownloadJournal = async () => {
    try {
      const blob = await downloadJournal(run.runId);
      downloadBlob(blob, `yevmiye-${year}-${String(month).padStart(2,'0')}.xml`);
    } catch { /* handled by API interceptor */ }
  };

  const handleDownloadLedger = async () => {
    try {
      const blob = await downloadLedger(run.runId);
      downloadBlob(blob, `kebir-${year}-${String(month).padStart(2,'0')}.xml`);
    } catch { /* handled by API interceptor */ }
  };

  const hasRun = !!run;

  return (
    <div style={{
      border: '1px solid var(--line)',
      borderRadius: 8,
      padding: 16,
      background: hasRun ? 'var(--surface)' : 'var(--surface-2, var(--surface))',
      minHeight: 130,
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontWeight: 600, fontSize: 14 }}>{monthName}</div>
        {hasRun
          ? <span className="pill yesil" style={{ fontSize: 11 }}><span className="dot" />Üretildi</span>
          : <span className="pill" style={{ fontSize: 11, background: 'var(--surface-2)' }}>Üretilmedi</span>
        }
      </div>

      {hasRun ? (
        <>
          <div style={{ fontSize: 12, color: 'var(--text-2)' }}>
            <span>{run.journalEntryCount || 0} fiş / {run.journalLineCount || 0} satır</span>
            {run.generatedAt && (
              <span style={{ marginLeft: 8 }}>{new Date(run.generatedAt).toLocaleDateString('tr-TR')}</span>
            )}
          </div>
          <div className="col gap-4" style={{ marginTop: 4 }}>
            <button className="btn ghost sm" style={{ width: '100%', fontSize: 12 }} onClick={handleDownloadJournal}>
              Yevmiye XML İndir
            </button>
            <button className="btn ghost sm" style={{ width: '100%', fontSize: 12 }} onClick={handleDownloadLedger}>
              Kebir XML İndir
            </button>
            <div className="row gap-4">
              <button className="btn ghost sm" style={{ flex: 1, fontSize: 11 }} onClick={() => setRegenOpen(true)} disabled={isRegeneratePending}>
                Yeniden Üret
              </button>
              <button className="btn ghost sm" style={{ flex: 1, fontSize: 11, color: 'var(--red)' }} onClick={() => setDeleteOpen(true)} disabled={isDeletePending}>
                Sil
              </button>
            </div>
          </div>
        </>
      ) : (
        <div style={{ flex: 1, display: 'flex', alignItems: 'flex-end' }}>
          <button className="btn primary sm" style={{ width: '100%' }} onClick={() => setGenerateOpen(true)} disabled={isGeneratePending}>
            {isGeneratePending ? 'Üretiliyor...' : 'Üret'}
          </button>
        </div>
      )}

      <ConfirmModal
        open={generateOpen}
        onClose={() => setGenerateOpen(false)}
        onConfirm={handleGenerate}
        isPending={isGeneratePending}
        title="e-Defter Üret"
        message={`${year} ${monthName} ayı için e-Defter (Yevmiye + Kebir XML) üretilecek. Devam edilsin mi?`}
      />
      <ConfirmModal
        open={regenOpen}
        onClose={() => setRegenOpen(false)}
        onConfirm={handleRegen}
        isPending={isRegeneratePending}
        title="Yeniden Üret"
        message="Mevcut e-Defter silinecek ve yeniden üretilecek. Bu işlem geri alınamaz. Devam edilsin mi?"
      />
      <ConfirmModal
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDelete}
        isPending={isDeletePending}
        title="e-Defter Sil"
        message="e-Defter çalışması silinecek. Devam edilsin mi?"
      />
    </div>
  );
}
