package com.MuhasebePlus.demo.dashboard.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiWidgetPromptBuilder {

    public String buildSystemInstruction() {
        String today = LocalDate.now().toString(); // YYYY-MM-DD
        String thisMonthStart = LocalDate.now().withDayOfMonth(1).toString();
        String last30Start = LocalDate.now().minusDays(30).toString();
        String last90Start = LocalDate.now().minusDays(90).toString();
        String thisYearStart = LocalDate.now().withDayOfYear(1).toString();

        return """
                Sen MuhasebePlus adlı Türk muhasebe uygulaması için dashboard widget konfigürasyonu üreten bir AI asistanısın.
                Kullanıcının Türkçe doğal dil talebini alır, anlarsın ve uygun bir widget konfigürasyonu üretirsin.

                Bugünün tarihi: %s

                ─── ÇIKTI FORMATI ───
                Her yanıtın mutlaka bir JSON nesnesi olmalı ve şu iki alan içermeli:
                1. "assistantMessage": Kullanıcıya Türkçe açıklama/onay mesajı.
                2. "widgetDefinition": Widget konfigürasyonu (oluşturulamadıysa null bırak).

                ─── WİDGET TİPLERİ ───
                - DATA_KPI: Tek sayısal değer (toplam gelir, müşteri sayısı vb.)
                - DATA_CHART: Grafik (bar, line, pie)
                - DATA_TABLE: Veri tablosu

                ─── GÖRSEL KONFİGÜRASYON (visualConfig.chartType) ───
                - KPI → DATA_KPI ile kullan
                - BAR → kategorik karşılaştırma
                - LINE → zaman serisi / trend
                - PIE → oran/dağılım
                - TABLE → satır listesi
                - PROGRESS → hedef vs gerçekleşme

                ─── VERİ KAYNAKLARI ───
                INVOICE: totalAmount/subtotal/vatAmount (SUM/AVG/MIN/MAX) | invoiceType(sale|purchase), paymentStatus(draft|pending|paid|overdue), customer.name, customer.city | tarih: dueDate, createdAt
                TRANSACTION: amount (SUM/AVG/MIN/MAX) | transactionType(INCOME|EXPENSE), category, account.bankName, isRecurring | tarih: transactionDate, createdAt
                CUSTOMER: COUNT | city, type(INDIVIDUAL|CORPORATE) | tarih: createdAt
                PRODUCT: salePrice/costPrice/vatRate (AVG/MIN/MAX) | unit | tarih: createdAt
                STOCK: quantity (SUM/AVG/MIN/MAX) | product.name, product.barcode | tarih: lastCountDate, createdAt
                BANK_ACCOUNT: COUNT | bankName, currency(TRY|USD|EUR) | tarih: createdAt
                INVOICE_LINE_ITEM: quantity/unitPrice/lineTotal (SUM/AVG/MIN/MAX) | invoice.invoiceType, invoice.paymentStatus, product.name, product.barcode | tarih: createdAt
                TEMPLATE: COUNT | templateType, period | tarih: createdAt
                REPORT: fileSize (SUM/AVG/MIN/MAX) | reportType, format | tarih: startDate, endDate, createdAt

                ─── FILTER OPERATÖRLERI (SADECE BUNLARI KULLAN) ───
                NUMBER: EQ, NE, GT, GTE, LT, LTE
                DATE: EQ, GT, GTE, LT, LTE  (value: "YYYY-MM-DD" formatında ISO tarih stringi)
                TEXT: EQ, NE, LIKE
                ENUM: EQ, NE, IN  (IN için value tek bir JSON array string: ["val1","val2"])
                BOOLEAN: EQ  (value: "true" veya "false")

                YASAK OPERATÖRLER: BEFORE, AFTER — bunları kullanma, GT/LT kullan.

                ─── TARİH HESAPLAMA REHBERİ ───
                Bugün: %s
                Bu ayın başı: %s  → Bu ay filtresi için: transactionDate GTE "%s"
                Son 30 gün başı: %s  → Son 30 gün için: transactionDate GTE "%s"
                Son 90 gün başı: %s  → Son 90 gün için: transactionDate GTE "%s"
                Bu yılın başı: %s  → Bu yıl için: createdAt GTE "%s"

                Son N gün için: bugünden N gün öncesinin ISO tarihini hesapla ve GT/GTE operatörü kullan.

                ─── ÖNEMLI KURALLAR ───
                1. company_id, user_id, tenant_id alanlarını ASLA filtreleme.
                2. Sadece yukarıdaki dataSource ve field adlarını kullan — uydurma.
                3. Zaman serisi için groupBy + transform(MONTH/YEAR) kullan, DATE filtresi değil.
                4. Tek aggregate kullan (alias her zaman "value").
                5. Belirsiz isteklerde kullanıcıdan açıklama iste, widgetDefinition üretme.
                """.formatted(today, today, thisMonthStart, thisMonthStart,
                last30Start, last30Start, last90Start, last90Start, thisYearStart, thisYearStart);
    }

    public Map<String, Object> buildResponseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("required", List.of("assistantMessage"));

        Map<String, Object> properties = new LinkedHashMap<>();

        // assistantMessage
        properties.put("assistantMessage", Map.of("type", "STRING"));

        // widgetDefinition (nullable)
        Map<String, Object> widgetDef = buildWidgetDefinitionSchema();
        widgetDef.put("nullable", true);
        properties.put("widgetDefinition", widgetDef);

        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> buildWidgetDefinitionSchema() {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("type", "OBJECT");

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", Map.of("type", "STRING"));
        props.put("description", Map.of("type", "STRING"));
        props.put("widgetType", Map.of("type", "STRING",
                "enum", List.of("DATA_KPI", "DATA_CHART", "DATA_TABLE")));
        props.put("dataSource", Map.of("type", "STRING",
                "enum", List.of("INVOICE", "TRANSACTION", "CUSTOMER", "PRODUCT",
                        "STOCK", "BANK_ACCOUNT", "INVOICE_LINE_ITEM", "TEMPLATE", "REPORT")));
        props.put("queryConfig", buildQueryConfigSchema());
        props.put("visualConfig", buildVisualConfigSchema());

        obj.put("properties", props);
        return obj;
    }

    private Map<String, Object> buildQueryConfigSchema() {
        // Flat enough to avoid truncation; prompt explains inner structure
        Map<String, Object> filterItem = new LinkedHashMap<>();
        filterItem.put("type", "OBJECT");
        filterItem.put("properties", Map.of(
                "field", Map.of("type", "STRING"),
                "operator", Map.of("type", "STRING"),
                "value", Map.of("type", "STRING")
        ));

        Map<String, Object> groupByItem = new LinkedHashMap<>();
        groupByItem.put("type", "OBJECT");
        groupByItem.put("properties", Map.of(
                "field", Map.of("type", "STRING"),
                "transform", Map.of("type", "STRING")
        ));

        Map<String, Object> aggregateSchema = new LinkedHashMap<>();
        aggregateSchema.put("type", "OBJECT");
        aggregateSchema.put("properties", Map.of(
                "function", Map.of("type", "STRING"),
                "field", Map.of("type", "STRING"),
                "alias", Map.of("type", "STRING")
        ));

        Map<String, Object> sortItem = new LinkedHashMap<>();
        sortItem.put("type", "OBJECT");
        sortItem.put("properties", Map.of(
                "field", Map.of("type", "STRING"),
                "direction", Map.of("type", "STRING")
        ));

        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("type", "OBJECT");
        obj.put("properties", Map.of(
                "dataSource", Map.of("type", "STRING"),
                "filters", Map.of("type", "ARRAY", "items", filterItem),
                "groupBy", Map.of("type", "ARRAY", "items", groupByItem),
                "aggregate", aggregateSchema,
                "sort", Map.of("type", "ARRAY", "items", sortItem),
                "limit", Map.of("type", "INTEGER")
        ));
        return obj;
    }

    private Map<String, Object> buildVisualConfigSchema() {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("type", "OBJECT");
        obj.put("properties", Map.of(
                "chartType", Map.of("type", "STRING",
                        "enum", List.of("KPI", "BAR", "LINE", "PIE", "TABLE", "PROGRESS")),
                "title", Map.of("type", "STRING"),
                "color", Map.of("type", "STRING")
        ));
        return obj;
    }
}
