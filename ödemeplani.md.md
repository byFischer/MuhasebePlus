# Fatura Ödeme Kayıt Sistemi (InvoicePayment)

## Context

Şu anda fatura oluşturulduğunda otomatik olarak `pending` durumunda başlıyor ([InvoiceService.java:95](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L95)). Ancak `pending → paid` geçişi tamamen manuel: kullanıcı bir endpoint'i (`PUT /api/invoices/{id}/payment-status?status=paid`) çağırarak status'u doğrudan değiştiriyor. Hiçbir ödeme detayı tutulmuyor — tutar, tarih, yöntem (nakit/kart/havale), kısmi ödeme, vs.

Mevcut `Transaction` entity'si genel "gelir/gider" amaçlı kullanılıyor ve `invoice_id` alanı opsiyonel olarak var ama hiçbir yerde fatura tahsilatı için kullanılmıyor.

**Hedef**: Gerçek muhasebe akışı kuralım. Bir faturaya birden fazla ödeme yapılabilsin, her ödeme detaylı kaydedilsin (tutar, tarih, yöntem, hesap), kısmi ödemeler `partially_paid` olarak işaretlensin, toplam = fatura tutarı olunca **otomatik** `paid` olsun. Mevcut "Gelir/Gider" sayfasıyla karışmasın ama banka bakiyeleri otomatik güncellensin.

## Mimari Karar

İki domain'i ayır ama veri tutarlılığını koru:
- **InvoicePayment** (yeni) → fatura tahsilat domain'i; "Bu faturaya ne kadar ödendi?"
- **Transaction** (mevcut) → nakit akışı domain'i; "Banka hesabıma ne girdi/çıktı?"

Her `InvoicePayment` kaydedildiğinde **aynı `@Transactional` içinde** otomatik bir `Transaction` (type=INCOME, category="Fatura Tahsilatı") da oluşur. Silme de cascade. Böylece kullanıcı `Transaction` listesini elle değiştirmeye gerek duymaz, fatura sayfasında "Ödemeler" tabı net olur, banka bakiyeleri otomatik tutarlı kalır.

## Yapılacak Değişiklikler

### 1. Enum güncellemeleri

**[Backend/src/main/java/com/MuhasebePlus/demo/invoice/entity/PaymentStatus.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/entity/PaymentStatus.java)** — `partially_paid` değerini ekle:
```java
public enum PaymentStatus {
    draft, pending, partially_paid, paid, overdue
}
```
DB tarafında varchar olarak tutulduğu için ([V5__convert_invoice_enums_to_varchar.sql](Backend/src/main/resources/db/migration/V5__convert_invoice_enums_to_varchar.sql)) ek migration gerekmiyor.

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/invoice/entity/PaymentMethod.java`
```java
public enum PaymentMethod { cash, credit_card, bank_transfer, check, other }
```

### 2. InvoicePayment Entity

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/invoice/entity/InvoicePayment.java`

