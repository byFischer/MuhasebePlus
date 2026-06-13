import { useState, useEffect } from 'react';
import Icon from '@/components/mp/Icon';

const LS_KEY = 'muhasebeplus_prefs';

function loadPrefs() {
  try { return JSON.parse(localStorage.getItem(LS_KEY)) || {}; } catch { return {}; }
}

function savePrefs(prefs) {
  localStorage.setItem(LS_KEY, JSON.stringify(prefs));
}

export default function PreferencesTab() {
  const [prefs, setPrefs] = useState(loadPrefs);

  useEffect(() => { savePrefs(prefs); }, [prefs]);

  const set = (key, val) => {
    setPrefs(p => ({ ...p, [key]: val }));
    if (key === 'theme') {
      document.documentElement.setAttribute('data-theme', val);
      localStorage.setItem('theme', val);
    }
  };

  const isDark = prefs.theme ? prefs.theme === 'dark' : document.documentElement.getAttribute('data-theme') === 'dark';

  return (
    <div className="settings-form-area">
      <div className="settings-section">
        <div className="settings-section-title"><Icon name="moon" size={14} /> Görünüm</div>
        <div className="settings-field-row">
          <div>
            <div className="settings-field-label">Tema</div>
            <div className="settings-field-desc">Uygulama renk modu</div>
          </div>
          <div style={{ display: 'flex' }}>
            <button className={`seg ${!isDark ? 'on' : ''}`} onClick={() => set('theme', 'light')}>
              <Icon name="moon" size={12} /> Açık
            </button>
            <button className={`seg ${isDark ? 'on' : ''}`} onClick={() => set('theme', 'dark')}>
              <Icon name="moon" size={12} /> Koyu
            </button>
          </div>
        </div>
        <div className="divider" />
        <div className="settings-field-row">
          <div>
            <div className="settings-field-label">Yoğunluk</div>
            <div className="settings-field-desc">Sayfa içerik sıklığı</div>
          </div>
          <select className="input" style={{ width: 160 }} value={prefs.density || 'default'} onChange={e => set('density', e.target.value)}>
            <option value="default">Varsayılan</option>
            <option value="compact">Sıkışık</option>
          </select>
        </div>
      </div>

      <div className="divider" />

      <div className="settings-section">
        <div className="settings-section-title"><Icon name="bank" size={14} /> Varsayılanlar</div>
        <div className="settings-field-row">
          <div>
            <div className="settings-field-label">Para Birimi</div>
            <div className="settings-field-desc">Yeni işlemlerde varsayılan</div>
          </div>
          <select className="input" style={{ width: 120 }} value={prefs.currency || 'TRY'} onChange={e => set('currency', e.target.value)}>
            <option value="TRY">₺ TRY</option>
            <option value="USD">$ USD</option>
            <option value="EUR">€ EUR</option>
            <option value="GBP">£ GBP</option>
          </select>
        </div>
        <div className="divider" />
        <div className="settings-field-row">
          <div>
            <div className="settings-field-label">Açılış Sayfası</div>
            <div className="settings-field-desc">Giriş yapınca açılacak sayfa</div>
          </div>
          <select className="input" style={{ width: 180 }} value={prefs.homepage || '/dashboard'} onChange={e => set('homepage', e.target.value)}>
            <option value="/dashboard">Dashboard</option>
            <option value="/cari">Cari Yönetimi</option>
            <option value="/fatura">Fatura Yönetimi</option>
            <option value="/banka">Banka & Kasa</option>
          </select>
        </div>
      </div>
    </div>
  );
}
