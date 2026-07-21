import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import BarLineChart from '@/components/mp/charts/BarLineChart';
import {
  TRY,
  fmtN,
  formatBytes,
  fmtDate,
  fmtDateRange,
  monthLabelTR,
  computeRange,
} from '@/lib/format';
import {
  useReports,
  useReportPreview,
  useGenerateReport,
  useDownloadReport,
  useDeleteReport,
  useStatement,
  useReconciliation,
} from '@/hooks/useReports';
import { useCustomers } from '@/hooks/useCustomers';

const REPORT_TYPES = [
  { value: 'PROFIT_LOSS',  label: 'Kar-Zarar' },
  { value: 'CASH_FLOW',    label: 'Nakit Akış' },
  { value: 'AR_AGING',     label: 'Cari Yaşlandırma' },
  { value: 'VAT_PREP',     label: 'KDV Beyanname Hazırlık' },
  { value: 'STOCK_STATUS', label: 'Stok Durum' },
  { value: "COLLECTION_PERFORMANCE", label: "Tahsilat Performansı" },
  { value: "SLOW_INVENTORY", label: "Yavaş Dönen Stok" },
  { value: "BUDGET_VARIANCE", label: "Bütçe-Gerçekleşen Sapma" },
  { value: "BANK_RECONCILIATION", label: "Banka/Kasa Mutabakat" },
  { value: "EXECUTIVE_SUMMARY", label: "Yönetici Finans Sağlığı Özeti" },
  { value: "STATEMENT", label: "Cari Hesap Ekstresi" },
  { value: "RECONCILIATION", label: "Cari Mutabakat Mektubu" },
  { value: "TRIAL_BALANCE",    label: "Mizan" },
  { value: "INCOME_STATEMENT", label: "Gelir Tablosu" },
  { value: "BALANCE_SHEET",    label: "Bilanço" },
  { value: "JOURNAL_LISTING",  label: "Yevmiye Dökümü" },
];

const PERIODS = ['Bugün', 'Bu hafta', 'Bu ay', 'Bu çeyrek', 'Bu yıl', 'Özel'];

const TYPE_LABELS = REPORT_TYPES.reduce((acc, t) => {
  acc[t.value] = t.label;
  return acc;
}, {});
const isKnownReportType = (value) => REPORT_TYPES.some(t => t.value === value);

// Anlık (date-bağımsız) raporlar — UI'da tarih kontrolleri pasif görünür
const SNAPSHOT_TYPES = new Set(['AR_AGING', 'STOCK_STATUS']);
const CUSTOMER_TYPES = new Set(['STATEMENT', 'RECONCILIATION']);

function deriveReportName(reportType, startDate, endDate) {
  const base = TYPE_LABELS[reportType] || reportType || 'Rapor';
  if (SNAPSHOT_TYPES.has(reportType)) return base;
  const sm = monthLabelTR(startDate);
  const em = monthLabelTR(endDate);
  if (sm && em) return sm === em ? `${base} ${sm}` : `${base} ${sm}-${em}`;
  return base;
}

function toneColor(tone) {
  if (tone === 'pos') return 'var(--pos)';
  if (tone === 'neg') return 'var(--neg)';
  return undefined;
}

function formatKpiValue(v, fmt) {
  const n = Number(v ?? 0);
  if (fmt === 'count') return fmtN(n);
  return TRY(n);
}

function gridClassFor(n) {
  if (n <= 1) return 'col';
  if (n === 2) return 'grid-2';
  if (n === 3) return 'grid-3';
  return 'grid-4';
}

// Backend'den gelen preview hem yeni shape ({ sections: [...] }) hem de eski
// flat shape ({ kpis, monthlySeries, ... }) olabilir. Ikisini de tek-elemanli
// section array'ine normalize et.
function toSections(preview) {
  if (!preview) return [];
  if (Array.isArray(preview.sections)) return preview.sections;
  return [{
    title: null,
    kpis: preview.kpis || [],
    progressValue: preview.progressValue ?? null,
    progressLabel: preview.progressLabel ?? null,
    monthlySeries: preview.monthlySeries || [],
    buckets: preview.buckets || [],
  }];
}

