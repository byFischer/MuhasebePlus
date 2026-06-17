import { useState, useEffect, useRef } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import { NAV } from '@/lib/routes';
import { useAuth } from '@/context/AuthContext';
import LiveCurrencyRates from '@/components/layout/LiveCurrencyRates';

export default function Sidebar({ onOpenCmdk }) {
  const { user } = useAuth();
  const location = useLocation();
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [expandedSections, setExpandedSections] = useState({});

  useEffect(() => {
    const newExpanded = {};
    NAV.forEach((n) => {
      if (n.children) {
        const hasActiveChild = n.children.some((c) => location.pathname === c.to);
        if (hasActiveChild) newExpanded[n.to] = true;
      }
    });
    setExpandedSections((prev) => ({ ...prev, ...newExpanded }));
  }, [location.pathname]);

  const toggleSection = (to) => {
    setExpandedSections((prev) => ({ ...prev, [to]: !prev[to] }));
  };

  const fullName = user ? `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email : 'Yükleniyor...';
  const role = user?.role === 'ADMIN' ? 'Yönetici' : 'Kullanıcı';
  const initials = user ? `${(user.firstName?.[0]||'').toUpperCase()}${(user.lastName?.[0]||'').toUpperCase()}` : '??';

  return (
    <aside className="sidebar">
      <NavLink to="/dashboard" className="brand">
        <img src="/logo.svg" alt="Muhasebe+" className="sidebar-brand-mark" />
        <div>
          <div className="brand-name">Muhasebe<span style={{ color: 'var(--accent)' }}>+</span></div>
          <div className="brand-tag">{user?.companyName || 'Muhasebe+'}</div>
        </div>
      </NavLink>
      <div className="sb-search" onClick={onOpenCmdk} role="button" tabIndex={0}>
        <Icon name="search" size={14} />
        <input placeholder="Ara: müşteri, fatura..." readOnly />
        <kbd>⌘K</kbd>
      </div>
      <nav className="nav">
        {NAV.filter(n => !n.adminOnly || user?.role === 'ADMIN').map((n, i) => n.section ? (
          <div key={i} className="nav-section">{n.section}</div>
        ) : n.children ? (
          <div key={n.to}>
            <button
              className={`nav-item nav-group-header ${expandedSections[n.to] ? 'expanded' : ''}`}
              onClick={() => toggleSection(n.to)}
            >
              <Icon name={n.icon} size={15} />
              <span>{n.label}</span>
              <Icon name="chevDown" size={12} className="nav-chevron" />
            </button>
            {expandedSections[n.to] && (
              <div className="nav-children">
                {n.children.map((c) => (
                  <NavLink key={c.to} to={c.to} end className={({ isActive }) => `nav-item nav-child ${isActive ? 'on' : ''}`}>
                    <span>{c.label}</span>
                  </NavLink>
                ))}
              </div>
            )}
          </div>
        ) : (
          <NavLink key={n.to} to={n.to} end className={({ isActive }) => `nav-item ${isActive ? 'on' : ''}`}>
            <Icon name={n.icon} size={15} />
            <span>{n.label}</span>
          </NavLink>
        ))}
      </nav>
      <LiveCurrencyRates />
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
        <div className="um-item" onClick={() => { navigate('/settings'); onClose(); }}><Icon name="settings" size={14} /> <span>Hesap ayarları</span></div>
      </div>
      <div className="um-section">
        <div className="um-item danger" onClick={() => { logoutUser(); navigate('/login'); }}>
          <Icon name="logout" size={14} /> <span>Çıkış yap</span>
        </div>
      </div>
    </div>
  );
}
