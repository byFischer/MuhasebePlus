# Fatura Paylaşım Linki — Planlama Dokümanı

> **Özellik:** Mali müşavir için kayıt gerektirmeyen, canlı güncellenen fatura paylaşım linki
> **Durum:** Planlama (henüz uygulanmadı)
> **Tarih:** 2026-06-11

---

## 1. Amaç ve İş Mantığı

Ön muhasebe kullanan iş sahipleri, dönem içinde kestikleri/aldıkları faturaları mali
müşavirlerine WhatsApp veya benzeri mesajlaşma uygulamaları üzerinden tek tek manuel
gönderiyor. Bu süreç zahmetli, zaman alıcı ve hataya açık (eksik/mükerrer fatura gönderimi).

**Hedef:** İş sahibi, şirketine özel **tek bir kalıcı link** üretir ve müşaviriyle bir kez
paylaşır (örn. WhatsApp ile). Müşavir bu linki açtığında — **hiçbir kayıt/giriş yapmadan** —
şirketin faturalarını canlı güncellenen bir liste halinde görür, filtreler ve fatura
PDF'lerini indirir. İş sahibi yeni fatura kestikçe link içeriği otomatik güncel kalır.

### Netleşen ürün kararları

| Karar | Seçim |
|---|---|
| Kapsam | **Satış + Alış** faturaları (sayfada tip filtresi ile) |
| PDF erişimi | Müşavir linkten **tekil fatura PDF'i indirebilir** |
| Link modeli | **Şirket başına tek kalıcı link**; iptal (revoke) ve yeniden oluşturma (regenerate) destekli |

---

## 2. Mevcut Durum — Doğrulanmış Repo Gerçekleri

Tasarım, kod tabanında doğrulanan şu gerçekler üzerine kuruludur:

- **Stack:** Spring Boot 4 (Java 21) + PostgreSQL/Flyway + Spring Data JPA + JWT (jjwt);
  React 18 + Vite + React Router v7 + TanStack Query.
- **Migration:** Son migration `V63__financial_transaction_categories.sql` → yeni migration **V64** olacak.
- **Enum'lar küçük harf:** `InvoiceType { sale, purchase }`,
  `PaymentStatus { draft, pending, partially_paid, paid, overdue }`
  (`Backend/.../invoice/entity/InvoiceType.java`).
- **SecurityConfig** (`Backend/.../config/SecurityConfig.java`) açık `permitAll` listesi
  kullanıyor; `JwtAuthenticationFilter` Authorization header yoksa isteğe dokunmadan geçiriyor
  → public uçlar için **sadece yeni bir `permitAll` matcher eklemek yeterli**, filtre değişikliği gerekmez.
- **`CompanyContext.getCurrentCompanyId()`** anonim istekte exception fırlatır
  → public servis şirketi güvenlik bağlamından değil, **token'dan** çözmelidir.
- **`PdfInvoiceService.generatePdf(invoiceId)`** CompanyContext'e bağımlı ve invoice'u şirket
  filtresi olmadan düz `findById` ile yüklüyor → `generatePdf(invoiceId, companyId)` overload'u
  gerekli (bu arada mevcut kapsamsız yükleme de düzeltilmiş olur).
- **`RateLimitFilter`** IP bazlı: auth uçları 10/dk, diğer tüm `/api/**` 60/dk
  → public uçlar için ayrı, daha sıkı bir bucket (30/dk) önerilir.
- **`GlobalExceptionHandler`'da 404 mapping yok**; `RuntimeException` → 500 dönüyor
  → özel `ShareLinkNotFoundException` + 404 handler eklenmeli.
- **Frontend `api.js`** Bearer'ı yalnızca token varsa ekliyor, ancak response interceptor'ı
  **her 401'de `/login`'e yönlendiriyor** → public sayfa için **ayrı axios instance** şart.
- **Router:** `Frontend/src/app/App.jsx` içinde `/login`, `/forgot-password` gibi public rotalar
  `ProtectedRoute`/`AppShell` dışında; catch-all `*` → `/dashboard`. Varsayılan `VITE_ROUTER=hash`
  → paylaşım URL'i `{origin}/#/paylasim/<token>` formatında olacak.
