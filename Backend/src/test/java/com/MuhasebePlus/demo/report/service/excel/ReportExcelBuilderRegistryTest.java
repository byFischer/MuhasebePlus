package com.MuhasebePlus.demo.report.service.excel;

import com.MuhasebePlus.demo.report.entity.ReportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportExcelBuilderRegistryTest {

    // ── Tüm ReportType değerleri kayıtlı ─────────────────────────────────────

    @Test
    void registry_coversAllReportTypes() {
        List<ReportExcelBuilder> builders = Arrays.stream(ReportType.values())
                .map(type -> {
                    ReportExcelBuilder b = mock(ReportExcelBuilder.class);
                    when(b.supports()).thenReturn(type);
                    return b;
                })
                .toList();

        ReportExcelBuilderRegistry registry = new ReportExcelBuilderRegistry(builders);

        for (ReportType type : ReportType.values()) {
            assertThatNoException()
                    .as("Builder for %s should be registered", type)
                    .isThrownBy(() -> registry.forType(type));
        }
    }

    // ── forType doğru builder'ı döner ─────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(ReportType.class)
    void forType_returnsCorrectBuilder(ReportType type) {
        ReportExcelBuilder builder = mock(ReportExcelBuilder.class);
        when(builder.supports()).thenReturn(type);

        ReportExcelBuilderRegistry registry = new ReportExcelBuilderRegistry(List.of(builder));

        assertThat(registry.forType(type)).isSameAs(builder);
    }

    // ── Kayıtlı olmayan tip → IllegalArgumentException ────────────────────────

    @Test
    void forType_whenTypeNotRegistered_throwsIllegalArgumentException() {
        ReportExcelBuilder profitLossBuilder = mock(ReportExcelBuilder.class);
        when(profitLossBuilder.supports()).thenReturn(ReportType.PROFIT_LOSS);

        ReportExcelBuilderRegistry registry = new ReportExcelBuilderRegistry(List.of(profitLossBuilder));

        assertThatThrownBy(() -> registry.forType(ReportType.INCOME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported report type");
    }

    // ── Boş registry → tüm tipler exception fırlatır ─────────────────────────

    @Test
    void forType_whenRegistryEmpty_throwsIllegalArgumentException() {
        ReportExcelBuilderRegistry registry = new ReportExcelBuilderRegistry(List.of());

        assertThatThrownBy(() -> registry.forType(ReportType.CASH_FLOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Registry builder sayısı = ReportType sayısı ───────────────────────────

    @Test
    void registry_builderCount_matchesReportTypeCount() {
        int expectedCount = ReportType.values().length;

        List<ReportExcelBuilder> builders = Arrays.stream(ReportType.values())
                .map(type -> {
                    ReportExcelBuilder b = mock(ReportExcelBuilder.class);
                    when(b.supports()).thenReturn(type);
                    return b;
                })
                .toList();

        assertThat(builders).hasSize(expectedCount);
        ReportExcelBuilderRegistry registry = new ReportExcelBuilderRegistry(builders);

        // 16 tip var, hepsine erişim başarılı olmalı
        assertThat(ReportType.values()).hasSize(16);
        for (ReportType type : ReportType.values()) {
            assertThat(registry.forType(type)).isNotNull();
        }
    }
}
