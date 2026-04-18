# Invoice Modülü — Test Planı

## 1. Test Stratejisi (Piramit)

| Katman | Test Türü | Araç | Hız | Kapsam |
|---|---|---|---|---|
| **Unit** | Saf Java/Mockito | JUnit 5 + Mockito + AssertJ | ⚡ çok hızlı | Service iş mantığı, izole |
| **Slice** | `@DataJpaTest` | Spring Boot Test + Testcontainers | 🐢 orta | Repository query metodları |
| **Slice** | `@WebMvcTest` | MockMvc + `@MockitoBean` | ⚡ hızlı | Controller + validation + security |
| **Integration** | `@SpringBootTest` | Full context + Testcontainers + MockMvc | 🐌 yavaş | Uçtan uca akış (HTTP → DB) |

**Referans mevcut pattern:** `JwtUtilTest.java` saf unit test pattern'i, `MuhasebePlusApplicationTests.java` ise `@SpringBootTest + @Import(TestcontainersConfiguration.class) + @ActiveProfiles("test")` pattern'i.

---

## 2. Oluşturulacak Test Dosyaları

### A. `InvoiceServiceTest` (Unit — Mockito)

**Konum:** `Backend/src/test/java/com/MuhasebePlus/demo/invoice/service/InvoiceServiceTest.java`

**Setup:**
- `@ExtendWith(MockitoExtension.class)` — Spring context YÜKLENMEZ
- `@Mock InvoiceRepository invoiceRepository;`
- `@InjectMocks InvoiceService invoiceService;`
- `@BeforeEach` içinde örnek `Invoice` ve `InvoiceRequestDto` fixture'ları hazırla

**Test Cases:**

| # | Metot | Senaryo | Beklenti |
|---|---|---|---|
| 1 | `createInvoice` | Yeni invoice number — happy path | `save()` çağrılır, dönen DTO alanları DTO girdisine eşit |
| 2 | `createInvoice` | `existsByInvoiceNumber` true döner | `RuntimeException` fırlatılır, `save()` ÇAĞRILMAZ |
| 3 | `getAllInvoices` | Repo 3 invoice döner | List boyutu 3, map'lenmiş DTO'lar doğru |
| 4 | `getAllInvoices` | Repo boş liste döner | Empty list, exception yok |
| 5 | `getInvoiceById` | Geçerli ID | DTO döner, alanlar eşleşir |
| 6 | `getInvoiceById` | ID bulunamıyor (`Optional.empty()`) | `RuntimeException("Invoice not found with id: ...")` |
| 7 | `updateInvoice` | Geçerli ID | Alanlar güncelleniyor, `save()` çağrılıyor |
| 8 | `updateInvoice` | ID bulunamıyor | `RuntimeException` |
| 9 | `deleteInvoiceById` | `existsById` true | `deleteById()` çağrılır |
| 10 | `deleteInvoiceById` | `existsById` false | `RuntimeException`, `deleteById` ÇAĞRILMAZ |
| 11 | `getInvoiceByCustomerId` | Aynı customer'a 2 invoice | Doğru sayı ve içerik |
| 12 | `getInvoiceByFilters` | Her ikisi null | `findAll()` çağrılır |
| 13 | `getInvoiceByFilters` | Sadece paymentStatus | `findByPaymentStatus` çağrılır |
| 14 | `getInvoiceByFilters` | Sadece invoiceType | `findByInvoiceType` çağrılır |
| 15 | `getInvoiceByFilters` | İkisi de dolu | `findByPaymentStatusAndInvoiceType` çağrılır |
| 16 | `updatePaymentStatus` | Geçerli ID | `paymentStatus` güncellenir, kaydedilir |
| 17 | `updatePaymentStatus` | ID yok | `RuntimeException` |

**Mock verification örüntüleri:** `verify(invoiceRepository).save(any())`, `verify(invoiceRepository, never()).save(any())`, `ArgumentCaptor<Invoice>` ile save edilen entity'nin alanlarını doğrula (özellikle update ve status güncelleme senaryolarında).

---

### B. `InvoiceRepositoryTest` (Slice — `@DataJpaTest`)

**Konum:** `Backend/src/test/java/com/MuhasebePlus/demo/invoice/repository/InvoiceRepositoryTest.java`

