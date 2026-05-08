// MuhasebePlus — route metadata (single source of truth)
export const ROUTE_META = {
  '/dashboard':   { crumbs: ['Dashboard'],                   icon: 'dashboard', label: 'Dashboard' },
  '/cari':        { crumbs: ['İşlemler', 'Cari Yönetimi'],   icon: 'users',     label: 'Cari (Müşteri)', shortcut: 'm' },
  '/fatura':      { crumbs: ['İşlemler', 'Fatura', 'Faturalar'], icon: 'invoice',   label: 'Faturalar',          shortcut: 'n' },
  '/fatura/odemeler': { crumbs: ['İşlemler', 'Fatura', 'Ödemeler'], icon: 'payment', label: 'Fatura Ödemeleri' },
  '/stok':        { crumbs: ['İşlemler', 'Stok Yönetimi'],   icon: 'box',       label: 'Stok',            shortcut: 's' },
  '/gelir-gider': { crumbs: ['İşlemler', 'Gelir / Gider'],   icon: 'swap',      label: 'Gelir / Gider',   shortcut: 'g' },
  '/banka':       { crumbs: ['İşlemler', 'Banka & Kasa'],    icon: 'bank',      label: 'Banka & Kasa',    shortcut: 'b' },
  '/sablon':      { crumbs: ['Otomasyon', 'Şablonlar'],      icon: 'template',  label: 'Şablonlar',       shortcut: 't' },
  '/rapor':       { crumbs: ['Otomasyon', 'Raporlar'],       icon: 'chart',     label: 'Raporlar',        shortcut: 'r' },
  '/butce':       { crumbs: ['Otomasyon', 'Bütçe'],         icon: 'flash',    label: 'Bütçe',           shortcut: 'u' },
  '/log':         { crumbs: ['Sistem', 'Loglar'],             icon: 'log',       label: 'Sistem Logları',  shortcut: 'l' },
};

export const NAV = [
  { section: 'GENEL' },
  { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
  { section: 'İŞLEMLER' },
  { to: '/cari',        label: 'Cari (Müşteri)', icon: 'users' },
  { to: '/fatura', label: 'Fatura', icon: 'invoice', children: [
    { to: '/fatura', label: 'Faturalar' },
    { to: '/fatura/odemeler', label: 'Fatura Ödemeleri' },
  ]},
  { to: '/stok',        label: 'Stok',            icon: 'box' },
  { to: '/gelir-gider', label: 'Gelir / Gider',   icon: 'swap' },
  { to: '/banka',       label: 'Banka & Kasa',    icon: 'bank' },
  { section: 'OTOMASYON' },
  { to: '/sablon', label: 'Şablonlar', icon: 'template' },
  { to: '/rapor',  label: 'Raporlar',  icon: 'chart' },
  { to: '/butce',  label: 'Bütçe',     icon: 'flash' },
  { section: 'SİSTEM' },
  { to: '/log', label: 'Sistem Logları', icon: 'log' },
];