Pattern olarak [Invoice.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/entity/Invoice.java) ve [Transaction.java](Backend/src/main/java/com/MuhasebePlus/demo/financial/entity/Transaction.java) takip edilecek. Alanlar:
- `paymentId` (PK, IDENTITY)
- `company` (`@ManyToOne`, multi-tenant — projedeki standart pattern)
- `invoiceId` + `Invoice` insertable=false readonly ilişkisi
- `amount` (BigDecimal, precision=15, scale=2)
- `paymentDate` (LocalDate, NOT NULL)
- `paymentMethod` (PaymentMethod enum, `@Enumerated(STRING)`)
- `bankAccountId` (Long, NOT NULL — nakit için "Nakit Kasa" hesabı)
- `notes` (String, length=500, nullable)
- `transactionId` (Long, oluşturulan Transaction'a referans — silme cascade için)
- `extends SoftDeletableEntity` ([SoftDeletableEntity.java](Backend/src/main/java/com/MuhasebePlus/demo/common/entity/SoftDeletableEntity.java))

### 3. Repository

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/invoice/repository/InvoicePaymentRepository.java`

Pattern: [InvoiceLineItemRepository.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/repository/InvoiceLineItemRepository.java)
- `List<InvoicePayment> findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(Long invoiceId, Long companyId)`
- `Optional<BigDecimal> sumAmountByInvoiceId(Long invoiceId)` — JPQL: `SELECT COALESCE(SUM(p.amount), 0) FROM InvoicePayment p WHERE p.invoiceId = :id AND p.isDeleted = false`
- `Optional<InvoicePayment> findByPaymentIdAndCompanyCompanyId(Long paymentId, Long companyId)`

### 4. DTO'lar

**Yeni dosyalar**:
- `Backend/src/main/java/com/MuhasebePlus/demo/invoice/dto/request/InvoicePaymentRequestDto.java` — record: `(BigDecimal amount, LocalDate paymentDate, PaymentMethod paymentMethod, Long bankAccountId, String notes)` — validation: `@NotNull`, `@Positive` for amount, `@PastOrPresent` for date.
- `Backend/src/main/java/com/MuhasebePlus/demo/invoice/dto/response/InvoicePaymentResponseDto.java` — record: `(Long paymentId, Long invoiceId, BigDecimal amount, LocalDate paymentDate, PaymentMethod paymentMethod, Long bankAccountId, String bankAccountName, String notes, LocalDateTime createdAt)`

### 5. InvoicePaymentService (kritik iş mantığı)

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoicePaymentService.java`

Pattern: [InvoiceService.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java) — `@Service @Transactional @RequiredArgsConstructor`, `CompanyContext` ile multi-tenant, `BusinessException` + `SystemLogService` kullanımı.

**Bağımlılıklar**: `InvoicePaymentRepository`, `InvoiceRepository`, `BankAccountRepository`, `TransactionRepository`, `CompanyContext`, `CompanyRepository`, `SystemLogService`.

**Public metodlar**:

```java
public InvoicePaymentResponseDto createPayment(Long invoiceId, InvoicePaymentRequestDto dto)
```
1. Faturayı bul (companyId scope ile), `draft` veya `paid` ise `BusinessException` at.
2. BankAccount'u doğrula (companyId'ye ait olmalı, soft-deleted olmamalı).
3. Mevcut ödemelerin toplamını al: `sumAmountByInvoiceId(invoiceId)`.
4. Eğer `mevcutToplam + dto.amount > invoice.totalAmount` ise `BusinessException("Ödeme tutarı kalan bakiyeyi aşıyor")`.
5. **Önce Transaction oluştur** (type=INCOME, invoiceId=X, accountId=Y, amount=dto.amount, transactionDate=dto.paymentDate, description="Fatura tahsilatı: " + invoice.invoiceNumber, category="Fatura Tahsilatı"). Kaydet, `transactionId` al.
6. InvoicePayment oluştur, `transactionId`'i set et, kaydet.
7. `recalculateInvoiceStatus(invoiceId)` çağır → status'u günceller (`pending`/`partially_paid`/`paid`).
8. Log: `INFO: Fatura ödemesi kaydedildi: <invoiceNumber> - <amount>`.
9. Response DTO'yu döndür.

```java
public List<InvoicePaymentResponseDto> getPaymentsByInvoiceId(Long invoiceId)
```
Faturayı doğrula, ödemeleri yükle, BankAccount adlarını batch yükle, response'a map et.

```java
public void deletePayment(Long paymentId)
```
1. Ödemeyi bul (companyId scope), soft-delete et.
2. İlişkili Transaction'ı bul (`payment.transactionId`), soft-delete et.
3. `recalculateInvoiceStatus(payment.invoiceId)` çağır.
4. Log: `WARNING: Fatura ödemesi silindi: <paymentId>`.

**Private helper**:

```java
private void recalculateInvoiceStatus(Long invoiceId)
```
- `paidTotal = sumAmountByInvoiceId(invoiceId)`
- Mevcut status'u koru: eğer `draft` veya `overdue` ise hesaplama farklı olabilir (`overdue`'yu yalnız vade kontrolü değiştirir).
- Kurallar:
  - `paidTotal == 0` → `pending` (eğer mevcut status `partially_paid` veya `paid` idi ise geri al)
  - `0 < paidTotal < totalAmount` → `partially_paid`
  - `paidTotal >= totalAmount` → `paid`
- `invoiceRepository.save(invoice)` ile persist.

### 6. Controller

**Yeni dosya**: `Backend/src/main/java/com/MuhasebePlus/demo/invoice/controller/InvoicePaymentController.java`

Pattern: [InvoiceController.java](Backend/src/main/java/com/MuhasebePlus/demo/invoice/controller/InvoiceController.java) — `@PreAuthorize("hasAnyRole('ADMIN','USER')")`.

Endpoint'ler:
- `POST /api/invoices/{invoiceId}/payments` → `createPayment` (201 Created)
- `GET /api/invoices/{invoiceId}/payments` → `getPaymentsByInvoiceId`
- `DELETE /api/invoices/{invoiceId}/payments/{paymentId}` → `deletePayment` (`@PreAuthorize("hasRole('ADMIN')")` — silme sadece admin)

### 7. Mevcut InvoiceService düzenlemeleri

**[InvoiceService.updatePaymentStatus](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L274-L287)** — manuel status set'i kısıtla:
- `paid` veya `partially_paid` artık manuel set edilemez (`BusinessException`: "Use /payments endpoint"). Sadece `overdue` ve `pending` arası manuel geçişe izin ver.
- Bu endpoint backwards-compat için kalır ama dar kapsamla.

