import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import AnimatedThemeToggler from '@/components/mp/AnimatedThemeToggler';
import { ROUTE_META } from '@/lib/routes';
import { AnimatedList } from '@/components/ui/animated-list';
import api from '@/services/api';

function formatNotifDate(value) {
  if (!value) return '';
  const d = new Date(value);
  if (isNaN(d.getTime())) return '';
  return d.toLocaleDateString('tr-TR', { day: '2-digit', month: 'short' });
}

export default function Topbar() {
  const location = useLocation();
  const meta = ROUTE_META[location.pathname] || ROUTE_META['/dashboard'];
  const crumbs = meta?.crumbs || ['Dashboard'];

  // --- Notification state (ported from Header.tsx) ---
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifOpen, setNotifOpen] = useState(false);

  async function loadNotifications() {
    try {
      const [countRes, notifRes] = await Promise.all([
        api.get('/api/notifications/unread-count'),
        api.get('/api/notifications'),
      ]);
      setUnreadCount(countRes.data?.count ?? 0);
      setNotifications((notifRes.data ?? []).slice(0, 5));
    } catch { setUnreadCount(0); setNotifications([]); }
  }

  useEffect(() => { loadNotifications(); }, []);

  async function handleNotifClick(n) {
    try {
      if (!n.read) {
        await api.patch(`/api/notifications/${n.notificationId}/read`);
        await loadNotifications();
      }
    } catch (err) { console.error('Notification read error:', err); }
  }

  return (
    <header className="topbar">
      <div className="crumbs">
        {crumbs.map((c, i) => (
          <React.Fragment key={i}>
            {i > 0 && <Icon name="chevRight" size={12} />}
            {i === crumbs.length - 1 ? <b>{c}</b> : <span>{c}</span>}
          </React.Fragment>
        ))}
      </div>
      {/* Notification bell */}
      <div style={{ position: 'relative', marginLeft: 'auto' }}>
        <button className="tb-icon-btn" title="Bildirimler" onClick={() => { setNotifOpen(o => !o); loadNotifications(); }}>
          <Icon name="bell" size={14} />
          {unreadCount > 0 && <span className="dot-badge" />}
        </button>
        {notifOpen && (
          <div style={{ position: 'absolute', top: 36, right: 0, width: 320, background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 'var(--r-lg)', boxShadow: '0 12px 32px rgba(0,0,0,0.16)', zIndex: 100, overflow: 'hidden' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 14px', borderBottom: '1px solid var(--line)', fontSize: 13, fontWeight: 600 }}>
              <span>Bildirimler</span>
              {unreadCount > 0 && <span style={{ color: 'var(--neg)', fontSize: 12 }}>{unreadCount}</span>}
            </div>
            {notifications.length === 0 ? (
              <div className="empty">Henüz bildirim yok</div>
            ) : (
              <div style={{ maxHeight: 320, overflowY: 'auto' }}>
                <AnimatedList delay={80} key={notifOpen ? 'open' : 'closed'}>
                  {notifications.map(n => (
                    <div key={n.notificationId} onClick={() => handleNotifClick(n)}
                      style={{ padding: '10px 14px', borderBottom: '1px solid var(--line)', cursor: 'pointer', background: n.read ? 'transparent' : 'var(--bg-2)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                        <span style={{ fontSize: 13, fontWeight: 600 }}>{n.subject}</span>
                        <span className="muted" style={{ fontSize: 11 }}>{formatNotifDate(n.sentAt)}</span>
                      </div>
                      <div className="muted" style={{ fontSize: 12 }}>{n.message}</div>
                    </div>
                  ))}
                </AnimatedList>
              </div>
            )}
          </div>
        )}
      </div>

      <AnimatedThemeToggler size={36} />
    </header>
  );
}
