package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.entity.AiWidgetRecipe;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.repository.AiInsightLogRepository;
import com.MuhasebePlus.demo.dashboard.repository.AiWidgetRecipeRepository;
import com.MuhasebePlus.demo.dashboard.dto.response.AiWidgetChatResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWidgetChatServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private AiWidgetPromptBuilder promptBuilder;
    @Mock private AiQuotaGuardService quotaGuard;
    @Mock private DynamicQueryService dynamicQueryService;
    @Mock private AiWidgetRecipeRepository recipeRepository;
    @Mock private AiInsightLogRepository logRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    private AiWidgetChatService service;

    @BeforeEach
    void setUp() {
        service = new AiWidgetChatService(
                geminiService,
                promptBuilder,
                quotaGuard,
                dynamicQueryService,
                recipeRepository,
                logRepository,
                companyContext,
                companyRepository,
                "chat-model");
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
    }

    @Test
    void chat_whenWidgetDefinitionMissing_returnsTextOnlyResponseAndStillSavesRecipe() {
        stubPrompt();
        when(geminiService.generateStructured(anyString(), anyList(), anyMap()))
                .thenReturn("{\"assistantMessage\":\"Biraz daha bilgi gerekli\"}");
        when(geminiService.estimateTokens(anyString())).thenReturn(5);
        when(companyRepository.findById(1L)).thenThrow(new RuntimeException("log db down"));

        AiWidgetChatResponseDto response = service.chat(List.of(
                new ChatMessage("assistant", "onceki"),
                new ChatMessage("user", "son soru")));

        assertThat(response.assistantMessage()).isEqualTo("Biraz daha bilgi gerekli");
        assertThat(response.widgetDefinition()).isNull();
        assertThat(response.previewSuccess()).isFalse();
        ArgumentCaptor<AiWidgetRecipe> recipeCaptor = ArgumentCaptor.forClass(AiWidgetRecipe.class);
        verify(recipeRepository).save(recipeCaptor.capture());
        assertThat(recipeCaptor.getValue().getUserPrompt()).isEqualTo("son soru");
        verify(logRepository, never()).save(any());
    }

    @Test
    void chat_whenWidgetDefinitionExists_returnsSanitizedPreviewData() {
        stubPrompt();
        Company company = Company.builder().companyId(1L).companyName("Test").build();
        when(geminiService.generateStructured(anyString(), anyList(), anyMap())).thenReturn("""
                {
                  "assistantMessage": "Hazir",
                  "widgetDefinition": {
                    "name": "Gelir grafigi",
                    "description": "Aylik gelir",
                    "widgetType": "DATA_CHART",
                    "dataSource": "INVOICE",
                    "queryConfig": {
                      "dataSource": "INVOICE",
                      "filters": [
                        {"field": "", "operator": "EQ", "value": "x"},
                        {"field": "paymentStatus", "operator": "EQ", "value": "paid"}
                      ],
                      "groupBy": [
                        {"field": "createdAt", "transform": "MONTH"},
                        {"field": "", "transform": "YEAR"}
                      ],
                      "sort": [
                        {"field": "totalAmount", "direction": "DESC"},
                        {"field": "", "direction": "ASC"}
                      ],
                      "limit": 5
                    },
                    "visualConfig": {"chartType": "BAR", "title": "Gelir"}
                  }
                }
                """);
        when(geminiService.estimateTokens(anyString())).thenReturn(12);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(dynamicQueryService.executeQuery(any(QueryConfigDto.class), eq(1L)))
                .thenReturn(new DynamicQueryService.QueryResult(
                        List.of(Map.of("value", 100)),
                        List.of(new DynamicQueryService.ColumnMeta("value", "Deger", "NUMBER")),
                        Map.of("alias", "value"),
                        1L));

        AiWidgetChatResponseDto response = service.chat(List.of(new ChatMessage("user", "geliri goster")));

        assertThat(response.previewSuccess()).isTrue();
        assertThat(response.previewData()).containsExactly(Map.of("value", 100));
        assertThat(response.previewColumns()).containsExactly(Map.of("key", "value", "label", "Deger", "type", "NUMBER"));
        assertThat(response.widgetDefinition().widgetType()).isEqualTo(WidgetType.DATA_CHART);
        assertThat(response.widgetDefinition().config()).isEmpty();
        verify(quotaGuard).recordUsage(1L, 12, 12);

        ArgumentCaptor<QueryConfigDto> queryCaptor = ArgumentCaptor.forClass(QueryConfigDto.class);
        verify(dynamicQueryService).executeQuery(queryCaptor.capture(), eq(1L));
        assertThat(queryCaptor.getValue().filters()).hasSize(1);
        assertThat(queryCaptor.getValue().groupBy()).hasSize(1);
        assertThat(queryCaptor.getValue().sort()).hasSize(1);
    }

    @Test
    void chat_whenWidgetTypeInvalid_defaultsToDataChartAndReturnsPreviewError() {
        stubPrompt();
        when(geminiService.generateStructured(anyString(), anyList(), anyMap())).thenReturn("""
                {
                  "assistantMessage": "Olusturdum",
                  "widgetDefinition": {
                    "name": "Widget",
                    "widgetType": "NOT_A_TYPE",
                    "dataSource": "TRANSACTION",
                    "queryConfig": {"dataSource": "TRANSACTION"},
                    "visualConfig": {}
                  }
                }
                """);
        when(geminiService.estimateTokens(anyString())).thenReturn(4);
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());
        when(dynamicQueryService.executeQuery(any(QueryConfigDto.class), eq(1L)))
                .thenThrow(new RuntimeException("syntax"));

        AiWidgetChatResponseDto response = service.chat(List.of(new ChatMessage("user", "grafik")));

        assertThat(response.widgetDefinition().widgetType()).isEqualTo(WidgetType.DATA_CHART);
        assertThat(response.previewSuccess()).isFalse();
        assertThat(response.previewError()).contains("syntax");
    }

    @Test
    void chat_whenGeminiReturnsInvalidJson_throwsParseExceptionAfterQuotaAccounting() {
        stubPrompt();
        when(geminiService.generateStructured(anyString(), anyList(), anyMap())).thenReturn("{bad");
        when(geminiService.estimateTokens(anyString())).thenReturn(3);

        assertThatThrownBy(() -> service.chat(List.of(new ChatMessage("user", "grafik"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JSON");

        verify(quotaGuard).recordUsage(1L, 3, 3);
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void chat_whenGeminiFails_wrapsExceptionBeforeTokenAccounting() {
        stubPrompt();
        when(geminiService.generateStructured(anyString(), anyList(), anyMap()))
                .thenThrow(new RuntimeException("unavailable"));

        assertThatThrownBy(() -> service.chat(List.of(new ChatMessage("user", "grafik"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI")
                .hasMessageContaining("unavailable");

        verify(quotaGuard, never()).recordUsage(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    private void stubPrompt() {
        when(promptBuilder.buildSystemInstruction()).thenReturn("system");
        when(promptBuilder.buildResponseSchema()).thenReturn(Map.of("type", "OBJECT"));
    }
}
