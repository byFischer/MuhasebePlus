import { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import Icon from '@/components/mp/Icon';
import customerService from '@/services/customerService';
import invoiceService from '@/services/invoiceService';

const DEFAULT_ITEMS = [
  {
    kind: 'Hızlı',
    label: 'Ayarlar',
    desc: 'Hesap ve uygulama ayarları',
    path: '/settings',
    icon: 'settings',
    keywords: ['profil', 'tercihler', 'bildirimler', 'ayarlar'],
  },
  {
    kind: 'Hızlı',
    label: 'Widget Oluştur',
    desc: 'Dashboard için özel widget tasarla',
    path: '/widget-builder',
    icon: 'sparkle',
    keywords: ['widget', 'dashboard widget', 'özel panel', 'ozel panel', 'grafik oluştur', 'grafik olustur'],
  },
];

const PAGE_ITEMS = [
  { kind: 'Sayfa', label: 'Dashboard', desc: 'Ana panel', path: '/dashboard', icon: 'dashboard', keywords: ['ana sayfa', 'panel', 'özet', 'ozet'] },
  { kind: 'Sayfa', label: 'Cari (Müşteri)', desc: 'Müşteri ve cari yönetimi', path: '/cari', icon: 'users', keywords: ['müşteri', 'musteri', 'tedarikçi', 'tedarikci', 'cari kart', 'cari hesap'] },
  { kind: 'Sayfa', label: 'Fatura', desc: 'Satış ve alış faturaları', path: '/fatura', icon: 'invoice', keywords: ['faturalar', 'satış faturası', 'satis faturasi', 'alış faturası', 'alis faturasi'] },
  { kind: 'Sayfa', label: 'Fatura Ödemeleri', desc: 'Tahsilat ve ödeme kayıtları', path: '/fatura/odemeler', icon: 'payment', keywords: ['tahsilat', 'ödeme', 'odeme', 'fatura ödemeleri', 'fatura odemeleri'] },
  { kind: 'Sayfa', label: 'Stok', desc: 'Ürün ve stok yönetimi', path: '/stok', icon: 'box', keywords: ['ürün', 'urun', 'stok kartı', 'stok karti', 'barkod'] },
  { kind: 'Sayfa', label: 'Gelir / Gider', desc: 'Gelir, gider ve virman kayıtları', path: '/gelir-gider', icon: 'swap', keywords: ['işlem', 'islem', 'gelir gider', 'virman'] },
  { kind: 'Sayfa', label: 'Banka & Kasa', desc: 'Banka, kasa ve POS hesapları', path: '/banka', icon: 'bank', keywords: ['banka', 'kasa', 'pos', 'hesap yönetimi', 'hesap yonetimi'] },
  { kind: 'Sayfa', label: 'Çek / Senet', desc: 'Çek ve senet portföyü', path: '/cek', icon: 'cheque', keywords: ['çek', 'cek', 'senet', 'portföy', 'portfoy'] },
  { kind: 'Sayfa', label: 'Hesap Planı', desc: 'Tek Düzen Hesap Planı', path: '/hesap-plani', icon: 'accounting', keywords: ['tdhp', 'tek düzen', 'tek duzen', 'muhasebe hesapları', 'muhasebe hesaplari'] },
  { kind: 'Sayfa', label: 'Yevmiye Defteri', desc: 'Muhasebe fişleri ve defter ekranı', path: '/yevmiye', icon: 'journal', keywords: ['yevmiye', 'muhasebe defteri', 'fiş', 'fis', 'mahsup'] },
  { kind: 'Sayfa', label: 'Muhasebe Dönemi', desc: 'Dönem açma ve kapama', path: '/donem', icon: 'calendar', keywords: ['dönem', 'donem', 'kapanış', 'kapanis', 'açılış', 'acilis'] },
  { kind: 'Sayfa', label: 'Beyannameler', desc: 'KDV, Muhtasar, Ba-Bs ve vergi beyannameleri', path: '/beyanname', icon: 'document', keywords: ['beyanname', 'kdv', 'muhtasar', 'ba bs', 'vergi'] },
  { kind: 'Sayfa', label: 'e-Defter', desc: 'e-Defter üretim ve gönderim ekranı', path: '/edefter', icon: 'archive', keywords: ['edefter', 'e defter', 'berat', 'gib'] },
  { kind: 'Sayfa', label: 'Sabit Kıymetler', desc: 'Demirbaş ve amortisman takibi', path: '/sabit-kiymet', icon: 'building', keywords: ['sabit kıymet', 'sabit kiymet', 'demirbaş', 'demirbas', 'amortisman'] },
  { kind: 'Sayfa', label: 'Şablonlar', desc: 'Hızlı işlem şablonları', path: '/sablon', icon: 'template', keywords: ['şablon', 'sablon', 'otomasyon', 'tekrarlayan işlem', 'tekrarlayan islem'] },
  { kind: 'Sayfa', label: 'Raporlar', desc: 'Finansal raporlar ve çıktılar', path: '/rapor', icon: 'chart', keywords: ['rapor', 'raporlar', 'çıktı', 'cikti', 'excel'] },
  { kind: 'Sayfa', label: 'Bütçe', desc: 'Bütçe planı ve gerçekleşme takibi', path: '/butce', icon: 'flash', keywords: ['bütçe', 'butce', 'plan', 'sapma'] },
  { kind: 'Sayfa', label: 'Sistem Logları', desc: 'İşlem geçmişi ve denetim kayıtları', path: '/log', icon: 'log', keywords: ['log', 'sistem logu', 'işlem geçmişi', 'islem gecmisi', 'denetim'] },
];

const SUBPAGE_ITEMS = [
  { kind: 'Alt Sayfa', label: 'Satış Faturaları', desc: 'Fatura > Satış faturaları', path: '/fatura?type=sale', icon: 'invoice', keywords: ['satış faturası', 'satis faturasi', 'satış faturaları', 'satis faturalari', 'kestiğim fatura', 'kestigim fatura', 'sales'] },
  { kind: 'Alt Sayfa', label: 'Alış Faturaları', desc: 'Fatura > Alış faturaları', path: '/fatura?type=purchase', icon: 'invoice', keywords: ['alış faturası', 'alis faturasi', 'alış faturaları', 'alis faturalari', 'gelen fatura', 'tedarikçi faturası', 'tedarikci faturasi', 'purchase'] },
  { kind: 'Alt Sayfa', label: 'Ödenmiş Faturalar', desc: 'Fatura > Ödendi', path: '/fatura?status=paid', icon: 'invoice', keywords: ['ödenmiş fatura', 'odenmis fatura', 'ödenen fatura', 'odenen fatura', 'tahsil edilen fatura', 'paid fatura'] },
  { kind: 'Alt Sayfa', label: 'Ödenmemiş Faturalar', desc: 'Fatura > Ödenmedi', path: '/fatura?status=unpaid', icon: 'invoice', keywords: ['ödenmemiş fatura', 'odenmemis fatura', 'ödenmeyen fatura', 'odenmeyen fatura', 'açık fatura', 'acik fatura', 'borçlu fatura', 'borclu fatura', 'unpaid fatura'] },
  { kind: 'Alt Sayfa', label: 'Bekleyen Faturalar', desc: 'Fatura > Beklemede', path: '/fatura?status=pending', icon: 'invoice', keywords: ['bekleyen fatura', 'beklemede fatura', 'pending fatura'] },
  { kind: 'Alt Sayfa', label: 'Gelirler', desc: 'Gelir / Gider içindeki gelir kayıtları', path: '/gelir-gider?tab=gelir', icon: 'swap', keywords: ['gelir', 'gelir kısmı', 'gelir kismi', 'gelir tabı', 'gelir tabi', 'gelir işlemleri', 'gelir islemleri'] },
  { kind: 'Alt Sayfa', label: 'Giderler', desc: 'Gelir / Gider içindeki gider kayıtları', path: '/gelir-gider?tab=gider', icon: 'swap', keywords: ['gider', 'gider kısmı', 'gider kismi', 'gider tabı', 'gider tabi', 'gider işlemleri', 'gider islemleri'] },
  { kind: 'Alt Sayfa', label: 'Tüm Gelir / Gider İşlemleri', desc: 'Gelir / Gider içindeki tüm kayıtlar', path: '/gelir-gider?tab=hepsi', icon: 'swap', keywords: ['tüm işlemler', 'tum islemler', 'hepsi gelir gider'] },
  { kind: 'Alt Sayfa', label: 'Yevmiye Defteri', desc: 'Muhasebe Defteri > Yevmiye', path: '/yevmiye?tab=journal', icon: 'journal', keywords: ['yevmiye kayıtları', 'yevmiye kayitlari', 'muhasebe fişleri', 'muhasebe fisleri'] },
  { kind: 'Alt Sayfa', label: 'Mizan', desc: 'Muhasebe Defteri > Mizan', path: '/yevmiye?tab=trial', icon: 'chart', keywords: ['mizan kısmı', 'mizan kismi', 'trial balance', 'mizan raporu'] },
  { kind: 'Alt Sayfa', label: 'Gelir Tablosu', desc: 'Muhasebe Defteri > Gelir Tablosu', path: '/yevmiye?tab=income', icon: 'chart', keywords: ['gelir tablosu', 'kar zarar tablosu', 'kar-zarar tablosu'] },
  { kind: 'Alt Sayfa', label: 'Bilanço', desc: 'Muhasebe Defteri > Bilanço', path: '/yevmiye?tab=balance', icon: 'chart', keywords: ['bilanço sayfası', 'bilanco sayfasi', 'bilanço kısmı', 'bilanco kismi', 'balance sheet'] },
  { kind: 'Alt Sayfa', label: 'Manuel Fiş', desc: 'Muhasebe Defteri > Manuel Fiş', path: '/yevmiye?tab=manual', icon: 'journal', keywords: ['mahsup fişi', 'mahsup fisi', 'manuel yevmiye', 'fiş girişi', 'fis girisi'] },
  { kind: 'Alt Sayfa', label: 'Sağlıklı Stoklar', desc: 'Stok > Sağlıklı filtre', path: '/stok?filter=saglikli', icon: 'box', keywords: ['sağlıklı stok', 'saglikli stok', 'normal stok'] },
  { kind: 'Alt Sayfa', label: 'Düşük Stoklar', desc: 'Stok > Düşük filtre', path: '/stok?filter=dusuk', icon: 'box', keywords: ['düşük stok', 'dusuk stok', 'azalan stok', 'kritik stok'] },
  { kind: 'Alt Sayfa', label: 'Tükenmiş Stoklar', desc: 'Stok > Tükenmiş filtre', path: '/stok?filter=tukenmis', icon: 'box', keywords: ['tükenmiş stok', 'tukenmis stok', 'stok bitti', 'sıfır stok', 'sifir stok'] },
  { kind: 'Alt Sayfa', label: 'Demirbaşlar', desc: 'Sabit Kıymetler > Demirbaşlar', path: '/sabit-kiymet?tab=assets', icon: 'building', keywords: ['demirbaş listesi', 'demirbas listesi', 'sabit kıymet listesi', 'sabit kiymet listesi'] },
  { kind: 'Alt Sayfa', label: 'Sabit Kıymet Kategorileri', desc: 'Sabit Kıymetler > Kategoriler', path: '/sabit-kiymet?tab=categories', icon: 'building', keywords: ['amortisman kategorileri', 'demirbaş kategorileri', 'demirbas kategorileri'] },
  { kind: 'Alt Sayfa', label: 'Amortisman Tablosu', desc: 'Sabit Kıymetler > Amortisman Tablosu', path: '/sabit-kiymet?tab=schedule', icon: 'chart', keywords: ['amortisman planı', 'amortisman plani', 'amortisman çizelgesi', 'amortisman cizelgesi'] },
];

const DECLARATION_ITEMS = [
  { kind: 'Beyanname', label: 'KDV1', desc: 'KDV1 beyannamesi', path: '/beyanname?tab=kdv1', icon: 'document', keywords: ['kdv 1', 'katma değer vergisi', 'katma deger vergisi'] },
  { kind: 'Beyanname', label: 'KDV2', desc: 'KDV2 beyannamesi', path: '/beyanname?tab=kdv2', icon: 'document', keywords: ['kdv 2', 'tevkifat kdv'] },
  { kind: 'Beyanname', label: 'Muhtasar', desc: 'Muhtasar beyannamesi', path: '/beyanname?tab=muhtasar', icon: 'document', keywords: ['muhtasar beyanname', 'stopaj', 'withholding'] },
  { kind: 'Beyanname', label: 'Ba-Bs', desc: 'Ba-Bs formları', path: '/beyanname?tab=babs', icon: 'document', keywords: ['ba', 'bs', 'ba bs', 'ba-bs', 'alış satış formu', 'alis satis formu'] },
  { kind: 'Beyanname', label: 'Geçici Vergi', desc: 'Geçici vergi beyannamesi', path: '/beyanname?tab=gecici', icon: 'document', keywords: ['geçici', 'gecici', 'geçici vergi', 'gecici vergi'] },
  { kind: 'Beyanname', label: 'Kurumlar Vergisi', desc: 'Kurumlar vergisi beyannamesi', path: '/beyanname?tab=kurumlar', icon: 'document', keywords: ['kurumlar', 'kurumlar vergisi', 'yıllık beyanname', 'yillik beyanname'] },
];

const REPORT_ITEMS = [
  { value: 'PROFIT_LOSS', label: 'Kar-Zarar', keywords: ['kar zarar', 'kâr zarar', 'gelir gider raporu'] },
  { value: 'CASH_FLOW', label: 'Nakit Akış', keywords: ['nakit akışı', 'nakit akisi', 'cash flow'] },
  { value: 'AR_AGING', label: 'Cari Yaşlandırma', keywords: ['cari yaşlandırma', 'cari yaslandirma', 'vade yaşlandırma', 'vade yaslandirma'] },
  { value: 'VAT_PREP', label: 'KDV Beyanname Hazırlık', keywords: ['kdv hazırlık', 'kdv hazirlik', 'kdv raporu'] },
  { value: 'STOCK_STATUS', label: 'Stok Durum', keywords: ['stok raporu', 'stok durumu', 'stok durum'] },
  { value: 'COLLECTION_PERFORMANCE', label: 'Tahsilat Performansı', keywords: ['tahsilat performansı', 'tahsilat performansi'] },
  { value: 'SLOW_INVENTORY', label: 'Yavaş Dönen Stok', keywords: ['yavaş stok', 'yavas stok', 'yavaş dönen', 'yavas donen'] },
  { value: 'BUDGET_VARIANCE', label: 'Bütçe-Gerçekleşen Sapma', keywords: ['bütçe sapma', 'butce sapma', 'gerçekleşen', 'gerceklesen'] },
  { value: 'BANK_RECONCILIATION', label: 'Banka/Kasa Mutabakat', keywords: ['banka mutabakat', 'kasa mutabakat'] },
  { value: 'EXECUTIVE_SUMMARY', label: 'Yönetici Finans Sağlığı Özeti', keywords: ['yönetici özeti', 'yonetici ozeti', 'finans sağlığı', 'finans sagligi'] },
  { value: 'STATEMENT', label: 'Cari Hesap Ekstresi', keywords: ['ekstre', 'cari ekstre', 'hesap ekstresi'] },
  { value: 'RECONCILIATION', label: 'Cari Mutabakat Mektubu', keywords: ['cari mutabakat', 'mutabakat mektubu'] },
  { value: 'TRIAL_BALANCE', label: 'Mizan', keywords: ['mizan raporu', 'trial balance'] },
  { value: 'INCOME_STATEMENT', label: 'Gelir Tablosu', keywords: ['gelir tablosu raporu', 'kar zarar tablosu'] },
  { value: 'BALANCE_SHEET', label: 'Bilanço', keywords: ['bilanço raporu', 'bilanco raporu', 'balance sheet'] },
  { value: 'JOURNAL_LISTING', label: 'Yevmiye Dökümü', keywords: ['yevmiye dökümü', 'yevmiye dokumu', 'yevmiye raporu'] },
].map(report => ({
  kind: 'Rapor',
  label: report.label,
  desc: 'Raporlar içinde aç',
  path: `/rapor?type=${report.value}`,
  icon: 'chart',
  keywords: report.keywords,
}));

const SETTINGS_ITEMS = [
  { kind: 'Ayar', label: 'Hesap Ayarları', desc: 'Ayarlar > Hesap', path: '/settings?tab=account', icon: 'settings', keywords: ['hesap', 'profil', 'kullanıcı ayarları', 'kullanici ayarlari'] },
  { kind: 'Ayar', label: 'Şirket Profili', desc: 'Ayarlar > Şirket Profili', path: '/settings?tab=company', icon: 'document', keywords: ['şirket ayarları', 'sirket ayarlari', 'firma bilgileri', 'company'] },
  { kind: 'Ayar', label: 'Tercihler', desc: 'Ayarlar > Tercihler', path: '/settings?tab=preferences', icon: 'edit', keywords: ['tema', 'dil', 'ana sayfa', 'tercih'] },
  { kind: 'Ayar', label: 'Bildirimler', desc: 'Ayarlar > Bildirimler', path: '/settings?tab=notifications', icon: 'bell', keywords: ['bildirim', 'e-posta', 'email', 'hatırlatma', 'hatirlatma'] },
];

const ACTION_ITEMS = [
  { kind: 'Eylem', label: 'Yeni Müşteri Ekle', desc: 'Cari kaydı oluştur', path: '/cari?new=1', icon: 'plus', keywords: ['müşteri ekle', 'musteri ekle', 'yeni müşteri', 'yeni musteri', 'cari ekle'] },
  { kind: 'Eylem', label: 'Yeni Fatura Kes', desc: 'Fatura oluşturma ekranı', path: '/fatura?new=1', icon: 'plus', keywords: ['fatura kes', 'fatura oluştur', 'fatura olustur', 'yeni fatura', 'satış faturası', 'satis faturasi'] },
  { kind: 'Eylem', label: 'Yeni Tahsilat', desc: 'Satış faturası tahsilatı ekle', path: '/fatura/odemeler?new=sale', icon: 'plus', keywords: ['tahsilat ekle', 'yeni tahsilat', 'fatura tahsilatı', 'fatura tahsilati'] },
  { kind: 'Eylem', label: 'Yeni Ödeme', desc: 'Alış faturası ödemesi ekle', path: '/fatura/odemeler?new=purchase', icon: 'plus', keywords: ['ödeme ekle', 'odeme ekle', 'yeni ödeme', 'yeni odeme', 'fatura ödemesi', 'fatura odemesi'] },
  { kind: 'Eylem', label: 'Yeni Ürün Ekle', desc: 'Stok kartı oluştur', path: '/stok?new=1', icon: 'plus', keywords: ['stoktan yeni ürün ekle', 'stoktan yeni urun ekle', 'ürün ekle', 'urun ekle', 'yeni ürün', 'yeni urun'] },
  { kind: 'Eylem', label: 'Yeni İşlem', desc: 'Gelir, gider veya virman kaydı oluştur', path: '/gelir-gider?new=1', icon: 'plus', keywords: ['yeni işlem', 'yeni islem', 'gelir ekle', 'gider ekle', 'virman ekle'] },
  { kind: 'Eylem', label: 'Yeni Banka Hesabı', desc: 'Banka, kasa veya POS hesabı ekle', path: '/banka?new=1', icon: 'plus', keywords: ['yeni banka', 'banka ekle', 'kasa ekle', 'pos ekle', 'yeni kasa', 'yeni pos'] },
  { kind: 'Eylem', label: 'Yeni Çek / Senet', desc: 'Portföy kaydı oluştur', path: '/cek?new=1', icon: 'plus', keywords: ['yeni çek', 'yeni cek', 'yeni çelk', 'yeni celk', 'çek ekle', 'cek ekle', 'senet ekle', 'yeni senet'] },
  { kind: 'Eylem', label: 'Yeni Bütçe Kalemi', desc: 'Gelir veya gider planı ekle', path: '/butce?new=1', icon: 'plus', keywords: ['bütçe ekle', 'butce ekle', 'yeni bütçe', 'yeni butce', 'yeni kalem'] },
  { kind: 'Eylem', label: 'Yeni Hesap Planı Hesabı', desc: 'TDHP hesabı oluştur', path: '/hesap-plani?new=1', icon: 'plus', keywords: ['hesap planı ekle', 'hesap plani ekle', 'tdhp hesap ekle', 'yeni hesap'] },
  { kind: 'Eylem', label: 'Yeni Şablon', desc: 'Tekrarlayan işlem şablonu oluştur', path: '/sablon?new=1', icon: 'plus', keywords: ['şablon ekle', 'sablon ekle', 'yeni şablon', 'yeni sablon'] },
  { kind: 'Eylem', label: 'Yeni Demirbaş', desc: 'Sabit kıymet kaydı oluştur', path: '/sabit-kiymet?new=1', icon: 'plus', keywords: ['demirbaş ekle', 'demirbas ekle', 'sabit kıymet ekle', 'sabit kiymet ekle'] },
  { kind: 'Eylem', label: 'Yeni Sabit Kıymet Kategorisi', desc: 'Amortisman kategorisi oluştur', path: '/sabit-kiymet?category=1', icon: 'plus', keywords: ['sabit kıymet kategorisi', 'sabit kiymet kategorisi', 'amortisman kategorisi', 'kategori ekle'] },
];

const SEARCH_ITEMS = [
  ...DEFAULT_ITEMS,
  ...PAGE_ITEMS,
  ...SUBPAGE_ITEMS,
  ...DECLARATION_ITEMS,
  ...REPORT_ITEMS,
  ...SETTINGS_ITEMS,
  ...ACTION_ITEMS,
];

const KIND_ICONS = {
  Hızlı: 'sparkle',
  Sayfa: 'chevRight',
  'Alt Sayfa': 'chevRight',
  Beyanname: 'document',
  Rapor: 'chart',
  Ayar: 'settings',
  Eylem: 'flash',
  Müşteri: 'users',
  Fatura: 'invoice',
};

function normalizeText(value) {
  return String(value || '')
    .toLocaleLowerCase('tr')
    .replace(/ı/g, 'i')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
}

function scoreItem(item, needle) {
  const label = normalizeText(item.label);
  const keywordText = (item.keywords || []).join(' ');
  const haystack = normalizeText(`${item.label} ${item.desc || ''} ${item.kind || ''} ${keywordText}`);
  const tokens = needle.split(/\s+/).filter(Boolean);

  if (!needle) return 1;
  if (label === needle) return 140;
  if ((item.keywords || []).some(k => normalizeText(k) === needle)) return 130;
  if (label.startsWith(needle)) return 110;
  if ((item.keywords || []).some(k => normalizeText(k).startsWith(needle))) return 95;
  if (haystack.includes(needle)) return 75;
  if (tokens.length > 1 && tokens.every(token => haystack.includes(token))) return 55;
  if (tokens.some(token => token.length > 2 && label.includes(token))) return 40;
  return 0;
}

function useDebouncedValue(value, delay = 180) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}

