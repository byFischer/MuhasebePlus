package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import com.MuhasebePlus.demo.dashboard.dto.request.AiWidgetChatRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardLayoutRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardWidgetRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetReorderRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.AiWidgetChatResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardLayoutResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardWidgetResponseDto;
import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.service.AiQuotaGuardService;
import com.MuhasebePlus.demo.dashboard.service.AiWidgetChatService;
import com.MuhasebePlus.demo.dashboard.service.AiWidgetGeneratorService;
import com.MuhasebePlus.demo.dashboard.service.DashboardLayoutService;
import com.MuhasebePlus.demo.dashboard.service.DashboardWidgetService;
import com.MuhasebePlus.demo.dashboard.service.WidgetDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardEndpointControllersTest {

    @Mock private DashboardLayoutService layoutService;
    @Mock private DashboardWidgetService widgetService;
    @Mock private WidgetDataService widgetDataService;
    @Mock private AiWidgetGeneratorService generatorService;
    @Mock private AiWidgetChatService chatService;
    @Mock private AiQuotaGuardService quotaGuard;
    @Mock private CompanyContext companyContext;

    private DashboardLayoutController layoutController;
    private DashboardWidgetController widgetController;
    private WidgetDataController widgetDataController;
    private AiWidgetController aiWidgetController;

    @BeforeEach
    void setUp() {
        layoutController = new DashboardLayoutController(layoutService);
        widgetController = new DashboardWidgetController(widgetService);
        widgetDataController = new WidgetDataController(widgetDataService);
        aiWidgetController = new AiWidgetController(generatorService, chatService, quotaGuard, companyContext);
    }

    @Test
    void dashboardLayoutController_delegatesAllCrudEndpoints() {
        DashboardLayoutRequestDto request = new DashboardLayoutRequestDto("Ana", true, "dark", "#000", "LAYOUT_3");
        DashboardLayoutResponseDto response = new DashboardLayoutResponseDto(
                1L, "Ana", true, "dark", "#000", "LAYOUT_3", List.of(), null, null);
        when(layoutService.getLayouts()).thenReturn(List.of(response));
        when(layoutService.getDefaultLayout()).thenReturn(response);
        when(layoutService.getLayout(1L)).thenReturn(response);
        when(layoutService.createLayout(request)).thenReturn(response);
        when(layoutService.updateLayout(1L, request)).thenReturn(response);
        when(layoutService.cloneLayout(1L)).thenReturn(response);

        assertThat(layoutController.getLayouts().getBody()).containsExactly(response);
        assertThat(layoutController.getDefaultLayout().getBody()).isEqualTo(response);
        assertThat(layoutController.getLayout(1L).getBody()).isEqualTo(response);
        assertThat(layoutController.createLayout(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(layoutController.updateLayout(1L, request).getBody()).isEqualTo(response);
        assertThat(layoutController.deleteLayout(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(layoutController.cloneLayout(1L).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(layoutService).deleteLayout(1L);
    }

    @Test
    void dashboardWidgetController_delegatesWidgetEndpoints() {
        DashboardWidgetRequestDto request = new DashboardWidgetRequestDto(
                WidgetType.KPI, null, "KPI", 1, 0, 0, 4, 3, "{}");
        DashboardWidgetResponseDto response = new DashboardWidgetResponseDto(
                2L, WidgetType.KPI, null, "KPI", 1, 0, 0, 4, 3, "{}", null, null, null, null);
        WidgetReorderRequestDto reorder = new WidgetReorderRequestDto(List.of(2L));
        when(widgetService.getWidgets(1L)).thenReturn(List.of(response));
        when(widgetService.addWidget(1L, request)).thenReturn(response);
        when(widgetService.updateWidget(1L, 2L, request)).thenReturn(response);

        assertThat(widgetController.getWidgets(1L).getBody()).containsExactly(response);
        assertThat(widgetController.addWidget(1L, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(widgetController.updateWidget(1L, 2L, request).getBody()).isEqualTo(response);
        assertThat(widgetController.deleteWidget(1L, 2L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(widgetController.reorderWidgets(1L, reorder).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(widgetService).deleteWidget(1L, 2L);
        verify(widgetService).reorderWidgets(1L, reorder);
    }

    @Test
    void widgetDataController_returnsWidgetDataAndCatalogEntries() {
        when(widgetDataService.getWidgetData(1L, 2L)).thenReturn(Map.of("value", 42));

        assertThat(widgetDataController.getWidgetData(1L, 2L).getBody()).containsEntry("value", 42);
        var catalog = widgetDataController.getWidgetCatalog().getBody();

        assertThat(catalog).isNotNull();
        assertThat(catalog).hasSize(19);
        assertThat(catalog).anySatisfy(entry -> assertThat(entry).containsEntry("type", "DATA_CHART"));
    }

    @Test
    void aiWidgetController_generateHandlesValidationSuccessAndServiceFailure() {
        when(generatorService.generateWidgetConfig("gelir")).thenReturn("{\"type\":\"KPI\"}");
        when(generatorService.generateWidgetConfig("hata")).thenThrow(new RuntimeException("boom"));

        assertThat(aiWidgetController.generateWidget(Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(aiWidgetController.generateWidget(Map.of("prompt", " ")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(aiWidgetController.generateWidget(Map.of("prompt", "gelir")).getBody())
                .containsEntry("config", "{\"type\":\"KPI\"}")
                .containsEntry("prompt", "gelir");
        assertThat(aiWidgetController.generateWidget(Map.of("prompt", "hata")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void aiWidgetController_chatMapsRuntimeErrorsToHttpStatus() {
        AiWidgetChatRequestDto request = new AiWidgetChatRequestDto(List.of(new ChatMessage("user", "merhaba")));
        AiWidgetChatResponseDto response = new AiWidgetChatResponseDto("ok", null, null, null, false, null);
        when(chatService.chat(request.messages()))
                .thenReturn(response)
                .thenThrow(new RuntimeException("kota doldu"))
                .thenThrow(new RuntimeException("API anahtari yok API anahtarı yok API anahtarÄ± yok"))
                .thenThrow(new RuntimeException("baska hata"));

        assertThat(aiWidgetController.chat(request).getBody()).isEqualTo(response);
        assertThatThrownBy(() -> aiWidgetController.chat(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThatThrownBy(() -> aiWidgetController.chat(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> aiWidgetController.chat(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void aiWidgetController_getQuotaCalculatesRemainingTokens() {
        AiQuota quota = new AiQuota();
        quota.setMonthlyTokenBudget(1000);
        quota.setTokensUsedThisMonth(250);
        quota.setResetAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(quotaGuard.getQuota(1L)).thenReturn(quota);

        var response = aiWidgetController.getQuota();

        assertThat(response.getBody())
                .containsEntry("used", 250)
                .containsEntry("budget", 1000)
                .containsEntry("remaining", 750)
                .containsEntry("resetAt", "2026-07-01T00:00");
    }
}
