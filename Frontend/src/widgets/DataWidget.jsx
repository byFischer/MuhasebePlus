import { BarChart, Bar, LineChart, Line, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { TRY } from '@/lib/format';

const COLORS = {
  accent: '#f97316',
  pos: '#22c55e',
  neg: '#ef4444',
  warn: '#eab308',
  info: '#3b82f6',
  purple: '#a855f7',
  teal: '#14b8a6',
  pink: '#ec4899',
};

function getColor(colorKey) {
  return COLORS[colorKey] || COLORS.accent;
}

// Ham DB değerlerini Türkçe'ye çevirir (enum, boolean vb.)
const VALUE_TR = {
  // İşlem tipi
  INCOME: 'Gelir', EXPENSE: 'Gider',
  // Fatura tipi
  sale: 'Satış', purchase: 'Alış',
  SALE: 'Satış', PURCHASE: 'Alış',
  // Ödeme durumu
  paid: 'Ödendi', pending: 'Bekliyor', draft: 'Taslak', overdue: 'Vadesi Geçmiş',
  PAID: 'Ödendi', PENDING: 'Bekliyor', DRAFT: 'Taslak', OVERDUE: 'Vadesi Geçmiş',
  // Müşteri tipi
  INDIVIDUAL: 'Bireysel', CORPORATE: 'Kurumsal',
  // Para birimi
  TRY: '₺ TL', USD: '$ Dolar', EUR: '€ Euro',
  // Boolean
  true: 'Evet', false: 'Hayır',
  // Şablon tipi
  INVOICE: 'Fatura', STOCK_ADJUSTMENT: 'Stok Düzeltme',
  CUSTOMER_TRANSACTION: 'Müşteri İşlemi', BANK_TRANSFER: 'Banka Transferi',
  // Rapor tipi
  PROFIT_LOSS: 'Kar/Zarar', CASH_FLOW: 'Nakit Akışı', AR_AGING: 'Alacak Yaşlandırma',
  VAT_PREP: 'KDV Hazırlık', STOCK_STATUS: 'Stok Durumu',
  COLLECTION_PERFORMANCE: 'Tahsilat Performansı', SLOW_INVENTORY: 'Yavaş Stok',
  BUDGET_VARIANCE: 'Bütçe Sapması', BANK_RECONCILIATION: 'Banka Mutabakatı',
  EXECUTIVE_SUMMARY: 'Yönetici Özeti',
  // Format
  EXCEL: 'Excel',
};

function translateValue(value) {
  if (value == null) return '-';
  const str = String(value);
  return VALUE_TR[str] ?? str;
}

function KpiRenderer({ data, config }) {
  const rows = data?.data || [];
  if (rows.length === 0) return <Empty />;
  const row = rows[0];
  const valueKey = Object.keys(row).find(k => k !== 'label') || 'value';
  const value = row[valueKey];
  const numeric = Number(value) || 0;

  return (
    <div style={{ textAlign: 'center', padding: '20px 0' }}>
      <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--ink)' }}>
        {typeof value === 'number' || !isNaN(numeric) ? TRY(numeric) : value}
      </div>
      <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 4 }}>
        {config.title || 'Toplam'}
      </div>
    </div>
  );
}

