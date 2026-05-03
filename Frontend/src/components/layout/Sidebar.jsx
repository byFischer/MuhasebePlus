import React, { useState, useEffect, useRef } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import { NAV } from '@/lib/routes';
import { useAuth } from '@/context/AuthContext';
import { toast } from '@/lib/toast';

export default function Sidebar() {
  const { user, logoutUser } = useAuth();
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  const fullName = user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email : 'Yükleniyor...';
  const role = user?.role === 'ADMIN' ? 'Yönetici' : 'Kullanıcı';
  const initials = user ? `${(user.firstName?.[0]||'').toUpperCase()}${(user.lastName?.[0]||'').toUpperCase()}` : '??';

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="sidebar-brand-mark">M+</div>
        <div>
          <div className="brand-name">Muhasebe<span style={{ color: 'var(--accent)' }}>+</span></div>
          <div className="brand-tag">{user?.companyName || 'Muhasebe+'}</div>
        </div>
      </div>
      <nav className="nav">
        {NAV.map((n, i) => n.section ? (
          <div key={i} className="nav-section">{n.section}</div>
        ) : (
          <NavLink key={n.to} to={n.to} className={({ isActive }) => `nav-item ${isActive ? 'on' : ''}`}>
            <Icon name={n.icon} size={15} />
            <span>{n.label}</span>
          </NavLink>
        ))}
      </nav>
      <div className="sb-foot" onClick={() => setUserMenuOpen(o => !o)} style={{ cursor: 'pointer', position: 'relative' }}>
        <div className="avatar">{initials}</div>
        <div className="flex-1">
          <div className="user-name">{fullName}</div>
          <div className="user-role">{role}</div>
        </div>
        <button className="tb-icon-btn" title="Ayarlar" onClick={(e) => { e.stopPropagation(); setUserMenuOpen(o => !o); }}>
          <Icon name="settings" size={14} />
        </button>
        {userMenuOpen && <UserMenu onClose={() => setUserMenuOpen(false)} />}
      </div>
    </aside>
  );
}

function UserMenu({ onClose }) {
  const ref = useRef();
  const navigate = useNavigate();
  const { user, logoutUser } = useAuth();

  const fullName = user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email : '—';
  const email = user?.email || '—';
  const initials = user ? `${(user.firstName?.[0] || '').toUpperCase()}${(user.lastName?.[0] || '').toUpperCase()}` || '?' : '?';

  useEffect(() => {
    let mounted = false;
    const arm = setTimeout(() => { mounted = true; }, 0);
    const onDoc = (e) => { if (mounted && ref.current && !ref.current.contains(e.target)) onClose(); };
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => { clearTimeout(arm); document.removeEventListener('mousedown', onDoc); document.removeEventListener('keydown', onKey); };
  }, [onClose]);

  const click = (msg) => { toast.ok(msg); onClose(); };

  return (
    <div ref={ref} className="user-menu" onClick={(e) => e.stopPropagation()}>
      <div className="um-head">
        <div className="avatar lg">{initials}</div>
        <div className="flex-1" style={{ minWidth: 0 }}>
          <div className="um-name">{fullName}</div>
          <div className="um-mail">{email}</div>
        </div>
      </div>
      <div className="um-section">
        <div className="um-item" onClick={() => click('Profil & ayarlar yakında')}><Icon name="settings" size={14} /> <span>Hesap ayarları</span></div>
        <div className="um-item" onClick={() => click('Dil ayarı yakında')}><Icon name="globe" size={14} /> <span>Dil</span></div>
        <div className="um-item" onClick={() => click('Bildirim tercihleri açılıyor')}><Icon name="bell" size={14} /> <span>Bildirimler</span></div>
      </div>
      <div className="um-section">
        <div className="um-item" onClick={() => click('Yardım merkezi açılıyor')}><Icon name="help" size={14} /> <span>Yardım & destek</span></div>
        <div className="um-item" onClick={() => click('Geri bildiriminiz iletildi')}><Icon name="message" size={14} /> <span>Geri bildirim gönder</span></div>
        <div className="um-item" onClick={() => click('Yenilikler · v4.2')}><Icon name="sparkle" size={14} /> <span>Yenilikler</span> <span className="um-tag">Yeni</span></div>
      </div>
      <div className="um-section">
        <div className="um-item" onClick={() => click('Plan yükseltme yakında')}><Icon name="crown" size={14} /> <span>Planı yükselt</span></div>
        <div className="um-item" onClick={() => click('Entegrasyonlar yakında')}><Icon name="plug" size={14} /> <span>Entegrasyonlar</span></div>
      </div>
      <div className="um-section">
        <div className="um-item danger" onClick={() => { logoutUser(); navigate('/login'); }}>
          <Icon name="logout" size={14} /> <span>Çıkış yap</span>
        </div>
      </div>
    </div>
  );
}
