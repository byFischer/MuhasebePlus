package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import com.MuhasebePlus.demo.dashboard.dto.query.FilterClause;
import com.MuhasebePlus.demo.dashboard.dto.query.GroupByClause;
import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.dto.query.SortClause;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetDefinitionRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.AiWidgetChatResponseDto;
import com.MuhasebePlus.demo.dashboard.entity.AiInsightLog;
import com.MuhasebePlus.demo.dashboard.entity.AiWidgetRecipe;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.repository.AiInsightLogRepository;
import com.MuhasebePlus.demo.dashboard.repository.AiWidgetRecipeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiWidgetChatService {

    private final GeminiService geminiService;
    private final AiWidgetPromptBuilder promptBuilder;
    private final AiQuotaGuardService quotaGuard;
    private final DynamicQueryService dynamicQueryService;
    private final AiWidgetRecipeRepository recipeRepository;
    private final AiInsightLogRepository logRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String model;

    public AiWidgetChatService(
            GeminiService geminiService,
            AiWidgetPromptBuilder promptBuilder,
            AiQuotaGuardService quotaGuard,
            DynamicQueryService dynamicQueryService,
            AiWidgetRecipeRepository recipeRepository,
            AiInsightLogRepository logRepository,
            CompanyContext companyContext,
            CompanyRepository companyRepository,
            @Value("${app.ai.gemini-model:gemini-2.5-flash}") String model) {
        this.geminiService = geminiService;
        this.promptBuilder = promptBuilder;
        this.quotaGuard = quotaGuard;
        this.dynamicQueryService = dynamicQueryService;
        this.recipeRepository = recipeRepository;
        this.logRepository = logRepository;
        this.companyContext = companyContext;
        this.companyRepository = companyRepository;
        this.model = model;
    }

    public AiWidgetChatResponseDto chat(List<ChatMessage> messages) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        quotaGuard.check(companyId);

        String systemInstruction = promptBuilder.buildSystemInstruction();
        Map<String, Object> responseSchema = promptBuilder.buildResponseSchema();

        long start = Instant.now().toEpochMilli();
        String rawJson;
        try {
            rawJson = geminiService.generateStructured(systemInstruction, messages, responseSchema);
        } catch (Exception e) {
            throw new RuntimeException("AI çağrısı başarısız: " + e.getMessage(), e);
        }
        int latencyMs = (int) (Instant.now().toEpochMilli() - start);

        // Token accounting
        String allText = messages.stream().map(ChatMessage::content).reduce("", String::concat);
        int inputTokens = geminiService.estimateTokens(systemInstruction + allText);
        int outputTokens = geminiService.estimateTokens(rawJson);
        quotaGuard.recordUsage(companyId, inputTokens, outputTokens);

        // Parse Gemini response
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new RuntimeException("AI yanıtı geçerli JSON değil: " + e.getMessage(), e);
        }

        String assistantMessage = root.path("assistantMessage").asText("Anlayamadım, lütfen tekrar deneyin.");
        JsonNode defNode = root.path("widgetDefinition");

        // Extract last user prompt for logging
        String lastUserPrompt = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((first, second) -> second)
                .map(ChatMessage::content)
                .orElse("");

        // Save recipe & insight log
        saveAuditLog(lastUserPrompt, rawJson, userId, companyId, "/api/ai/widgets/chat",
                inputTokens, outputTokens, latencyMs);

        // If no widget definition, return text-only response
        if (defNode == null || defNode.isNull() || defNode.isMissingNode()) {
            return new AiWidgetChatResponseDto(assistantMessage, null, null, null, false, null);
        }

        // Parse widget definition
        WidgetDefinitionRequestDto widgetDef = parseWidgetDefinition(defNode);
        if (widgetDef == null) {
            return new AiWidgetChatResponseDto(assistantMessage, null, null, null, false,
                    "Widget konfigürasyonu ayrıştırılamadı.");
        }

        // Run preview query
        List<Map<String, Object>> previewRows = null;
        List<Map<String, Object>> previewColumns = null;
        boolean previewSuccess = false;
        String previewError = null;

        try {
            QueryConfigDto queryConfig = sanitize(objectMapper.convertValue(widgetDef.queryConfig(), QueryConfigDto.class));
            DynamicQueryService.QueryResult result = dynamicQueryService.executeQuery(queryConfig, companyId);
            previewRows = result.rows();
            previewColumns = result.columns().stream()
                    .map(col -> Map.<String, Object>of(
                            "key", col.key(),
                            "label", col.label(),
                            "type", col.type()))
                    .toList();
            previewSuccess = true;
        } catch (Exception e) {
            previewError = "Önizleme başarısız: " + e.getMessage();
        }

        return new AiWidgetChatResponseDto(
                assistantMessage, widgetDef, previewRows, previewColumns, previewSuccess, previewError);
    }

    @SuppressWarnings("unchecked")
    private WidgetDefinitionRequestDto parseWidgetDefinition(JsonNode node) {
        try {
            String name = node.path("name").asText("AI Widget");
            String description = node.path("description").asText(null);
            String widgetTypeStr = node.path("widgetType").asText("DATA_CHART");
            String dataSource = node.path("dataSource").asText("");

            WidgetType widgetType;
            try {
                widgetType = WidgetType.valueOf(widgetTypeStr);
            } catch (IllegalArgumentException e) {
                widgetType = WidgetType.DATA_CHART;
            }

            Map<String, Object> queryConfig = objectMapper.convertValue(node.path("queryConfig"), Map.class);
            Map<String, Object> visualConfig = objectMapper.convertValue(node.path("visualConfig"), Map.class);

            return new WidgetDefinitionRequestDto(name, description, widgetType, dataSource,
                    queryConfig != null ? queryConfig : Map.of(),
                    visualConfig != null ? visualConfig : Map.of(),
                    Map.of());
        } catch (Exception e) {
            return null;
        }
    }

    private QueryConfigDto sanitize(QueryConfigDto q) {
        if (q == null) return null;
        List<FilterClause> filters = q.filters() == null ? List.of() :
                q.filters().stream()
                        .filter(f -> f != null && f.field() != null && !f.field().isBlank() && f.operator() != null)
                        .toList();
        List<GroupByClause> groupBy = q.groupBy() == null ? List.of() :
                q.groupBy().stream()
                        .filter(g -> g != null && g.field() != null && !g.field().isBlank())
                        .toList();
        List<SortClause> sort = q.sort() == null ? List.of() :
                q.sort().stream()
                        .filter(s -> s != null && s.field() != null && !s.field().isBlank())
                        .toList();
        return new QueryConfigDto(q.dataSource(), filters, groupBy, q.aggregate(), q.having(), sort, q.limit());
    }

    private void saveAuditLog(String userPrompt, String generatedConfig, Long userId, Long companyId,
                               String endpoint, int inputTokens, int outputTokens, int latencyMs) {
        AiWidgetRecipe recipe = new AiWidgetRecipe();
        recipe.setUserPrompt(userPrompt);
        recipe.setGeneratedConfig(generatedConfig);
        recipe.setModelUsed(model);
        recipe.setCreatedByUserId(userId);
        recipeRepository.save(recipe);

        try {
            var company = companyRepository.findById(companyId).orElse(null);
            AiInsightLog auditLog = new AiInsightLog();
            auditLog.setUserId(userId);
            auditLog.setCompany(company);
            auditLog.setEndpoint(endpoint);
            auditLog.setInputTokens(inputTokens);
            auditLog.setOutputTokens(outputTokens);
            auditLog.setLatencyMs(latencyMs);
            logRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write AI insight audit log userId={} companyId={}", userId, companyId, e);
        }
    }
}
