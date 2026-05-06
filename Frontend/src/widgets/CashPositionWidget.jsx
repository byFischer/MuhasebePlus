import Donut from '@/components/mp/charts/Donut';
import { TRY } from '@/lib/format';
import { useColorMode } from '@/components/ui/color-mode';

const currencySymbol = (c) => c === 'TRY' ? '₺' : c === 'USD' ? '$' : '€';

export default function CashPositionWidget({ D, mode, variant }) {
  const { colorMode } = useColorMode();
  const isDark = colorMode === 'dark';

  const v = variant || (mode === 'detail' ? 'l' : 'm');

  const hue = isDark ? 245 : 38;
  const chromaBase = isDark ? 0.20 : 0.14;
  const lightBase = isDark ? 0.64 : 0.62;

  const segments = D.banks.map((b, i) => ({
    color: `oklch(${lightBase - i * 0.05} ${chromaBase - i * 0.02} ${hue})`,
    value: b.balance || 0,
    label: b.name,
  }));

  if (v === 'l') {
    const detailSegments = D.banks.map((b, i) => ({
      color: `oklch(${lightBase - i * 0.04 - 0.02} ${chromaBase - i * 0.014} ${hue + i * 6})`,
      value: b.balance || 0,
      label: b.name,
    }));
    return (
      <div className="col gap-16">
        <div className="row" style={{ justifyContent: 'center' }}>
          <Donut size={280} segments={detailSegments} />
        </div>
        <table className="table">
          <thead>
            <tr><th>Hesap</th><th>IBAN</th><th>Para</th><th className="num">Bakiye</th><th>Son Hareket</th></tr>
          </thead>
          <tbody>
            {D.banks.map(b => (
              <tr key={b.id}>
                <td><b>{b.name}</b></td>
                <td className="mono muted" style={{ fontSize: 11 }}>{b.iban}</td>
                <td>{b.currency}</td>
                <td className="num mono tnum"><b>{TRY(b.balance || 0, { currency: currencySymbol(b.currency) })}</b></td>
                <td className="muted">{b.last}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (v === 's') {
    const total = D.banks.reduce((s, b) => s + (b.balance || 0), 0);
    return (
      <div className="col gap-8" style={{ alignItems: 'center', textAlign: 'center' }}>
        {D.banks.length > 0 && <Donut size={140} segments={segments} />}
        <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>Toplam Nakit</div>
        <div className="mono tnum" style={{ fontSize: 16, fontWeight: 600 }}>{TRY(total)}</div>
      </div>
    );
  }

  return (
    <div className="col gap-8">
      <div className="row" style={{ justifyContent: 'center', marginBottom: 4 }}>
        {D.banks.length > 0 && <Donut size={200} segments={segments} />}
      </div>
      {D.banks.map(b => (
        <div key={b.id} className="row" style={{ justifyContent: 'space-between', padding: '3px 0', fontSize: 12 }}>
          <span style={{ color: 'var(--ink-2)' }}>
            {b.name} <span className="muted">· {b.currency}</span>
          </span>
          <span className="mono tnum">{TRY(b.balance || 0, { currency: currencySymbol(b.currency) })}</span>
        </div>
      ))}
    </div>
  );
}