**[InvoiceService.deleteInvoice](Backend/src/main/java/com/MuhasebePlus/demo/invoice/service/InvoiceService.java#L203-L221)** — `partially_paid` faturanın silinmesini engelle (mevcut `paid` kontrolünün yanına ekle): "Üzerinde ödeme bulunan faturalar silinemez. Önce ödemeleri silin."

### 8. DB Migration

**Yeni dosya**: `Backend/src/main/resources/db/migration/V28__invoice_payment_schema.sql`

```sql
-- invoice_payment tablosu
CREATE TABLE invoice_payment (
    payment_id      BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES company(company_id),
    invoice_id      BIGINT NOT NULL REFERENCES invoice(invoice_id),
    amount          NUMERIC(15,2) NOT NULL,
    payment_date    DATE NOT NULL,
    payment_method  VARCHAR(20) NOT NULL,
    bank_account_id BIGINT NOT NULL REFERENCES bank_account(account_id),
    transaction_id  BIGINT REFERENCES transaction(transaction_id),
    notes           VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_invoice_payment_invoice ON invoice_payment(invoice_id) WHERE is_deleted = false;
CREATE INDEX idx_invoice_payment_company ON invoice_payment(company_id) WHERE is_deleted = false;

-- Default "Nakit Kasa" hesabı her şirket için
-- (BankAccount şemasına göre uyarlanacak — V17 incelenip alanlar dolduruslacak)
INSERT INTO bank_account (company_id, bank_name, account_holder, iban, currency, balance, created_at, updated_at, is_deleted)
SELECT c.company_id, 'Nakit Kasa', 'Nakit', 'CASH-' || c.company_id, 'TRY', 0, NOW(), NOW(), false
FROM company c
WHERE NOT EXISTS (
    SELECT 1 FROM bank_account ba
    WHERE ba.company_id = c.company_id AND ba.bank_name = 'Nakit Kasa' AND ba.is_deleted = false
);
```

> Not: BankAccount'un gerçek kolon yapısı [V17__bank_account_schema_upgrade.sql](Backend/src/main/resources/db/migration/V17__bank_account_schema_upgrade.sql) ve `BankAccount.java` entity'si okunarak migration'a yansıtılacak (özellikle NOT NULL alanlar ve currency tip kontrolleri).

## Verification (Test Planı)

End-to-end manuel test akışı:

1. **Setup**: `mvn spring-boot:run` ile backend'i çalıştır. Postman/curl ile login ol.
2. **Migration kontrolü**: `invoice_payment` tablosunun oluştuğunu, her aktif şirket için bir "Nakit Kasa" BankAccount kaydının oluştuğunu PG'de doğrula.
3. **Tam ödeme akışı**:
   - Yeni fatura oluştur (totalAmount = 1500). Status = `pending`.
   - `POST /api/invoices/{id}/payments` body: `{amount: 1500, paymentDate: "2026-05-08", paymentMethod: "cash", bankAccountId: <nakitKasaId>}`.
   - GET ile fatura çek → status = `paid` olmalı.
   - GET `/api/invoices/{id}/payments` → 1 ödeme dönmeli.
   - GET `/api/transactions` → karşılık gelen INCOME transaction görünmeli.
   - GET `/api/bank-accounts/{nakitKasaId}` → bakiye 1500 artmış olmalı.
4. **Kısmi ödeme**:
   - Yeni fatura (totalAmount = 1000).
   - `POST /payments` amount=400 → status `partially_paid`.
   - `POST /payments` amount=300 → status `partially_paid` (toplam=700).
   - `POST /payments` amount=400 → `BusinessException: Ödeme tutarı kalan bakiyeyi aşıyor` (kalan 300, 400 göndermeye çalıştık).
   - `POST /payments` amount=300 → status `paid`.
5. **Silme cascade**:
   - Bir ödeme sil → invoice status `partially_paid` veya `pending`'e düşmeli, ilgili Transaction de soft-deleted olmalı, banka bakiyesi azalmalı.
6. **Validation testleri**:
   - `paid` faturaya yeni ödeme → reddedilmeli.
   - `draft` faturaya ödeme → reddedilmeli.
   - Başka şirketin faturasına ödeme → multi-tenant kontrolü reddetmeli.
   - `paymentStatus` endpoint'inden manuel `paid` set → reddedilmeli.
7. **Silme kısıtı**: `partially_paid` faturayı silmeye çalış → reddedilmeli.

## Kapsam Dışı

- Frontend tarafı (UI'da "Ödeme Al" butonu, ödemeler listesi vs.) bu plan kapsamında değil — sadece backend API.
- `overdue` durumu için otomatik scheduler bu plan kapsamında değil (ayrı bir iş).
- Para birimi (currency) çoklu desteği — şu anda hep TRY varsayılıyor, ileri bir iş.
