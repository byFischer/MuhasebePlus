package com.MuhasebePlus.demo.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/data-sources")
public class DataSourceController {

    // Roller
    private static final String METRIC          = "METRIC";
    private static final String DIMENSION       = "DIMENSION";
    private static final String DATE_DIMENSION  = "DATE_DIMENSION";
    private static final String INFO            = "INFO";

    // Yaygın aggregation listeleri
    private static final List<String> ALL_AGG = List.of("SUM", "AVG", "MIN", "MAX");
    private static final List<String> AVG_ONLY = List.of("AVG", "MIN", "MAX");
    private static final List<String> NO_AGG  = List.of();

    private static final List<Map<String, Object>> DATA_SOURCES = withSupportedAggregations(List.of(
            Map.of(
                    "key", "INVOICE",
                    "label", "Faturalar",
                    "description", "Satış ve alış faturaları",
                    "fields", List.of(
                            field("invoiceNumber", "Fatura No",        "TEXT",   INFO,            NO_AGG, false),
                            enumField("invoiceType",   "Fatura Tipi",     DIMENSION, true,  List.of("sale","purchase")),
                            enumField("paymentStatus", "Ödeme Durumu",    DIMENSION, true,  List.of("draft","pending","paid","overdue")),
                            field("dueDate",       "Vade Tarihi",      "DATE",   DATE_DIMENSION,  NO_AGG, true),
                            field("totalAmount",   "Toplam Tutar",     "NUMBER", METRIC,          ALL_AGG, false),
                            field("subtotal",      "Ara Toplam",       "NUMBER", METRIC,          ALL_AGG, false),
                            field("vatAmount",     "KDV Tutarı",       "NUMBER", METRIC,          ALL_AGG, false),
                            field("customer.name", "Müşteri Adı",      "TEXT",   DIMENSION,       NO_AGG, true),
                            field("customer.city", "Müşteri Şehir",    "TEXT",   DIMENSION,       NO_AGG, true),
                            field("createdAt",     "Oluşturma Tarihi", "DATE",   DATE_DIMENSION,  NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "TRANSACTION",
                    "label", "İşlemler",
                    "description", "Gelir ve gider işlemleri",
                    "fields", List.of(
                            enumField("transactionType",   "İşlem Tipi",       DIMENSION, true, List.of("INCOME","EXPENSE")),
                            field("amount",            "Tutar",            "NUMBER",  METRIC,         ALL_AGG, false),
                            field("transactionDate",   "İşlem Tarihi",     "DATE",    DATE_DIMENSION, NO_AGG, true),
                            field("category",          "Kategori",         "TEXT",    DIMENSION,      NO_AGG, true),
                            field("account.bankName",  "Banka Adı",        "TEXT",    DIMENSION,      NO_AGG, true),
                            field("isRecurring",       "Tekrarlayan",      "BOOLEAN", DIMENSION,      NO_AGG, true),
                            field("createdAt",         "Oluşturma Tarihi", "DATE",    DATE_DIMENSION, NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "CUSTOMER",
                    "label", "Müşteriler",
                    "description", "Müşteri cari hesapları",
                    "fields", List.of(
                            field("name",        "Müşteri Adı",      "TEXT", INFO,            NO_AGG, false),
                            field("email",       "E-posta",          "TEXT", INFO,            NO_AGG, false),
                            field("taxNumber",   "Vergi No",         "TEXT", INFO,            NO_AGG, false),
                            field("city",        "Şehir",            "TEXT", DIMENSION,       NO_AGG, true),
                            enumField("type",        "Müşteri Tipi",     DIMENSION, true, List.of("INDIVIDUAL","CORPORATE")),
                            field("phoneNumber", "Telefon",          "TEXT", INFO,            NO_AGG, false),
                            field("createdAt",   "Oluşturma Tarihi", "DATE", DATE_DIMENSION,  NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "PRODUCT",
                    "label", "Ürünler",
                    "description", "Stoktaki ürün tanımları",
                    "fields", List.of(
                            field("barcode",   "Barkod",            "TEXT",   INFO,            NO_AGG, false),
                            field("name",      "Ürün Adı",          "TEXT",   INFO,            NO_AGG, false),
                            field("unit",      "Birim",             "TEXT",   DIMENSION,       NO_AGG, true),
                            field("salePrice", "Satış Fiyatı",      "NUMBER", METRIC,          AVG_ONLY, false),
                            field("costPrice", "Maliyet Fiyatı",    "NUMBER", METRIC,          AVG_ONLY, false),
                            field("vatRate",   "KDV Oranı",         "NUMBER", METRIC,          AVG_ONLY, false),
                            field("createdAt", "Oluşturma Tarihi",  "DATE",   DATE_DIMENSION,  NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "STOCK",
                    "label", "Stoklar",
                    "description", "Ürün stok hareketleri ve seviyeleri",
                    "fields", List.of(
                            field("product.name",    "Ürün Adı",         "TEXT",   DIMENSION,       NO_AGG, true),
                            field("product.barcode", "Barkod",           "TEXT",   DIMENSION,       NO_AGG, true),
                            field("quantity",        "Mevcut Miktar",    "NUMBER", METRIC,          ALL_AGG, false),
                            field("minQuantity",     "Kritik Seviye",    "NUMBER", INFO,            NO_AGG, false),
                            field("lastCountDate",   "Son Sayım Tarihi", "DATE",   DATE_DIMENSION,  NO_AGG, true),
                            field("createdAt",       "Oluşturma Tarihi", "DATE",   DATE_DIMENSION,  NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "BANK_ACCOUNT",
                    "label", "Banka Hesapları",
                    "description", "Banka hesabı bilgileri",
                    "fields", List.of(
                            field("bankName",  "Banka Adı",        "TEXT", DIMENSION,      NO_AGG, true),
                            field("iban",      "IBAN",             "TEXT", INFO,           NO_AGG, false),
                            enumField("currency",  "Para Birimi",      DIMENSION, true, List.of("TRY","USD","EUR")),
                            field("createdAt", "Oluşturma Tarihi", "DATE", DATE_DIMENSION, NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "INVOICE_LINE_ITEM",
                    "label", "Fatura Kalemleri",
                    "description", "Fatura içindeki ürün satırları",
                    "fields", List.of(
                            field("invoice.invoiceNumber", "Fatura No",        "TEXT",   INFO,            NO_AGG, false),
                            enumField("invoice.invoiceType",   "Fatura Tipi",      DIMENSION, true, List.of("sale","purchase")),
                            enumField("invoice.paymentStatus", "Ödeme Durumu",     DIMENSION, true, List.of("draft","pending","paid","overdue")),
                            field("product.name",          "Ürün Adı",         "TEXT",   DIMENSION,       NO_AGG, true),
                            field("product.barcode",       "Barkod",           "TEXT",   DIMENSION,       NO_AGG, true),
                            field("quantity",              "Miktar",           "NUMBER", METRIC,          ALL_AGG, false),
                            field("unitPrice",             "Birim Fiyat",      "NUMBER", METRIC,          ALL_AGG, false),
                            field("lineTotal",             "Satır Toplamı",    "NUMBER", METRIC,          ALL_AGG, false),
                            field("createdAt",             "Oluşturma Tarihi", "DATE",   DATE_DIMENSION,  NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "TEMPLATE",
                    "label", "Şablonlar",
                    "description", "Sistem şablonları",
                    "fields", List.of(
                            field("templateCode",  "Şablon Kodu",      "TEXT", INFO,           NO_AGG, false),
                            field("templateName",  "Şablon Adı",       "TEXT", INFO,           NO_AGG, false),
                            enumField("templateType",  "Şablon Tipi",      DIMENSION, true, List.of("INCOME","EXPENSE","INVOICE","STOCK_ADJUSTMENT","CUSTOMER_TRANSACTION","BANK_TRANSFER")),
                            field("period",        "Dönem",            "TEXT", DIMENSION,      NO_AGG, true),
                            field("createdAt",     "Oluşturma Tarihi", "DATE", DATE_DIMENSION, NO_AGG, true)
                    )
            ),
            Map.of(
                    "key", "REPORT",
                    "label", "Raporlar",
                    "description", "Oluşturulan raporlar",
                    "fields", List.of(
                            enumField("reportType", "Rapor Tipi",       DIMENSION, true, List.of("PROFIT_LOSS","INCOME","EXPENSE","CASH_FLOW","AR_AGING","VAT_PREP","STOCK_STATUS","COLLECTION_PERFORMANCE","SLOW_INVENTORY","BUDGET_VARIANCE","BANK_RECONCILIATION","EXECUTIVE_SUMMARY")),
                            field("startDate",  "Başlangıç Tarihi", "DATE",   DATE_DIMENSION, NO_AGG, true),
                            field("endDate",    "Bitiş Tarihi",     "DATE",   DATE_DIMENSION, NO_AGG, true),
                            enumField("format",     "Format",           DIMENSION, true, List.of("EXCEL")),
                            field("fileSize",   "Dosya Boyutu",     "NUMBER", METRIC,         ALL_AGG, false),
                            field("createdAt",  "Oluşturma Tarihi", "DATE",   DATE_DIMENSION, NO_AGG, true)
                    )
            )
    ));

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Map<String, Object>>> getDataSources() {
        return ResponseEntity.ok(DATA_SOURCES);
    }

    @GetMapping("/{source}/fields")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<Map<String, Object>>> getFields(@PathVariable String source) {
        return DATA_SOURCES.stream()
                .filter(ds -> ds.get("key").equals(source))
                .findFirst()
                .map(ds -> ResponseEntity.ok((List<Map<String, Object>>) ds.get("fields")))
                .orElse(ResponseEntity.notFound().build());
    }

    /** METRIC olmayan veya ENUM olmayan field'lar için. */
    private static Map<String, Object> field(
            String key, String label, String type,
            String role, List<String> applicableAggregations, boolean groupable
    ) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("label", label);
        map.put("type", type);
        map.put("role", role);
        map.put("applicableAggregations", applicableAggregations);
        map.put("groupable", groupable);
        map.put("applicableFilterOps", defaultFilterOps(type));
        return map;
    }

    /** ENUM tipi için kısa yol — type ve filtre ops sabit, options eklenir. */
    private static Map<String, Object> enumField(
            String key, String label, String role, boolean groupable, List<String> options
    ) {
        Map<String, Object> map = field(key, label, "ENUM", role, NO_AGG, groupable);
        map.put("options", options);
        return map;
    }

    private static List<String> defaultFilterOps(String type) {
        return switch (type) {
            case "NUMBER"  -> List.of("EQ", "NE", "GT", "LT", "GTE", "LTE");
            case "DATE"    -> List.of("EQ", "BEFORE", "AFTER", "BETWEEN");
            case "TEXT"    -> List.of("EQ", "NE", "LIKE");
            case "ENUM"    -> List.of("EQ", "NE", "IN");
            case "BOOLEAN" -> List.of("EQ");
            default        -> List.of("EQ", "NE");
        };
    }

    /** Her DataSource için METRIC alanlarının uygulanabilir aggregation'larını birleştirip COUNT ekler. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> withSupportedAggregations(List<Map<String, Object>> sources) {
        return sources.stream().map(ds -> {
            List<Map<String, Object>> fields = (List<Map<String, Object>>) ds.get("fields");
            Set<String> agg = new LinkedHashSet<>();
            agg.add("COUNT");
            if (fields != null) {
                for (Map<String, Object> f : fields) {
                    Object apps = f.get("applicableAggregations");
                    if (apps instanceof List<?>) {
                        for (Object a : (List<?>) apps) {
                            if (a instanceof String s) agg.add(s);
                        }
                    }
                }
            }
            Map<String, Object> copy = new LinkedHashMap<>(ds);
            copy.put("supportedAggregations", new ArrayList<>(agg));
            return copy;
        }).toList();
    }
}