function PreviewSectionView({ section, loading }) {
  const kpis = section.kpis || [];
  const monthly = section.monthlySeries || [];
  const buckets = section.buckets || [];
  const progressValue = section.progressValue;
  const progressLabel = section.progressLabel;

  const months = monthly.map(m => m.month);
  const revenueArr = monthly.map(m => Number(m.income || 0));
  const expenseArr = monthly.map(m => Number(m.expense || 0));

  const bucketLabels = buckets.map(b => b.label);
  const bucketValues = buckets.map(b => Number(b.value || 0));
  const bucketZeroes = buckets.map(() => 0);

  const marginNum = Number(progressValue ?? 0);
  const marginClamped = Math.max(0, Math.min(100, marginNum));

  const empty =
    !loading && kpis.length === 0 && monthly.length === 0 && buckets.length === 0;

  return (
    <div className="col gap-12">
      {section.title && <h4 style={{ margin: 0 }}>{section.title}</h4>}

      {kpis.length > 0 && (
        <div className={gridClassFor(kpis.length)}>
          {kpis.map((k, i) => (
            <div key={i}>
              <div className="muted" style={{ fontSize: 11 }}>{k.label}</div>
              <div
                className="mono tnum"
                style={{ fontSize: 18, fontWeight: 600, color: toneColor(k.tone) }}
              >
                {loading ? '—' : formatKpiValue(k.value, k.format)}
              </div>
            </div>
          ))}
        </div>
      )}

      {progressValue != null && (
        <div>
          <div
            className="row"
            style={{ justifyContent: 'space-between', fontSize: 12, marginBottom: 6 }}
          >
            <span>{progressLabel || 'İlerleme'}</span>
            <span className="mono">
              <b>{loading ? '—' : `${marginNum.toFixed(1)}%`}</b>
            </span>
          </div>
          <div className="bar"><span style={{ width: `${marginClamped}%` }} /></div>
        </div>
      )}

      {monthly.length > 0 && (
        <BarLineChart months={months} revenue={revenueArr} expense={expenseArr} />
      )}

      {monthly.length === 0 && buckets.length > 0 && (
        <BarLineChart months={bucketLabels} revenue={bucketValues} expense={bucketZeroes} />
      )}

      {empty && <div className="empty">Görüntülenecek veri yok</div>}
    </div>
  );
}

