import { useEffect, useState } from 'react';
import Icon from '@/components/mp/Icon';
import Drawer from '@/components/mp/Drawer';
import { fmtN, fmtDate } from '@/lib/format';
import { useAdminCompanies, useAdminCompanyDetail, useSetCompanyActive } from '@/hooks/useAdmin';

export default function AdminCompaniesPage() {
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [activeF, setActiveF] = useState(null); // null | true | false
  const [page, setPage] = useState(0);
  const [detailId, setDetailId] = useState(null);

  useEffect(() => {
    const t = setTimeout(() => { setSearch(q.trim()); setPage(0); }, 400);
    return () => clearTimeout(t);
  }, [q]);

  const params = {
    search: search || undefined,
    active: activeF == null ? undefined : activeF,
    page,
  };
  const { data, isLoading, isError, refetch } = useAdminCompanies(params);
  const setActiveMut = useSetCompanyActive();

  const content       = data?.content       ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages    = data?.totalPages    ?? 1;

  if (isLoading && !data) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError)            return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Şirket Yönetimi</h1>
          <p className="page-sub">{totalElements} şirket</p>
        </div>
      </div>

      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}>
            <Icon name="search" size={14} />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Şirket adı, vergi no veya şehir ara..." />
          </div>
          <div className="seg">
            {[[null, 'Hepsi'], [true, 'Aktif'], [false, 'Pasif']].map(([k, label]) => (
              <button key={label} className={activeF === k ? 'on' : ''} onClick={() => { setActiveF(k); setPage(0); }}>{label}</button>
            ))}
          </div>
        </div>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Şirket</th><th>Vergi No</th><th>Şehir</th>
                <th className="num">Kullanıcı</th><th>Durum</th><th>Kayıt</th><th></th>
              </tr>
            </thead>
            <tbody>
              {content.map(c => (
                <tr key={c.companyId}>
                  <td style={{ fontWeight: 500 }}>{c.companyName}</td>
                  <td className="mono">{c.taxNumber || '—'}</td>
                  <td>{c.city || '—'}</td>
                  <td className="num mono">{fmtN(c.userCount)}</td>
                  <td>
                    <span className={`pill ${c.isActive ? 'pos' : ''}`}>
                      <span className="dot" />{c.isActive ? 'Aktif' : 'Pasif'}
                    </span>
                  </td>
                  <td className="muted">{fmtDate(c.createdAt)}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                      <button className="tb-icon-btn" title="Detay" onClick={() => setDetailId(c.companyId)}>
                        <Icon name="eye" size={14} />
                      </button>
                      <button className="tb-icon-btn" title={c.isActive ? 'Pasifleştir' : 'Aktifleştir'}
                              disabled={setActiveMut.isPending}
                              onClick={() => setActiveMut.mutate({ id: c.companyId, active: !c.isActive })}>
                        <Icon name={c.isActive ? 'minus' : 'check'} size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {content.length === 0 && <div className="empty">Şirket bulunamadı</div>}
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

      <CompanyDetailDrawer companyId={detailId} onClose={() => setDetailId(null)} />
    </div>
  );
}

function CompanyDetailDrawer({ companyId, onClose }) {
  const { data, isLoading } = useAdminCompanyDetail(companyId);
  if (companyId == null) return null;

  const c = data?.company;
  const quota = data?.aiQuota;
  const quotaPct = quota && quota.monthlyTokenBudget > 0
    ? Math.min(100, Math.round((quota.tokensUsedThisMonth / quota.monthlyTokenBudget) * 100))
    : 0;

  return (
    <Drawer open onClose={onClose} title={c ? c.companyName : 'Şirket Detayı'} width={480}>
      {isLoading || !data ? (
        <div className="card" style={{ height: 120 }} />
      ) : (
        <div className="col gap-12">
          <div className="grid-2" style={{ gap: 8 }}>
            <div className="kpi"><div className="kpi-label">Cari Sayısı</div><div className="kpi-val">{fmtN(data.customerCount)}</div></div>
            <div className="kpi"><div className="kpi-label">Fatura Sayısı</div><div className="kpi-val">{fmtN(data.invoiceCount)}</div></div>
          </div>

          {[
            ['Vergi No', c.taxNumber || '—'],
            ['Vergi Dairesi', c.taxOffice || '—'],
            ['Şehir', c.city || '—'],
            ['Telefon', c.phone || '—'],
            ['E-posta', c.email || '—'],
            ['Durum', c.isActive ? 'Aktif' : 'Pasif'],
            ['Kayıt Tarihi', fmtDate(c.createdAt)],
          ].map(([label, value]) => (
            <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, fontSize: 13, borderBottom: '1px solid var(--line)', paddingBottom: 8 }}>
              <span className="muted">{label}</span>
              <span style={{ textAlign: 'right' }}>{value}</span>
            </div>
          ))}

          <div>
            <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 6 }}>AI Kotası</div>
            {quota ? (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
                  <span className="muted">Bu ay kullanılan</span>
                  <span className="mono">{fmtN(quota.tokensUsedThisMonth)} / {fmtN(quota.monthlyTokenBudget)} token</span>
                </div>
                <div style={{ height: 6, borderRadius: 3, background: 'var(--bg-elev)', overflow: 'hidden' }}>
                  <div style={{ width: `${quotaPct}%`, height: '100%', borderRadius: 3, background: quotaPct >= 90 ? 'var(--neg)' : 'var(--accent)' }} />
                </div>
              </>
            ) : (
              <div className="muted" style={{ fontSize: 12 }}>Henüz AI kullanımı yok</div>
            )}
          </div>

          <div>
            <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 6 }}>Kullanıcılar ({(data.users ?? []).length})</div>
            {(data.users ?? []).map(u => (
              <div key={u.userId} style={{ display: 'flex', justifyContent: 'space-between', gap: 8, fontSize: 12, padding: '6px 0', borderBottom: '1px solid var(--line)' }}>
                <span>{`${u.firstName || ''} ${u.lastName || ''}`.trim() || u.email}</span>
                <span className="muted mono">{u.email}</span>
                <span className={`pill ${u.role === 'ADMIN' ? 'pos' : ''}`} style={{ flexShrink: 0 }}>
                  <span className="dot" />{u.role}
                </span>
              </div>
            ))}
            {(data.users ?? []).length === 0 && <div className="muted" style={{ fontSize: 12 }}>Bu şirkete bağlı kullanıcı yok</div>}
          </div>
        </div>
      )}
    </Drawer>
  );
}
