import React from 'react';
import Icon from '@/components/mp/Icon';

const ACTIONS = [
  { ic: 'invoice',  l: 'Fatura Kes',      s: '⌘ N', route: '/fatura' },
  { ic: 'users',    l: 'Müşteri Ekle',    s: '⌘ M', route: '/cari' },
  { ic: 'swap',     l: 'Gelir/Gider',     s: '⌘ G', route: '/gelir-gider' },
  { ic: 'box',      l: 'Stok Kaydı',      s: '⌘ S', route: '/stok' },
  { ic: 'template', l: 'Şablon Uygula',   s: '⌘ T', route: '/sablon' },
  { ic: 'chart',    l: 'Kar-Zarar',       s: '⌘ R', route: '/rapor' },
  { ic: 'bank',     l: 'Banka Hareketi',  s: '⌘ B', route: '/banka' },
  { ic: 'log',      l: 'Sistem Logları',  s: '⌘ L', route: '/log' },
];

export default function QuickActionsWidget({ onNav }) {
  return (
    <div className="qa-grid">
      {ACTIONS.map(q => (
        <button key={q.l} className="qa" onClick={() => onNav(q.route)}>
          <div className="qa-icon"><Icon name={q.ic} size={14} /></div>
          <div className="qa-label">{q.l}</div>
          <div className="qa-sub mono">{q.s}</div>
        </button>
      ))}
    </div>
  );
}