function StatementPreview({ data, loading }) {
  if (loading) return <div className="empty">Yükleniyor…</div>;
  if (!data) return <div className="empty">Veri yok</div>;
  const lines = data.lines || [];
  return (
    <div className="col gap-12">
      <div className="grid-2" style={{ gap: 8 }}>
        <div className="field"><label>Açılış Bakiyesi</label><b className="mono">{TRY(data.openingBalance)}</b></div>
        <div className="field"><label>Kapanış Bakiyesi</label><b className="mono">{TRY(data.closingBalance)}</b></div>
        <div className="field"><label>Toplam Borç</label><b className="mono">{TRY(data.totalDebit)}</b></div>
        <div className="field"><label>Toplam Alacak</label><b className="mono">{TRY(data.totalCredit)}</b></div>
      </div>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Tarih</th><th>Açıklama</th><th>Belge</th><th>Borç</th><th>Alacak</th><th>Bakiye</th></tr>
          </thead>
          <tbody>
            {lines.length === 0 && <tr><td colSpan="6" className="empty">Hareket yok</td></tr>}
            {lines.map((l, i) => (
              <tr key={i}>
                <td className="muted">{fmtDate(l.date)}</td>
                <td>{l.description}</td>
                <td className="mono">{l.documentNumber || '—'}</td>
                <td className="mono">{TRY(l.debit)}</td>
                <td className="mono">{TRY(l.credit)}</td>
                <td className="mono">{TRY(l.balance)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ReconciliationPreview({ data, loading }) {
  if (loading) return <div className="empty">Yükleniyor…</div>;
  if (!data) return <div className="empty">Veri yok</div>;
  const items = data.openItems || [];
  return (
    <div className="col gap-12">
      <div className="grid-2" style={{ gap: 8 }}>
        <div className="field"><label>Açılış Bakiyesi</label><b className="mono">{TRY(data.openingBalance)}</b></div>
        <div className="field"><label>Kapanış Bakiyesi</label><b className="mono">{TRY(data.closingBalance)}</b></div>
        <div className="field"><label>Toplam Faturalanan</label><b className="mono">{TRY(data.totalInvoiced)}</b></div>
        <div className="field"><label>Toplam Tahsil Edilen</label><b className="mono">{TRY(data.totalCollected)}</b></div>
      </div>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Fatura</th><th>Tarih</th><th>Vade</th><th>Tutar</th><th>Ödenen</th><th>Kalan</th><th>Gecikme</th></tr>
          </thead>
          <tbody>
            {items.length === 0 && <tr><td colSpan="7" className="empty">Açık kalem yok</td></tr>}
            {items.map((it, i) => (
              <tr key={i}>
                <td className="mono">{it.invoiceNumber}</td>
                <td className="muted">{fmtDate(it.invoiceDate)}</td>
                <td className="muted">{fmtDate(it.dueDate)}</td>
                <td className="mono">{TRY(it.totalAmount)}</td>
                <td className="mono">{TRY(it.paidAmount)}</td>
                <td className="mono">{TRY(it.remainingAmount)}</td>
                <td className="muted">{it.daysOverdue > 0 ? `${it.daysOverdue} gün` : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function RaporPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [reportType, setReportType] = useState(() => {
    const type = searchParams.get('type');
    return isKnownReportType(type) ? type : 'PROFIT_LOSS';
  });
  const [period, setPeriod] = useState('Bu ay');
  const initial = useMemo(() => computeRange('Bu ay'), []);
  const [startDate, setStartDate] = useState(initial.startDate);
  const [endDate, setEndDate] = useState(initial.endDate);
  const [customerId, setCustomerId] = useState('');
  const { data: customers = [] } = useCustomers();

  const showCustomer = CUSTOMER_TYPES.has(reportType);

  useEffect(() => {
    const type = searchParams.get('type');
    if (isKnownReportType(type) && type !== reportType) {
      setReportType(type);
    }
  }, [searchParams, reportType]);

  const changeReportType = (nextType) => {
    setReportType(nextType);
    const next = new URLSearchParams(searchParams);
    next.set('type', nextType);
    setSearchParams(next, { replace: true });
  };

  useEffect(() => {
    if (period === 'Özel') return;
    const r = computeRange(period);
    setStartDate(r.startDate);
    setEndDate(r.endDate);
  }, [period]);

  const previewDto = useMemo(
    () => ({ reportType, startDate, endDate }),
    [reportType, startDate, endDate]
  );

  const { data: reports = [], isLoading: reportsLoading, isError: reportsError, refetch: refetchReports } = useReports();
  // Hook her zaman çağrılır (rules-of-hooks); müşteri bazlı raporlarda enabled=false ile devre dışı bırakılır
  const { data: preview, isLoading: previewLoading, isError: previewError, refetch: refetchPreview } =
    useReportPreview(previewDto, !CUSTOMER_TYPES.has(reportType));

  const { data: statementData, isLoading: statementLoading } = useStatement(
    customerId, startDate, endDate,
    reportType === 'STATEMENT' && !!customerId
  );
  const { data: reconciliationData, isLoading: reconciliationLoading } = useReconciliation(
    customerId, startDate, endDate,
    reportType === 'RECONCILIATION' && !!customerId
  );
  const generateMut = useGenerateReport();
  const downloadMut = useDownloadReport();
  const deleteMut = useDeleteReport();

  const sections = useMemo(() => toSections(preview), [preview]);
  const isMultiSection = sections.length > 1;

  const previewTitle = `Önizleme — ${TYPE_LABELS[reportType] || reportType}`;
  const isSnapshot = SNAPSHOT_TYPES.has(reportType);
  const previewSub = isSnapshot ? 'anlık veriden' : `${period} · canlı veriden`;

  const onGenerate = () => {
    if (!reportType || !startDate || !endDate) return;
    generateMut.mutate({ reportType, startDate, endDate, format: 'EXCEL' });
  };

  const onDelete = (id) => {
    if (window.confirm('Bu raporu silmek istediğinize emin misiniz?')) {
      deleteMut.mutate(id);
    }
  };

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Raporlar</h1>
          <p className="page-sub">Kar-Zarar, nakit akış, KDV hazırlık ve cari yaşlandırma raporları</p>
        </div>

      </div>

      <div className="grid-2" style={{ gap: 16 }}>
        {/* SOL: Rapor Oluştur */}
        <div className="card">
          <div className="card-h">
            <div>
              <h3>Rapor Oluştur</h3>
              <div className="sub">Tarih aralığı ve format seç, anlık önizle</div>
            </div>
          </div>
          <div className="card-b col gap-12">
            <div className="grid-2">
              <div className="field">
                <label>Rapor Türü</label>
                <select
                  className="select"
                  value={reportType}
                  onChange={(e) => changeReportType(e.target.value)}
                >
                  {REPORT_TYPES.map(t => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>Format</label>
                <div>
                  <span className="pill">XLSX</span>
                </div>
              </div>
            </div>

            <div className="field">
              <label>Dönem</label>
              <div className="seg" style={{ flexWrap: 'wrap', opacity: isSnapshot ? 0.5 : 1 }}>
                {PERIODS.map(p => (
                  <button
                    key={p}
                    className={period === p ? 'on' : ''}
                    onClick={() => !isSnapshot && setPeriod(p)}
                    type="button"
                    disabled={isSnapshot}
                  >
                    {p}
                  </button>
                ))}
              </div>
              {isSnapshot && (
                <div className="muted" style={{ fontSize: 11, marginTop: 6 }}>
                  Bu rapor anlık veriyi kullanır, tarih aralığı dikkate alınmaz.
                </div>
              )}
            </div>

            <div className="grid-2" style={{ opacity: isSnapshot ? 0.5 : 1 }}>
              <div className="field">
                <label>Başlangıç</label>
                <input
                  type="date"
                  className="input mono"
                  value={startDate}
                  onChange={(e) => { setStartDate(e.target.value); setPeriod('Özel'); }}
                  disabled={isSnapshot}
                />
              </div>
              <div className="field">
                <label>Bitiş</label>
                <input
                  type="date"
                  className="input mono"
                  value={endDate}
                  onChange={(e) => { setEndDate(e.target.value); setPeriod('Özel'); }}
                  disabled={isSnapshot}
                />
              </div>
            </div>

            {showCustomer && (
              <div className="field">
                <label>Müşteri *</label>
                <select className="select" value={customerId} onChange={e => setCustomerId(e.target.value)}>
                  <option value="">Müşteri seçin...</option>
                  {customers.map(c => <option key={c.customerId} value={c.customerId}>{c.name} ({c.accountCode || c.taxNumber})</option>)}
                </select>
              </div>
            )}

            <button
              className="btn primary sm"
              style={{ alignSelf: 'flex-start' }}
              onClick={onGenerate}
              disabled={generateMut.isPending}
            >
              <Icon name="play" /> {generateMut.isPending ? 'Oluşturuluyor…' : 'Rapor Oluştur'}
            </button>
          </div>
        </div>

        {/* SAĞ: Önizleme */}
        <div className="card">
          <div className="card-h">
            <div>
              <h3>{previewTitle}</h3>
              <div className="sub">{showCustomer ? (customerId ? customers.find(c => c.customerId === Number(customerId))?.name || 'Bilinmeyen' : 'Müşteri seçin') : previewSub}</div>
            </div>
            {!showCustomer && <span className="pill info">Otomatik güncel</span>}
          </div>
          <div className="card-b col gap-12">
            {reportType === 'STATEMENT' && statementData && (
              <StatementPreview data={statementData} loading={statementLoading} />
            )}
            {reportType === 'STATEMENT' && !statementData && !statementLoading && (
              <div className="empty">{customerId ? 'Veri alınamadı' : 'Önizleme için müşteri seçin'}</div>
            )}
            {reportType === 'RECONCILIATION' && reconciliationData && (
              <ReconciliationPreview data={reconciliationData} loading={reconciliationLoading} />
            )}
            {reportType === 'RECONCILIATION' && !reconciliationData && !reconciliationLoading && (
              <div className="empty">{customerId ? 'Veri alınamadı' : 'Önizleme için müşteri seçin'}</div>
            )}

            {!CUSTOMER_TYPES.has(reportType) && (<>
            {previewError ? (
              <div className="empty">
                Önizleme alınamadı{' '}
                <button className="btn sm" onClick={() => refetchPreview()}>Tekrar Dene</button>
              </div>
            ) : isMultiSection ? (
              <div className="col gap-12">
                {sections.map((s, i) => (
                  <div
                    key={i}
                    style={{
                      padding: 12,
                      border: '1px solid var(--line)',
                      borderRadius: 8,
                    }}
                  >
                    <PreviewSectionView section={s} loading={previewLoading} />
                  </div>
                ))}
              </div>
            ) : (
              <PreviewSectionView
                section={sections[0] || { kpis: [], monthlySeries: [], buckets: [] }}
                loading={previewLoading}
              />
            )}
            </>)}
          </div>
        </div>
      </div>

      {/* Geçmiş Raporlar */}
      <div className="card mt-16">
        <div className="card-h"><h3>Geçmiş Raporlar</h3></div>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Rapor Adı</th>
                <th>Aralık</th>
                <th>Format</th>
                <th>Boyut</th>
                <th>Oluşturan</th>
                <th>Tarih</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {reportsLoading && (
                <tr><td colSpan="8" className="empty">Yükleniyor…</td></tr>
              )}
              {!reportsLoading && reportsError && (
                <tr>
                  <td colSpan="8" className="empty">
                    Veri alınamadı{' '}
                    <button className="btn sm" onClick={() => refetchReports()}>Tekrar Dene</button>
                  </td>
                </tr>
              )}
              {!reportsLoading && !reportsError && reports.map(r => (
                <tr key={r.reportId}>
                  <td className="mono">R-{String(r.reportId).padStart(4, '0')}</td>
                  <td><b>{deriveReportName(r.reportType, r.startDate, r.endDate)}</b></td>
                  <td className="muted">
                    {SNAPSHOT_TYPES.has(r.reportType) ? fmtDate(r.generatedAt || r.createdAt) : fmtDateRange(r.startDate, r.endDate)}
                  </td>
                  <td><span className="pill">{r.format === 'EXCEL' ? 'XLSX' : (r.format || 'XLSX')}</span></td>
                  <td className="muted mono">{formatBytes(r.fileSize)}</td>
                  <td className="muted">{r.createdByUsername || '—'}</td>
                  <td className="muted">{fmtDate(r.generatedAt || r.createdAt)}</td>
                  <td>
                    <div className="row gap-4">
                      <button className="tb-icon-btn" title="İndir" onClick={() => downloadMut.mutate(r.reportId)}>
                        <Icon name="download" size={14} />
                      </button>
                      <button className="tb-icon-btn" title="Sil" onClick={() => onDelete(r.reportId)}>
                        <Icon name="trash" size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {!reportsLoading && !reportsError && reports.length === 0 && (
                <tr><td colSpan="8" className="empty">Henüz rapor yok</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
