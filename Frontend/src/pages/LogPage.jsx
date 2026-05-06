import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { useSystemLogs, useExportSystemLogs } from '@/hooks/useSystemLogs';

export default function LogPage() {
  const [lvl, setLvl] = useState(null);
  const { data: list = [], isLoading, isError, refetch } = useSystemLogs({ level: lvl });
  const exportMut = useExportSystemLogs();
  const [q, setQ] = useState('');

  const filtered = list.filter(x => (x.details + (x.userEmail || '')).toLowerCase().includes(q.toLowerCase()));

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError) return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div><h1 className="page-title">Sistem Logları</h1><p className="page-sub">{list.length} kayıt</p></div>
        <div className="page-actions">
          <button className="btn" onClick={() => exportMut.mutate({ level: lvl })}><Icon name="download" /> Dışa Aktar</button>
        </div>
      </div>
      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}><Icon name="search" size={14} /><input value={q} onChange={e => setQ(e.target.value)} placeholder="Mesaj ara..." /></div>
          <div className="seg">
            {[null, 'INFO', 'WARNING', 'ERROR'].map(k => (
              <button key={k || 'all'} className={lvl === k ? 'on' : ''} onClick={() => setLvl(k)}>{k || 'Hepsi'}</button>
            ))}
          </div>
        </div>
        <div>
          <div className="log-row" style={{ background: 'var(--bg-elev)', fontWeight: 500, color: 'var(--ink-3)', textTransform: 'uppercase', fontSize: 10, letterSpacing: '0.05em' }}>
            <div>Tarih/Saat</div><div>Seviye</div><div>IP Adresi</div><div>Mesaj</div>
          </div>
          {filtered.map((x, i) => (
            <div key={i} className="log-row">
              <div className="ts">{x.timestamp}</div>
              <div className={`lvl ${x.logLevel}`}>{x.logLevel}</div>
              <div>{x.ipAddress || '—'}</div>
              <div><span>{x.details}</span>{x.userEmail && <span className="actor"> · {x.userEmail}</span>}</div>
            </div>
          ))}
          {filtered.length === 0 && <div className="empty">Kayıt bulunamadı</div>}
        </div>
      </div>
    </div>
  );
}
