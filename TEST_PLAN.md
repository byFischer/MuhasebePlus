# MuhasebePlus — Test Mühendisliği İncelemesi ve Test Yazma Planı

## Bağlam

MuhasebePlus; Java 21 / Spring Boot 4 backend, React 18 + Vite frontend ve Electron masaüstü
sarmalayıcısından oluşan çok kiracılı (multi-tenant) bir Türkçe muhasebe uygulaması.
Para ve vergi hesaplayan bir sistem olmasına rağmen test kapsamı çok düşük:

- **Backend:** 471 main Java dosyasına (48 controller, 65+ servis) karşılık sadece **22 test
  sınıfı** var. Test altyapısı sağlam (JUnit 5, Mockito, AssertJ, Testcontainers/PostgreSQL,
  JaCoCo, CI'da test adımı var) ama en riskli sınıflar — `InvoiceService` (KDV, iskonto,
  tevkifat, durum makinesi), `declaration` modülü (beyannameler), `DepreciationService`,
  `ReconciliationService`, rapor üreticileri — **tamamen testsiz**.
- **Frontend:** Test altyapısı **hiç yok** — framework kurulu değil, tek test dosyası yok,
  CI sadece lint + build yapıyor. Oysa IBAN/TCKN/VKN checksum doğrulayıcıları ve para/tarih
  formatlama gibi saf mantık birimleri var.
- **Electron:** Test yok (185 satırlık main process).

Amaç: riske göre önceliklendirilmiş, fazlara bölünmüş, doğrudan uygulanabilir bir test yazma
planı. (Bu görevde sadece plan üretildi; kod yazılmadı.)

## Uyulacak Konvansiyonlar (mevcut testlerden türetildi)

Referans desen: `Backend/src/test/java/com/MuhasebePlus/demo/accounting/service/JournalEntryServiceTest.java`

- Test paketleri main yapısını aynalar: `invoice/service/InvoiceServiceTest.java` gibi.
- **Unit:** `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`, Spring context yok.
  Tenancy stub'ı: `when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID)`.
- **İsimlendirme:** İngilizce camelCase `methodName_whenCondition_expectedOutcome`
  (ör. `createManualEntry_whenLinesUnbalanced_throwsBusinessException`); Türkçe sadece
  assert edilen mesajlarda (`hasMessageContaining("borç")`).
- **Stil:** AssertJ (`assertThatThrownBy`), negatif yollarda `verify(repo, never()).save(any())`,
  ID atamak için `thenAnswer`, `// ── metotAdı ──` bölüm ayraçları, private DTO builder helper'ları.
