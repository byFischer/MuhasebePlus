
const TRY = (v) => v == null ? '—' : Number(v).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY', minimumFractionDigits: 2 });

const MONTHS_TR = ['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'];

function periodLabel(type, d) {
  if (!d) return '';
  if (type === 'MONTH') return `${d.year} ${MONTHS_TR[(d.month || 1) - 1]}`;
  if (type === 'QUARTER') return `${d.year} Q${d.period}`;
  return `${d.year}`;
}

function StatusBadge({ status }) {
  const map = { DRAFT: ['gri', 'Taslak'], FINALIZED: ['mavi', 'Onaylandı'], SUBMITTED: ['yesil', 'Gönderildi'] };
  const [cls, label] = map[status] || ['gri', status];
  return <span className={`pill ${cls}`}><span className="dot" />{label}</span>;
}

function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

function VatDetail({ d }) {
  const lines = d?.declarationLines || [];
  const grouped = {};
  for (const l of lines) { grouped[l.lineCode] = l; }

  return (
    <div className="col gap-12">
      <div>
        <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 8 }}>Tablo I — Matrah ve Hesaplanan KDV</div>
        <table className="tbl" style={{ width: '100%' }}>
          <thead><tr><th>Oran</th><th className="num">Matrah</th><th className="num">KDV</th></tr></thead>
          <tbody>
            {lines.filter(l => ['30','35','39'].includes(l.lineCode)).map(l => (
              <tr key={l.lineCode}>
                <td>%{l.lineCode === '30' ? '1' : l.lineCode === '35' ? '10' : '20'}</td>
                <td className="num mono">{TRY(l.baseAmount)}</td>
                <td className="num mono">{TRY(l.vatAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="row gap-24" style={{ flexWrap: 'wrap' }}>
        <div><div className="muted" style={{ fontSize: 11 }}>Toplam KDV</div><div className="mono" style={{ fontWeight: 700 }}>{TRY(d?.totalSalesVat)}</div></div>
        <div><div className="muted" style={{ fontSize: 11 }}>İnd. KDV</div><div className="mono">{TRY(d?.totalPurchasesVat)}</div></div>
        <div><div className="muted" style={{ fontSize: 11 }}>Devreden</div><div className="mono">{TRY(d?.previousPeriodCarryover)}</div></div>
        <div><div className="muted" style={{ fontSize: 11, color: 'var(--accent)' }}>Ödenecek</div><div className="mono" style={{ fontWeight: 700, color: 'var(--accent)' }}>{TRY(d?.payableVat)}</div></div>
        <div><div className="muted" style={{ fontSize: 11 }}>Sonraki Devir</div><div className="mono">{TRY(d?.nextPeriodCarryover)}</div></div>
      </div>
    </div>
  );
}

function WithholdingDetail({ d }) {
  const lines = d?.declarationLines || [];
  return (
    <div>
      <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 8 }}>Tevkifat Kodu Bazlı Satırlar</div>
      <table className="tbl" style={{ width: '100%' }}>
        <thead><tr><th>Kod</th><th>Açıklama</th><th className="num">Belge</th><th className="num">Matrah</th><th className="num">Tevkifat</th></tr></thead>
        <tbody>
          {lines.map((l, i) => (
            <tr key={i}>
              <td className="mono">{l.lineCode}</td>
              <td className="muted" style={{ fontSize: 12 }}>{l.description}</td>
              <td className="num">{l.documentCount || '—'}</td>
              <td className="num mono">{TRY(l.baseAmount)}</td>
              <td className="num mono" style={{ fontWeight: 600 }}>{TRY(l.vatAmount)}</td>
            </tr>
          ))}
          {lines.length === 0 && <tr><td colSpan={5} className="empty">Tevkifat kaydı bulunamadı</td></tr>}
        </tbody>
      </table>
    </div>
  );
}

function BaBsDetail({ d }) {
  const lines = d?.declarationLines || [];
  return (
    <div>
      <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 8 }}>
        {d?.babsType === 'BA' ? 'Ba Formu — Alış' : 'Bs Formu — Satış'} (≥ 5.000 TL Mükellefler)
      </div>
      <table className="tbl" style={{ width: '100%' }}>
        <thead><tr><th>VKN</th><th>Ünvan</th><th className="num">Belge</th><th className="num">Tutar (KDV hariç)</th></tr></thead>
        <tbody>
          {lines.map((l, i) => (
            <tr key={i}>
              <td className="mono" style={{ fontSize: 12 }}>{l.taxNumber || l.lineCode}</td>
              <td className="muted" style={{ fontSize: 12 }}>{l.description}</td>
              <td className="num">{l.documentCount || '—'}</td>
              <td className="num mono">{TRY(l.baseAmount)}</td>
            </tr>
          ))}
          {lines.length === 0 && <tr><td colSpan={4} className="empty">Kayıt bulunamadı</td></tr>}
        </tbody>
      </table>
    </div>
  );
}