**Setup:**
- `@DataJpaTest`
- `@Import(TestcontainersConfiguration.class)` — gerçek PostgreSQL container (enum tipleri H2'de çalışmaz!)
- `@ActiveProfiles("test")`
- `@AutoConfigureTestDatabase(replace = Replace.NONE)` — Testcontainers kullanmak için varsayılan in-memory DB'yi devre dışı bırak
- `@Autowired InvoiceRepository`, `@Autowired TestEntityManager`

**Neden Testcontainers şart:** V1 migration'da `invoice_type` ve `payment_status` PostgreSQL-native ENUM tipleri. H2 bunları desteklemez. Mevcut `TestcontainersConfiguration` zaten hazır, onu import et.

**Test Cases:**

| # | Metot | Senaryo |
|---|---|---|
| 1 | `findByInvoiceNumber` | Var olan number → `Optional.isPresent()` |
| 2 | `findByInvoiceNumber` | Olmayan number → `Optional.isEmpty()` |
| 3 | `existsByInvoiceNumber` | Var → `true`, yok → `false` |
| 4 | `findByCustomerId` | Aynı customer'a 2 invoice, farklı customer'a 1 → sadece 2 dönmeli |
| 5 | `findByPaymentStatus` | `pending` olanlar → doğru sayı |
| 6 | `findByInvoiceType` | `sale` olanlar → doğru sayı |
| 7 | `findByPaymentStatusAndInvoiceType` | Kombinasyon → sadece ikisi de eşleşen |
| 8 | `findByDueDateBefore` | Tarih öncesi olanlar → doğru kümeleme |
| 9 | **Enum persistence sanity** | `sale` enum'ını kaydet, raw sorgu ile DB'deki değer **küçük harf** mi kontrol et (kritik — enum case uyumunu kanıtla) |

---

### C. `InvoiceControllerTest` (Slice — `@WebMvcTest`)

**Konum:** `Backend/src/test/java/com/MuhasebePlus/demo/invoice/controller/InvoiceControllerTest.java`

**Setup:**
- `@WebMvcTest(InvoiceController.class)`
- `@MockitoBean InvoiceService invoiceService;` (Spring Boot 3.4+ / 4.x — eski `@MockBean` yerine)
- `@MockitoBean` ayrıca `JwtUtil`, `UserDetailsService` gibi Security zincirinin bean'leri için (veya `@AutoConfigureMockMvc(addFilters = false)` ile security zincirini bypass et — iki yol da geçerli; tavsiyem security'yi açık bırakıp `@WithMockUser` kullanmak)
- `@Autowired MockMvc mockMvc;`
- `@Autowired ObjectMapper objectMapper;`

**Test Cases:**

| # | Endpoint | Senaryo | Beklenti |
|---|---|---|---|
| 1 | `POST /api/invoices` | Valid body + `@WithMockUser(roles="USER")` | 201, JSON body, `service.createInvoice` çağrıldı |
| 2 | `POST /api/invoices` | `invoiceNumber` boş | 400 (validation) |
| 3 | `POST /api/invoices` | `customerId` null | 400 |
| 4 | `POST /api/invoices` | `subtotal` negatif | 400 (`@DecimalMin`) |
| 5 | `POST /api/invoices` | Geçersiz `invoiceType` ("invalid") | 400 (enum deserialization) |
| 6 | `POST /api/invoices` | Kimlik doğrulaması yok | 401 |
| 7 | `GET /api/invoices` | `@WithMockUser(roles="USER")` | 200, List döner |
| 8 | `GET /api/invoices?status=pending&type=sale` | Query param'lar service'e doğru iletildi mi | ArgumentCaptor ile doğrula |
| 9 | `GET /api/invoices/{id}` | Var | 200 |
| 10 | `GET /api/invoices/{id}` | Service exception | 500 (global handler yok — bu gerçek davranış) |
| 11 | `PUT /api/invoices/{id}` | Valid | 200 |
| 12 | `PUT /api/invoices/{id}` | Invalid body | 400 |
| 13 | `DELETE /api/invoices/{id}` | `@WithMockUser(roles="ADMIN")` | 204 |
| 14 | `DELETE /api/invoices/{id}` | `@WithMockUser(roles="USER")` | 403 (kritik — sadece ADMIN) |
| 15 | `GET /api/invoices/customer/{id}` | USER | 200 |
| 16 | `PUT /api/invoices/{id}/status?status=paid` | USER | 200, service'e enum olarak iletildi |
| 17 | `PUT /api/invoices/{id}/status?status=invalid` | Hatalı enum | 400 |

---

### D. `InvoiceIntegrationTest` (Full E2E — `@SpringBootTest`)

**Konum:** `Backend/src/test/java/com/MuhasebePlus/demo/invoice/InvoiceIntegrationTest.java`

