import { Link } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import { fmtN } from '@/lib/format';
import { useAdminStats } from '@/hooks/useAdmin';

const QUICK_LINKS = [
  { to: '/admin/kullanicilar', icon: 'users',    label: 'Kullanıcılar' },
  { to: '/admin/sirketler',    icon: 'building', label: 'Şirketler' },
  { to: '/admin/ayarlar',      icon: 'settings', label: 'Sistem Ayarları' },
  { to: '/admin/loglar',       icon: 'log',      label: 'Loglar & Denetim' },
];

export default function AdminDashboardPage() {
  const { data: stats, isLoading, isError, refetch } = useAdminStats();

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError)   return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Yönetim Paneli</h1>
          <p className="page-sub">Platform geneli özet</p>
        </div>
        <div className="page-actions">
          {QUICK_LINKS.map(l => (
            <Link key={l.to} to={l.to} className="btn"><Icon name={l.icon} size={14} /> {l.label}</Link>
          ))}
        </div>
      </div>

      <div className="kpis" style={{ marginBottom: 16, gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
        <div className="kpi">
          <div className="kpi-label">Toplam Kullanıcı</div>
          <div className="kpi-val">{fmtN(stats.totalUsers)}</div>
          <div className="muted" style={{ fontSize: 11 }}>{fmtN(stats.adminCount)} yönetici</div>
        </div>
        <div className="kpi">
          <div className="kpi-label">Aktif / Kilitli / Pasif</div>
          <div className="kpi-val">
            {fmtN(stats.activeUsers)}
            <span style={{ color: 'var(--warn)' }}> / {fmtN(stats.lockedUsers)}</span>
            <span className="muted"> / {fmtN(stats.passiveUsers)}</span>
          </div>
        </div>
        <div className="kpi">
          <div className="kpi-label">Toplam Şirket</div>
          <div className="kpi-val">{fmtN(stats.totalCompanies)}</div>
          <div className="muted" style={{ fontSize: 11 }}>{fmtN(stats.activeCompanies)} aktif</div>
        </div>
        <div className="kpi">
          <div className="kpi-label">Son 30 Gün Kayıt</div>
          <div className="kpi-val">{fmtN(stats.newUsersLast30Days)}</div>
        </div>
        <div className="kpi">
          <div className="kpi-label">Yapay Zeka</div>
          <div className="kpi-val" style={{ color: stats.aiEnabled ? 'var(--pos)' : 'var(--neg)' }}>
            {stats.aiEnabled ? 'Aktif' : 'Devre Dışı'}
          </div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-h"><h3>AI Kota Kullanımı (Top 10)</h3></div>
          <div className="card-b">
            {(stats.aiTopConsumers ?? []).map(c => {
              const pct = c.monthlyTokenBudget > 0
                ? Math.min(100, Math.round((c.tokensUsedThisMonth / c.monthlyTokenBudget) * 100))
                : 0;
              return (
                <div key={c.companyId} style={{ marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
                    <span>{c.companyName || `Şirket #${c.companyId}`}</span>
                    <span className="muted mono">{fmtN(c.tokensUsedThisMonth)} / {fmtN(c.monthlyTokenBudget)} token</span>
                  </div>
                  <div style={{ height: 6, borderRadius: 3, background: 'var(--bg-elev)', overflow: 'hidden' }}>
                    <div style={{ width: `${pct}%`, height: '100%', borderRadius: 3, background: pct >= 90 ? 'var(--neg)' : pct >= 70 ? 'var(--warn)' : 'var(--accent)' }} />
                  </div>
                </div>
              );
            })}
            {(stats.aiTopConsumers ?? []).length === 0 && <div className="empty">Bu ay henüz AI kullanımı yok</div>}
          </div>
        </div>

        <div className="card">
          <div className="card-h">
            <h3>Son Loglar</h3>
            <Link to="/admin/loglar" className="btn sm">Tümü <Icon name="chevRight" size={12} /></Link>
          </div>
          <div className="card-b" style={{ maxHeight: 420, overflowY: 'auto' }}>
            {(stats.recentLogs ?? []).map(x => (
              <div key={x.logId} style={{ display: 'flex', gap: 8, alignItems: 'baseline', padding: '6px 0', borderBottom: '1px solid var(--line)', fontSize: 12 }}>
                <span className={`pill ${x.logLevel === 'ERROR' ? 'neg' : x.logLevel === 'WARNING' ? 'warn' : 'info'}`} style={{ flexShrink: 0 }}>
                  <span className="dot" />{x.logLevel}
                </span>
                <span style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={x.details}>
                  {x.details}
                </span>
                <span className="muted mono" style={{ flexShrink: 0, fontSize: 11 }}>{x.companyName || '—'}</span>
              </div>
            ))}
            {(stats.recentLogs ?? []).length === 0 && <div className="empty">Henüz log kaydı yok</div>}
          </div>
        </div>
      </div>
    </div>
  );
}