- **`InvoiceRepository`'de yeniden kullanılabilir şirket-kapsamlı sorgular hazır:**
  `findByFiltersWithCustomerPage(companyId, status, type, startDate, endDate, pageable)`,
  `searchInvoices(companyId, q, pageable)`, `findByInvoiceIdAndCompanyCompanyId(...)`.
- **CORS** yalnızca frontend origin'ine izinli; public sayfa aynı frontend'den sunulduğu için
  CORS değişikliği gerekmez.
- Tablo adlandırma: `company(company_id)`, kullanıcı tablosu `"user"` (tırnaklı, rezerve kelime);
  `BaseEntity` `created_at`/`updated_at` sağlıyor. Repo'da mevcut bir public/paylaşım altyapısı **yok**.

---

## 3. Veri Modeli

### Migration: `Backend/src/main/resources/db/migration/V64__invoice_share_link.sql`

```sql
CREATE TABLE invoice_share_link (
  id               BIGSERIAL PRIMARY KEY,
  company_id       BIGINT      NOT NULL REFERENCES company(company_id),
  token            VARCHAR(64) NOT NULL,
  is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
  created_by       BIGINT      REFERENCES "user"(user_id),
  revoked_at       TIMESTAMP,
  last_accessed_at TIMESTAMP,
  access_count     BIGINT      NOT NULL DEFAULT 0,
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP
);

CREATE UNIQUE INDEX uq_invoice_share_link_token ON invoice_share_link(token);
-- Şirket başına yalnızca BİR aktif link; iptal edilen satırlar audit geçmişi olarak kalır
CREATE UNIQUE INDEX uq_invoice_share_link_active_company
  ON invoice_share_link(company_id) WHERE is_active;
```

**Regenerate davranışı:** Eski satırda `is_active = false, revoked_at = now()` yapılır,
yeni satır eklenir. Geçmiş audit amaçlı korunur.

### Token kararı: düz metin saklanır (hash'lenmez)

- Token **salt-okunur** erişim verir ve koruduğu veri **aynı veritabanında** —
  `invoice_share_link.token`'ı okuyabilen bir saldırgan zaten `invoice` tablosunu da okur;
  bu tehdit modelinde hash anlamlı bir koruma sağlamaz.
- Ürün gereksinimi **kalıcı, tekrar kopyalanabilir link** (örn. yeni telefondan WhatsApp ile
  yeniden gönderme). Hash'li saklama "görüntülemek için yeniden oluştur" zorunluluğu getirir
  ve müşavirin yer imine aldığı linki sessizce kırar.
- Token üretimi: 256-bit `SecureRandom` → Base64URL, padding'siz (32 bayt → 43 karakter):
  `Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)`. Rate limit arkasında
  çevrimiçi tahmin pratikte imkânsız. İptal `is_active` ile anlıktır.
- **Alternatif (ileride istenirse):** SHA-256-at-rest'e geçiş yalnızca
  `findByTokenAndIsActiveTrue` çağrısından önce hash'leme eklemeyi ve
  "yalnızca regenerate ile görüntüleme" UX'ini kabul etmeyi gerektirir.

---

## 4. Backend Tasarımı

Yeni domain paketi: `com.MuhasebePlus.demo.sharelink` — mevcut
`{entity, dto/response, repository, service, controller}` konvansiyonuna uygun.

### 4.1 Yeni dosyalar

