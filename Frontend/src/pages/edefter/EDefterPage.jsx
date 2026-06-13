import { useState } from 'react';
import MonthCard from './components/MonthCard';
import eDefterService from '@/services/eDefterService';
import { useEDefterByYear, useGenerateEDefter, useRegenerateEDefter, useDeleteEDefter } from '@/hooks/useEDefter';

const CUR_YEAR = new Date().getFullYear();
const YEARS = Array.from({ length: 5 }, (_, i) => CUR_YEAR - i);
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

export default function EDefterPage() {
  const [year, setYear] = useState(CUR_YEAR);
  const { data: runs = [], isLoading } = useEDefterByYear(year);
  const generateMut   = useGenerateEDefter();
  const regenerateMut = useRegenerateEDefter();
  const deleteMut     = useDeleteEDefter();

  const runByMonth = {};
  for (const r of runs) { runByMonth[r.month] = r; }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">e-Defter</h1>
          <p className="page-sub">Yevmiye Defteri ve Defter-i Kebir XML üretimi (XBRL-GL formatı)</p>
        </div>
      </div>

      <div style={{
        padding: '10px 16px',
        background: 'var(--warn-soft, #fffbe6)',
        border: '1px solid var(--warn, #d97706)',
        borderRadius: 6,
        fontSize: 13,
        color: 'var(--warn-dark, #92400e)',
        marginBottom: 16,
      }}>
        ⚠ Bu modülde <strong>XAdES dijital imza</strong> üretilmez. Resmi GİB yüklemesi için
        mali müşavirinizin LUCA/ETA gibi sertifikalı yazılımına XML dosyalarını aktarın.
      </div>

      <div className="card">
        <div style={{ padding: '12px 16px', borderBottom: '1px solid var(--line)', display: 'flex', alignItems: 'center', gap: 12 }}>
          <label style={{ fontWeight: 600, fontSize: 13 }}>Yıl</label>
          <select className="input" style={{ width: 100 }} value={year} onChange={e => setYear(Number(e.target.value))}>
            {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
          </select>
          {isLoading && <span className="muted" style={{ fontSize: 12 }}>Yükleniyor...</span>}
          <span className="muted" style={{ fontSize: 12, marginLeft: 'auto' }}>
            {runs.length} / 12 ay üretildi
          </span>
        </div>

        <div style={{ padding: 16 }}>
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
            gap: 12,
          }}>
            {MONTHS.map(m => {
              const run = runByMonth[m];
              return (
                <MonthCard
                  key={m}
                  year={year}
                  month={m}
                  run={run || null}
                  isGeneratePending={generateMut.isPending}
                  isRegeneratePending={regenerateMut.isPending}
                  isDeletePending={deleteMut.isPending}
                  onGenerate={() => generateMut.mutate({ year, month: m })}
                  onRegenerate={() => run && regenerateMut.mutate(run.runId)}
                  onDelete={() => run && deleteMut.mutate(run.runId)}
                  downloadJournal={eDefterService.downloadJournal}
                  downloadLedger={eDefterService.downloadLedger}
                />
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
