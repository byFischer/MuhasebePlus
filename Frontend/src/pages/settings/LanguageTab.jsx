import React, { useState, useEffect } from 'react';
import Icon from '@/components/mp/Icon';

const LS_KEY = 'muhasebeplus_lang';

const LANGUAGES = [
  { code: 'tr', label: 'Türkçe', flag: '🇹🇷' },
  { code: 'en', label: 'English', flag: '🇬🇧' },
];

export default function LanguageTab() {
  const [language, setLanguage] = useState(() => {
    try { return localStorage.getItem(LS_KEY) || 'tr'; } catch { return 'tr'; }
  });

  useEffect(() => {
    localStorage.setItem(LS_KEY, language);
  }, [language]);

  return (
    <div className="settings-form-area">
      <div className="settings-section">
        <div className="settings-section-title"><Icon name="globe" size={14} /> Uygulama Dili</div>
        {LANGUAGES.map((lang) => (
          <div key={lang.code} className="settings-lang-item" onClick={() => setLanguage(lang.code)}>
            <span className="settings-lang-flag">{lang.flag}</span>
            <span className="settings-lang-label">{lang.label}</span>
            {language === lang.code && <span className="pill pos" style={{ marginLeft: 'auto' }}><Icon name="check" size={10} /> Seçili</span>}
          </div>
        ))}
      </div>
    </div>
  );
}
