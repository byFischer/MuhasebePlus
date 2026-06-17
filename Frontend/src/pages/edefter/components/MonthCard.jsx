import { useState } from 'react';
import Icon from '@/components/mp/Icon';

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
    <div className={`edc ${hasRun ? 'done' : 'idle'}`}>
      <div className="edc-head">
        <div className="edc-month">{monthName}</div>
        {hasRun
          ? <span className="chip ok"><span className="chip-dot" />Üretildi</span>
          : <span className="chip idle">Üretilmedi</span>
        }
      </div>

      {hasRun ? (
        <>
          <div className="edc-meta">
            <span><span className="num">{run.journalEntryCount || 0}</span> fiş · <span className="num">{run.journalLineCount || 0}</span> satır</span>
            {run.generatedAt && (
              <span className="dt">{new Date(run.generatedAt).toLocaleDateString('tr-TR')}</span>
            )}
          </div>
          <div className="edc-dl">
            <button className="btn ghost sm" style={{ width: '100%', justifyContent: 'flex-start' }} onClick={handleDownloadJournal}>
              <Icon name="download" size={13} /> Yevmiye XML İndir
            </button>
            <button className="btn ghost sm" style={{ width: '100%', justifyContent: 'flex-start' }} onClick={handleDownloadLedger}>
              <Icon name="download" size={13} /> Kebir XML İndir
            </button>
          </div>
          <div className="edc-foot">
            <button className="btn ghost sm" onClick={() => setRegenOpen(true)} disabled={isRegeneratePending}>
              <Icon name="refresh" size={12} /> Yeniden Üret
            </button>
            <button className="btn ghost sm" style={{ color: 'var(--neg)' }} onClick={() => setDeleteOpen(true)} disabled={isDeletePending}>
              <Icon name="trash" size={12} /> Sil
            </button>
          </div>
        </>
      ) : (
        <div className="edc-empty">
          <Icon className="ic" name="file" size={26} />
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
