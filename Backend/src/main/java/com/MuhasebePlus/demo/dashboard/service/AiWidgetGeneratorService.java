package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.entity.AiInsightLog;
import com.MuhasebePlus.demo.dashboard.entity.AiWidgetRecipe;
import com.MuhasebePlus.demo.dashboard.repository.AiInsightLogRepository;
import com.MuhasebePlus.demo.dashboard.repository.AiWidgetRecipeRepository;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AiWidgetGeneratorService {

    private static final String SYSTEM_PROMPT = """
        Sen bir Türk muhasebe uygulaması için dashboard widget konfigürasyonu üreten bir AI asistanısın.
        Kullanıcının Türkçe talebini aşağıdaki JSON şemasına dönüştür.

        Kullanılabilir widget tipleri: KPI, TIME_SERIES, PIE, TOP_N, ACTIVITY_FEED, PROGRESS_GOAL, ANOMALY, MOOD_METER
        Kullanılabilir veri kaynakları: transactions, invoices, transaction_types, invoice_types, customers
        Tarih aralıkları: THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, THIS_YEAR

        Şema:
        {
          "type": "WIDGET_TYPE",
          "title": "Widget Başlığı",
          "dataSource": "veri_kaynagi",
          "dateRange": "THIS_MONTH",
          "metric": "net_revenue|total_income|total_expense|unpaid_invoices|overdue_invoices",
          "dimension": "customers",
          "limit": 10,
          "months": 6,
          "chartType": "REVENUE|CASHFLOW",
          "goal": "10000"
        }

        Sadece geçerli JSON döndür, başka metin ekleme. JSON'dan önce veya sonra açıklama yapma.
        """;

    private final GeminiService geminiService;
    private final AiWidgetRecipeRepository recipeRepository;
    private final AiInsightLogRepository logRepository;
    private final AiQuotaGuardService quotaGuard;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String model;

    public AiWidgetGeneratorService(
            GeminiService geminiService,
            AiWidgetRecipeRepository recipeRepository,
            AiInsightLogRepository logRepository,
            AiQuotaGuardService quotaGuard,
            CompanyContext companyContext,
            CompanyRepository companyRepository,
            @Value("${app.ai.gemini-model:gemini-2.5-flash}") String model) {
        this.geminiService = geminiService;
        this.recipeRepository = recipeRepository;
        this.logRepository = logRepository;
        this.quotaGuard = quotaGuard;
        this.companyContext = companyContext;
        this.companyRepository = companyRepository;
        this.model = model;
    }

    public String generateWidgetConfig(String userPrompt) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        quotaGuard.check(companyId);

        long start = Instant.now().toEpochMilli();
        String rawResponse;
        try {
            rawResponse = geminiService.generate(SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            throw new RuntimeException("Widget üretimi başarısız: " + e.getMessage(), e);
        }
        int latency = (int) (Instant.now().toEpochMilli() - start);

        String configJson = extractJson(rawResponse);

        validateJson(configJson);

        int inputTokens = geminiService.estimateTokens(SYSTEM_PROMPT + userPrompt);
        int outputTokens = geminiService.estimateTokens(rawResponse);
        quotaGuard.recordUsage(companyId, inputTokens, outputTokens);

        AiWidgetRecipe recipe = new AiWidgetRecipe();
        recipe.setUserPrompt(userPrompt);
        recipe.setGeneratedConfig(configJson);
        recipe.setModelUsed(model);
        recipe.setCreatedByUserId(userId);
        recipeRepository.save(recipe);

        var company = companyRepository.findById(companyId).orElse(null);
        AiInsightLog log = new AiInsightLog();
        log.setUserId(userId);
        log.setCompany(company);
        log.setEndpoint("/api/ai/widgets/generate");
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setLatencyMs(latency);
        logRepository.save(log);

        return configJson;
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new RuntimeException("AI yanıtında geçerli JSON bulunamadı");
        }
        return text.substring(start, end + 1);
    }

    private void validateJson(String json) {
        try {
            var node = objectMapper.readTree(json);
            if (!node.has("type")) {
                throw new RuntimeException("AI çıktısında 'type' alanı eksik");
            }
        } catch (Exception e) {
            throw new RuntimeException("AI çıktısı geçerli JSON değil: " + e.getMessage());
        }
    }
}
