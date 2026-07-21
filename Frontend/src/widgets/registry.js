// MuhasebePlus — single source of truth for dashboard widget metadata.
// Add new widget types here; WIDGET_COMPONENTS in DashboardPage wires the actual components.

// Standart varyant tanımları (her widget aynı boyut/etiket setini paylaşır)
export const VARIANT_DEFS = {
  s: { size: 'narrow', label: 'Kompakt',  desc: 'Tek bakışta özet',          width: 1 },
  m: { size: 'wide',   label: 'Standart', desc: 'Detaylı kart görünümü',     width: 2 },
  l: { size: 'full',   label: 'Detaylı',  desc: 'Geniş — tablo & grafik',    width: 3 },
};

export const VARIANT_KEYS = ['s', 'm', 'l'];

// Eski size string'i → yeni varyant key
const SIZE_TO_VARIANT = { narrow: 's', third: 's', wide: 'm', full: 'l' };

const RAW_REGISTRY = [
  {
    id: 'kpis',
    backendType: 'KPI',
    name: 'KPI Kartları',
    desc: 'Gelir, gider, net kar, açık alacak — tek bakışta',
    size: 'full',
    icon: 'chart',
    queryKeys: [['transactions'], ['invoices']],
    defaultConfig: {},
  },
  {
    id: 'chart',
    backendType: 'TIME_SERIES',
    name: 'Gelir & Gider Trendi',
    desc: '12 aylık karşılaştırmalı bar/çizgi grafik',
    size: 'wide',
    icon: 'chart',
    sub: 'Son 12 ay · TRY',
    queryKeys: [['transactions']],
    defaultConfig: { months: 12 },
    configFields: {
      months: { type: 'select', label: 'Kaç ay göster?', options: [3, 6, 12, 24] },
    },
  },
  {
    id: 'cash',
    backendType: 'PIE',
    name: 'Nakit Pozisyonu',
    desc: 'Tüm banka hesaplarının donut dağılımı',
    size: 'narrow',
    icon: 'bank',
    queryKeys: [['bankAccounts']],
    defaultConfig: { accounts: [] },
    configFields: {
      accounts: { type: 'multiselect', label: 'Hangi hesaplar?' },
    },
  },
  {
    id: 'aging',
    backendType: 'TOP_N',
    name: 'Cari Yaşlandırma',
    desc: 'Açık alacakların 0-30, 31-60, 61-90, 90+ kırılımı',
    size: 'third',
    icon: 'users',
    actionRoute: '/cari',
    actionLabel: 'Tümü',
    queryKeys: [['invoices']],
    defaultConfig: { thresholds: [30, 60, 90] },
    configFields: {
      thresholds: { type: 'thresholds', label: 'Eşikler (gün)', buckets: 4 },
    },
  },
  {
    id: 'lowstock',
    backendType: 'ANOMALY',
    name: 'Düşük Stok Uyarıları',
    desc: 'Min eşiğin altındaki ürünler ve sipariş aksiyonları',
    size: 'third',
    icon: 'box',
    actionRoute: '/stok',
    actionLabel: 'Stoklar',
    queryKeys: [['stocks', 'low']],
    defaultConfig: {},
  },
  {
    id: 'quick',
    backendType: 'QUICK_ACTIONS',
    name: 'Hızlı İşlemler',
    desc: 'Sık kullanılan işlemler için kısayollar',
    size: 'third',
    icon: 'flash',
    queryKeys: [],
    defaultConfig: {},
  },
  {
    id: 'recent',
    backendType: 'ACTIVITY_FEED',
    name: 'Son Faturalar',
    desc: 'En son kesilen faturalar listesi',
    size: 'wide',
    icon: 'invoice',
    noPad: true,
    actionRoute: '/fatura',
    actionLabel: 'Tümü',
    queryKeys: [['invoices']],
    defaultConfig: { limit: 6 },
    configFields: {
      limit: { type: 'select', label: 'Kaç fatura?', options: [5, 10, 20] },
    },
  },
  {
    id: 'tasks',
    backendType: 'DAILY_BRIEF',
    name: 'Yaklaşan Görevler',
    desc: 'Planlanmış işlemler ve hatırlatmalar',
    size: 'narrow',
    icon: 'clock',
    queryKeys: [],
    defaultConfig: {},
  },
  {
    id: 'dailybrief',
    backendType: 'DAILY_BRIEF',
    name: 'Gün Özeti',
    desc: 'Sabah açılışta özet: vadesi gelenler, gecikmeler, dün net kar',
    size: 'wide',
    icon: 'info',
    queryKeys: [['invoices'], ['transactions']],
    defaultConfig: {},
  },
  {
    id: 'pinned',
    backendType: 'PINNED_NOTE',
    name: 'Notum',
    desc: 'Dashboardda sabit kalacak serbest metin notu',
    size: 'narrow',
    icon: 'file',
    queryKeys: [],
    defaultConfig: { content: '' },
    configFields: {
      content: { type: 'text', label: 'Not içeriği' },
    },
  },
  {
    id: 'goal',
    backendType: 'PROGRESS_GOAL',
    name: 'Hedef Takibi',
    desc: 'Bu ay gelir/gider hedefi ve kalan gün',
    size: 'third',
    icon: 'chart',
    queryKeys: [['transactions']],
    defaultConfig: { targetAmount: 100000, period: 'MONTHLY' },
    configFields: {
      targetAmount: { type: 'number', label: 'Hedef tutar (₺)' },
      period: { type: 'select', label: 'Dönem', options: ['MONTHLY', 'QUARTERLY'] },
    },
  },
  {
    id: 'forecast',
    backendType: 'FORECAST',
    name: 'Trend Tahmini',
    desc: 'Son aylardan lineer regresyon ile gelecek 3 ay projeksiyonu',
    size: 'wide',
    icon: 'chart',
    queryKeys: [['transactions']],
    defaultConfig: {},
  },
  {
    id: 'heatmap',
    backendType: 'HEATMAP',
    name: 'İşlem Yoğunluğu',
    desc: 'Son 12 haftanın günlük işlem hacmi ızgarası',
    size: 'narrow',
    icon: 'calendar',
    queryKeys: [['transactions']],
    defaultConfig: {},
  },
  {
    id: 'templates',
    backendType: 'TEMPLATES',
    name: 'Şablon Hızlı Uygulama',
    desc: 'Tek tıkla şablon uygulayın — düzenli işlemler anında hazır',
    size: 'wide',
    icon: 'template',
    queryKeys: [['templates']],
    defaultConfig: {},
  },
];

