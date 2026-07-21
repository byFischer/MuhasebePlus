import { useEffect, useState } from 'react';
import Icon from '@/components/mp/Icon';
import Drawer from '@/components/mp/Drawer';
import { fmtDate } from '@/lib/format';
import { useAuth } from '@/context/AuthContext';
import {
  useAdminUsers, useUpdateUserRole, useLockUser, useUnlockUser,
  useActivateUser, useDeactivateUser, useResetUserPassword, useDeleteUser,
} from '@/hooks/useAdmin';

const ROLE_LABELS = { ADMIN: 'Yönetici', USER: 'Kullanıcı' };

function statusOf(u) {
  if (u.isLocked) return { label: 'Kilitli', cls: 'neg' };
  if (!u.isActive) return { label: 'Pasif', cls: '' };
  return { label: 'Aktif', cls: 'pos' };
}

export default function AdminUsersPage() {
  const { user: me } = useAuth();
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [roleF, setRoleF] = useState(null);
  const [statusF, setStatusF] = useState(null);
  const [page, setPage] = useState(0);
  const [modal, setModal] = useState(null);   // { type: 'role'|'password'|'delete', user }
  const [detail, setDetail] = useState(null); // user

  useEffect(() => {
    const t = setTimeout(() => { setSearch(q.trim()); setPage(0); }, 400);
    return () => clearTimeout(t);
  }, [q]);

  const params = {
    search: search || undefined,
    role: roleF || undefined,
    status: statusF || undefined,
    page,
  };
  const { data, isLoading, isError, refetch } = useAdminUsers(params);

  const lockMut       = useLockUser();
  const unlockMut     = useUnlockUser();
  const activateMut   = useActivateUser();
  const deactivateMut = useDeactivateUser();

  const content       = data?.content       ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages    = data?.totalPages    ?? 1;

  if (isLoading && !data) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError)            return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Kullanıcı Yönetimi</h1>
          <p className="page-sub">{totalElements} kullanıcı</p>
        </div>
      </div>

      <div className="card">
        <div className="toolbar">
          <div className="tb-search" style={{ margin: 0, width: 280 }}>
            <Icon name="search" size={14} />
            <input value={q} onChange={e => setQ(e.target.value)} placeholder="Ad, e-posta veya şirket ara..." />
          </div>
          <div className="seg">
            {[[null, 'Tüm Roller'], ['ADMIN', 'Yönetici'], ['USER', 'Kullanıcı']].map(([k, label]) => (
              <button key={label} className={roleF === k ? 'on' : ''} onClick={() => { setRoleF(k); setPage(0); }}>{label}</button>
            ))}
          </div>
          <div className="seg">
            {[[null, 'Hepsi'], ['ACTIVE', 'Aktif'], ['LOCKED', 'Kilitli'], ['PASSIVE', 'Pasif']].map(([k, label]) => (
              <button key={label} className={statusF === k ? 'on' : ''} onClick={() => { setStatusF(k); setPage(0); }}>{label}</button>
            ))}
          </div>
        </div>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Ad Soyad</th><th>E-posta</th><th>Şirket</th><th>Rol</th>
                <th>Durum</th><th className="num">Hatalı Giriş</th><th>Kayıt</th><th></th>
              </tr>
            </thead>
            <tbody>
              {content.map(u => {
                const st = statusOf(u);
                const isSelf = me?.userId === u.userId;
                return (
                  <tr key={u.userId}>
                    <td>
                      {`${u.firstName || ''} ${u.lastName || ''}`.trim() || '—'}
                      {isSelf && <span className="muted" style={{ fontSize: 11 }}> (siz)</span>}
                    </td>
                    <td className="mono" style={{ fontSize: 12 }}>{u.email}</td>
                    <td>{u.companyName || '—'}</td>
                    <td>
                      <span className={`pill ${u.role === 'ADMIN' ? 'pos' : ''}`}>
                        <span className="dot" />{ROLE_LABELS[u.role] || u.role}
                      </span>
                    </td>
                    <td><span className={`pill ${st.cls}`}><span className="dot" />{st.label}</span></td>
                    <td className="num mono">{u.failedLoginAttempts}</td>
                    <td className="muted">{fmtDate(u.createdAt)}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                        <button className="tb-icon-btn" title="Detay" onClick={() => setDetail(u)}>
                          <Icon name="eye" size={14} />
                        </button>
                        <button className="tb-icon-btn" title="Rol değiştir" disabled={isSelf}
                                onClick={() => setModal({ type: 'role', user: u })}>
                          <Icon name="edit" size={14} />
                        </button>
                        {u.isLocked ? (
                          <button className="tb-icon-btn" title="Kilidi aç" disabled={unlockMut.isPending}
                                  onClick={() => unlockMut.mutate(u.userId)}>
                            <Icon name="unlock" size={14} />
                          </button>
                        ) : (
                          <button className="tb-icon-btn" title="Kilitle" disabled={isSelf || lockMut.isPending}
                                  onClick={() => lockMut.mutate(u.userId)}>
                            <Icon name="lock" size={14} />
                          </button>
                        )}
                        {u.isActive ? (
                          <button className="tb-icon-btn" title="Pasifleştir" disabled={isSelf || deactivateMut.isPending}
                                  onClick={() => deactivateMut.mutate(u.userId)}>
                            <Icon name="minus" size={14} />
                          </button>
                        ) : (
                          <button className="tb-icon-btn" title="Aktifleştir" disabled={activateMut.isPending}
                                  onClick={() => activateMut.mutate(u.userId)}>
                            <Icon name="check" size={14} />
                          </button>
                        )}
                        <button className="tb-icon-btn" title="Şifre sıfırla"
                                onClick={() => setModal({ type: 'password', user: u })}>
                          <Icon name="key" size={14} />
                        </button>
                        <button className="tb-icon-btn" title="Sil" disabled={isSelf}
                                onClick={() => setModal({ type: 'delete', user: u })}>
                          <Icon name="trash" size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {content.length === 0 && <div className="empty">Kullanıcı bulunamadı</div>}
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

      {modal?.type === 'role'     && <RoleModal user={modal.user} onClose={() => setModal(null)} />}
      {modal?.type === 'password' && <PasswordModal user={modal.user} onClose={() => setModal(null)} />}
      {modal?.type === 'delete'   && <DeleteModal user={modal.user} onClose={() => setModal(null)} />}
      <UserDetailDrawer user={detail} onClose={() => setDetail(null)} />
    </div>
  );
}

function RoleModal({ user, onClose }) {
  const [role, setRole] = useState(user.role);
  const mut = useUpdateUserRole();
  return (
    <div className="scrim" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Rol Değiştir</h3>
        <p className="muted" style={{ fontSize: 13 }}>{user.email}</p>
        <div className="field" style={{ margin: '12px 0' }}>
          <label>Yeni Rol</label>
          <select className="input" value={role} onChange={e => setRole(e.target.value)}>
            <option value="USER">Kullanıcı (USER)</option>
            <option value="ADMIN">Yönetici (ADMIN)</option>
          </select>
        </div>
        {role === 'ADMIN' && user.role !== 'ADMIN' && (
          <p style={{ fontSize: 12, color: 'var(--warn)' }}>
            <Icon name="alert" size={12} /> Yönetici rolü tüm kullanıcı ve şirketlere erişim verir.
          </p>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={role === user.role || mut.isPending}
                  onClick={() => mut.mutate({ id: user.userId, role }, { onSuccess: onClose })}>
            Kaydet
          </button>
        </div>
      </div>
    </div>
  );
}

function PasswordModal({ user, onClose }) {
  const [pwd, setPwd] = useState('');
  const [show, setShow] = useState(false);
  const mut = useResetUserPassword();
  const valid = pwd.length >= 8;
  return (
    <div className="scrim" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <h3>Şifre Sıfırla</h3>
        <p className="muted" style={{ fontSize: 13 }}>
          {user.email} için yeni şifre belirleyin. Hesap kilidi de açılır.
        </p>
        <div className="field" style={{ margin: '12px 0' }}>
          <label>Yeni Şifre <span className="muted" style={{ fontSize: 11 }}>en az 8 karakter</span></label>
          <div style={{ display: 'flex', gap: 8 }}>
            <input className="input" type={show ? 'text' : 'password'} value={pwd}
                   onChange={e => setPwd(e.target.value)} placeholder="••••••••" style={{ flex: 1 }} />
            <button className="tb-icon-btn" type="button" onClick={() => setShow(v => !v)} title={show ? 'Gizle' : 'Göster'}>
              <Icon name="eye" size={14} />
            </button>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid || mut.isPending}
                  onClick={() => mut.mutate({ id: user.userId, newPassword: pwd }, { onSuccess: onClose })}>
            Sıfırla
          </button>
        </div>
      </div>
    </div>
  );
}

function DeleteModal({ user, onClose }) {
  const mut = useDeleteUser();
  return (
    <div className="scrim" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400 }}>
        <h3>Kullanıcıyı Sil</h3>
        <p style={{ fontSize: 13 }}>
          <b>{user.email}</b> kalıcı olarak silinecek. Bu işlem geri alınamaz.
          Kullanıcıya bağlı kayıtlar varsa silme engellenir; bunun yerine pasifleştirmeyi değerlendirin.
        </p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" style={{ background: 'var(--neg)' }} disabled={mut.isPending}
                  onClick={() => mut.mutate(user.userId, { onSuccess: onClose })}>
            <Icon name="trash" size={14} /> Sil
          </button>
        </div>
      </div>
    </div>
  );
}

function UserDetailDrawer({ user, onClose }) {
  if (!user) return null;
  const st = statusOf(user);
  const rows = [
    ['Ad Soyad', `${user.firstName || ''} ${user.lastName || ''}`.trim() || '—'],
    ['E-posta', user.email],
    ['Telefon', user.phoneNumber || '—'],
    ['Doğum Tarihi', user.birthDate ? fmtDate(user.birthDate) : '—'],
    ['Şirket', user.companyName ? `${user.companyName} (#${user.companyId})` : '—'],
    ['Rol', ROLE_LABELS[user.role] || user.role],
    ['Durum', st.label],
    ['Hatalı Giriş Denemesi', user.failedLoginAttempts],
    ['Kayıt Tarihi', fmtDate(user.createdAt)],
  ];
  return (
    <Drawer open onClose={onClose} title={`Kullanıcı #${user.userId}`}>
      <div className="col gap-12">
        {rows.map(([label, value]) => (
          <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, fontSize: 13, borderBottom: '1px solid var(--line)', paddingBottom: 8 }}>
            <span className="muted">{label}</span>
            <span style={{ textAlign: 'right' }}>{value}</span>
          </div>
        ))}
      </div>
    </Drawer>
  );
}
