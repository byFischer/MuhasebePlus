import Icon from '@/components/mp/Icon';
import Spark from '@/components/mp/charts/Spark';

export function Kpi({ label, value, delta, data, pos, warn, color }) {
  return (
    <div className="kpi widget">
      <div className="kpi-label"><span>{label}</span></div>
      <div className="kpi-val">
        <span className="cur">₺</span>{(value || 0).toLocaleString('tr-TR')}
      </div>
      <div className={`kpi-delta ${pos ? 'pos' : ''}`} style={warn ? { color: 'var(--warn)' } : {}}>
        {pos && <Icon name="arrowUp" size={12} />}
        {delta}
      </div>
      {data && <div className="kpi-spark"><Spark data={data} color={color || 'var(--accent)'} /></div>}
    </div>
  );
}

export default function KpisWidget({ D, mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');
  const net = D.thisRev - D.thisExp;
  const netSpark = D.revenue.map((val, i) => val - D.expense[i]);

  if (v === 's') {
    return (
      <div className="kpis kpis-compact">
        <Kpi label="Net Kar (Bu Ay)" value={net} delta={D.netDelta} pos={D.netPos} data={netSpark} />
      </div>
    );
  }

  return (
    <div className={v === 'l' ? 'col gap-12' : 'kpis'}>
      <Kpi label="Toplam Gelir (Bu Ay)" value={D.thisRev} delta={D.revDelta} pos={D.revPos} data={D.revenue} />
      <Kpi label="Toplam Gider (Bu Ay)" value={D.thisExp} delta={D.expDelta} data={D.expense} color="var(--ink-3)" />
      <Kpi label="Net Kar" value={net} delta={D.netDelta} pos={D.netPos} data={netSpark} />
      <Kpi label="Açık Alacak" value={D.openInvoicesTotal} delta={`${D.openInvoicesCount} fatura gecikmiş`} warn />
    </div>
  );
}
