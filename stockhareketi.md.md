# Stok Hareket Defteri (StockMovement) + Satın Alma Faturasında Yeni Ürün Oluşturma

## Context

Şu anki sistemde stok yönetiminde iki temel sorun var:

1. **Stok değişiklikleri sebepsiz ve izsiz**: [Stock.java](Backend/src/main/java/com/MuhasebePlus/demo/stock/entity/Stock.java) sadece bir `quantity` alanı tutuyor. [StockController.java:68-82](Backend/src/main/java/com/MuhasebePlus/demo/stock/controller/StockController.java#L68-L82) `/add` ve `/remove` endpoint'leri ile sebep belirtmeden stok artırılıp azaltılabiliyor. [StockAdjustmentRequestDto](Backend/src/main/java/com/MuhasebePlus/demo/stock/dto/request/StockAdjustmentRequestDto.java) içindeki `reason` zaten opsiyonel ve [StockService.addStock](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java#L123-L131) içinde **hiç kullanılmıyor** — alınıp atılıyor. Audit trail yok.

2. **Satın alma faturası stoku artırmıyor (kritik bug)**: [InvoiceService.java:103-105](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L103-L105) sadece `sale` tipi faturalarda `decreaseStock` çağırıyor. `purchase` tipi faturada hiçbir stok hareketi yok. Yani kullanıcı tedarikçiden mal alıp faturasını kestiğinde, stoku elle eklemek zorunda kalıyor — bu da iki kayıt arasında bağ olmamasına yol açıyor.

3. **Satın alma faturasında yeni ürün giremiyoruz**: [InvoiceService.fetchAndValidateProducts](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L298-L312) sadece mevcut ürünleri arıyor, bulamazsa hata atıyor. Frontend tarafında dropdown'dan sadece var olan ürünler seçilebiliyor. Oysa satın alma faturasında yeni bir ürünü de aynı anda sisteme eklemek doğal bir akış (tedarikçiden ilk kez alınan bir ürün).

**Hedef**: Stoğun her değişikliğini, sebebini ve kaynağını izleyebileceğimiz bir hareket defteri (`StockMovement`) kuralım. Stok değişiklikleri ya bir faturadan otomatik olarak gelsin ya da kullanıcı manuel girerken **mutlaka bir hareket türü seçsin** (alış / satış / iade / fire / vs.). Aynı zamanda satın alma faturasında yeni ürün eklemeye izin verelim.

## Mimari Kararlar

### Karar 1: Hareket türü için **enum + entity** yaklaşımı

`movementType` bir **enum** (StringEnum) — sabit liste; veritabanında zorlanan tutarlılık. Reason ise serbest metin (örn: "Stand fire", "Müşteri iadesi - kutusu hasarlı"). Hareket başına hem tip hem sebep tutulur.

**Neden enum?** Hareket türleri sınırlı ve değişmez (alış/satış/iade/manuel düzeltme/açılış). Kullanıcı serbest metin yazsaydı yazım hataları, çoklu varyant ("alış" vs "alis" vs "alim") raporları bozardı.

### Karar 2: `Stock.quantity` artık **denormalize/cached değer**

Tek doğruluk kaynağı `StockMovement` tablosu. `Stock.quantity` her movement sonrası güncellenir (transactional). Tutarlılığı korumak için bir admin endpoint'i de eklenir (`/recalculate`) → tüm hareketlerden quantity'yi yeniden hesaplar.

### Karar 3: Hareket defteri **append-only**

Bir movement kaydedildikten sonra **silinmez ve değiştirilmez**. Hata varsa ters hareket eklenir (örn: yanlışlıkla 100 ADJUSTMENT_IN girildi → +100 değil, -100 reverse hareketi eklenir). Bu defter mantığıdır, muhasebede standardı budur.

> Mevcut SoftDeletableEntity pattern'i ile uyumlu kalmak için `is_deleted` kolonu olur ama service tarafında **delete metodu hiç eklenmez**. Sadece fatura silme akışında **otomatik** reverse hareket oluşur (kullanıcı çağıramaz).

### Karar 4: Kapsam dışı tutulanlar

- Çoklu depo (warehouse) — bu projede tek depo varsayılıyor.
- Lot/parti takibi (son kullanım tarihi vs.) — gıda/eczane için, bu projede gerekmez.
- Detaylı sayım (count) akışı — kullanıcı bu noktada istemiyor.
- FIFO/ortalama maliyet otomasyonu — `unitCost` alanı zaten kaydedilecek (gelecekte rapor için), ama otomatik kâr-zarar hesabı bu plan kapsamı dışı.

## Yapılacak Değişiklikler

### 1. MovementType Enum

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/stock/entity/MovementType.java`

```java
public enum MovementType {
    PURCHASE,           // Satın alma faturası → stok artar (otomatik)
    SALE,               // Satış faturası → stok azalır (otomatik)
    RETURN_IN,          // Müşteri iadesi → stok artar (manuel veya iade fatura)
    RETURN_OUT,         // Tedarikçiye iade → stok azalır (manuel veya iade fatura)
    ADJUSTMENT_IN,      // Manuel artış (hediye, buluntu, düzeltme)
    ADJUSTMENT_OUT,     // Manuel azalış (fire, kayıp, bozulma, çalıntı)
    PRODUCTION_IN,      // Üretim mamul giriş (manuel)
    PRODUCTION_OUT,     // Üretim hammadde çıkış (manuel)
    OPENING_BALANCE     // Sistem açılış bakiyesi (ürün ilk yaratıldığında initialQuantity > 0 ise)
}
```

İşaret kuralı (servisin kendi içinde doğrular): `_IN`, `PURCHASE`, `OPENING_BALANCE` → quantity > 0 zorunlu. `_OUT`, `SALE` → quantity > 0 verilir, service negatif olarak kaydeder.

### 2. StockMovement Entity

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/stock/entity/StockMovement.java`

Pattern olarak [Stock.java](Backend/src/main/java/com/MuhasebePlus/demo/stock/entity/Stock.java) ve [Transaction.java](Backend/src/main/java/com/MuhasebePlus/demo/financial/entity/Transaction.java) takip edilecek.

Alanlar:
- `movementId` — PK, IDENTITY (Long)
- `company` — `@ManyToOne` (multi-tenant)
- `productId` (Integer, NOT NULL) + `Product` readonly ilişki (insertable=false)
- `quantity` (Integer, NOT NULL) — pozitif veya negatif (signed). Stok artıran hareketlerde +, azaltanlarda −.
- `movementType` (MovementType enum, `@Enumerated(STRING)`)
- `sourceType` (String, enum gibi: `INVOICE` | `MANUAL` | `INITIAL`) — hareket kaynağı
- `sourceId` (Long, nullable) — `INVOICE` ise invoiceId
- `unitCost` (BigDecimal, nullable, precision=15, scale=2) — birim maliyet snapshot'ı (PURCHASE için zorunlu, gelecekteki maliyet hesabı için)
- `reason` (String, length=255, manuel hareketlerde NOT NULL)
- `createdByUserId` (Long, nullable — V12'de `created_by` zaten var mı kontrol edilecek; yoksa nullable bırakılır)
- `extends SoftDeletableEntity` ([SoftDeletableEntity.java](Backend/src/main/java/com/MuhasebePlus/demo/common/entity/SoftDeletableEntity.java)) — pattern uyumu için, ama service'te delete metodu yok.

### 3. StockMovementRepository

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/stock/repository/StockMovementRepository.java`

- `List<StockMovement> findByProductIdAndCompanyCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(Integer productId, Long companyId)` — bir ürünün hareket geçmişi
- `List<StockMovement> findByCompanyCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(Long companyId)` — tüm hareketler (paged sürümü de eklenebilir)
- `Optional<Integer> sumQuantityByProductId(Integer productId, Long companyId)` — JPQL: `SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m WHERE m.productId = :id AND m.company.companyId = :cid AND m.isDeleted = false`
- `List<StockMovement> findBySourceTypeAndSourceId(String sourceType, Long sourceId)` — fatura silme akışında ilgili movement'ları bulmak için

### 4. DTO'lar

**Yeni dosyalar**:

`Backend/src/main/java/com/MuhasebePlus/demo/stock/dto/request/StockMovementRequestDto.java`
```java
public record StockMovementRequestDto(
    @NotNull Integer productId,
    @NotNull @Min(1) Integer quantity,                    // her zaman pozitif girilir, service işaretler
    @NotNull MovementType movementType,                   // sadece manuel olanlar kabul edilir (aşağıda kontrol)
    @Size(max = 255) String reason,                       // manuel hareketlerde zorunlu hale gelir
    BigDecimal unitCost                                   // opsiyonel (manuel girişte tedarikçiden gelmiş bir mal için)
) {}
```

`Backend/src/main/java/com/MuhasebePlus/demo/stock/dto/response/StockMovementResponseDto.java`
```java
public record StockMovementResponseDto(
    Long movementId,
    Integer productId,
    String productName,
    Integer quantity,
    MovementType movementType,
    String sourceType,
    Long sourceId,
    BigDecimal unitCost,
    String reason,
    LocalDateTime createdAt
) {}
```

### 5. StockMovementService

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockMovementService.java`

Pattern: [InvoiceService.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java) — `@Service @Transactional @RequiredArgsConstructor`, `BusinessException` + `SystemLogService`.

**Bağımlılıklar**: `StockMovementRepository`, `StockRepository`, `ProductRepository`, `CompanyContext`, `CompanyRepository`, `SystemLogService`.

**İç (paket-private) API — diğer service'ler kullanır**:

```java
StockMovement recordMovement(Integer productId, int signedQuantity, MovementType type,
                              String sourceType, Long sourceId, BigDecimal unitCost, String reason)
```
Tek "yazma" yöntemi. Tüm akışlar buradan geçer.
1. Ürünü doğrula (companyId + isDeleted=false).
2. Stoku bul (yoksa quantity=0 ile oluştur — purchase'ta yeni ürün kullanılırken bu otomatik kuruluyor olabilir).
3. **Stok azalan hareketlerde** (`SALE`, `RETURN_OUT`, `ADJUSTMENT_OUT`, `PRODUCTION_OUT`) `stock.quantity + signedQuantity >= 0` doğrula. Aksi halde `BusinessException("Yetersiz stok")`.
4. StockMovement kaydı oluştur ve persist et.
5. `Stock.quantity += signedQuantity`, persist.
6. `SystemLog.INFO`: hareket detayı.

**Dışa açık (public) API — manuel hareket girişi**:

```java
public StockMovementResponseDto createManualMovement(StockMovementRequestDto dto)
```
1. **Whitelist kontrol**: `dto.movementType` sadece `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `RETURN_IN`, `RETURN_OUT`, `PRODUCTION_IN`, `PRODUCTION_OUT`, `OPENING_BALANCE` olabilir. `PURCHASE` ve `SALE` manuel girilemez (sadece fatura ile) → `BusinessException`.
2. **Reason zorunluluğu**: Manuel hareketlerde `reason` boş olamaz → `BusinessException("Hareket sebebi zorunludur")`.
3. İşaret hesapla: `_IN`, `OPENING_BALANCE` için `+dto.quantity`; `_OUT` için `-dto.quantity`.
4. `recordMovement(...)` çağır.
5. Response döndür.

```java
public List<StockMovementResponseDto> getMovementsByProduct(Integer productId)
public List<StockMovementResponseDto> getAllMovements()
```
Listeler — frontend'de "Stok Hareket Geçmişi" tabı için.

```java
public void recordReverseMovementsForInvoice(Long invoiceId)
```
Fatura silinirken çağrılır. İlgili tüm movement'ların ters işaretli kopyalarını ekler (idempotent değil, dikkat: aynı fatura iki kez silinemez zaten).

### 6. StockController değişiklikleri

**[StockController.java](Backend/src/main/java/com/MuhasebePlus/demo/stock/controller/StockController.java)** — eski endpoint'leri kaldır, yenisini ekle.

**Kaldırılacak endpoint'ler** (eski "sebepsiz" ayarlama endpoint'leri):
- `POST /api/stocks/{productId}/add` ([:68-74](Backend/src/main/java/com/MuhasebePlus/demo/stock/controller/StockController.java#L68-L74))
- `POST /api/stocks/{productId}/remove` ([:76-82](Backend/src/main/java/com/MuhasebePlus/demo/stock/controller/StockController.java#L76-L82))
- `PUT /api/stocks/{productId}/count` (`markCounted`) ([:84-90](Backend/src/main/java/com/MuhasebePlus/demo/stock/controller/StockController.java#L84-L90)) → manuel hareket akışına yönlendirilebilir veya geçici olarak ADJUSTMENT_IN/OUT olarak yapılır.

**Eklenecek endpoint'ler** (yeni `StockMovementController.java`):
- `POST /api/stock-movements` → `createManualMovement` (reason ve type zorunlu)
- `GET /api/stock-movements?productId={id}` → `getMovementsByProduct` (productId varsa filtreli, yoksa tümü)
- `GET /api/stock-movements/{movementId}` → tekil hareket detayı (opsiyonel)

**Korunan endpoint'ler**:
- `GET /api/stocks` (liste)
- `GET /api/stocks/product/{productId}`
- `POST /api/stocks` (createStock — sadece quantity=0 ile boş kayıt; gerçek miktar OPENING_BALANCE ile gelir)
- `PUT /api/stocks/{productId}` (sadece minQuantity güncellemek için — quantity'yi değiştirmiyor zaten)
- `GET /api/stocks/low`
- `DELETE /api/stocks/{productId}`, `PUT /api/stocks/{productId}/restore` (admin)

### 7. StockService değişiklikleri

**[StockService.java](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java)** içindeki şu metodlar **silinecek** (StockMovementService'e taşındı):
- `addStock` ([:123-131](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java#L123-L131))
- `removeStock` ([:133-147](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java#L133-L147))
- `setQuantity` ([:149-160](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java#L149-L160))
- `markCounted` ([:210-212](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java#L210-L212))
- `decreaseStock` ([:188-205](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/StockService.java#L188-L205)) → InvoiceService artık `StockMovementService.recordMovement(SALE)` çağıracak
- `checkStock` kalır (rezervasyon kontrolü için fatura öncesinde kullanılıyor — ama yeni mantıkta bu da `StockMovementService`'te recordMovement içinde yapılabilir; geçiş kolaylığı için şimdilik StockService'te kalsın)

### 8. ProductService değişiklikleri

**[ProductService.createProduct](Backend/src/main/java/com/MuhasebePlus/demo/stock/service/ProductService.java#L36-L65)** — şu anda `initialQuantity`'yi doğrudan `stock.quantity`'ye yazıyor. Yeni mantık:

1. Product'ı oluştur (mevcut akış).
2. Stock kaydını **quantity=0** ile oluştur (denormalize için boş kayıt).
3. Eğer `dto.initialQuantity() > 0` → `stockMovementService.recordMovement(productId, +initialQuantity, MovementType.OPENING_BALANCE, "INITIAL", null, dto.costPrice(), "Açılış bakiyesi")` çağır.

Böylece açılış bakiyesi de bir hareket kaydı olarak izlenir.

### 9. InvoiceService değişiklikleri (En Kritik Bölüm)

**[InvoiceService.createInvoice](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L60-L109)** — büyük revizyon:

#### 9a. SALE faturası akışı (mevcut + yenileme)

[:103-105](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L103-L105) yerine:
```java
if (dto.invoiceType() == InvoiceType.sale) {
    for (InvoiceLineItemRequestDto item : dto.lineItems()) {
        Product p = productMap.get(item.productId());
        stockMovementService.recordMovement(
            item.productId(), -item.quantity(), MovementType.SALE,
            "INVOICE", savedInvoice.getInvoiceId(), null, null
        );
    }
}
```

Stok yetersizliği kontrolü artık `recordMovement` içinde merkezileştirildi (mevcut `checkStock` + `decreaseStock` ikilisini değiştirir).

#### 9b. PURCHASE faturası akışı (YENİ — kritik bug düzeltmesi)

```java
if (dto.invoiceType() == InvoiceType.purchase) {
    for (InvoiceLineItemRequestDto item : dto.lineItems()) {
        Product p = productMap.get(item.productId());
        stockMovementService.recordMovement(
            item.productId(), +item.quantity(), MovementType.PURCHASE,
            "INVOICE", savedInvoice.getInvoiceId(), p.getCostPrice(), null
        );
    }
}
```

Artık satın alma faturası kesildiğinde stok otomatik artar.

#### 9c. **YENİ FEATURE — Satın alma faturasında yeni ürün eklenebilmesi**

**[InvoiceLineItemRequestDto.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/dto/request/InvoiceLineItemRequestDto.java)** güncellenir:

```java
public record InvoiceLineItemRequestDto(
    Integer productId,                        // artık nullable — yeni ürünse null olur
    @NotNull @Min(1) Integer quantity,
    @Valid NewProductRequestDto newProduct    // productId null ise zorunlu
) {
    @AssertTrue(message = "productId veya newProduct'tan biri verilmeli")
    public boolean isValidProductReference() {
        return (productId != null) ^ (newProduct != null);  // XOR — ikisi birden olamaz, ikisi birden null olamaz
    }
}
```

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/invoice/dto/request/NewProductRequestDto.java`

```java
public record NewProductRequestDto(
    @NotBlank @Size(max=100) String barcode,
    @NotBlank @Size(max=100) String name,
    @Size(max=200) String description,
    @NotBlank @Size(max=50) String unit,
    @NotNull @DecimalMin(value="0.0", inclusive=false) BigDecimal salePrice,
    @NotNull @DecimalMin(value="0.0", inclusive=true) BigDecimal vatRate,
    @NotNull @DecimalMin(value="0.0", inclusive=false) BigDecimal costPrice,
    @Min(0) Integer minQuantity
) {}
```

**[InvoiceService](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java) yeni private metod**:

```java
private Map<Integer, Product> resolveProducts(List<InvoiceLineItemRequestDto> items, InvoiceType invoiceType)
```

Mantık:
1. Her satır için: `productId != null` ise mevcut ürünü doğrula (eski `fetchAndValidateProducts` mantığı).
2. `newProduct != null` ise:
   - Sadece `purchase` faturada izin ver. `sale` faturada `BusinessException("Satış faturasında yeni ürün eklenemez")`.
   - `productService.createProduct(...)` çağır → yeni Product + boş Stock oluşturulur.
   - Yeni `productId`'i map'e ekle, line item'ın `productId`'sini buna güncelle (servis içinde line item DTO'su mutable değil → işlenmiş productId'yi ayrı bir map'te tut, line item kaydederken bu map'ten oku).
3. Aynı request içinde aynı barcode'la 2 yeni ürün gelirse → `BusinessException`.
4. Mevcut işlem sırası korunur (validate → create invoice → create line items → record movements).

**[InvoiceService.fetchAndValidateProducts](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L298-L312)** yerine `resolveProducts` çağrılır.

**[InvoiceService.saveLineItems](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L347-L379)** içinde line item kaydedilirken artık çözümlenmiş productId kullanılır.

#### 9d. Fatura silme akışı

**[InvoiceService.deleteInvoice](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L203-L221)** içinde fatura soft-delete edilirken:

```java
stockMovementService.recordReverseMovementsForInvoice(invoiceId);
```

Yani fatura silinirse stok hareketi de geri alınır (ters hareket eklenir, mevcut hareket kalır → defter mantığı korunur).

> Not: Faturanın confirm akışı ([InvoiceService.confirmInvoice](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L111-L158)) içindeki `decreaseStock` çağrısı da `stockMovementService.recordMovement(SALE)`'a güncellenir.

#### 9e. Update akışı (dikkat)

**[InvoiceService.updateInvoice](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L179-L201)** — şu anda line item güncellemiyor zaten. Bu plan kapsamında **dokunulmuyor**, çünkü:
- Mevcut implementasyon line item'ları update'lemiyor.
- Eğer line item update edilseydi, eski movement'ları reverse + yeni movement'lar yazma akışı gerekiyordu — ek karmaşıklık.
- Bunu istersen ayrı bir iş olarak ele alalım.

### 10. DB Migration

**Yeni dosya**: `Backend/src/main/resources/db/migration/V28__stock_movement_schema.sql`

```sql
CREATE TABLE stock_movement (
    movement_id        BIGSERIAL PRIMARY KEY,
    company_id         BIGINT NOT NULL REFERENCES company(company_id),
    product_id         INTEGER NOT NULL REFERENCES product(product_id),
    quantity           INTEGER NOT NULL,
    movement_type      VARCHAR(30) NOT NULL,
    source_type        VARCHAR(20) NOT NULL,         -- INVOICE | MANUAL | INITIAL
    source_id          BIGINT,                        -- INVOICE ise invoice_id
    unit_cost          NUMERIC(15,2),
    reason             VARCHAR(255),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at         TIMESTAMP
);

CREATE INDEX idx_stock_movement_product   ON stock_movement(product_id) WHERE is_deleted = false;
CREATE INDEX idx_stock_movement_company   ON stock_movement(company_id) WHERE is_deleted = false;
CREATE INDEX idx_stock_movement_source    ON stock_movement(source_type, source_id);
CREATE INDEX idx_stock_movement_type      ON stock_movement(movement_type);

-- Veri tutarlılığı için: mevcut stok değerlerini OPENING_BALANCE olarak ledger'a aktar
-- (Sistem zaten kullanımdaysa, geçmişe dair tek bir "açılış" satırı ekle ki Stock.quantity
--  ile SUM(stock_movement.quantity) tutarlı kalsın.)
INSERT INTO stock_movement (company_id, product_id, quantity, movement_type, source_type, reason, created_at, updated_at, is_deleted)
SELECT s.company_id, s.product_id, s.quantity, 'OPENING_BALANCE', 'INITIAL',
       'Migration V28 - Mevcut stok devri', NOW(), NOW(), false
FROM stock s
WHERE s.is_deleted = false AND s.quantity IS NOT NULL AND s.quantity > 0;
```

> Migration'ın son INSERT'i kritik: V28 deploy edilince mevcut `stock.quantity` değerleri ledger'a "açılış" olarak yansır, böylece `SUM(stock_movement)` ile `stock.quantity` arasında tutarlılık sağlanır.

## Etkilenen Dosyalar — Özet Liste

**Yeni dosyalar:**
- `stock/entity/MovementType.java`
- `stock/entity/StockMovement.java`
- `stock/repository/StockMovementRepository.java`
- `stock/dto/request/StockMovementRequestDto.java`
- `stock/dto/response/StockMovementResponseDto.java`
- `stock/service/StockMovementService.java`
- `stock/controller/StockMovementController.java`
- `invoice/dto/request/NewProductRequestDto.java`
- `resources/db/migration/V28__stock_movement_schema.sql`

**Değişiklik gören dosyalar:**
- `stock/service/StockService.java` — `addStock`, `removeStock`, `setQuantity`, `markCounted`, `decreaseStock` kaldırılıyor.
- `stock/controller/StockController.java` — yukarıdaki metodlar çağıran endpoint'ler kaldırılıyor.
- `stock/service/ProductService.java` — `createProduct` içinde initialQuantity → OPENING_BALANCE movement.
- `invoice/dto/request/InvoiceLineItemRequestDto.java` — `productId` nullable olur, `newProduct` alanı eklenir, XOR validation.
- `invoice/service/InvoiceService.java` — `createInvoice`, `confirmInvoice`, `deleteInvoice` metodları güncellenir; `resolveProducts` private helper eklenir; `decreaseStock` çağrıları yerine `stockMovementService.recordMovement` çağrıları.

**Silinmeyecek ama kullanılmayacak (kapsam dışı):**
- `stock/dto/request/StockAdjustmentRequestDto.java` — eski endpoint'lerin DTO'su, ileride temizlenebilir.

## Build Sırası (Önerilen Geliştirme Akışı)

1. **Faz 1 — Defter altyapısı**: MovementType enum, StockMovement entity, repository, V28 migration. Test: migration çalışıyor mu, mevcut stoklar OPENING_BALANCE olarak yansıyor mu.
2. **Faz 2 — Servis ve manuel akış**: StockMovementService (`recordMovement` + `createManualMovement`), StockMovementController. Test: manuel hareket girilebiliyor mu, reason zorunlu mu, geçersiz tip reddediliyor mu.
3. **Faz 3 — ProductService entegrasyonu**: `createProduct` → OPENING_BALANCE. Test: yeni ürün açılış bakiyesi ile yaratıldığında hareket defterinde gözüküyor mu.
4. **Faz 4 — InvoiceService SALE entegrasyonu**: SALE faturası → SALE movement; eski `decreaseStock` kullanımları temizlenir. Test: satış faturası kesince stok düşüyor ve hareket kaydı oluşuyor mu.
5. **Faz 5 — InvoiceService PURCHASE entegrasyonu (kritik bug fix)**: PURCHASE faturası → PURCHASE movement. Test: alış faturası kesince stok artıyor mu.
6. **Faz 6 — Yeni ürün ile fatura**: InvoiceLineItemRequestDto güncellemesi, `resolveProducts`, NewProductRequestDto. Test: alış faturasında yeni ürün eklenip aynı anda hem ürün hem stok hem PURCHASE movement oluşuyor mu.
7. **Faz 7 — Fatura silme akışı**: `recordReverseMovementsForInvoice` ile cascade. Test: silinen fatura sonrası stok geri alınıyor mu, hareket defteri tutarlı mı.
8. **Faz 8 — Eski endpoint'leri temizle**: StockController'dan eski `/add`, `/remove`, `/count` kaldırılır, StockService'ten ilgili metodlar silinir.

## Verification — Test Planı

End-to-end manuel test akışı (Postman / curl):

1. **Migration kontrolü**: `mvn spring-boot:run` → DB'de `stock_movement` tablosu var mı, mevcut stoklar `OPENING_BALANCE` ile yansımış mı?

2. **Manuel ayarlama akışı**:
   - `POST /api/stock-movements` body: `{productId: 1, quantity: 5, movementType: "ADJUSTMENT_IN", reason: "Buluntu mal"}` → 201, stok 5 artmalı.
   - Aynı endpoint reason boş: → 400, "Hareket sebebi zorunludur".
   - Aynı endpoint movementType: "PURCHASE" → 400, "Manuel girilemez".
   - `POST /api/stock-movements` body: `{quantity: 100, movementType: "ADJUSTMENT_OUT", reason: "fire"}` → mevcut stok 5'ti, 100 düşmeye çalışıyor → 400, "Yetersiz stok".

3. **Satış faturası akışı**:
   - Mevcut stoğu 50 olan bir ürünle satış faturası oluştur, qty=10 → 201, stok 40'a düşmeli, stock_movement'ta `SALE / -10 / sourceType=INVOICE / sourceId=<invoiceId>` kaydı oluşmalı.

4. **Alış faturası akışı (kritik bug fix doğrulaması)**:
   - Mevcut bir ürün için alış faturası oluştur, qty=20 → 201, stok 20 artmalı, `PURCHASE / +20 / unitCost=<costPrice>` kaydı oluşmalı.

5. **Alış faturasında yeni ürün**:
   - `POST /api/invoices` body:
     ```json
     {
       "invoiceType": "purchase",
       "lineItems": [
         { "newProduct": { "barcode":"NEW001", "name":"Yeni Mal", "unit":"adet", "salePrice":15, "vatRate":20, "costPrice":10 }, "quantity": 100 }
       ]
     }
     ```
   - → 201, yeni ürün oluşmuş olmalı, stok 100 olmalı, hareket defterinde `PURCHASE / +100` görünmeli.
   - Aynı request satış faturasıyla yapılırsa → 400, "Satış faturasında yeni ürün eklenemez".
   - Aynı request içinde 2 yeni ürün aynı barcode ile → 400, "Aynı request içinde aynı barcode tekrar edilemiş".

6. **Fatura silme akışı**:
   - 4'teki alış faturasını sil → stok eski haline (yani 20 azalır) dönmeli, ledger'da hem `PURCHASE +20` hem `PURCHASE -20` (reverse) kayıtları var olmalı.

7. **Hareket geçmişi**:
   - `GET /api/stock-movements?productId=1` → ürünün tüm hareketleri kronolojik (yeni → eski) dönmeli.

8. **Multi-tenancy**:
   - Başka şirketin ürün ID'si ile hareket girmeye çalış → erişim reddi.

## Kapsam Dışı (İstersen Sonra)

- Sayım fişi (inventory count) UX'i
- Çoklu depo (warehouse)
- Otomatik ortalama maliyet / kâr-zarar hesabı (unitCost zaten kaydediliyor, raporlamada kullanılabilir)
- Satış faturasında line item update sırasında ters movement akışı
- Frontend tarafı (UI değişiklikleri ayrı bir iş)
