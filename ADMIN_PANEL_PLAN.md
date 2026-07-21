# MuhasebePlus — Admin Paneli Planı

> Bu doküman bir **plan**dır; henüz kod yazılmamıştır. Amaç, projeye en uygun admin paneli
> mimarisini belirlemek, modülleri tanımlamak ve uygulamayı fazlara bölmek.

---

## 1. Amaç

MuhasebePlus'ı kullanan son kullanıcılar (USER rolü) kendi şirketlerinin muhasebesini yönetiyor.
Admin paneli ise **platformun sahibinin / işletmecisinin** (ADMIN rolü) şunları yapabilmesi için:

- Tüm kullanıcıları ve şirketleri (tenant'ları) görmek ve yönetmek
- Hesapları kilitlemek / açmak / pasifleştirmek, rol atamak
- Sistem genelindeki ayarları yönetmek (örn. Gemini AI anahtarı, kota limitleri)
- Sistem loglarını ve denetim kayıtlarını incelemek
- Platformun genel sağlığını ve kullanım istatistiklerini izlemek

## 2. Mevcut Durum Analizi

İyi haber: **Backend tarafında admin altyapısının önemli bir kısmı zaten var.** Eksik olan
ağırlıklı olarak frontend ve birkaç tamamlayıcı backend parçası.

### Zaten var olanlar ✅

| Bileşen | Durum |
|---|---|
| `UserRole` enum'ı | `USER` ve `ADMIN` rolleri tanımlı |
| JWT | Token payload'ında `role` ve `companyId` taşınıyor |
| Admin endpoint'leri | `UserController`'da 7 adet `@PreAuthorize("hasRole('ADMIN')")` korumalı endpoint (listele, sil, rol değiştir, kilitle/aç, pasifleştir) |
| Şirket yönetimi | `CompanyController`'da tüm şirketleri listeleme/silme (ADMIN korumalı) |
| AI ayarları | `PUT /api/system/ai-settings` (ADMIN korumalı) |
| Loglama | `SystemLog` entity'si + `/api/logs` endpoint'i |
| Multi-tenancy | Tüm tablolarda `company_id` izolasyonu |
| Güvenlik filtreleri | Rate limit, XSS, idempotency, security headers |

### Eksik olanlar ❌

| Eksik | Açıklama |
|---|---|
| **Admin UI** | Frontend'de admin'e özel hiçbir sayfa yok; mevcut endpoint'ler hiçbir ekrandan çağrılmıyor |
| **İlk admin oluşturma** | `registerUser` herkese `USER` rolü veriyor; ilk ADMIN'i oluşturmanın tanımlı bir yolu yok |
| **URL seviyesinde koruma** | `SecurityConfig`'de `/api/admin/**` gibi merkezi bir kural yok; koruma sadece metod bazlı `@PreAuthorize` ile |
| **Sayfalama** | `GET /api/users` ve `GET /api/companies` sayfalama/arama desteklemiyor (kullanıcı sayısı artınca sorun olur) |
| **Admin istatistikleri** | Toplam kullanıcı/şirket/fatura sayısı, AI kota kullanımı gibi özet veriler için endpoint yok |
| **Admin işlem denetimi** | Admin'in yaptığı kritik işlemlerin (rol değiştirme, silme vb.) ayrıca denetim kaydına yazılması garanti değil |

---

## 3. Mimari Karar: Nasıl Bir Admin Paneli?

Üç seçenek değerlendirildi:

### Seçenek A — Mevcut React uygulaması içinde `/admin` bölümü ⭐ (ÖNERİLEN)

Mevcut frontend'in içine, sadece `role === 'ADMIN'` olan kullanıcıların görebildiği ayrı bir
rota grubu ve ayrı bir layout eklenir.

- ✅ Mevcut altyapının tamamı yeniden kullanılır: auth (JWT + AuthContext), axios interceptor,
  React Query, Chakra/Tailwind bileşenleri, tema sistemi
- ✅ Electron (masaüstü) dağıtımıyla otomatik uyumlu — ayrı build, ayrı port, ayrı deploy yok
- ✅ Tek CI/CD hattı, tek bakım yükü
- ✅ Backend'deki mevcut `@PreAuthorize` korumaları olduğu gibi kullanılır
- ⚠️ Admin kodu son kullanıcının indirdiği bundle'a dahil olur — bu bir veri sızıntısı riski
  DEĞİLDİR (asıl koruma her zaman backend'dedir), sadece bundle boyutuna birkaç KB ekler.
  Lazy-loading (React.lazy) ile admin sayfaları ayrı chunk'a bölünerek bu da minimize edilir.

### Seçenek B — Ayrı bir admin frontend uygulaması

`AdminFrontend/` adıyla ikinci bir Vite + React projesi.

- ✅ Tam izolasyon, admin kodu kullanıcı bundle'ında hiç yer almaz
- ❌ Auth, servis katmanı, UI bileşenleri, tema → hepsi ya kopyalanır ya da monorepo paket
  yapısına geçilir (büyük refactor)
- ❌ Electron paketlemesi karmaşıklaşır, ikinci CI hattı gerekir
- ❌ Projenin mevcut ölçeği için aşırı mühendislik

### Seçenek C — Hazır araç (Spring Boot Admin, Retool, Metabase vb.)

- ❌ Spring Boot Admin sadece teknik metrik izler, iş verisi (kullanıcı/şirket) yönetemez
- ❌ Retool/Metabase gibi araçlar DB'ye doğrudan bağlanır → servis katmanındaki iş kuralları
  ve denetim atlanır; masaüstü dağıtımda çalışmaz

**Karar: Seçenek A.** Proje tek kişilik/küçük ekip tarafından geliştiriliyor, masaüstü + web
ikili dağıtımı var ve backend zaten role-based korumaya sahip. İleride gerçekten ayrı bir
panele ihtiyaç doğarsa, A seçeneğindeki sayfalar B'ye taşınabilir (kayıp iş olmaz).

### Dağıtım modeline göre anlamı

Bu mimari her iki dağıtım modelinde de doğru çalışır:

- **Sunucu (SaaS) modunda:** Admin paneli, tüm müşterilerin (şirketlerin) yönetildiği gerçek
  bir operatör panelidir. Asıl hedef senaryo budur.
- **Masaüstü (Electron) modunda:** Her kurulumun kendi yerel veritabanı olduğu için admin
  paneli o kurulumdaki yerel kullanıcıları yönetir. Panel görünür olur ama pratikte daha az
  anlam taşır; istenirse desktop profilinde menüden gizlenebilir (karar — bkz. §10).

---

## 4. Rol Modeli

Mevcut iki rol şimdilik **yeterli**, anlamları netleştirilerek korunmalı:

| Rol | Anlamı | Kapsam |
|---|---|---|
| `ADMIN` | Platform yöneticisi (sen / işletmeci) | Tüm şirketler, tüm kullanıcılar, sistem ayarları |
| `USER` | Müşteri (şirket sahibi/çalışanı) | Yalnızca kendi `company_id`'sine ait veriler |

**Şimdilik yapılmaması önerilen:** `COMPANY_ADMIN` (şirket içi yönetici) gibi üçüncü bir rol.
Şu an bir şirkete birden fazla kullanıcı bağlanabilse de uygulama pratikte 1 kullanıcı = 1
şirket akışıyla çalışıyor. Şirket içi ekip yönetimi ayrı bir özellik olarak ileride ele alınmalı.

**Çözülmesi gereken: İlk admin nasıl oluşacak?** Önerilen yöntem (güvenli ve otomatik):

- Uygulama açılışında çalışan bir `AdminBootstrap` bileşeni: `ADMIN_EMAIL` + `ADMIN_PASSWORD`
  ortam değişkenleri tanımlıysa ve sistemde hiç ADMIN yoksa, bu bilgilerle bir admin kullanıcı
  oluşturur (kendine ait bir "sistem" şirketi ile). Değişkenler tanımlı değilse hiçbir şey yapmaz.
- Alternatif: Flyway migration ile seed (sabit şifre riski taşıdığı için önerilmez) veya
  manuel SQL (her ortamda elle işlem gerektirir).

---

## 5. Admin Paneli Modülleri

Panel 5 ana modülden oluşur. Her modül için: amaç → ekran içeriği → API durumu.

### 5.1 Genel Bakış (Admin Dashboard) — `/admin`

**Amaç:** Tek bakışta platformun sağlığı ve büyüklüğü.

**Ekran içeriği:**
- Özet kartlar: toplam kullanıcı, toplam şirket, aktif/kilitli kullanıcı sayısı,
  son 30 günde kayıt olan kullanıcı sayısı
- AI kota kullanımı: şirket bazında aylık token tüketimi (en çok tüketen ilk 10)
- Son sistem logları (son 20 kayıt, hata seviyesindekiler vurgulu)
- Sistem bilgisi: uygulama versiyonu, DB durumu (actuator/health)

**API:** ❌ Yeni endpoint gerekli → `GET /api/admin/stats` (özet sayılar tek istekte)

### 5.2 Kullanıcı Yönetimi — `/admin/kullanicilar`

**Amaç:** Tüm kullanıcıları listele, ara, yönet.

**Ekran içeriği:**
- Tablo: ad-soyad, e-posta, şirket, rol, durum (aktif/pasif/kilitli), başarısız giriş sayısı, kayıt tarihi
- Filtre/arama: e-posta, şirket adı, rol, durum
- Satır işlemleri: detay görüntüle, rolünü değiştir, kilitle/kilidini aç, pasifleştir, sil
- Tehlikeli işlemler (sil, rol değiştir) onay modalı ile
- Kullanıcı detay çekmecesi (drawer): profil bilgileri + bağlı şirket + son oturumları

**API:** ✅ Büyük ölçüde hazır (`/api/users` altındaki ADMIN endpoint'leri).
Eklenecekler: sayfalama + arama parametreleri, admin'in şifre sıfırlama tetikleyebilmesi.

### 5.3 Şirket (Tenant) Yönetimi — `/admin/sirketler`

**Amaç:** Tüm tenant'ları görmek ve yönetmek.

**Ekran içeriği:**
- Tablo: şirket adı, vergi no, şehir, kullanıcı sayısı, aktif/pasif, kayıt tarihi
- Şirket detayı: bağlı kullanıcılar, AI kota durumu, temel kullanım sayıları
  (fatura adedi, cari adedi — tenant'ın "ne kadar aktif" olduğunu gösterir)
- İşlemler: pasifleştir/aktifleştir, sil (onaylı; bağlı verisi olan şirkette engellenmeli
  veya soft-delete olmalı)

**API:** ✅ Kısmen hazır (`/api/companies`). Eklenecekler: sayfalama, şirket-detay
endpoint'i (kullanıcıları + kullanım sayıları ile birlikte), güvenli silme kuralı.

### 5.4 Sistem Ayarları — `/admin/ayarlar`

**Amaç:** Platform genelindeki yapılandırma.

**Ekran içeriği:**
- AI ayarları: Gemini API anahtarı (maskeli göster), model seçimi, aylık token bütçesi,
  sıcaklık/çıktı limitleri — mevcut `PUT /api/system/ai-settings` kullanılır
- Güvenlik parametreleri (ileride): max giriş denemesi, JWT süresi
- Kayıt kontrolü (ileride): yeni kullanıcı kaydını aç/kapat

**API:** ✅ AI ayarları hazır. Diğerleri ileriki fazlarda `AppSettings` tablosu üzerinden.

### 5.5 Loglar ve Denetim — `/admin/loglar`

**Amaç:** Tüm sistemin (tüm şirketlerin) loglarını tek yerden incelemek.

**Ekran içeriği:**
- Mevcut `LogPage`'in admin versiyonu: şirket filtresi eklenmiş hali
  (normal kullanıcı sadece kendi şirketinin loglarını görür, admin hepsini)
- Filtreler: seviye (INFO/WARN/ERROR), tarih aralığı, şirket, kullanıcı, aksiyon
- Admin işlemleri ayrı bir sekmede: "Denetim Kaydı" (kim, ne zaman, hangi kullanıcıya/şirkete
  ne yaptı)

**API:** ✅ `/api/logs` mevcut; admin için "tüm şirketler" kapsamı ve şirket filtresi eklenecek.
Admin denetim kaydı için `SystemLog`'a yazan bir `AdminAuditService` eklenecek.

### 5.6 Gelecek modüller (bu planın kapsamı DIŞINDA, yer ayrılıyor)

- **Abonelik / Lisans yönetimi:** Şu an `Subscription` benzeri bir entity yok. Ücretli plana
  geçilecekse `/admin/abonelikler` modülü ve ilgili entity'ler ayrı bir planla eklenmeli.
- **Duyuru sistemi:** Admin'in tüm kullanıcılara bildirim göndermesi (mevcut `Notification`
  altyapısı buna uygun).
- **Feature flag yönetimi:** `/api/system/features` endpoint'i mevcut; UI'dan yönetimi.

---

## 6. Backend Yapılacaklar

Yeni bir `admin` paketi açmak yerine **mevcut modüllerin içine admin uçları eklemek** yerine,
önerilen: `com.MuhasebePlus.demo.admin` paketi altında toplamak. Gerekçe: admin'e özel
mantık (cross-tenant sorgular) normal servislerin tenant-scoped mantığıyla karışmasın.

```
Backend/src/main/java/com/MuhasebePlus/demo/admin/
├── controller/
│   ├── AdminStatsController.java      → GET /api/admin/stats
│   ├── AdminUserController.java       → GET /api/admin/users (sayfalı+aramalı)
│   ├── AdminCompanyController.java    → GET /api/admin/companies, GET /api/admin/companies/{id}
│   └── AdminLogController.java        → GET /api/admin/logs (tüm şirketler)
├── service/
│   ├── AdminStatsService.java
│   └── AdminAuditService.java         → admin işlemlerini SystemLog'a yazar
├── dto/
│   └── (stats, sayfalı liste cevapları)
└── bootstrap/
    └── AdminBootstrap.java            → ilk admin'i env değişkenlerinden oluşturur
```

### Yapılacak işler

1. **`SecurityConfig`'e URL seviyesi kural** (savunma derinliği — defense in depth):
   ```java
   .requestMatchers("/api/admin/**").hasRole("ADMIN")
   ```
   Böylece biri `@PreAuthorize` eklemeyi unutsa bile `/api/admin/**` koruması düşmez.
2. **`GET /api/admin/stats`**: tek istekte özet sayılar (kullanıcı/şirket/aktiflik sayıları,
   AI kota özetleri). Dashboard'ın tek ihtiyacı bu endpoint.
3. **Sayfalı + aramalı listeler**: `GET /api/admin/users?page=&size=&search=&role=&status=`
   ve aynısı şirketler için (Spring `Pageable` ile).
4. **`AdminBootstrap`**: §4'teki ilk admin oluşturma mantığı.
5. **`AdminAuditService`**: rol değiştirme, kilitleme, silme gibi işlemleri kim-ne-ne zaman
   formatında `SystemLog`'a yazar. Mevcut admin endpoint'lerinin servislerine eklenir.
6. **Mevcut admin endpoint'lerinde iyileştirme**: `DELETE /api/users/{id}` admin'in kendini
   silememesi / son admin'in silinememesi kontrolü; `PUT /{id}/role` için aynı koruma.
7. **Şirket silme güvenliği**: bağlı verisi olan şirkette hard delete engellenir;
   pasifleştirme önerilir (veya `SoftDeletableEntity`'ye geçirilir).

> Not: Mevcut `UserController`'daki ADMIN endpoint'leri bozulmaz; istenirse zamanla
> `/api/admin/users/**` altına taşınır (taşıma Faz 2+ konusu, zorunlu değil).

---

## 7. Frontend Yapılacaklar

### Rota ve koruma

```
/admin                → AdminDashboardPage   (genel bakış)
/admin/kullanicilar   → AdminUsersPage
/admin/sirketler      → AdminCompaniesPage
/admin/ayarlar        → AdminSettingsPage    (AI ayarları vb.)
/admin/loglar         → AdminLogsPage
```

- **`AdminRoute` bileşeni**: `ProtectedRoute`'un rol kontrolü yapan versiyonu.
  `user.role !== 'ADMIN'` ise `/dashboard`'a yönlendirir. (Bu kontrol yalnızca UX içindir;
  gerçek güvenlik backend'dedir.)
- **Lazy loading**: Admin sayfaları `React.lazy` ile ayrı chunk'a alınır — normal
  kullanıcının bundle yükü etkilenmez.
- **Navigasyon**: Mevcut sidebar'a, sadece admin'e görünen "YÖNETİM" bölümü eklenir
  (`routes.js`'teki bölüm yapısına `adminOnly: true` bayrağıyla). Alternatif olarak tamamen
  ayrı bir admin layout'u da yapılabilir; ilk fazda mevcut `AppShell` içinde kalmak daha hızlı.

### Dosya yapısı (mevcut konvansiyonlara uygun)

```
Frontend/src/
├── pages/admin/
│   ├── AdminDashboardPage.jsx
│   ├── AdminUsersPage.jsx
│   ├── AdminCompaniesPage.jsx
│   ├── AdminSettingsPage.jsx
│   ├── AdminLogsPage.jsx
│   └── components/            (UserTable, UserDetailDrawer, CompanyDetailDrawer,
│                               StatCards, ConfirmActionModal ...)
├── services/adminService.js   (axios; /api/admin/* + mevcut admin uçları)
├── components/AdminRoute.jsx
```

- **Veri katmanı**: Mevcut desenle aynı — React Query (`useQuery`/`useMutation`),
  mutation sonrası ilgili query invalidation.
- **UI**: Mevcut Chakra/Tailwind bileşenleri ve `mp/` bileşen seti yeniden kullanılır;
  tablo + filtre + drawer deseni `CariPage`'dekiyle tutarlı olur.
- **Tehlikeli işlemler**: Silme/rol değiştirme için onay modalı; geri bildirim toaster ile.

---

## 8. Güvenlik Notları

1. **Asıl koruma backend'de**: Frontend'deki rol kontrolü sadece kullanıcı deneyimi içindir.
   JWT'deki `role` claim'i değiştirilemez (imzalı), ama her endpoint yine de sunucuda
   `hasRole('ADMIN')` ile korunmalı.
2. **Çifte katman**: Metod seviyesi `@PreAuthorize` + `SecurityConfig`'de `/api/admin/**`
   URL kuralı birlikte kullanılmalı.
3. **Tenant izolasyonunun bilinçli aşılması**: Admin servisleri `company_id` filtresi olmadan
   sorgu yapar — bu sorgular SADECE `admin` paketinde yaşamalı ki normal servislere
   cross-tenant sorgu sızmasın.
4. **Denetlenebilirlik**: Her admin işlemi (özellikle silme, rol değiştirme, kilitleme)
   `SystemLog`'a aktör bilgisiyle yazılmalı.
5. **Kendi kendini kilitleme koruması**: Admin kendini silememeli/pasifleştirememeli;
   sistemdeki son admin'in rolü düşürülememeli.
6. **Rate limit**: Mevcut `RateLimitFilter` admin uçlarını da kapsamalı (zaten global).
7. **Hassas veri maskeleme**: AI API anahtarı UI'da asla düz metin gösterilmemeli
   (son 4 karakter formatı), loglara yazılmamalı.

---

## 9. Uygulama Fazları

Her faz kendi başına teslim edilebilir ve test edilebilir durumda biter.

| Faz | İçerik | Bağımlılık |
|---|---|---|
| **Faz 1 — Backend temeli** | `admin` paketi, `SecurityConfig` URL kuralı, `AdminBootstrap` (ilk admin), `GET /api/admin/stats`, sayfalı kullanıcı/şirket listeleri, `AdminAuditService` | — |
| **Faz 2 — Frontend iskeleti** | `AdminRoute`, sidebar'a admin bölümü, lazy-loaded rotalar, `adminService.js`, boş sayfa iskeletleri + `/admin` dashboard (stat kartları) | Faz 1 |
| **Faz 3 — Kullanıcı yönetimi** | `AdminUsersPage`: tablo, filtre, detay drawer, rol/kilit/pasif/sil işlemleri + onay modalları | Faz 2 |
| **Faz 4 — Şirket yönetimi** | `AdminCompaniesPage`: tablo, şirket detayı (kullanıcılar + kullanım sayıları + AI kotası), pasifleştirme | Faz 2 |
| **Faz 5 — Ayarlar + Loglar** | `AdminSettingsPage` (AI ayarları UI), `AdminLogsPage` (tüm şirketler + denetim sekmesi) | Faz 2 |
| **Faz 6 — Cila** | Dashboard'a AI kota top-10, son loglar; uçtan uca test; desktop profilinde panel görünürlüğü kararının uygulanması | Faz 3–5 |

Tahmini büyüklük: Faz 1 ≈ 8-10 yeni backend dosyası; Faz 2-5 ≈ 10-12 yeni frontend dosyası.
Mevcut kodda değişiklik minimum düzeyde kalır (`SecurityConfig`, `routes.js`, `App.jsx`,
birkaç servis dosyası).

---

## 10. Karar Bekleyen Sorular

Uygulamaya başlamadan önce netleşmesi gerekenler:

1. **Dağıtım hedefi:** Admin panelinin asıl kullanılacağı yer sunucu (SaaS) kurulumu mu?
   Masaüstü (Electron) sürümünde panel görünsün mü, gizlensin mi?
2. **İlk admin:** §4'teki env-değişkeni yaklaşımı (`ADMIN_EMAIL`/`ADMIN_PASSWORD`) uygun mu?
3. **Kapsam onayı:** §5.6'daki gelecek modüller (abonelik/lisans, duyuru, feature flag)
   gerçekten bu planın dışında mı kalsın, yoksa abonelik şimdiden mi planlansın?
4. **Mevcut admin endpoint'lerinin taşınması:** `UserController`'daki admin uçları
   `/api/admin/users/**` altına taşınsın mı (daha temiz), yoksa olduğu yerde mi kalsın
   (daha az risk)? Öneri: ilk fazda olduğu yerde kalsın.