export default function CommandPalette({ onClose }) {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [idx, setIdx] = useState(0);
  const ref = useRef();
  useEffect(() => { ref.current?.focus(); }, []);

  const query = q.trim();
  const needle = useMemo(() => normalizeText(query), [query]);
  const debouncedQuery = useDebouncedValue(query);
  const liveSearchEnabled = debouncedQuery.length >= 2;

  const { data: customers = [], isFetching: customersFetching } = useQuery({
    queryKey: ['cmdk', 'customers', debouncedQuery],
    queryFn: () => customerService.list({ search: debouncedQuery, size: 5 }),
    enabled: liveSearchEnabled,
    staleTime: 20_000,
  });

  const { data: invoices = [], isFetching: invoicesFetching } = useQuery({
    queryKey: ['cmdk', 'invoices', debouncedQuery],
    queryFn: () => invoiceService.list({ search: debouncedQuery, size: 5 }),
    enabled: liveSearchEnabled,
    staleTime: 20_000,
  });

  const staticItems = useMemo(() => {
    if (!needle) return DEFAULT_ITEMS;
    return SEARCH_ITEMS
      .map(item => ({ ...item, score: scoreItem(item, needle) }))
      .filter(item => item.score > 0)
      .sort((a, b) => b.score - a.score);
  }, [needle]);

  const customerItems = useMemo(() => customers.slice(0, 5).map(customer => ({
    kind: 'Müşteri',
    label: customer.name,
    desc: [customer.accountCode, customer.taxNumber].filter(Boolean).join(' · ') || 'Cari kart',
    path: `/cari?customerId=${customer.customerId}`,
    icon: 'users',
  })), [customers]);

  const invoiceItems = useMemo(() => invoices.slice(0, 5).map(invoice => ({
    kind: 'Fatura',
    label: invoice.invoiceNumber || `Fatura #${invoice.invoiceId}`,
    desc: [invoice.customerName, invoice.invoiceDate || invoice.dueDate].filter(Boolean).join(' · '),
    path: `/fatura?invoiceId=${invoice.invoiceId}`,
    icon: 'invoice',
  })), [invoices]);

  const filtered = useMemo(() => {
    const liveItems = liveSearchEnabled ? [...customerItems, ...invoiceItems] : [];
    return [...liveItems, ...staticItems];
  }, [customerItems, invoiceItems, liveSearchEnabled, staticItems]);

  const grouped = filtered.reduce((acc, it) => { (acc[it.kind] ||= []).push(it); return acc; }, {});
  const isFetching = liveSearchEnabled && (customersFetching || invoicesFetching);

  useEffect(() => { setIdx(0); }, [q]);
  useEffect(() => {
    const select = (item) => {
      navigate(item.path);
      onClose();
    };
    const h = (e) => {
      if (e.key === 'ArrowDown') { e.preventDefault(); setIdx(i => Math.min(filtered.length - 1, i + 1)); }
      if (e.key === 'ArrowUp')   { e.preventDefault(); setIdx(i => Math.max(0, i - 1)); }
      if (e.key === 'Enter')     { e.preventDefault(); const item = filtered[idx]; if (item) select(item); }
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
          <input
            ref={ref}
            placeholder="Sayfa, müşteri, fatura, rapor veya işlem ara..."
            value={q}
            onChange={e => setQ(e.target.value)}
          />
        </div>
        <div className="cmdk-list">
          {Object.entries(grouped).map(([k, arr]) => (
            <div key={k}>
              <div className="cmdk-section">{k}</div>
              {arr.map(it => {
                const i = counter++;
                return (
                  <div key={`${it.kind}-${it.path}-${it.label}`} className={`cmdk-item ${i === idx ? 'on' : ''}`}
                       onClick={() => { navigate(it.path); onClose(); }}
                       onMouseEnter={() => setIdx(i)}>
                    <Icon name={it.icon || KIND_ICONS[k] || 'flash'} size={14} />
                    <span>{it.label}</span>
                    <span className="desc">{it.desc}</span>
                  </div>
                );
              })}
            </div>
          ))}
          {isFetching && <div className="empty">Aranıyor...</div>}
          {!isFetching && filtered.length === 0 && <div className="empty">Sonuç bulunamadı</div>}
        </div>
      </div>
    </div>
  );
}
