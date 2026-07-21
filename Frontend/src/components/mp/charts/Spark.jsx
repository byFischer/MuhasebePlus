
export default function Spark({ data, color = 'currentColor', height = 28 }) {
  const w = 120, h = height;
  const min = Math.min(...data), max = Math.max(...data);
  const r = max - min || 1;
  const pts = data.map((v, i) => {
    const x = (i / (data.length - 1)) * w;
    const y = h - ((v - min) / r) * (h - 2) - 1;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
  const fillD = `M0,${h} L ${pts.split(' ').join(' L ')} L ${w},${h} Z`;
  return (
    <svg className="spark" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" style={{ color }}>
      <path d={fillD} fill={color} opacity="0.12" />
      <polyline points={pts} fill="none" stroke="currentColor" strokeWidth="1.4" />
    </svg>
  );
}
