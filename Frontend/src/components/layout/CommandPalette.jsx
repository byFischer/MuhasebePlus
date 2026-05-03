import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Icon from '@/components/mp/Icon';

const ITEMS = [
  { kind: 'Sayfa', label: 'Dashboard', desc: 'Ana panel', kbd: '', path: '/dashboard' },
  { kind: 'Sayfa', label: 'Cari (Müşteri)', desc: 'Müşteri yönetimi', kbd: '⌘M', path: '/cari' },
  { kind: 'Sayfa', label: 'Fatura', desc: 'Fatura yönetimi', kbd: '⌘N', path: '/fatura' },
  { kind: 'Sayfa', label: 'Stok', desc: 'Ürün ve stok', kbd: '⌘S', path: '/stok' },
  { kind: 'Sayfa', label: 'Gelir / Gider', desc: 'İşlem kaydı', kbd: '⌘G', path: '/gelir-gider' },
  { kind: 'Sayfa', label: 'Banka & Kasa', desc: 'Hesap yönetimi', kbd: '⌘B', path: '/banka' },
  { kind: 'Sayfa', label: 'Şablonlar', desc: 'Hızlı işlem şablonları', kbd: '⌘T', path: '/sablon' },
  { kind: 'Sayfa', label: 'Raporlar', desc: 'Kar-Zarar, Nakit Akış', kbd: '⌘R', path: '/rapor' },
  { kind: 'Sayfa', label: 'Sistem Logları', desc: 'Tüm işlem geçmişi', kbd: '⌘L', path: '/log' },
  { kind: 'Eylem', label: 'Yeni Müşteri Ekle', desc: 'Cari kaydı oluştur', path: '/cari' },
  { kind: 'Eylem', label: 'Yeni Fatura Kes', desc: '3 adımlı fatura', path: '/fatura' },
  { kind: 'Eylem', label: 'Yeni Banka Hesabı', desc: 'IBAN ile hesap ekle', path: '/banka' },
];

export default function CommandPalette({ onClose }) {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [idx, setIdx] = useState(0);
  const ref = useRef();
  useEffect(() => { ref.current?.focus(); }, []);

  const filtered = q ? ITEMS.filter(i => (i.label + i.desc + i.kind).toLowerCase().includes(q.toLowerCase())) : ITEMS;
  const grouped = filtered.reduce((acc, it) => { (acc[it.kind] ||= []).push(it); return acc; }, {});

  useEffect(() => { setIdx(0); }, [q]);
  useEffect(() => {
    const h = (e) => {
      if (e.key === 'ArrowDown') { e.preventDefault(); setIdx(i => Math.min(filtered.length - 1, i + 1)); }
      if (e.key === 'ArrowUp')   { e.preventDefault(); setIdx(i => Math.max(0, i - 1)); }
      if (e.key === 'Enter')     { e.preventDefault(); const item = filtered[idx]; if (item) { navigate(item.path); onClose(); } }
      if (e.key === 'Escape')    { onClose(); }
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [filtered, idx, navigate, onClose]);

  let counter = 0;
  return (
    <div className="cmdk-scrim" onClick={onClose}>
      <div className="cmdk" onClick={e => e.stopPropagation()}>
        <div className="cmdk-input">
          <Icon name="search" size={16} />
          <input ref={ref} placeholder="Bir sayfa, müşteri, fatura, eylem ara..." value={q} onChange={e => setQ(e.target.value)} />
          <kbd style={{ fontSize: 10, color: 'var(--ink-3)' }}>esc</kbd>
        </div>
        <div className="cmdk-list">
          {Object.entries(grouped).map(([k, arr]) => (
            <div key={k}>
              <div className="cmdk-section">{k}</div>
              {arr.map(it => {
                const i = counter++;
                return (
                  <div key={it.label} className={`cmdk-item ${i === idx ? 'on' : ''}`}
                       onClick={() => { navigate(it.path); onClose(); }}
                       onMouseEnter={() => setIdx(i)}>
                    <Icon name={k === 'Sayfa' ? 'chevRight' : 'flash'} size={14} />
                    <span>{it.label}</span>
                    <span className="desc">{it.desc}</span>
                    {it.kbd && <kbd>{it.kbd}</kbd>}
                  </div>
                );
              })}
            </div>
          ))}
          {filtered.length === 0 && <div className="empty">Sonuç bulunamadı</div>}
        </div>
      </div>
    </div>
  );
}