**Setup:**
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureMockMvc`
- `@Import(TestcontainersConfiguration.class)`
- `@ActiveProfiles("test")`
- `@Transactional` (her test sonunda rollback) VEYA `@BeforeEach` içinde DB temizle
- Gerçek `JwtUtil` ile token üret, `Authorization: Bearer ...` header'ı ekle

**Akış test senaryoları (az sayıda ama uçtan uca):**

| # | Akış |
|---|---|
| 1 | **Full lifecycle:** USER olarak login → token al → invoice oluştur → ID ile getir → güncelle → status=paid yap → listele (filtre ile) → ADMIN ile sil |
| 2 | **Unique constraint:** Aynı `invoiceNumber` ile 2 kez oluştur → ikincisi 500 (RuntimeException → generic error) |
| 3 | **Role separation:** USER token ile DELETE çağır → 403; ADMIN token → 204 |
| 4 | **Unauthorized:** Token'sız POST → 401 |
| 5 | **DB enum round-trip:** Invoice oluştur, native query ile DB'yi oku → `invoice_type='sale'` (küçük harf) olduğunu kanıtla |
| 6 | **Filter combination:** 3 farklı status/type ile invoice yarat, filtre kombinasyonlarının doğru alt küme döndürdüğünü doğrula |

**Not:** Integration testlerini küçük tut — her senaryo için unit + slice test zaten kapsıyor. Burada amaç "tüm katmanlar birlikte çalışıyor mu" sorusunu cevaplamak.

---

## 3. Yardımcı / Fixture Dosyası (Opsiyonel)

**Konum:** `Backend/src/test/java/com/MuhasebePlus/demo/invoice/InvoiceTestFixtures.java`

Statik factory metodlar (test kodunu DRY tutmak için):
- `sampleInvoice()` → tipik geçerli `Invoice` entity
- `sampleRequestDto()` → tipik geçerli `InvoiceRequestDto`
- `invoiceWithStatus(PaymentStatus)`, `invoiceOfType(InvoiceType)` — varyantlar

Bu dosya isteğe bağlı; ilk versiyonda her test kendi fixture'ını kursa da olur.

---

## 4. Uygulama Sırası (Önerilen)

1. **`InvoiceServiceTest`** (Mockito) — en hızlı, en çok kapsar, DB/Spring context yok
2. **`InvoiceRepositoryTest`** (`@DataJpaTest` + Testcontainers) — enum persistence kritik
3. **`InvoiceControllerTest`** (`@WebMvcTest` + MockMvc) — validation ve security burada kanıtlanır
4. **`InvoiceIntegrationTest`** — en son, sadece 4-6 "golden path" senaryo

---

## 5. Kritik Dikkat Noktaları

- **PostgreSQL ENUM tipi H2'de çalışmaz:** Repository testleri için Testcontainers zorunlu. In-memory DB'ye düşme.
- **`@MockBean` deprecated (Spring Boot 3.4+):** Yeni `@MockitoBean` annotation kullan (`org.springframework.test.context.bean.override.mockito.MockitoBean`) — Spring Boot 4.0.3 sürümünde geçerli.
- **Security zinciri `@WebMvcTest`'te eksik gelebilir:** `JwtAuthenticationFilter` ve bağımlılıkları için ya `@MockitoBean` ekle ya da `addFilters = false` ile kapat. Ama DELETE'in 403 döndüğünü kanıtlamak istiyorsak security açık olmalı → `@WithMockUser(roles="USER"/"ADMIN")` kullan.
- **`@Transactional` test sınıfında:** Integration testlerde DB temizlemek için en pratik yol; ama `commit()`-sonrası davranışa bağımlı bir şey test ediyorsan rollback seni yanıltabilir.
- **`RuntimeException` → 500:** Global exception handler yok. Controller testlerinde `not found` senaryolarında 404 DEĞİL 500 dönmesini bekle (veya ileride handler eklenince planı güncelle).
- **Enum deserialization case:** JSON'da `"sale"` → `InvoiceType.sale` OK; `"SALE"` → 400 (çünkü enum sabiti küçük harf). Jackson default case-sensitive.
- **Testcontainers Ryuk:** CI workflow'unda `TESTCONTAINERS_RYUK_DISABLED=true` set ediliyor — local çalıştırırken Docker Desktop açık olmalı.

---

## 6. Çalıştırma Komutları

```bash
cd Backend
./mvnw test                                                  # tüm testler
./mvnw test -Dtest=InvoiceServiceTest                        # sadece unit
./mvnw test -Dtest='Invoice*Test'                            # tüm invoice testleri
./mvnw test -Dtest=InvoiceRepositoryTest#findByCustomerId*   # tek metot
```

---

## 7. Özet Dosya Listesi

**Yeni test dosyaları:**
- `Backend/src/test/java/com/MuhasebePlus/demo/invoice/service/InvoiceServiceTest.java` (unit, Mockito)
- `Backend/src/test/java/com/MuhasebePlus/demo/invoice/repository/InvoiceRepositoryTest.java` (`@DataJpaTest` + Testcontainers)
- `Backend/src/test/java/com/MuhasebePlus/demo/invoice/controller/InvoiceControllerTest.java` (`@WebMvcTest` + MockMvc)
- `Backend/src/test/java/com/MuhasebePlus/demo/invoice/InvoiceIntegrationTest.java` (`@SpringBootTest` full E2E)
- (Opsiyonel) `Backend/src/test/java/com/MuhasebePlus/demo/invoice/InvoiceTestFixtures.java`

**Toplam tahmini test sayısı:** ~50 test (Service 17 + Repository 9 + Controller 17 + Integration 6).
