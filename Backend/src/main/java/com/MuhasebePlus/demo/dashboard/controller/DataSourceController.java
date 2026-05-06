package com.MuhasebePlus.demo.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-sources")
public class DataSourceController {

    private static final List<Map<String, Object>> DATA_SOURCES = List.of(
            Map.of(
                    "key", "INVOICE",
                    "label", "Faturalar",
                    "description", "Satış ve alış faturaları",
                    "fields", List.of(
                            field("invoiceNumber", "Fatura No", "TEXT", false),
                            field("invoiceType", "Fatura Tipi", "ENUM", false, List.of("sale", "purchase")),
                            field("paymentStatus", "Ödeme Durumu", "ENUM", false, List.of("draft", "pending", "paid", "overdue")),
                            field("dueDate", "Vade Tarihi", "DATE", false),
                            field("totalAmount", "Toplam Tutar", "NUMBER", true),
                            field("subtotal", "Ara Toplam", "NUMBER", true),
                            field("vatAmount", "KDV Tutarı", "NUMBER", true),
                            field("customer.name", "Müşteri Adı", "TEXT", false),
                            field("customer.city", "Müşteri Şehir", "TEXT", false),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "TRANSACTION",
                    "label", "İşlemler",
                    "description", "Gelir ve gider işlemleri",
                    "fields", List.of(
                            field("transactionType", "İşlem Tipi", "ENUM", false, List.of("INCOME", "EXPENSE")),
                            field("amount", "Tutar", "NUMBER", true),
                            field("transactionDate", "İşlem Tarihi", "DATE", false),
                            field("category", "Kategori", "TEXT", false),
                            field("account.bankName", "Banka Adı", "TEXT", false),
                            field("isRecurring", "Tekrarlayan", "BOOLEAN", false),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "CUSTOMER",
                    "label", "Müşteriler",
                    "description", "Müşteri cari hesapları",
                    "fields", List.of(
                            field("name", "Müşteri Adı", "TEXT", false),
                            field("email", "E-posta", "TEXT", false),
                            field("taxNumber", "Vergi No", "TEXT", false),
                            field("city", "Şehir", "TEXT", false),
                            field("type", "Müşteri Tipi", "ENUM", false, List.of("INDIVIDUAL", "CORPORATE")),
                            field("phoneNumber", "Telefon", "TEXT", false),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "PRODUCT",
                    "label", "Ürünler",
                    "description", "Stoktaki ürün tanımları",
                    "fields", List.of(
                            field("barcode", "Barkod", "TEXT", false),
                            field("name", "Ürün Adı", "TEXT", false),
                            field("unit", "Birim", "TEXT", false),
                            field("salePrice", "Satış Fiyatı", "NUMBER", true),
                            field("costPrice", "Maliyet Fiyatı", "NUMBER", true),
                            field("vatRate", "KDV Oranı", "NUMBER", true),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "STOCK",
                    "label", "Stoklar",
                    "description", "Ürün stok hareketleri ve seviyeleri",
                    "fields", List.of(
                            field("product.name", "Ürün Adı", "TEXT", false),
                            field("product.barcode", "Barkod", "TEXT", false),
                            field("quantity", "Mevcut Miktar", "NUMBER", true),
                            field("minQuantity", "Kritik Seviye", "NUMBER", true),
                            field("lastCountDate", "Son Sayım Tarihi", "DATE", false),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "BANK_ACCOUNT",
                    "label", "Banka Hesapları",
                    "description", "Banka hesabı bilgileri",
                    "fields", List.of(
                            field("bankName", "Banka Adı", "TEXT", false),
                            field("iban", "IBAN", "TEXT", false),
                            field("currency", "Para Birimi", "ENUM", false, List.of("TRY", "USD", "EUR")),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "INVOICE_LINE_ITEM",
                    "label", "Fatura Kalemleri",
                    "description", "Fatura içindeki ürün satırları",
                    "fields", List.of(
                            field("invoice.invoiceNumber", "Fatura No", "TEXT", false),
                            field("invoice.invoiceType", "Fatura Tipi", "ENUM", false, List.of("sale", "purchase")),
                            field("invoice.paymentStatus", "Ödeme Durumu", "ENUM", false, List.of("draft", "pending", "paid", "overdue")),
                            field("product.name", "Ürün Adı", "TEXT", false),
                            field("product.barcode", "Barkod", "TEXT", false),
                            field("quantity", "Miktar", "NUMBER", true),
                            field("unitPrice", "Birim Fiyat", "NUMBER", true),
                            field("lineTotal", "Satır Toplamı", "NUMBER", true),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "TEMPLATE",
                    "label", "Şablonlar",
                    "description", "Sistem şablonları",
                    "fields", List.of(
                            field("templateCode", "Şablon Kodu", "TEXT", false),
                            field("templateName", "Şablon Adı", "TEXT", false),
                            field("templateType", "Şablon Tipi", "ENUM", false, List.of("INCOME", "EXPENSE", "INVOICE", "STOCK_ADJUSTMENT", "CUSTOMER_TRANSACTION", "BANK_TRANSFER")),
                            field("period", "Dönem", "TEXT", false),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            ),
            Map.of(
                    "key", "REPORT",
                    "label", "Raporlar",
                    "description", "Oluşturulan raporlar",
                    "fields", List.of(
                            field("reportType", "Rapor Tipi", "ENUM", false, List.of("PROFIT_LOSS", "INCOME", "EXPENSE", "CASH_FLOW", "AR_AGING", "VAT_PREP", "STOCK_STATUS", "COLLECTION_PERFORMANCE", "SLOW_INVENTORY", "BUDGET_VARIANCE", "BANK_RECONCILIATION", "EXECUTIVE_SUMMARY")),
                            field("startDate", "Başlangıç Tarihi", "DATE", false),
                            field("endDate", "Bitiş Tarihi", "DATE", false),
                            field("format", "Format", "ENUM", false, List.of("EXCEL")),
                            field("fileSize", "Dosya Boyutu", "NUMBER", true),
                            field("createdAt", "Oluşturma Tarihi", "DATE", false)
                    )
            )
    );

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Map<String, Object>>> getDataSources() {
        return ResponseEntity.ok(DATA_SOURCES);
    }

    @GetMapping("/{source}/fields")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Map<String, Object>>> getFields(@PathVariable String source) {
        return DATA_SOURCES.stream()
                .filter(ds -> ds.get("key").equals(source))
                .findFirst()
                .map(ds -> ResponseEntity.ok((List<Map<String, Object>>) ds.get("fields")))
                .orElse(ResponseEntity.notFound().build());
    }

    private static Map<String, Object> field(String key, String label, String type, boolean aggregateable) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("key", key);
        map.put("label", label);
        map.put("type", type);
        map.put("aggregateable", aggregateable);
        return map;
    }

    private static Map<String, Object> field(String key, String label, String type, boolean aggregateable, List<String> options) {
        Map<String, Object> map = field(key, label, type, aggregateable);
        map.put("options", options);
        return map;
    }
}
