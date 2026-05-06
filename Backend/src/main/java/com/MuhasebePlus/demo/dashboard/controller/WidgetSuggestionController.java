package com.MuhasebePlus.demo.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/widget-suggestions")
public class WidgetSuggestionController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getSuggestions(@RequestParam String dataSource) {
        return ResponseEntity.ok(buildSuggestions(dataSource.toUpperCase()));
    }

    private Map<String, Object> buildSuggestions(String dataSource) {
        return switch (dataSource) {
            case "INVOICE" -> Map.of(
                    "title", "Faturalar",
                    "description", "Satış ve alış faturalarınızı analiz edin. Ödeme durumu dağılımı, aylık gelir trendi veya müşteri bazlı en çok gelir getirenler gibi raporlar oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "PIE", "label", "Pasta Grafiği", "useCase", "Ödeme durumu veya fatura tipi dağılımı"),
                            Map.of("type", "DATA_CHART", "label", "Sütun/Çizgi Grafiği", "useCase", "Aylık satış/alış trendi"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Müşteri bazlı detaylı liste"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam satış tutarı veya fatura adedi")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "SUM", "field", "totalAmount", "label", "Toplam Tutar", "unit", "TRY"),
                            Map.of("function", "SUM", "field", "subtotal", "label", "Ara Toplam", "unit", "TRY"),
                            Map.of("function", "SUM", "field", "vatAmount", "label", "KDV Tutarı", "unit", "TRY"),
                            Map.of("function", "COUNT", "field", "*", "label", "Fatura Adedi", "unit", "adet")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "paymentStatus", "label", "Ödeme Durumu", "hint", "Ödenen, bekleyen, gecikmiş faturalar"),
                            Map.of("field", "invoiceType", "label", "Fatura Tipi", "hint", "Satış veya alış bazında"),
                            Map.of("field", "customer.name", "label", "Müşteri Adı", "hint", "Hangi müşteriden ne kadar gelir elde edildi"),
                            Map.of("field", "customer.city", "label", "Şehir", "hint", "Coğrafi bazlı dağılım"),
                            Map.of("field", "dueDate", "transform", "month", "label", "Vade Tarihi (Ay)", "hint", "Aylık trend analizi"),
                            Map.of("field", "dueDate", "transform", "year", "label", "Vade Tarihi (Yıl)", "hint", "Yıllık karşılaştırma")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "paymentStatus", "operator", "EQ", "label", "Ödeme durumu seçin", "type", "enum"),
                            Map.of("field", "invoiceType", "operator", "EQ", "label", "Fatura tipi seçin", "type", "enum"),
                            Map.of("field", "dueDate", "operator", "BETWEEN", "label", "Tarih aralığı seçin", "type", "date")
                    )
            );
            case "TRANSACTION" -> Map.of(
                    "title", "İşlemler",
                    "description", "Gelir ve gider hareketlerinizi analiz edin. Kategori bazlı harcama dağılımı, banka bazlı işlem özetleri veya aylık nakit akışı raporları oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "DATA_CHART", "label", "Sütun/Çizgi Grafiği", "useCase", "Aylık gelir-gider trendi"),
                            Map.of("type", "PIE", "label", "Pasta Grafiği", "useCase", "Gelir-gider oranı veya kategori dağılımı"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Banka bazlı işlem listesi"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Net gelir veya toplam gider")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "SUM", "field", "amount", "label", "Toplam Tutar", "unit", "TRY"),
                            Map.of("function", "COUNT", "field", "*", "label", "İşlem Adedi", "unit", "adet"),
                            Map.of("function", "AVG", "field", "amount", "label", "Ortalama Tutar", "unit", "TRY")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "transactionType", "label", "İşlem Tipi", "hint", "Gelir veya gider ayrımı"),
                            Map.of("field", "category", "label", "Kategori", "hint", "Harcama kategorilerine göre"),
                            Map.of("field", "account.bankName", "label", "Banka Adı", "hint", "Hangi bankadan işlem yapılmış"),
                            Map.of("field", "transactionDate", "transform", "month", "label", "İşlem Tarihi (Ay)", "hint", "Aylık trend"),
                            Map.of("field", "transactionDate", "transform", "year", "label", "İşlem Tarihi (Yıl)", "hint", "Yıllık karşılaştırma")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "transactionType", "operator", "EQ", "label", "İşlem tipi seçin", "type", "enum"),
                            Map.of("field", "category", "operator", "LIKE", "label", "Kategori ara", "type", "text"),
                            Map.of("field", "transactionDate", "operator", "BETWEEN", "label", "Tarih aralığı seçin", "type", "date")
                    )
            );
            case "CUSTOMER" -> Map.of(
                    "title", "Müşteriler",
                    "description", "Müşteri portföyünüzü analiz edin. Şehir bazlı müşteri dağılımı, müşteri tipi oranları veya yeni müşteri kazanım trendleri oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "PIE", "label", "Pasta Grafiği", "useCase", "Müşteri tipi veya şehir dağılımı"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Müşteri listesi ve detayları"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam müşteri sayısı")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "COUNT", "field", "*", "label", "Müşteri Sayısı", "unit", "müşteri")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "type", "label", "Müşteri Tipi", "hint", "Bireysel veya kurumsal"),
                            Map.of("field", "city", "label", "Şehir", "hint", "Coğrafi dağılım"),
                            Map.of("field", "createdAt", "transform", "month", "label", "Kayıt Tarihi (Ay)", "hint", "Aylık yeni müşteri trendi")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "type", "operator", "EQ", "label", "Müşteri tipi seçin", "type", "enum"),
                            Map.of("field", "city", "operator", "LIKE", "label", "Şehir ara", "type", "text")
                    )
            );
            case "PRODUCT" -> Map.of(
                    "title", "Ürünler",
                    "description", "Ürün kataloğunuzu analiz edin. Fiyat aralığı dağılımı, KDV oranları veya ürün sayısı özetleri oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Ürün listesi ve fiyatlar"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam ürün sayısı veya ortalama fiyat")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "COUNT", "field", "*", "label", "Ürün Sayısı", "unit", "ürün"),
                            Map.of("function", "AVG", "field", "salePrice", "label", "Ortalama Satış Fiyatı", "unit", "TRY"),
                            Map.of("function", "AVG", "field", "costPrice", "label", "Ortalama Maliyet", "unit", "TRY")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "unit", "label", "Birim", "hint", "Adet, kg, lt vb."),
                            Map.of("field", "vatRate", "label", "KDV Oranı", "hint", "KDV kademelerine göre"),
                            Map.of("field", "createdAt", "transform", "month", "label", "Oluşturma Tarihi (Ay)", "hint", "Yeni ürün ekleme trendi")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "name", "operator", "LIKE", "label", "Ürün adı ara", "type", "text")
                    )
            );
            case "STOCK" -> Map.of(
                    "title", "Stoklar",
                    "description", "Stok seviyelerinizi takip edin. Kritik seviyedeki ürünler, ürün bazlı stok miktarları veya stok değeri raporları oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Kritik stok listesi"),
                            Map.of("type", "DATA_CHART", "label", "Sütun Grafiği", "useCase", "Ürün bazlı stok miktarları"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam stok adedi veya kritik ürün sayısı")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "SUM", "field", "quantity", "label", "Toplam Stok Miktarı", "unit", "adet"),
                            Map.of("function", "COUNT", "field", "*", "label", "Stokta Olan Ürün Çeşidi", "unit", "ürün")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "product.name", "label", "Ürün Adı", "hint", "Hangi üründen ne kadar var"),
                            Map.of("field", "product.barcode", "label", "Barkod", "hint", "Barkod bazlı gruplama")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "quantity", "operator", "LTE", "label", "Kritik seviye ve altı", "type", "number"),
                            Map.of("field", "product.name", "operator", "LIKE", "label", "Ürün adı ara", "type", "text")
                    )
            );
            case "BANK_ACCOUNT" -> Map.of(
                    "title", "Banka Hesapları",
                    "description", "Banka hesaplarınızı analiz edin. Banka bazlı hesap sayıları, para birimi dağılımı veya IBAN yapısı özetleri oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "PIE", "label", "Pasta Grafiği", "useCase", "Para birimi dağılımı"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Banka hesap listesi"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam hesap sayısı")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "COUNT", "field", "*", "label", "Hesap Sayısı", "unit", "hesap")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "bankName", "label", "Banka Adı", "hint", "Hangi bankada kaç hesap"),
                            Map.of("field", "currency", "label", "Para Birimi", "hint", "TRY, USD, EUR dağılımı")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "currency", "operator", "EQ", "label", "Para birimi seçin", "type", "enum"),
                            Map.of("field", "bankName", "operator", "LIKE", "label", "Banka adı ara", "type", "text")
                    )
            );
            case "INVOICE_LINE_ITEM" -> Map.of(
                    "title", "Fatura Kalemleri",
                    "description", "Fatura içindeki ürün satırlarını analiz edin. En çok satan ürünler, ürün bazlı gelirler veya satış trendleri oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "TOP_N", "label", "İlk N", "useCase", "En çok satan ürünler"),
                            Map.of("type", "DATA_CHART", "label", "Sütun Grafiği", "useCase", "Ürün bazlı satış miktarları"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Detaylı satış kalemleri listesi"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam satılan adet veya gelir")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "SUM", "field", "quantity", "label", "Toplam Satılan Adet", "unit", "adet"),
                            Map.of("function", "SUM", "field", "lineTotal", "label", "Toplam Satış Tutarı", "unit", "TRY"),
                            Map.of("function", "AVG", "field", "unitPrice", "label", "Ortalama Birim Fiyat", "unit", "TRY"),
                            Map.of("function", "COUNT", "field", "*", "label", "Satır Sayısı", "unit", "satır")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "product.name", "label", "Ürün Adı", "hint", "Hangi üründen ne kadar satıldı"),
                            Map.of("field", "invoice.invoiceType", "label", "Fatura Tipi", "hint", "Satış veya alış kalemleri"),
                            Map.of("field", "invoice.paymentStatus", "label", "Ödeme Durumu", "hint", "Ödenen/bekleyen kalemler"),
                            Map.of("field", "createdAt", "transform", "month", "label", "Tarih (Ay)", "hint", "Aylık satış trendi")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "invoice.invoiceType", "operator", "EQ", "label", "Fatura tipi seçin", "type", "enum"),
                            Map.of("field", "product.name", "operator", "LIKE", "label", "Ürün adı ara", "type", "text")
                    )
            );
            case "TEMPLATE" -> Map.of(
                    "title", "Şablonlar",
                    "description", "Şablon kullanımınızı analiz edin. Şablon tipi dağılımı, en çok kullanılan şablonlar veya dönemsel şablon özetleri oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "PIE", "label", "Pasta Grafiği", "useCase", "Şablon tipi dağılımı"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Şablon listesi"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam şablon sayısı")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "COUNT", "field", "*", "label", "Şablon Sayısı", "unit", "şablon")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "templateType", "label", "Şablon Tipi", "hint", "Gelir, gider, fatura vb."),
                            Map.of("field", "period", "label", "Dönem", "hint", "Aylık, yıllık vb."),
                            Map.of("field", "createdAt", "transform", "month", "label", "Oluşturma Tarihi (Ay)", "hint", "Yeni şablon ekleme trendi")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "templateType", "operator", "EQ", "label", "Şablon tipi seçin", "type", "enum"),
                            Map.of("field", "templateName", "operator", "LIKE", "label", "Şablon adı ara", "type", "text")
                    )
            );
            case "REPORT" -> Map.of(
                    "title", "Raporlar",
                    "description", "Oluşturduğunuz raporları analiz edin. Rapor tipi dağılımı, aylık raporlama aktivitesi veya format bazlı özetler oluşturabilirsiniz.",
                    "suggestedWidgetTypes", List.of(
                            Map.of("type", "PIE", "label", "Pasta Grafiği", "useCase", "Rapor tipi dağılımı"),
                            Map.of("type", "DATA_TABLE", "label", "Tablo", "useCase", "Rapor listesi"),
                            Map.of("type", "DATA_KPI", "label", "Büyük Sayı (KPI)", "useCase", "Toplam rapor sayısı")
                    ),
                    "suggestedAggregates", List.of(
                            Map.of("function", "COUNT", "field", "*", "label", "Rapor Sayısı", "unit", "rapor"),
                            Map.of("function", "SUM", "field", "fileSize", "label", "Toplam Dosya Boyutu", "unit", "byte")
                    ),
                    "suggestedGroupBy", List.of(
                            Map.of("field", "reportType", "label", "Rapor Tipi", "hint", "Gelir-gider, cari yaşlandırma vb."),
                            Map.of("field", "format", "label", "Format", "hint", "Excel vb."),
                            Map.of("field", "createdAt", "transform", "month", "label", "Oluşturma Tarihi (Ay)", "hint", "Aylık raporlama trendi")
                    ),
                    "suggestedFilters", List.of(
                            Map.of("field", "reportType", "operator", "EQ", "label", "Rapor tipi seçin", "type", "enum"),
                            Map.of("field", "startDate", "operator", "BETWEEN", "label", "Tarih aralığı seçin", "type", "date")
                    )
            );
            default -> Map.of(
                    "title", dataSource,
                    "description", "Bu veri kaynağı için henüz öneri bulunmamaktadır.",
                    "suggestedWidgetTypes", List.of(),
                    "suggestedAggregates", List.of(),
                    "suggestedGroupBy", List.of(),
                    "suggestedFilters", List.of()
            );
        };
    }
}