function GenericDetail({ d }) {
  const snap = d?.dataSnapshot || {};
  return (
    <div className="col gap-12">
      <div className="row gap-24" style={{ flexWrap: 'wrap' }}>
        {snap.totalRevenue != null && <div><div className="muted" style={{ fontSize: 11 }}>Toplam Gelir</div><div className="mono">{TRY(snap.totalRevenue)}</div></div>}
        {snap.totalExpense != null && <div><div className="muted" style={{ fontSize: 11 }}>Toplam Gider</div><div className="mono">{TRY(snap.totalExpense)}</div></div>}
        {snap.netProfit != null && <div><div className="muted" style={{ fontSize: 11 }}>Net Kâr</div><div className="mono" style={{ fontWeight: 700 }}>{TRY(snap.netProfit)}</div></div>}
        {snap.taxRate != null && <div><div className="muted" style={{ fontSize: 11 }}>Vergi Oranı</div><div className="mono">%{snap.taxRate}</div></div>}
        {d?.totalPayable != null && <div><div className="muted" style={{ fontSize: 11, color: 'var(--accent)' }}>Ödenecek</div><div className="mono" style={{ fontWeight: 700, color: 'var(--accent)' }}>{TRY(d.totalPayable)}</div></div>}
      </div>
      {d?.declarationLines?.length > 0 && (
        <table className="tbl" style={{ width: '100%' }}>
          <thead><tr><th>Satır</th><th>Açıklama</th><th className="num">Matrah</th><th className="num">Vergi</th></tr></thead>
          <tbody>
            {d.declarationLines.map((l, i) => (
              <tr key={i}>
                <td className="mono" style={{ fontSize: 12 }}>{l.lineCode}</td>
                <td className="muted" style={{ fontSize: 12 }}>{l.description}</td>
                <td className="num mono">{TRY(l.baseAmount)}</td>
                <td className="num mono">{TRY(l.vatAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default function BeyannameDetailDrawer({
  open, onClose, declaration, declarationType, periodType,
  onFinalize, onSubmit, onDelete, isFinalizePending, isSubmitPending, isDeletePending,
  downloadService,
}) {
  if (!open || !declaration) return null;

  const d = declaration;
  const isDraft      = d.status === 'DRAFT';
  const isFinalized  = d.status === 'FINALIZED';

  const handleDownloadXml = async () => {
    try {
      const blob = await downloadService.downloadXml(d.declarationId || d.id);
      downloadBlob(blob, `beyanname-${d.year}-${d.month || d.period || ''}.xml`);
    } catch { /* error handled by API */ }
  };

  const handleDownloadExcel = async () => {
    try {
      const blob = await downloadService.downloadExcel(d.declarationId || d.id);
      downloadBlob(blob, `beyanname-${d.year}-${d.month || d.period || ''}.xlsx`);
    } catch { /* error handled by API */ }
  };

  const renderBody = () => {
    if (declarationType === 'KDV1' || declarationType === 'KDV2') return <VatDetail d={d} />;
    if (declarationType === 'MUHTASAR') return <WithholdingDetail d={d} />;
    if (declarationType === 'BA' || declarationType === 'BS') return <BaBsDetail d={d} />;
    return <GenericDetail d={d} />;
  };

  return (
    <div style={{ position: 'fixed', inset: 0, zIndex: 900, display: 'flex', justifyContent: 'flex-end' }}>
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.35)' }} onClick={onClose} />
      <div style={{
        position: 'relative', width: 560, height: '100%',
        background: 'var(--surface)', boxShadow: '-4px 0 24px rgba(0,0,0,0.12)',
        display: 'flex', flexDirection: 'column', overflowY: 'auto',
      }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--line)', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 15 }}>{declarationType} Beyannamesi</div>
            <div className="muted" style={{ fontSize: 12 }}>{periodLabel(periodType, d)}</div>
          </div>
          <StatusBadge status={d.status} />
          <button className="tb-icon-btn" onClick={onClose} style={{ fontSize: 18 }}>×</button>
        </div>

        <div style={{ flex: 1, padding: '20px' }}>
          {renderBody()}
        </div>

        <div style={{ padding: '12px 20px', borderTop: '1px solid var(--line)', display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
          {downloadService?.downloadXml && (
            <button className="btn ghost sm" onClick={handleDownloadXml}>XML İndir</button>
          )}
          <button className="btn ghost sm" onClick={handleDownloadExcel}>Excel İndir</button>
          {isDraft && onFinalize && (
            <button className="btn primary sm" onClick={() => onFinalize(d.declarationId || d.id)} disabled={isFinalizePending}>
              {isFinalizePending ? 'Onaylanıyor...' : 'Onayla'}
            </button>
          )}
          {isFinalized && onSubmit && (
            <button className="btn primary sm" onClick={() => onSubmit(d.declarationId || d.id)} disabled={isSubmitPending}>
              {isSubmitPending ? '...' : 'Gönderildi Olarak İşaretle'}
            </button>
          )}
          {isDraft && onDelete && (
            <button className="btn ghost sm" style={{ color: 'var(--red)' }} onClick={() => { onDelete(d.declarationId || d.id); onClose(); }} disabled={isDeletePending}>
              Sil
            </button>
          )}
          <button className="btn ghost sm" onClick={onClose}>Kapat</button>
        </div>
      </div>
    </div>
  );
}
