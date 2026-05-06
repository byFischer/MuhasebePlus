import React, { useState, useEffect } from 'react';
import Icon from '@/components/mp/Icon';

const LS_KEY = 'muhasebeplus_notifs';

function loadNotifs() {
  try { return JSON.parse(localStorage.getItem(LS_KEY)) || {}; } catch { return {}; }
}

export default function NotificationsTab() {
  const [notifs, setNotifs] = useState(() => {
    const saved = loadNotifs();
    return {
      emailNotifications: true,
      appNotifications: true,
      invoiceReminders: true,
      budgetAlerts: true,
      systemUpdates: false,
      ...saved,
    };
  });

  useEffect(() => {
    localStorage.setItem(LS_KEY, JSON.stringify(notifs));
  }, [notifs]);

  const toggle = (key) => setNotifs(p => ({ ...p, [key]: !p[key] }));

  const items = [
    { key: 'emailNotifications', label: 'E-posta Bildirimleri', desc: 'Önemli güncellemeler mail ile gönderilsin' },
    { key: 'appNotifications', label: 'Uygulama İçi Bildirimler', desc: 'Bildirimleri uygulama içinde göster' },
    { key: 'invoiceReminders', label: 'Fatura Hatırlatmaları', desc: 'Yaklaşan fatura son tarihleri' },
    { key: 'budgetAlerts', label: 'Bütçe Uyarıları', desc: 'Bütçe limitlerine yaklaşınca uyar' },
    { key: 'systemUpdates', label: 'Sistem Güncellemeleri', desc: 'Yeni özellik ve sürüm duyuruları' },
  ];

  return (
    <div className="settings-form-area">
      <div className="settings-section">
        <div className="settings-section-title"><Icon name="bell" size={14} /> Bildirim Tercihleri</div>
        {items.map((item, i) => (
          <React.Fragment key={item.key}>
            {i > 0 && <div className="divider" />}
            <div className="settings-toggle-row">
              <div className="flex-1">
                <div className="settings-field-label">{item.label}</div>
                <div className="settings-field-desc">{item.desc}</div>
              </div>
              <label className="settings-toggle">
                <input type="checkbox" checked={!!notifs[item.key]} onChange={() => toggle(item.key)} />
                <span className="settings-toggle-slider" />
              </label>
            </div>
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
