import { useSearchParams } from 'react-router-dom';
import AccountTab from './AccountTab';
import PreferencesTab from './PreferencesTab';
import NotificationsTab from './NotificationsTab';
import CompanyProfileTab from './CompanyProfileTab';
import '@/styles/settings.css';

const TABS = {
  account: { label: 'Hesap', component: AccountTab },
  company: { label: 'Şirket Profili', component: CompanyProfileTab },
  preferences: { label: 'Tercihler', component: PreferencesTab },
  notifications: { label: 'Bildirimler', component: NotificationsTab },
};

export default function SettingsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab');
  const activeTab = TABS[tabParam] ? tabParam : 'account';
  const ActiveComponent = TABS[activeTab].component;

  const setActiveTab = (nextTab) => {
    const next = new URLSearchParams(searchParams);
    if (nextTab === 'account') next.delete('tab');
    else next.set('tab', nextTab);
    setSearchParams(next);
  };

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
              {tab.label}
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