function TableRenderer({ data }) {
  const rows = data?.data || [];
  const columns = data?.columns || [];
  if (rows.length === 0) return <Empty />;

  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', fontSize: 12, borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--line)' }}>
            {columns.map(col => (
              <th key={col.key} style={{ textAlign: 'left', padding: '6px 8px', color: 'var(--ink-3)', fontWeight: 500 }}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} style={{ borderBottom: '1px solid var(--line)' }}>
              {columns.map(col => (
                <td key={col.key} style={{ padding: '6px 8px', color: 'var(--ink)' }}>
                  {formatCell(row[col.key], col.type)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function BarChartRenderer({ data, config }) {
  const rows = data?.data || [];
  if (rows.length === 0) return <Empty />;
  const color = getColor(config.color);
  const keys = Object.keys(rows[0]);
  const xKey = keys[0];
  const yKey = keys[1];

  return (
    <ResponsiveContainer width="100%" height={220}>
      <BarChart data={rows}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--line)" />
        <XAxis dataKey={xKey} tickFormatter={translateValue} tick={{ fontSize: 10, fill: 'var(--ink-3)' }} />
        <YAxis tick={{ fontSize: 10, fill: 'var(--ink-3)' }} />
        <Tooltip
          contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid var(--line)' }}
          formatter={(v) => [TRY(v)]}
          labelFormatter={translateValue}
        />
        <Bar dataKey={yKey} fill={color} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

function LineChartRenderer({ data, config }) {
  const rows = data?.data || [];
  if (rows.length === 0) return <Empty />;
  const color = getColor(config.color);
  const keys = Object.keys(rows[0]);
  const xKey = keys[0];
  const yKey = keys[1];

  return (
    <ResponsiveContainer width="100%" height={220}>
      <LineChart data={rows}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--line)" />
        <XAxis dataKey={xKey} tickFormatter={translateValue} tick={{ fontSize: 10, fill: 'var(--ink-3)' }} />
        <YAxis tick={{ fontSize: 10, fill: 'var(--ink-3)' }} />
        <Tooltip
          contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid var(--line)' }}
          formatter={(v) => [TRY(v)]}
          labelFormatter={translateValue}
        />
        <Line type="monotone" dataKey={yKey} stroke={color} strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}

function PieChartRenderer({ data }) {
  const rows = data?.data || [];
  if (rows.length === 0) return <Empty />;
  const keys = Object.keys(rows[0]);
  const nameKey = keys[0];
  const valueKey = keys[1];
  const palette = Object.values(COLORS);
  // Pie için verinin name değerlerini de çeviriyoruz
  const translatedRows = rows.map(r => ({ ...r, [nameKey]: translateValue(r[nameKey]) }));

  return (
    <ResponsiveContainer width="100%" height={220}>
      <PieChart>
        <Pie
          data={translatedRows}
          dataKey={valueKey}
          nameKey={nameKey}
          cx="50%"
          cy="50%"
          innerRadius={50}
          outerRadius={80}
          paddingAngle={3}
        >
          {translatedRows.map((_, i) => (
            <Cell key={i} fill={palette[i % palette.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{ fontSize: 12, borderRadius: 8 }}
          formatter={(v) => [TRY(v)]}
        />
        <Legend wrapperStyle={{ fontSize: 11 }} />
      </PieChart>
    </ResponsiveContainer>
  );
}

function ProgressRenderer({ data, config }) {
  const rows = data?.data || [];
  if (rows.length === 0) return <Empty />;
  const row = rows[0];
  const value = Number(row.value || row[Object.keys(row)[1]] || 0);
  const max = Number(row.max || row.goal || 100);
  const pct = max > 0 ? Math.min((value / max) * 100, 100) : 0;
  const color = getColor(config.color);

  return (
    <div style={{ padding: '12px 0' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 6 }}>
        <span style={{ color: 'var(--ink-3)' }}>{config.title || 'İlerleme'}</span>
        <span style={{ fontWeight: 600 }}>%{pct.toFixed(1)}</span>
      </div>
      <div style={{ height: 8, background: 'var(--bg-2)', borderRadius: 4, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 4, transition: 'width 0.5s ease' }} />
      </div>
    </div>
  );
}

function Empty() {
  return <div className="empty">Veri yok</div>;
}

function formatCell(value, type) {
  if (value == null) return '-';
  if (type === 'MEASURE' && (typeof value === 'number' || !isNaN(Number(value)))) return TRY(Number(value));
  if (typeof value === 'object') return JSON.stringify(value);
  return translateValue(value);
}

export default function DataWidget({ data, config }) {
  const cfg = config || {};
  const visual = cfg.visualConfig || cfg || {};
  const chartType = (visual.chartType || cfg.chartType || 'TABLE').toString().toUpperCase();

  if (!data) return <Empty />;

  switch (chartType) {
    case 'KPI':
      return <KpiRenderer data={data} config={visual} />;
    case 'BAR':
      return <BarChartRenderer data={data} config={visual} />;
    case 'LINE':
      return <LineChartRenderer data={data} config={visual} />;
    case 'PIE':
      return <PieChartRenderer data={data} config={visual} />;
    case 'PROGRESS':
      return <ProgressRenderer data={data} config={visual} />;
    case 'TABLE':
    default:
      return <TableRenderer data={data} config={visual} />;
  }
}
