import { describe, expect, it } from 'vitest';
import { TRY, computeRange, fmtDate, fmtDateRange, fmtN, formatBytes, monthLabelTR, toDate, toIsoDate } from './format';

describe('format helpers', () => {
  // Formats currency-like values using Turkish separators and preserves negative sign.
  it('TRY formats finite and invalid numbers safely', () => {
    expect(TRY(-1234.5, { currency: 'TL ' })).toBe('-TL 1.234,50');
    expect(TRY('not-a-number', { currency: 'TL ' })).toBe('TL 0,00');
    expect(fmtN(12500)).toBe('12.500');
  });

  // Formats byte values across zero, byte, kilobyte, and megabyte ranges.
  it('formatBytes returns compact storage labels', () => {
    expect(formatBytes(0)).toBe('0 KB');
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2 KB');
    expect(formatBytes(2 * 1024 * 1024)).toBe('2.0 MB');
  });

  // Formats valid dates and hides invalid date input.
  it('formats date values and ranges', () => {
    expect(fmtDate('2026-05-03T00:00:00')).toBe('03.05.2026');
    expect(fmtDate('bad-date')).toBe('');
    expect(fmtDateRange('2026-05-01', '2026-05-31')).toContain('01.05.2026');
    expect(fmtDateRange(null, '2026-05-31')).toBe('31.05.2026');
    expect(fmtDateRange('2026-05-01', null)).toBe('01.05.2026');
    expect(fmtDateRange(null, null)).toBe('');
    expect(toDate(null)).toBeNull();
    expect(toDate(new Date('bad-date'))).toBeNull();
    expect(toIsoDate(new Date(2026, 4, 9))).toBe('2026-05-09');
  });

  // Computes common accounting dashboard ranges from a fixed reference date.
  it('computeRange returns the current month boundaries', () => {
    const result = computeRange('Bu ay', new Date(2026, 4, 20));

    expect(result).toEqual({ startDate: '2026-05-01', endDate: '2026-05-31' });
    expect(monthLabelTR(new Date(2026, 4, 1))).toBeTruthy();
  });

  // Computes the main dashboard quick ranges from a fixed date.
  it('computeRange supports week, quarter, year, and fallback ranges', () => {
    const ref = new Date(2026, 4, 20);

    expect(computeRange('Bug\u00fcn', ref)).toEqual({ startDate: '2026-05-20', endDate: '2026-05-20' });
    expect(computeRange('Bu hafta', ref)).toEqual({ startDate: '2026-05-18', endDate: '2026-05-24' });
    expect(computeRange('Bu \u00e7eyrek', ref)).toEqual({ startDate: '2026-04-01', endDate: '2026-06-30' });
    expect(computeRange('Bu y\u0131l', ref)).toEqual({ startDate: '2026-01-01', endDate: '2026-12-31' });
    expect(computeRange('\u00d6zel', ref)).toEqual({ startDate: '2026-05-20', endDate: '2026-05-20' });
    expect(monthLabelTR('bad-date')).toBe('');
  });
});
