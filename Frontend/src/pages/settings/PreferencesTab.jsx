import { useState, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import { HOME_PAGE_OPTIONS, loadPreferences, savePreferences } from '@/lib/preferences';

export default function PreferencesTab() {
  const [prefs, setPrefs] = useState(loadPreferences);

  useEffect(() => { savePreferences(prefs); }, [prefs]);

  const set = (key, val) => {
    setPrefs(p => ({ ...p, [key]: val }));
  };

  return (
    <div className="settings-form-area">
      <div className="settings-section">
        <div className="settings-section-title"><Icon name="bank" size={14} /> Varsayılanlar</div>
        <div className="settings-field-row">
          <div>
            <div className="settings-field-label">Açılış Sayfası</div>
            <div className="settings-field-desc">Giriş yapınca açılacak sayfa</div>
          </div>
          <select className="input" style={{ width: 180 }} value={prefs.homepage || '/dashboard'} onChange={e => set('homepage', e.target.value)}>
            {HOME_PAGE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}
