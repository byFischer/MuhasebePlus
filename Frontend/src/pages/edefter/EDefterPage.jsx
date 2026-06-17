import { useState } from 'react';
import Icon from '@/components/mp/Icon';
import MonthCard from './components/MonthCard';
import eDefterService from '@/services/eDefterService';
import { useEDefterByYear, useGenerateEDefter, useRegenerateEDefter, useDeleteEDefter } from '@/hooks/useEDefter';

const CUR_YEAR = new Date().getFullYear();
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

export default function EDefterPage() {
  const [year, setYear] = useState(CUR_YEAR);
  const { data: runs = [], isLoading } = useEDefterByYear(year);
  const generateMut   = useGenerateEDefter();
  const regenerateMut = useRegenerateEDefter();
  const deleteMut     = useDeleteEDefter();

  const runByMonth = {};
  for (const r of runs) { runByMonth[r.month] = r; }

  const producedCount = runs.length;
  const pct = Math.round((producedCount / 12) * 100);

  return (
    <div className="page">
      <div className="page-head">
        <div className="mod-head">
          <div className="mod-head-ic"><Icon name="journal" size={22} /></div>
          <div>
            <h1 className="page-title">e-Defter</h1>
            <p className="page-sub">Yevmiye Defteri ve Defter-i Kebir XML üretimi (XBRL-GL formatı)</p>
          </div>
        </div>
      </div>

      <div className="notice warn">
        <div className="notice-ic"><Icon name="shield" size={16} /></div>
        <div>
          Bu modülde <strong>XAdES dijital imza</strong> üretilmez. Resmi GİB yüklemesi için
          mali müşavirinizin LUCA/ETA gibi sertifikalı yazılımına XML dosyalarını aktarın.
        </div>
      </div>

      <div className="ovw">
        <div className="ovw-left">
          <div className="yr-step">
            <button className="yr-btn" onClick={() => setYear(y => y - 1)} disabled={year <= CUR_YEAR - 4} aria-label="Önceki yıl">
              <Icon name="chevLeft" size={15} />
            </button>
            <span className="yr-val">{year}</span>
            <button className="yr-btn" onClick={() => setYear(y => y + 1)} disabled={year >= CUR_YEAR} aria-label="Sonraki yıl">
              <Icon name="chevRight" size={15} />
            </button>
          </div>
          <div className="ovw-stat">
            <b>{producedCount}<span style={{ color: 'var(--ink-3)', fontWeight: 400 }}> / 12</span></b>
            <span>ay üretildi</span>
          </div>
        </div>
        <div className="ovw-bar">
          <div className="ovw-bar-label">
            <span>{isLoading ? 'Yükleniyor…' : 'Yıllık ilerleme'}</span>
            <span>%{pct}</span>
          </div>
          <div className="ovw-track"><div className="ovw-fill pos" style={{ width: `${pct}%` }} /></div>
        </div>
      </div>

      <div className="mgrid">
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
  );
}
