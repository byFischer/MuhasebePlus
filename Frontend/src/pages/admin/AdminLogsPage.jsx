import { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { useAdminLogs, useExportAdminLogs, useAdminCompanies } from '@/hooks/useAdmin';

export default function AdminLogsPage() {
  const [tab, setTab] = useState('all'); // 'all' | 'audit'
  const [lvl, setLvl] = useState(null);
  const [companyId, setCompanyId] = useState('');
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');

  const params = {
    level: lvl || undefined,
    companyId: companyId || undefined,
    auditOnly: tab === 'audit' || undefined,
    page,
  };
  const { data, isLoading, isError, refetch } = useAdminLogs(params);
  const { data: companies } = useAdminCompanies({ size: 200, sort: 'companyName,asc' });
  const exportMut = useExportAdminLogs();

  const content       = data?.content       ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages    = data?.totalPages    ?? 1;

  const filtered = content.filter(x =>
    (x.details + (x.userEmail || '') + (x.companyName || '')).toLowerCase().includes(q.toLowerCase())
  );

  if (isLoading && !data) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError)            return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Loglar &amp; Denetim</h1>
          <p className="page-sub">{totalElements} kayıt · tüm şirketler</p>
        </div>
        <div className="page-actions">
          <button className="btn" onClick={() => {
            const end = new Date();
            const start = new Date(); start.setDate(start.getDate() - 30);
            exportMut.mutate({
              ...params,
              startDate: start.toISOString().slice(0, 10),
              endDate: end.toISOString().slice(0, 10),
            });
          }}>
            <Icon name="download" /> Dışa Aktar (30 gün)
          </button>
        </div>
      </div>

      <div className="card">
        <div className="toolbar">
          <div className="seg">
            <button className={tab === 'all' ? 'on' : ''} onClick={() => { setTab('all'); setPage(0); }}>Tüm Loglar</button>
            <button className={tab === 'audit' ? 'on' : ''} onClick={() => { setTab('audit'); setPage(0); }}>Denetim Kaydı</button>
          </div>
          <div className="tb-search" style={{ margin: 0, width: 240 }}>
            <Icon name="search" size={14} />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Mesaj ara..." />
          </div>
          <select className="input" style={{ width: 200 }} value={companyId}
                  onChange={e => { setCompanyId(e.target.value); setPage(0); }}>
            <option value="">Tüm Şirketler</option>
            {(companies?.content ?? []).map(c => (
              <option key={c.companyId} value={c.companyId}>{c.companyName}</option>
            ))}
          </select>
          <div className="seg">
            {[null, 'INFO', 'WARNING', 'ERROR'].map(k => (
              <button key={k || 'all'} className={lvl === k ? 'on' : ''} onClick={() => { setLvl(k); setPage(0); }}>
                {k || 'Hepsi'}
              </button>
            ))}
          </div>
        </div>

        <div>
          <div className="log-row" style={{ background: 'var(--bg-elev)', fontWeight: 500, color: 'var(--ink-3)', textTransform: 'uppercase', fontSize: 10, letterSpacing: '0.05em' }}>
            <div>Tarih/Saat</div><div>Seviye</div><div>Şirket</div><div>Mesaj</div>
          </div>
          {filtered.map(x => (
            <div key={x.logId} className="log-row">
              <div className="ts">{x.timestamp}</div>
              <div className={`lvl ${x.logLevel}`}>{x.logLevel}</div>
              <div>{x.companyName || '—'}</div>
              <div>
                <span>{x.details}</span>
                {x.userEmail && <span className="actor"> · {x.userEmail}</span>}
              </div>
            </div>
          ))}
          {filtered.length === 0 && <div className="empty">Kayıt bulunamadı</div>}
        </div>

        {totalPages > 1 && (
          <div className="toolbar" style={{ justifyContent: 'center', paddingTop: 8 }}>
            <button className="btn sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
              <Icon name="chevLeft" size={14} /> Önceki
            </button>
            <span style={{ padding: '0 12px', color: 'var(--ink-3)', fontSize: 13 }}>{page + 1} / {totalPages}</span>
            <button className="btn sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
              Sonraki <Icon name="chevRight" size={14} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