| Dosya | İçerik |
|---|---|
| `sharelink/entity/InvoiceShareLink.java` | `BaseEntity`'den türer; alanlar: `id`, `@ManyToOne(LAZY) company`, `token`, `isActive`, `createdBy` (Long user id — entity join'e gerek yok), `revokedAt`, `lastAccessedAt`, `accessCount` |
| `sharelink/repository/InvoiceShareLinkRepository.java` | `Optional<InvoiceShareLink> findByTokenAndIsActiveTrue(String token)`; `Optional<InvoiceShareLink> findByCompanyCompanyIdAndIsActiveTrue(Long companyId)`; erişim istatistiği için tek `@Modifying @Query("UPDATE ... SET lastAccessedAt = :now, accessCount = accessCount + 1 WHERE id = :id")` |
| `sharelink/service/ShareLinkService.java` | Sahip tarafı, `CompanyContext` kullanır: `getCurrent()` (aktif link veya boş durum), `regenerate()` (varsa eskiyi revoke + yenisini oluştur, tek `@Transactional`), `revoke()` |
| `sharelink/service/PublicShareService.java` | Anonim taraf. **`CompanyContext`/SecurityContext'e ASLA dokunmaz.** `resolveActiveLink(token)` → yoksa/iptal edilmişse `ShareLinkNotFoundException`; `getInfo(token)` → şirket adı; `getInvoices(token, filtreler, pageable)` → şirketi link'ten çözer, mevcut `InvoiceRepository.findByFiltersWithCustomerPage(...)` / `searchInvoices(...)` sorgularını `InvoiceService.getInvoicesPaged` ile aynı arama-öncelik mantığıyla yeniden kullanır, `PublicInvoiceDto`'ya map'ler, erişim istatistiğini artırır; `getSummary(token, startDate, endDate)`; `getInvoicePdf(token, invoiceId)` → PDF üretmeden **önce** `findByInvoiceIdAndCompanyCompanyId` + `!isDeleted()` ile sahiplik doğrulaması |
| `sharelink/controller/ShareLinkController.java` | `@RequestMapping("/api/share-links")`, tümü `@PreAuthorize("hasAnyRole('ADMIN','USER')")`: `GET /current`, `POST /regenerate`, `DELETE /current` |
| `sharelink/controller/PublicShareController.java` | `@RequestMapping("/api/public/share")`: `GET /{token}`, `GET /{token}/invoices` (param: `invoiceType`, `paymentStatus`, `startDate`, `endDate`, `search`, `@PageableDefault(size=20, sort="invoiceDate", DESC)`), `GET /{token}/summary`, `GET /{token}/invoices/{invoiceId}/pdf` |
| `sharelink/dto/response/ShareLinkResponseDto.java` | `record(boolean exists, String token, boolean active, LocalDateTime createdAt, LocalDateTime lastAccessedAt, long accessCount)` — tam URL'i frontend kurar (hash/browser router farkını frontend bilir) |
| `sharelink/dto/response/PublicShareInfoDto.java` | `record(String companyName)` — public sayfa başlığı için |
| `sharelink/dto/response/PublicInvoiceDto.java` | **Minimal alan seti** (aşağıya bakınız) |
| `sharelink/exception/ShareLinkNotFoundException.java` | `extends RuntimeException` |

### 4.2 Public DTO — veri minimizasyonu

`PublicInvoiceDto` yalnızca müşavirin ihtiyaç duyduğu alanları içerir:

