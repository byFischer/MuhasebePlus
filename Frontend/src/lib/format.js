// MuhasebePlus — currency & number formatters
export const TRY = (n, opts = {}) => {
  const cur = opts.currency || '₺';
  const v = Number.isFinite(Number(n)) ? Number(n) : 0;
  const sign = v < 0 ? '-' : '';
  return sign + cur + Math.abs(v).toLocaleString('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

export const fmtN = (n) => Number(n || 0).toLocaleString('tr-TR');

export const formatBytes = (bytes) => {
  const n = Number(bytes);
  if (!Number.isFinite(n) || n <= 0) return '0 KB';
  if (n < 1024) return `${n} B`;
  const kb = n / 1024;
  if (kb < 1024) return `${Math.round(kb)} KB`;
  return `${(kb / 1024).toFixed(1)} MB`;
};

const TR_MONTHS_FULL = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];

const pad2 = (n) => String(n).padStart(2, '0');

export const toDate = (v) => {
  if (!v) return null;
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? null : v;
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? null : d;
};

export const fmtDate = (v) => {
  const d = toDate(v);
  return d ? `${pad2(d.getDate())}.${pad2(d.getMonth() + 1)}.${d.getFullYear()}` : '';
};

export const fmtDateRange = (s, e) => {
  const a = fmtDate(s), b = fmtDate(e);
  if (!a && !b) return '';
  if (!a) return b;
  if (!b) return a;
  return `${a} – ${b}`;
};

export const monthLabelTR = (v) => {
  const d = toDate(v);
  return d ? TR_MONTHS_FULL[d.getMonth()] : '';
};

// ISO date string (yyyy-mm-dd) for <input type="date">
export const toIsoDate = (v) => {
  const d = toDate(v);
  return d ? `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}` : '';
};

// Compute date range for a named period; returns { startDate, endDate } as ISO strings.
export const computeRange = (period, ref = new Date()) => {
  const today = new Date(ref.getFullYear(), ref.getMonth(), ref.getDate());
  let s = today, e = today;
  switch (period) {
    case 'Bugün':
      s = today; e = today; break;
    case 'Bu hafta': {
      const day = today.getDay() || 7; // 1..7 (Mon..Sun)
      s = new Date(today); s.setDate(today.getDate() - (day - 1));
      e = new Date(s); e.setDate(s.getDate() + 6);
      break;
    }
    case 'Bu ay':
      s = new Date(today.getFullYear(), today.getMonth(), 1);
      e = new Date(today.getFullYear(), today.getMonth() + 1, 0);
      break;
    case 'Bu çeyrek': {
      const q = Math.floor(today.getMonth() / 3);
      s = new Date(today.getFullYear(), q * 3, 1);
      e = new Date(today.getFullYear(), q * 3 + 3, 0);
      break;
    }
    case 'Bu yıl':
      s = new Date(today.getFullYear(), 0, 1);
      e = new Date(today.getFullYear(), 11, 31);
      break;
    default:
      // 'Özel' or unknown — keep the reference date
      s = today; e = today;
  }
  return { startDate: toIsoDate(s), endDate: toIsoDate(e) };
};
