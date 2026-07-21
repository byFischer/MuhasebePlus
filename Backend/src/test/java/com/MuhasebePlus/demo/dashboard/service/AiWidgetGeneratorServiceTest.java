package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.dashboard.entity.AiInsightLog;
import com.MuhasebePlus.demo.dashboard.entity.AiWidgetRecipe;
import com.MuhasebePlus.demo.dashboard.repository.AiInsightLogRepository;
import com.MuhasebePlus.demo.dashboard.repository.AiWidgetRecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiWidgetGeneratorServiceTest {

    @Mock private GeminiService geminiService;
    @Mock private AiWidgetRecipeRepository recipeRepository;
    @Mock private AiInsightLogRepository logRepository;
    @Mock private AiQuotaGuardService quotaGuard;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    private AiWidgetGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new AiWidgetGeneratorService(
                geminiService,
                recipeRepository,
                logRepository,
                quotaGuard,
                companyContext,
                companyRepository,
                "test-model");
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
    }

    @Test
    void generateWidgetConfig_extractsJsonRecordsQuotaAndAuditRows() {
        Company company = Company.builder().companyId(1L).companyName("Test").build();
        when(geminiService.generate(anyString(), anyString()))
                .thenReturn("aciklama {\"type\":\"KPI\",\"title\":\"Gelir\"} son");
        when(geminiService.estimateTokens(anyString())).thenReturn(10);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        String result = service.generateWidgetConfig("gelir karti");

        assertThat(result).isEqualTo("{\"type\":\"KPI\",\"title\":\"Gelir\"}");
        verify(quotaGuard).check(1L);
        verify(quotaGuard).recordUsage(1L, 10, 10);

        ArgumentCaptor<AiWidgetRecipe> recipeCaptor = ArgumentCaptor.forClass(AiWidgetRecipe.class);
        verify(recipeRepository).save(recipeCaptor.capture());
        assertThat(recipeCaptor.getValue().getUserPrompt()).isEqualTo("gelir karti");
        assertThat(recipeCaptor.getValue().getGeneratedConfig()).contains("\"type\":\"KPI\"");
        assertThat(recipeCaptor.getValue().getModelUsed()).isEqualTo("test-model");
        assertThat(recipeCaptor.getValue().getCreatedByUserId()).isEqualTo(7L);

        ArgumentCaptor<AiInsightLog> logCaptor = ArgumentCaptor.forClass(AiInsightLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getCompany()).isEqualTo(company);
        assertThat(logCaptor.getValue().getEndpoint()).isEqualTo("/api/ai/widgets/generate");
    }

    @Test
    void generateWidgetConfig_whenGeminiFails_wrapsException() {
        when(geminiService.generate(anyString(), anyString()))
                .thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.generateWidgetConfig("grafik"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Widget")
                .hasMessageContaining("down");

        verify(quotaGuard).check(1L);
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void generateWidgetConfig_whenResponseHasNoJson_throwsBeforeRecordingUsage() {
        when(geminiService.generate(anyString(), anyString())).thenReturn("json yok");

        assertThatThrownBy(() -> service.generateWidgetConfig("grafik"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JSON");

        verify(quotaGuard, never()).recordUsage(anyLong(), anyInt(), anyInt());
    }

    @Test
    void generateWidgetConfig_whenJsonMissingType_throwsValidationError() {
        when(geminiService.generate(anyString(), anyString())).thenReturn("{\"title\":\"Gelir\"}");

        assertThatThrownBy(() -> service.generateWidgetConfig("grafik"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("type");

        verify(recipeRepository, never()).save(any());
    }
}
