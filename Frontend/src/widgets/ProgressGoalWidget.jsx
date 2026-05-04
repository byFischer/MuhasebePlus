import React from 'react';
import { TRY } from '@/lib/format';

export default function ProgressGoalWidget({ D, config }) {
  const target = Number(config?.targetAmount || 100000);
  const period = config?.period || 'MONTHLY';

  const now = new Date();
  const current = D.thisRev || 0;

  let daysInPeriod, dayOfPeriod;
  if (period === 'MONTHLY') {
    daysInPeriod = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    dayOfPeriod = now.getDate();
  } else {
    const q = Math.floor(now.getMonth() / 3);
    const qStart = new Date(now.getFullYear(), q * 3, 1);
    const qEnd = new Date(now.getFullYear(), q * 3 + 3, 0);
    daysInPeriod = Math.ceil((qEnd - qStart) / 86400000) + 1;
    dayOfPeriod = Math.ceil((now - qStart) / 86400000) + 1;
  }

  const pct = Math.min(100, Math.max(0, (current / target) * 100));
  const remaining = Math.max(0, target - current);
  const daysLeft = Math.max(0, daysInPeriod - dayOfPeriod);

  return (
    <div className="col gap-12">
      <div className="row" style={{ justifyContent: 'space-between', fontSize: 12 }}>
        <span className="muted">{period === 'MONTHLY' ? 'Bu Ay' : 'Bu Çeyrek'}</span>
        <span className="mono tnum" style={{ fontWeight: 600 }}>{pct.toFixed(1)}%</span>
      </div>
      <div className="bar" style={{ height: 10 }}>
        <span style={{ width: `${pct}%` }} />
      </div>
      <div className="row" style={{ justifyContent: 'space-between', fontSize: 12 }}>
        <span>{TRY(current)} / {TRY(target)}</span>
        <span className="muted">{daysLeft} gün kaldı</span>
      </div>
      {remaining > 0 && (
        <div className="hint" style={{ fontSize: 11 }}>
          Hedefe ulaşmak için günlük ortalama {TRY(Math.ceil(remaining / Math.max(1, daysLeft)))} gerekiyor.
        </div>
      )}
    </div>
  );
}
