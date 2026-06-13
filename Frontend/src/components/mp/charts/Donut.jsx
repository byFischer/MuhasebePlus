import { TRY } from '@/lib/format';

export default function Donut({ segments, size = 140 }) {
  const total = segments.reduce((s, x) => s + x.value, 0);
  const r = size / 2 - 12, cx = size / 2, cy = size / 2;
  let acc = 0;
  return (
    <svg width={size} height={size}>
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--line)" strokeWidth="14" />
      {segments.map((s, i) => {
        const frac = s.value / total;
        const start = acc; acc += frac;
        const len = 2 * Math.PI * r * frac;
        const gap = 2 * Math.PI * r - len;
        const offset = -2 * Math.PI * r * start;
        return (
          <circle key={i} cx={cx} cy={cy} r={r} fill="none"
                  stroke={s.color} strokeWidth="14"
                  strokeDasharray={`${len} ${gap}`} strokeDashoffset={offset}
                  transform={`rotate(-90 ${cx} ${cy})`} strokeLinecap="butt" />
        );
      })}
      <text x={cx} y={cy - 4} textAnchor="middle" fontSize="20" fontWeight="600" fill="var(--ink)">{TRY(total).replace(',00', '')}</text>
      <text x={cx} y={cy + 14} textAnchor="middle" fontSize="10" fill="var(--ink-3)">Toplam</text>
    </svg>
  );
}