- **Dahil:** `invoiceId` (PDF URL'i için gerekli), `invoiceNumber`, `customerName`,
  `invoiceType`, `invoiceDate`, `dueDate`, `paymentStatus`, `subtotal`, `vatAmount`,
  `totalAmount`, `currency`, `cancelled`
- **Hariç** (`InvoiceResponseDto`'ya kıyasla): `customerId`, satır kalemleri, `description`,
  `deliveryAddress`, `exchangeRate`, iskonto/tevkifat iç detayları, seri bilgileri,
  soft-delete/iptal-gerekçesi meta verileri

### 4.3 Değiştirilecek mevcut dosyalar

- **`config/SecurityConfig.java`** — `.anyRequest().authenticated()` öncesine
  `.requestMatchers("/api/public/**").permitAll()` eklenir.
- **`common/exception/GlobalExceptionHandler.java`** —
  `@ExceptionHandler(ShareLinkNotFoundException.class)` → **404**, jenerik mesaj
  ("Paylaşım bağlantısı bulunamadı veya iptal edilmiş"). 404 olmalı: 401 anonim kullanıcı için
  anlamsız, 500 ise mevcut `RuntimeException` handler'ının üreteceği yanlış sonuç. "Hiç var
  olmadı" ile "iptal edildi" durumları **ayırt edilemez** olmalı (token geçerliliği sızdırılmaz).
- **`invoice/service/PdfInvoiceService.java`** — `generatePdf(Long invoiceId)` yeni
  `generatePdf(Long invoiceId, Long companyId)` overload'una delege eder; eski imza
  `companyContext.getCurrentCompanyId()` çağırmaya devam eder, `InvoiceController` değişmez.
  Bu sırada içerideki invoice yüklemesi `findByInvoiceIdAndCompanyCompanyId`'ye çevrilir
  (mevcut kapsamsız `findById` hatası da düzelir).
- **`common/ratelimit/RateLimitFilter.java`** — genel kontrolden önce üçüncü kategori:
  `path.startsWith("/api/public/")` → ayrı bucket map'i, anahtar `"public:" + clientIp`,
  limit **30/dk** (PDF üretimi CPU-yoğun PDFBox işi; kimliksiz uç, kimlikli 60/dk'dan
  daha sıkı olmalı). Mevcut bucket/temizlik mekaniği aynen kullanılır.

### 4.4 İş kuralları (`PublicShareService` içinde zorlanır)

- `paymentStatus = draft` faturalar public listede ve özette **asla görünmez**
  (taslaklar kesinleşmiş belge değildir); query parametrelerinden bağımsız sunucu tarafında zorlanır.
- **İptal edilmiş faturalar görünür**, `cancelled` bayrağıyla işaretlenir
  (müşavirin iptal faturaları görmesi gerekir); UI rozet gösterir.
- **Özet ucu:** verilen tarih aralığı için tip bazında (`GROUP BY invoiceType` tek JPQL veya
  tip başına bir sorgu): `count`, `sum(subtotal)`, `sum(vatAmount)`, `sum(totalAmount)` —
  silinmiş, taslak ve iptal satırlar hariç. `InvoiceRepository`'ye yeni sorgu metodu eklenir.
- **Erişim istatistiği** yalnızca liste ucunda tek `@Modifying` UPDATE ile artırılır
  (PDF başına değil — yazma yükü minimal kalır).

---

## 5. Frontend Tasarımı

### 5.1 Yeni dosyalar

| Dosya | İçerik |
|---|---|
| `Frontend/src/services/publicApi.js` | `axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL })` — **Bearer interceptor'sız**, **401→/login yönlendirmesiz** (iptal edilmiş link login'e değil, dostane "link iptal edilmiş" ekranına düşmeli) |
| `Frontend/src/services/publicShareService.js` | `getInfo(token)`, `listInvoices(token, params)`, `getSummary(token, params)`, `downloadPdf(token, invoiceId)` (`responseType: 'blob'` → `URL.createObjectURL` + anchor click, mevcut PDF indirme deseniyle aynı) |
| `Frontend/src/services/shareLinkService.js` | Sahip tarafı, mevcut `api` instance'ı ile `/api/share-links`: `getCurrent()`, `regenerate()`, `revoke()` |
| `Frontend/src/hooks/usePublicShare.js` | `useQuery(['public-share', token])` (bilgi); `useQuery(['public-share', token, 'invoices', params], { refetchInterval: 30_000, refetchOnWindowFocus: true })` — uygulama varsayılanı `refetchOnWindowFocus:false`'u override ederek "canlı" davranışı sağlar; manuel "Yenile" için `refetch` expose edilir |
| `Frontend/src/hooks/useShareLink.js` | `useShareLink()` query + `useRegenerateShareLink()` / `useRevokeShareLink()` mutation'ları, `['share-link']` invalidation + `useInvoices.js`'teki gibi `toast.ok/err` |
| `Frontend/src/pages/PublicSharePage.jsx` | Bağımsız sayfa, `AppShell`/auth yok. `useParams().token` okur. Başlık: şirket adı + "Fatura Listesi" + manuel yenile + "son güncelleme" zamanı. Filtreler: tip segmenti (Tümü / Satış / Alış), tarih aralığı, ödeme durumu, arama — tümü query param olarak **sunucu taraflı** (müşavirin yüzlerce faturası olabilir; FaturaPage'in istemci taraflı filtrelemesinden farklı). Özet kartları (satış/alış toplamları + KDV). Satır başına PDF indirme butonlu tablo. 404 için hata ekranı. `page`/`size` ile sunucu taraflı sayfalama |
| `Frontend/src/components/invoice/ShareLinkModal.jsx` | Sahip modalı: mevcut link durumu; butonlar: **Linki Kopyala** (`navigator.clipboard.writeText`), **WhatsApp ile Gönder** (`https://wa.me/?text=${encodeURIComponent(mesaj + url)}`), **Yeniden Oluştur** (onaylı — eski linkin çalışmayacağı uyarısıyla), **İptal Et** (onaylı); `lastAccessedAt`/`accessCount` bilgi satırı |

**Paylaşım URL'i istemcide kurulur** (hash-router uyumu — varsayılan `VITE_ROUTER=hash`):

```js
const base = import.meta.env.VITE_ROUTER === 'hash'
  ? `${window.location.origin}${window.location.pathname}#`
  : window.location.origin;
const url = `${base}/paylasim/${token}`;
```

### 5.2 Değiştirilecek mevcut dosyalar

- **`Frontend/src/app/App.jsx`** — `PublicSharePage` import'u +
  `<Route path="/paylasim/:token" element={<PublicSharePage />} />` rotası, `/login` yanına
  (`ProtectedRoute` dışına, `*` catch-all'dan önce). `AuthProvider` sarması zararsızdır —
  auth'u yalnızca `ProtectedRoute` zorlar.
- **`Frontend/src/pages/FaturaPage.jsx`** — `page-actions` div'ine (≈129. satır) mevcut
  "Seriler"/"GİB" butonlarının yanına "Paylaş" butonu + `shareOpen` state'i +
  `<ShareLinkModal open={shareOpen} ... />`. En basit ve en keşfedilebilir yerleşim;
  ayar sayfası gerektirmez.
- **`Frontend/src/lib/routes.js`** — değişiklik gerekmez (public sayfanın shell/nav girdisi yok).

---

## 6. Uygulama Sırası

1. **Migration** — `V64__invoice_share_link.sql` (tablo + iki unique index)
2. **Entity + repository** — `InvoiceShareLink`, `InvoiceShareLinkRepository`
3. **Sahip tarafı backend** — `ShareLinkService`, `ShareLinkController`,
   `ShareLinkResponseDto`; public tarafa geçmeden curl + JWT ile test edilir
4. **PDF refactor** — `PdfInvoiceService.generatePdf(invoiceId, companyId)` overload'u;
   mevcut kimlikli PDF ucunun hâlâ çalıştığı doğrulanır
5. **Public backend** — `ShareLinkNotFoundException` + 404 handler; `PublicShareService`;
   `PublicInvoiceDto`/`PublicShareInfoDto`/özet DTO; `PublicShareController`;
   `SecurityConfig` permitAll; `RateLimitFilter` public bucket
6. **Frontend sahip tarafı** — `shareLinkService.js`, `useShareLink.js`,
   `ShareLinkModal.jsx`, FaturaPage butonu
7. **Frontend public tarafı** — `publicApi.js`, `publicShareService.js`,
   `usePublicShare.js`, `PublicSharePage.jsx`, `App.jsx` rotası

---

## 7. Doğrulama Planı

### Build / birim testleri
- `cd Backend && ./mvnw clean verify` (test başlangıcında Flyway V64'ü doğrular)
- Birim testleri: regenerate eski linki revoke eder ve tek aktif link kalır;
  revoke edilmiş token → `ShareLinkNotFoundException`; başka şirketin `invoiceId`'si için
  PDF isteği → 404 (PDF değil)

### Manuel akış
1. Şirket A olarak giriş yap → faturalar oluştur (satış + alış, bir taslak, bir iptal)
2. FaturaPage → **Paylaş** → linki kopyala
3. Linki **gizli pencerede** aç (sessionStorage'da token yok): liste yüklenir,
   taslak gizli, iptal rozetli, filtreler çalışır, PDF iner
4. **İptal Et** → gizli pencerede yenile → dostane 404 ekranı
5. **Yeniden Oluştur** → eski URL ölü, yeni URL canlı

### Güvenlik kontrolleri
- `curl /api/public/share/YANLIS_TOKEN/invoices` → **404** (500/401 değil)
- `curl /api/public/share/<şirketA-token>/invoices/<şirketB-fatura-id>/pdf` → **404**
- Public uca >30 istek/dk → **429**
- `/api/share-links` JWT'siz → **401**
- `VITE_ROUTER=hash` ile `/#/paylasim/<token>` çözümleniyor (Electron/desktop paritesi)
