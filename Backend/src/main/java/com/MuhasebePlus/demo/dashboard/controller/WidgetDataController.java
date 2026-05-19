package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.service.WidgetDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class WidgetDataController {

    private final WidgetDataService widgetDataService;

    @GetMapping("/layouts/{layoutId}/widgets/{widgetId}/data")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getWidgetData(
            @PathVariable Long layoutId,
            @PathVariable Long widgetId) {
        return ResponseEntity.ok(widgetDataService.getWidgetData(layoutId, widgetId));
    }

    @GetMapping("/widget-catalog")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<Map<String, Object>>> getWidgetCatalog() {
        List<Map<String, Object>> catalog = List.of(
            widgetEntry(WidgetType.KPI,           "KPI Kartı",          "Tek bir metriği öne çıkar (Gelir, Gider, Fatura sayısı...)", "Finansal",   4, 2),
            widgetEntry(WidgetType.TIME_SERIES,   "Zaman Serisi",       "Aylık gelir/gider trendini çizgi veya alan grafik ile göster", "Finansal",  8, 4),
            widgetEntry(WidgetType.PIE,           "Pasta Grafik",       "Gelir/gider veya fatura tipi dağılımını göster", "Finansal",              4, 4),
            widgetEntry(WidgetType.TOP_N,         "En İyi N",           "En çok ciro yapan müşterileri veya ürünleri sırala", "Operasyonel",        6, 4),
            widgetEntry(WidgetType.ACTIVITY_FEED, "Son İşlemler",       "Son finansal hareketleri liste olarak göster", "Operasyonel",             6, 5),
            widgetEntry(WidgetType.PROGRESS_GOAL, "Hedef İlerlemesi",   "Aylık gelir/gider hedefinize ne kadar yakınsınız?", "Finansal",            4, 3),
            widgetEntry(WidgetType.ANOMALY,       "Anomali Tespiti",    "Son 30 günde ortalamadan sapan olağandışı işlemleri işaretle", "Akıllı",    8, 4),
            widgetEntry(WidgetType.MOOD_METER,    "Finansal Nabız",     "Bakiye trendi ve ödenmemiş fatura oranından sağlık skoru", "Akıllı",       4, 3),
            widgetEntry(WidgetType.FORECAST,      "Nakit Akış Tahmini", "Geçmiş verilere dayalı gelecek 3 aylık projeksiyon", "AI",                 8, 4),
            widgetEntry(WidgetType.AI_INSIGHT,    "AI Analiz",          "Yapay zeka ile özel veri sorusu sor, anlık cevap al", "AI",                6, 4),
            widgetEntry(WidgetType.DAILY_BRIEF,   "Günlük Özet",        "Her gün açılışta AI tarafından hazırlanan kısa finansal bülten", "AI",      12, 2),
            widgetEntry(WidgetType.CONVERSATIONAL,"Sohbet Asistanı",    "Finansal verileriniz hakkında doğal dilde soru sor", "AI",                 6, 5),
            widgetEntry(WidgetType.QUICK_ACTIONS, "Hızlı Aksiyonlar",   "Sık kullanılan işlemlere tek tıkla eriş", "Mikro",                        4, 2),
            widgetEntry(WidgetType.PINNED_NOTE,   "Notlar",             "Dashboard'a yapıştırılabilir not alanı", "Mikro",                          4, 3),
            widgetEntry(WidgetType.HEATMAP,       "Aktivite Haritası",  "GitHub tarzı günlük işlem yoğunluğunu takvim üzerinde göster", "Görsel",   12, 4),
            widgetEntry(WidgetType.DATA_CHART,     "Özel Grafik",        "Kendi veri sorgunuzu oluşturun, bar/line/pie grafikte gösterin", "Özel",     8, 4),
            widgetEntry(WidgetType.DATA_KPI,       "Özel KPI",           "Kendi metriğinizi tanımlayın, tek değer olarak gösterin", "Özel",        4, 2),
            widgetEntry(WidgetType.DATA_TABLE,     "Özel Tablo",         "Kendi veri sorgunuzu tablo halinde listele", "Özel",              8, 5),
            widgetEntry(WidgetType.DATA_QUERY,     "Özel Sorgu",         "Tam esnek veri sorgusu ve görselleştirme", "Özel",                8, 4)
        );
        return ResponseEntity.ok(catalog);
    }

    private Map<String, Object> widgetEntry(WidgetType type, String name, String description,
                                             String category, int defaultWidth, int defaultHeight) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", type.name());
        entry.put("name", name);
        entry.put("description", description);
        entry.put("category", category);
        entry.put("defaultWidth", defaultWidth);
        entry.put("defaultHeight", defaultHeight);
        return entry;
    }
}