// Custom (DATA_*) widget'lar registry'de yer almaz — runtime'da dashboard_widget kayıtlarından
// dinamik üretilir. Her custom widget kendi widgetId'siyle bağımsız bir kart olarak render edilir.
export const CUSTOM_WIDGET_TYPES = ['DATA_QUERY', 'DATA_CHART', 'DATA_KPI', 'DATA_TABLE'];
export function isCustomWidgetType(backendType) {
  return CUSTOM_WIDGET_TYPES.includes(backendType);
}

// Her entry'ye variants + defaultVariant alanlarını otomatik bağla
export const WIDGET_REGISTRY = RAW_REGISTRY.map(w => ({
  ...w,
  variants: VARIANT_DEFS,
  defaultVariant: SIZE_TO_VARIANT[w.size] || 'm',
}));

// Derived lookup maps — use these instead of manual WIDGET_MAP / REV_MAP
export const BACKEND_TYPE = Object.fromEntries(WIDGET_REGISTRY.map(w => [w.id, w.backendType]));
export const FRONTEND_ID = Object.fromEntries(WIDGET_REGISTRY.map(w => [w.backendType, w.id]));
export const WIDGET_DEF = Object.fromEntries(WIDGET_REGISTRY.map(w => [w.id, w]));
export const DEFAULT_ORDER = WIDGET_REGISTRY.map(w => w.id);

// variant → grid size string + backend width
export function variantToSize(variant) {
  return (VARIANT_DEFS[variant] || VARIANT_DEFS.m).size;
}

export function variantToWidth(variant) {
  return (VARIANT_DEFS[variant] || VARIANT_DEFS.m).width;
}
