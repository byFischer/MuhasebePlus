import { useState } from 'react';
import GenerateConfirmModal from './GenerateConfirmModal';
import BeyannameDetailDrawer from './BeyannameDetailDrawer';

const MONTHS_TR = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];
const QUARTERS = ['Q1 (Oca-Mar)', 'Q2 (Nis-Haz)', 'Q3 (Tem-Eyl)', 'Q4 (Eki-Ara)'];
const CUR_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 5 }, (_, i) => CUR_YEAR - i);

function periodLabel(type, d) {
  if (!d) return '';
  if (type === 'MONTH') return `${d.year} ${MONTHS_TR[(d.month || 1) - 1]}`;
  if (type === 'QUARTER') return `${d.year} Q${d.period}`;
  return `${d.year}`;
}

function StatusBadge({ status }) {
  const map = {
    DRAFT:     ['', 'Taslak'],
    FINALIZED: ['mavi', 'Onaylandı'],
    SUBMITTED: ['yesil', 'Gönderildi'],
  };
  const [cls, label] = map[status] || ['', status];
  return (
    <span className={`pill ${cls}`} style={{ fontSize: 11 }}>
      <span className="dot" />{label}
    </span>
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

export default function BeyannameTabView({
  declarations = [],
  isLoading,
  periodType,
  typeName,
  declarationType,
  onGenerate,
  isGeneratePending,
  onFinalize,
  isFinalizePending,
  onSubmit,
  isSubmitPending,
  onDelete,
  isDeletePending,
  downloadService,
  extraToolbar,
}) {
  const [year, setYear] = useState(CUR_YEAR);
  const [period, setPeriod] = useState(periodType === 'YEAR' ? 0 : new Date().getMonth() + 1);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [selected, setSelected] = useState(null);

  const handleGenerate = () => {
    const effectivePeriod = periodType === 'QUARTER' ? Math.ceil(period / 3) : periodType === 'YEAR' ? 0 : period;
    onGenerate({ year, period: effectivePeriod, month: period }, {
      onSuccess: () => setConfirmOpen(false),
      onError: () => setConfirmOpen(false),
    });
  };

  const openDetail = (d) => { setSelected(d); setDetailOpen(true); };

  return (
    <div className="col gap-12">
      <div className="row gap-8" style={{ alignItems: 'center', flexWrap: 'wrap' }}>
        <select className="input" style={{ width: 100 }} value={year} onChange={e => setYear(Number(e.target.value))}>
          {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
        </select>

        {periodType === 'MONTH' && (
          <select className="input" style={{ width: 140 }} value={period} onChange={e => setPeriod(Number(e.target.value))}>
            {MONTHS_TR.map((m, i) => <option key={i} value={i + 1}>{m}</option>)}
          </select>
        )}
        {periodType === 'QUARTER' && (
          <select className="input" style={{ width: 180 }} value={period} onChange={e => setPeriod(Number(e.target.value))}>
            {QUARTERS.map((q, i) => <option key={i} value={i * 3 + 1}>{q}</option>)}
          </select>
        )}

        {extraToolbar}

        <button className="btn primary sm" onClick={() => setConfirmOpen(true)} disabled={isGeneratePending}>
          + Üret
        </button>
      </div>

      {isLoading ? (
        <div className="muted" style={{ padding: 24, textAlign: 'center' }}>Yükleniyor...</div>
      ) : declarations.length === 0 ? (
        <div className="empty" style={{ padding: 32, textAlign: 'center' }}>
          Bu dönem için henüz beyanname üretmediniz.<br />
          <span className="muted" style={{ fontSize: 12 }}>Yıl ve dönem seçip &quot;Üret&quot; butonuna basın.</span>
        </div>
      ) : (
        <table className="tbl" style={{ width: '100%' }}>
          <thead>
            <tr>
              <th>Dönem</th>
              <th>Durum</th>
              <th className="num">Tutar</th>
              <th>Üretim Tarihi</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {declarations.map((d) => {
              const id = d.declarationId || d.id;
              const amount = d.payableVat ?? d.totalPayable ?? d.totalWithholdingAmount;
              const date = d.generatedAt ? new Date(d.generatedAt).toLocaleDateString('tr-TR') : '—';
              return (
                <tr key={id}>
                  <td className="mono" style={{ fontSize: 13 }}>{periodLabel(periodType, d)}</td>
                  <td><StatusBadge status={d.status} /></td>
                  <td className="num mono">{amount != null ? Number(amount).toLocaleString('tr-TR', { minimumFractionDigits: 2 }) + ' ₺' : '—'}</td>
                  <td className="muted" style={{ fontSize: 12 }}>{date}</td>
                  <td>
                    <div className="row gap-4" style={{ justifyContent: 'flex-end' }}>
                      <button className="btn ghost sm" onClick={() => openDetail(d)}>Detay</button>
                      {downloadService?.downloadXml && (
                        <button className="btn ghost sm" onClick={async () => {
                          const blob = await downloadService.downloadXml(id);
                          downloadBlob(blob, `beyanname-${d.year}-${d.month || d.period || ''}.xml`);
                        }}>XML</button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}

      <GenerateConfirmModal
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleGenerate}
        isPending={isGeneratePending}
        typeName={typeName}
        periodType={periodType}
        year={year}
        period={periodType === 'QUARTER' ? Math.ceil(period / 3) : period}
      />

      <BeyannameDetailDrawer
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        declaration={selected}
        declarationType={declarationType}
        periodType={periodType}
        onFinalize={onFinalize}
        onSubmit={onSubmit}
        onDelete={onDelete}
        isFinalizePending={isFinalizePending}
        isSubmitPending={isSubmitPending}
        isDeletePending={isDeletePending}
        downloadService={downloadService}
      />
    </div>
  );
}
