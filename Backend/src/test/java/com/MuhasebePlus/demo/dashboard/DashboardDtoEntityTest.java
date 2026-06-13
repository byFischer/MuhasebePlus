package com.MuhasebePlus.demo.dashboard;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import com.MuhasebePlus.demo.dashboard.dto.query.AggregateClause;
import com.MuhasebePlus.demo.dashboard.dto.query.AggregateFunction;
import com.MuhasebePlus.demo.dashboard.dto.query.DateTransform;
import com.MuhasebePlus.demo.dashboard.dto.query.FilterClause;
import com.MuhasebePlus.demo.dashboard.dto.query.FilterOperator;
import com.MuhasebePlus.demo.dashboard.dto.query.GroupByClause;
import com.MuhasebePlus.demo.dashboard.dto.query.HavingClause;
import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.dto.query.SortClause;
import com.MuhasebePlus.demo.dashboard.dto.query.SortDirection;
import com.MuhasebePlus.demo.dashboard.dto.request.AiWidgetChatRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardLayoutRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardPreferenceRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.DashboardWidgetRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetDefinitionRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.request.WidgetReorderRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.AiWidgetChatResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardLayoutResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardPreferenceResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.DashboardWidgetResponseDto;
import com.MuhasebePlus.demo.dashboard.dto.response.WidgetDefinitionResponseDto;
import com.MuhasebePlus.demo.dashboard.entity.AiInsightLog;
import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import com.MuhasebePlus.demo.dashboard.entity.AiWidgetRecipe;
import com.MuhasebePlus.demo.dashboard.entity.DashboardLayout;
import com.MuhasebePlus.demo.dashboard.entity.DashboardPreference;
import com.MuhasebePlus.demo.dashboard.entity.DashboardWidget;
import com.MuhasebePlus.demo.dashboard.entity.WidgetDefinition;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.mapper.DashboardMapper;
import com.MuhasebePlus.demo.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardDtoEntityTest {

    @Test
    void queryEnumsAndRecordsExposeValues() {
        assertThat(AggregateFunction.from("sum")).isEqualTo(AggregateFunction.SUM);
        assertThat(AggregateFunction.from(null)).isNull();
        assertThat(DateTransform.from("month")).isEqualTo(DateTransform.MONTH);
        assertThat(FilterOperator.from("gte")).isEqualTo(FilterOperator.GTE);
        assertThat(SortDirection.from("desc")).isEqualTo(SortDirection.DESC);
        assertThat(SortDirection.from(null)).isEqualTo(SortDirection.ASC);

        FilterClause filter = new FilterClause("amount", FilterOperator.GT, "100");
        GroupByClause group = new GroupByClause("createdAt", DateTransform.YEAR);
        AggregateClause aggregate = new AggregateClause(AggregateFunction.SUM, "amount", "value");
        HavingClause having = new HavingClause(FilterOperator.GT, "500");
        SortClause sort = new SortClause("value", SortDirection.DESC);
        QueryConfigDto query = new QueryConfigDto(
                "TRANSACTION", List.of(filter), List.of(group), aggregate, List.of(having), List.of(sort), 25);

        assertThat(query.dataSource()).isEqualTo("TRANSACTION");
        assertThat(query.filters()).containsExactly(filter);
        assertThat(query.groupBy()).containsExactly(group);
        assertThat(query.aggregate()).isEqualTo(aggregate);
        assertThat(query.having()).containsExactly(having);
        assertThat(query.sort()).containsExactly(sort);
        assertThat(query.limit()).isEqualTo(25);
    }

    @Test
    void requestAndResponseRecordsExposeValues() {
        ChatMessage chatMessage = new ChatMessage("user", "gelir");
        AiWidgetChatRequestDto chatRequest = new AiWidgetChatRequestDto(List.of(chatMessage));
        DashboardLayoutRequestDto layoutRequest = new DashboardLayoutRequestDto("Ana", true, "dark", "#fff", "LAYOUT_3");
        DashboardWidgetRequestDto widgetRequest = new DashboardWidgetRequestDto(
                WidgetType.KPI, 1L, "KPI", 2, 0, 1, 4, 3, "{}");
        WidgetDefinitionRequestDto definitionRequest = new WidgetDefinitionRequestDto(
                "Def", "Desc", WidgetType.DATA_CHART, "INVOICE", Map.of(), Map.of(), Map.of());
        WidgetReorderRequestDto reorderRequest = new WidgetReorderRequestDto(List.of(1L, 2L));

        DashboardWidgetResponseDto widgetResponse = new DashboardWidgetResponseDto(
                1L, WidgetType.KPI, 2L, "KPI", 3, 0, 1, 4, 3, "{}",
                "INVOICE", Map.of("dataSource", "INVOICE"), Map.of("chartType", "KPI"), LocalDateTime.now());
        DashboardLayoutResponseDto layoutResponse = new DashboardLayoutResponseDto(
                10L, "Ana", true, "dark", "#fff", "LAYOUT_3", List.of(widgetResponse), null, null);
        WidgetDefinitionResponseDto definitionResponse = new WidgetDefinitionResponseDto(
                2L, "Def", "Desc", WidgetType.DATA_CHART, "INVOICE",
                Map.of(), Map.of(), Map.of(), false, true, null, null);
        AiWidgetChatResponseDto chatResponse = new AiWidgetChatResponseDto(
                "ok", definitionRequest, List.of(Map.of("value", 1)), List.of(Map.of("key", "value")), true, null);

        assertThat(chatRequest.messages()).containsExactly(chatMessage);
        assertThat(layoutRequest.isDefault()).isTrue();
        assertThat(widgetRequest.widgetType()).isEqualTo(WidgetType.KPI);
        assertThat(definitionRequest.dataSource()).isEqualTo("INVOICE");
        assertThat(reorderRequest.orderedWidgetIds()).containsExactly(1L, 2L);
        assertThat(layoutResponse.widgets()).containsExactly(widgetResponse);
        assertThat(definitionResponse.isTemplate()).isTrue();
        assertThat(chatResponse.previewSuccess()).isTrue();
        assertThat(new DashboardPreferenceRequestDto()).isEqualTo(new DashboardPreferenceRequestDto());
        assertThat(new DashboardPreferenceResponseDto()).isEqualTo(new DashboardPreferenceResponseDto());
    }

    @Test
    void entitiesAndLifecycleCallbacksSetExpectedState() {
        Company company = Company.builder().companyId(1L).companyName("Test").build();
        User user = new User();
        user.setUserId(7L);

        WidgetDefinition definition = new WidgetDefinition();
        definition.setDefinitionId(5L);
        definition.setCompany(company);
        definition.setUser(user);
        definition.setName("Def");
        definition.setDescription("Desc");
        definition.setWidgetType(WidgetType.DATA_CHART);
        definition.setConfig(Map.of("color", "blue"));
        definition.setDataSource("INVOICE");
        definition.setQueryConfig(Map.of("dataSource", "INVOICE"));
        definition.setVisualConfig(Map.of("chartType", "BAR"));
        definition.setTemplate(true);
        definition.setSystem(false);
        LocalDateTime beforeUpdate = definition.getUpdatedAt();
        definition.preUpdate();

        DashboardLayout layout = new DashboardLayout();
        layout.setLayoutId(8L);
        layout.setCompany(company);
        layout.setUserId(7L);
        layout.setName("Ana");
        layout.setDefault(true);
        layout.setTheme("dark");
        layout.setAccentColor("#123");
        layout.setLayoutPreset(null);

        DashboardWidget widget = new DashboardWidget();
        widget.setWidgetId(9L);
        widget.setLayout(layout);
        widget.setDefinition(definition);
        widget.setWidgetType(WidgetType.DATA_TABLE);
        widget.setTitle("Tablo");
        widget.setPositionX(1);
        widget.setPositionY(2);
        widget.setWidth(6);
        widget.setHeight(4);
        widget.setSlotIndex(3);
        widget.setConfig("{}");
        ReflectionTestUtils.invokeMethod(widget, "onCreate");
        LocalDateTime createdAt = widget.getCreatedAt();
        ReflectionTestUtils.invokeMethod(widget, "onUpdate");

        AiQuota quota = new AiQuota();
        quota.setCompany(company);
        quota.setMonthlyTokenBudget(1000);
        quota.setTokensUsedThisMonth(10);
        ReflectionTestUtils.invokeMethod(quota, "onCreate");
        LocalDateTime quotaUpdated = quota.getUpdatedAt();
        ReflectionTestUtils.invokeMethod(quota, "onUpdate");

        AiWidgetRecipe recipe = new AiWidgetRecipe();
        recipe.setRecipeId(11L);
        recipe.setUserPrompt("prompt");
        recipe.setGeneratedConfig("{}");
        recipe.setModelUsed("model");
        recipe.setCreatedByUserId(7L);

        AiInsightLog log = new AiInsightLog();
        log.setLogId(12L);
        log.setUserId(7L);
        log.setCompany(company);
        log.setEndpoint("/api");
        log.setInputTokens(1);
        log.setOutputTokens(2);
        log.setLatencyMs(3);

        assertThat(definition.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        assertThat(layout.isDefault()).isTrue();
        assertThat(layout.getWidgets()).isEmpty();
        assertThat(widget.getCreatedAt()).isEqualTo(createdAt);
        assertThat(widget.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(quota.getResetAt()).isNotNull();
        assertThat(quota.getUpdatedAt()).isAfterOrEqualTo(quotaUpdated);
        assertThat(recipe.getModelUsed()).isEqualTo("model");
        assertThat(log.getInputTokens() + log.getOutputTokens()).isEqualTo(3);
        assertThat(new DashboardPreference()).isNotNull();
        assertThat(new DashboardMapper()).isNotNull();
    }
}