- **Repository:** `@DataJpaTest` + `@Import(TestcontainersConfiguration.class)`
  (PostgreSQL 16-alpine, gerçek Flyway migration'ları çalışır).
- **Entegrasyon:** `@SpringBootTest` + Testcontainers + MockMvc (`AuthControllerIntegrationTest` deseni).
- **Controller slice:** MockMvc + spring-security-test `@WithMockUser` (`JournalEntryControllerTest` deseni).
- Backend için pom değişikliği gerekmez; JaCoCo 0.8.12 zaten bağlı.

---

## P0 — Para/Vergi Hesapları ve Güvenlik (~140 test, ilk yazılacaklar)

### P0.1 `invoice/service/InvoiceServiceTest.java` — unit, ~45-55 test
Kod tabanındaki en yüksek riskli sınıf. 13 constructor bağımlılığının tümü mock'lanır.

**Toplamlar / KDV (`applyTotals`):**
- Tek satır KDV %20, iskontosuz → subtotal/vatAmount/totalAmount birebir (HALF_UP, scale 2).
- Karışık oranlar (%20 + %10 + %1) → oran başına doğru `InvoiceVatSummary` satırları
  (`deleteByInvoiceId` sonra 3 özetle `saveAll` doğrulanır).
- Satır iskontosu %10 → KDV iskonto sonrası net üzerinden.
- Fatura geneli `PERCENTAGE` iskonto → satırlara oransal dağıtım; **kalan son satıra**
  (yuvarlama artığı bırakan senaryo: 33.33/33.33/33.34 üzerine 10 TL iskonto).
- Sabit tutarlı iskonto; iskonto > subtotal ise **subtotal'da kapanır** → final 0, KDV 0.
- Tevkifat: satır wtRate → iskonto sonrası baz üzerinden `wtAmount`,
  `totalAmount = finalSubtotal + KDV − tevkifat`. **Tevkifatın fatura-geneli iskonto
  dağıtımından ÖNCE hesaplandığını** belgeleyen test (InvoiceService.java ~684-687).
- Yuvarlama sınırı: 3+ ondalıklı miktar×birim fiyat, 0.005 sınırında HALF_UP.
- vatRate null → ZERO fallback satırı.
- `resolveUnitPrice` zinciri: açık fiyat → alış+costPrice → salePrice → costPrice → ZERO (4 test).

**Durum makinesi / yaşam döngüsü:**
- `createInvoice`: şirket içi mükerrer fatura no → hata; kapalı dönem
  (`ClosedPeriodException`) → hiçbir şey kaydedilmez; BLOKE müşteri → "bloke";
  PASİF → "pasif"; satışta `recordMovement` **negatif** miktar + `SALE`, alışta pozitif +
  `PURCHASE`; `journalEntryService.createForInvoice` bir kez çağrılır.
- Satış faturasında yeni ürün → hata; aynı istekte mükerrer barkod → hata; DB'de var olan barkod → hata.
- `confirmInvoice`: draft değilse / satırsızsa hata; başarıda stok + yevmiye + status pending.
- `updateInvoice`: paid → hata; numara çakışması → hata; başarıda
  `reverseForInvoice("Güncelleme")` sonra `createForInvoice` (`InOrder` ile sıra doğrulanır).
- `deleteInvoice`: paid → hata; partially_paid → "ödeme bulunan"; başarıda satırlar
  soft-delete, ters stok hareketi, yevmiye ters kaydı, `isDeleted` + `deletedAt`.
- `cancelInvoice` ve `updatePaymentStatus` geçiş kuralları (draft'a manuel dönüş yasak,
  paid'e `/payments` üzerinden vb.), `restoreInvoice`.

**İade faturası (`createReturnInvoice`):** iptal edilmiş orijinal → hata; satış→alış tip
çevrimi (ve tersi); satır oranlarının kopyalanması; silinmiş ürünlü satırın sessizce
atlanması (mevcut davranışı belgele); kur/`referenceInvoiceId` kopyası; stok yönleri.

**Çoklu para birimi ve tenancy:** USD, kur 30 → `totalAmountTry = total × 30`; TRY'de eşit;
currency null → TRY/1 varsayılanı; `findInvoiceById` şirketler arası erişim →
"görüntüleme yetkiniz yok".

**Seri:** `assignNextInvoiceNumber` artış + `PREFIX-000001` formatı; eksik seri → hata;
`createSeries` mükerrer kod+tip → hata.

### P0.2 `invoice/service/InvoicePaymentServiceTest.java` — unit, ~15 test
Kısmi ödeme → partially_paid, tam ödeme → paid, fazla ödeme reddi, ödeme silinince status
geri döner, iptal/silinmiş faturaya ödeme reddi, ödeme başına yevmiye kaydı.
İlgili: `invoice/service/LateFeeServiceTest.java` — gecikme faizi konfig/hesap (~6 test).

### P0.3 Beyanname servisleri (`declaration/service/`) — unit, ~40 test
- `VatDeclarationServiceTest.java` (~14): hesaplanan/indirilecek KDV dönem bazında toplama,
  devreden KDV, ödenecek/iade durumu, boş dönem, dönem sınır tarihleri.
  *Not:* `vat/service/VatDeclarationServiceTest` zaten var (farklı modül) — kapsam çakışmasın.
- `WithholdingDeclarationServiceTest.java` (~10): tevkifat kodu bazında toplama, çok kodlu fatura, dönem filtresi.
- `BaBsDeclarationServiceTest.java` (~10): 5.000 TL eşiği dahil/hariç, VKN bazında karşı
  taraf gruplama, BA/BS (alış/satış) ayrımı, iptal faturalar hariç.
- `VatDeclarationXmlBuilderTest.java` (~6): XML alanları, Türkçe karakter encoding, tutar formatı.

### P0.4 Güvenlik katmanı — ~30 test
- `config/SecurityConfigIntegrationTest.java` — `@SpringBootTest` + Testcontainers + MockMvc
  (~12): anonim → 401; USER rolü `/api/admin/**` → 403; public uçlar erişilir; CORS başlıkları.
- `common/.../RateLimitFilterTest.java` (~6): limit altı/üstü, anahtar başına izolasyon.
- `common/.../SecurityHeadersFilterTest.java` (~4): her başlık tek tek.
- `common/.../CompanyContextTest.java` (~5): multi-tenancy'nin kilit taşı — context'ten
  çıkarım, eksik context, kiracılar arası erişim reddi.
- `common/.../GlobalExceptionHandlerTest.java` (~6): BusinessException → 4xx gövde şekli,
  ClosedPeriodException eşlemesi, RuntimeException → 500 (stacktrace sızdırmadan).

---

## P1 — Çekirdek Domain Akışları (~100 test)

- `fixedasset/service/DepreciationServiceTest.java` (~14): doğrusal amortisman planı,
  ilk dönem kısmi yıl, tam amortisman durması, otomatik yevmiye (257/770 hesapları arg
  doğrulaması), kapalı dönem koruması, tekrar çalıştırma idempotency'si.
- `fixedasset/service/FixedAssetServiceTest.java` (~10): `disposeAsset` kar/zarar, çift elden çıkarma reddi, tenancy.
- `financial/service/ReconciliationServiceTest.java` (~16): tam tutar+tarih eşleşmesi,
  tarih toleransı, eşleşmeyen kova, mükerrer ekstre satırı koruması, zaten mutabık işlemin
  atlanması, çok adaylı belirsizlik, manuel eşle/çöz.
- `financial/service/TransactionServiceTest.java` (~12): işlem tipine göre bakiye etkisi,
  kapalı dönem, soft-delete geri alma, tenancy.
- `stock/service/StockServiceTest.java` (~12): satışta eksi stok engeli, min. miktar uyarısı,
  alışta maliyet güncelleme, barkod tekilliği. (StockMovementService zaten testli.)
- `accounting/service/ChartOfAccountServiceTest.java` (~12): hesap sınıfına göre borç/alacak
  netleme işareti, hiyerarşi roll-up (ebeveyn = çocukların toplamı), `isAccountingSetup`.
- **`invoice/InvoiceFlowIntegrationTest.java`** — `@SpringBootTest` + Testcontainers (~8 senaryo):
  planın en değerli entegrasyon testi. Satış faturası oluştur → `invoice_line_items`,
  `invoice_vat_summary`, `stock_movements`, `journal_entry_lines` (borç=alacak) tablolarında
  gerçek satırları doğrula; silmede ters kayıtlar; iki şirketle tenant izolasyonu; Flyway şema uyumu.
- Repository testleri `@DataJpaTest` (~18): `invoice/repository/InvoiceRepositoryTest.java`
  (Türkçe karakter/büyük-küçük arama, tarih aralığı sınırları, soft-delete hariç tutma,
  şirketler arası `existsByInvoiceNumber...`), `InvoiceVatSummaryRepositoryTest.java`.

---

## P2 — Destekleyici Modüller (~110 test, modül başına paralelleştirilebilir)

| Test sınıfı | Tip | ~Adet | Ana senaryolar |
|---|---|---|---|
| `customer/service/CustomerServiceTest` | unit | 12 | ACTIVE/PASSIVE/BLOCKED geçişleri, şirket içi VKN/TCKN tekilliği, açık faturalı müşteri silme |
| `report/service/ReportServiceTest` | unit | 12 | tarih aralığı toplama, boş veri, registry üzerinden builder seçimi |
| 3-4 Excel builder testi (KDV, gelir tablosu, cari ekstre) | unit (POI workbook assert) | 15 | başlıklar, hücre tipleri sayısal (string değil), toplam satırı = kalemler toplamı |
| `invoice/service/EInvoiceServiceTest` | unit, **GibClient tamamen mock** | 10 | UBL XML alan eşlemesi (`UblTrBuilder`), CryptoService üzerinden kimlik çözme, sent/accepted/rejected geçişleri |
| `invoice/service/PdfInvoiceServiceTest` | unit | 5 | boş olmayan PDF byte'ları — sadece smoke |
| `period/service/AccountingPeriodServiceTest` | unit | 8 | aç/kapat/yeniden aç, örtüşme reddi |
| `user/...` + `company/...` ServiceTest | unit | 14 | rol değişimi, şifre güncelleme, şirket ayarları |
| `admin/service/AdminUserServiceTest` (+AdminCompanyService) | unit | 10 | şirketler arası listeleme sadece admin'e, mutasyonda audit kaydı |
| `dashboard/service/DashboardServiceTest` | unit | 10 | KPI toplama, dönem filtreleri |
| `dashboard/.../GeminiServiceTest` + `AiWidgetGeneratorServiceTest` | unit, **AI istemcisi mock** | 10 | kota düşümü/tükenmesi, cache isabeti istemci çağrısını engeller, bozuk AI yanıtı fallback |
| Controller slice: `InvoiceControllerTest`, beyanname controller'ları | MockMvc | 16 | validasyon 400'leri, rol korumaları, Idempotency-Key başlığı |

---

## P3 — Frontend: Altyapı Kurulumu + Unit Testler (~95 test + altyapı)

### P3.1 Altyapı (tek PR, ürün koduna dokunmadan)
- `Frontend/package.json` devDependencies: `vitest`, `@vitest/coverage-v8`, `jsdom`,
  `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `msw`.
  Script'ler: `"test": "vitest run"`, `"test:watch": "vitest"`, `"test:coverage": "vitest run --coverage"`.
- `Frontend/vite.config.js` içine `test` bloğu (veya ayrı `vitest.config.js`):
  `environment: 'jsdom'`, `globals: true`, `setupFiles: './src/test/setup.js'`.
- `Frontend/src/test/setup.js`: jest-dom import + MSW server yaşam döngüsü.
- `.github/workflows/frontend-ci.yml`: lint ile build arasına `npm run test` adımı.

### P3.2 Birinci dalga — saf fonksiyonlar (~55 test, en hızlı getiri)
- `src/lib/validators.test.js` (~25): IBAN — geçerli TR IBAN, boşluk/küçük harf
  normalizasyonu, 25/23 hane reddi, TR olmayan, mod-97 checksum off-by-one; TCKN — geçerli
  numara, ilk hane 0, c10/c11 checksum hataları, uzunluk; VKN — 10 hane geçer, 9/11 reddi
  (**bulgu: `validateVKN` sadece uzunluk kontrolü yapıyor, checksum algoritması yok —
  mevcut davranışı test et, iyileştirme notu düş**); `validateTaxNumberByType` —
  INDIVIDUAL→TCKN, CORPORATE→VKN yönlendirmesi, boş → "VKN/TCKN zorunlu"; e-posta temel kontroller.
- `src/lib/format.test.js` (~30): `TRY` — işaret yeri `-₺1.234,56`, NaN → `₺0,00`;
  `formatBytes` sınırları (0, negatif, 1023 B, KB yuvarlama); `fmtDate`/`toIsoDate`/`fmtDateRange`
  null kombinasyonları; `computeRange` sabit `ref` tarihleriyle — **Pazar günü 'Bu hafta'**
  (`getDay()===0 → 7` dalı), 31 Ocak / artık yıl Şubat'ta 'Bu ay', çeyrek sınırlarında
  'Bu çeyrek', bilinmeyen dönem varsayılanı.

### P3.3 İkinci dalga (~40 test)
- `src/services/api.test.js`: interceptor'lar — auth başlığı enjeksiyonu, mutasyon
  isteklerinde idempotency key, 401 → token temizleme/yönlendirme (`window.location` mock).
- `src/context/AuthContext.test.jsx` + `ProtectedRoute`/`AdminRoute` testleri:
  MemoryRouter ile render, kimliksiz yönlendirme, rol kapısı.
- `src/hooks/useDeclarations.test.js`: QueryClient wrapper + MSW.
- `Pagination.test.jsx`: sayfa hesabı uç durumları.

**Ertelenecek:** `FaturaPage.jsx` (817 satır) ve `CariPage` gibi dev sayfa testleri —
bileşenlere ayrıştırılmadan yazılırsa kırılgan olur.

---

## P4 — Opsiyonel / Sonrası

- **Electron:** `main.js` içindeki saf yardımcıları (JWT secret üretimi, health-poll URL)
  ayıklayıp sadece onları vitest ile test et; `spawn`/auto-updater'ı otomatik test etme —
  manuel smoke kontrol listesi yeterli.
- **E2E (Playwright):** docker-compose yığınına karşı sadece 3 altın yol: login → fatura
  oluştur → listede gör; beyanname üretimi; admin kullanıcı yönetimi. P0–P2 sonrası.

## Test EDİLMEYECEKLER (bilinçli kapsam dışı)

1. GIB e-fatura canlı entegrasyonu — sadece mock/kontrat testi; CI'da asla GIB çağrılmaz.
2. Gemini AI dış çağrıları — istemci mock; sadece kota/cache/fallback mantığı.
3. Düz CRUD geçiş servisleri/controller'ları (saf `findAll` delegasyonları).
4. Lombok üretimi kod, DTO record'ları, entity getter'ları (coverage beklentisinden hariç).
5. PDF görsel çıktısı — sadece "byte'lar boş değil" smoke.
6. 14 Excel builder'ın tamamı — sadece para taşıyan 3-4 tanesi.

## Sıralama ve Efor Özeti

| Faz | ~Test | Efor | Kapı |
|---|---|---|---|
| P0 | ~140 | 5-7 gün | Fatura/beyanname feature işlerinden önce merge edilmeli |
| P1 | ~100 | 5-6 gün | Büyük Testcontainers akış testi dahil |
| P2 | ~110 | 5-6 gün | Modül başına paralel yürütülebilir |
| P3 | ~95 + altyapı | 3-4 gün | Önce altyapı PR'ı, sonra dalgalar |
| P4 | ~15 + E2E | opsiyonel | P2 sonrası |

JaCoCo önerisi: P0 sonrası `invoice`, `declaration`, `security` paketlerine ~%35 satır
kapsama `check` kuralı ekle; global eşik dayatmak yerine faz başına kademeli yükselt.

## Doğrulama

- Backend: `cd Backend && ./mvnw test` (Testcontainers için Docker gerekir; CI'da
  `backend-ci.yml` zaten Surefire + JaCoCo raporu üretiyor).
- Frontend (P3 sonrası): `cd Frontend && npm run test` ve `npm run test:coverage`;
  CI'da yeni test adımının yeşil olduğu doğrulanır.
- Her fazın sonunda JaCoCo HTML raporundan hedef paketlerin kapsama artışı kontrol edilir.

## Kritik Dosyalar

- `Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java` (en riskli sınıf)
- `Backend/src/test/java/com/MuhasebePlus/demo/accounting/service/JournalEntryServiceTest.java` (desen referansı)
- `Backend/src/test/java/com/MuhasebePlus/demo/TestcontainersConfiguration.java` (entegrasyon altyapısı)
- `Frontend/src/lib/validators.js`, `Frontend/src/lib/format.js` (ilk frontend testleri)
- `Frontend/package.json`, `.github/workflows/frontend-ci.yml` (P3 altyapı değişiklikleri)
