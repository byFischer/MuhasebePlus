import React, { useState } from 'react';
import AccountTab from './AccountTab';
import PreferencesTab from './PreferencesTab';
import NotificationsTab from './NotificationsTab';
import LanguageTab from './LanguageTab';
import AiTab from './AiTab';
import CompanyProfileTab from './CompanyProfileTab';
import Icon from '@/components/mp/Icon';
import '@/styles/settings.css';

const TABS = {
  account: { label: 'Hesap', icon: 'settings', component: AccountTab },
  company: { label: 'Şirket Profili', icon: 'document', component: CompanyProfileTab },
  preferences: { label: 'Tercihler', icon: 'edit', component: PreferencesTab },
  notifications: { label: 'Bildirimler', icon: 'bell', component: NotificationsTab },
  language: { label: 'Dil', icon: 'globe', component: LanguageTab },
  ai: { label: 'Yapay Zeka', icon: 'zap', component: AiTab },
};

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState('account');
  const ActiveComponent = TABS[activeTab].component;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Hesap Ayarları</h1>
          <p className="page-sub">Profil, tercihler ve uygulama ayarlarınızı yönetin</p>
        </div>
      </div>
      <div className="card">
        <div className="tabs">
          {Object.entries(TABS).map(([key, tab]) => (
            <button key={key} className={activeTab === key ? 'on' : ''} onClick={() => setActiveTab(key)}>
              <Icon name={tab.icon} size={13} /> {tab.label}
            </button>
          ))}
        </div>
        <div className="card-b">
          <ActiveComponent />
        </div>
      </div>
    </div>
  );
}
