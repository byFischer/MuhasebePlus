package com.MuhasebePlus.demo.dashboard.service.provider;

import com.MuhasebePlus.demo.dashboard.dto.query.AggregateFunction;
import com.MuhasebePlus.demo.dashboard.dto.query.QueryConfigDto;
import com.MuhasebePlus.demo.dashboard.entity.WidgetType;
import com.MuhasebePlus.demo.dashboard.service.DynamicQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataQueryProviderTest {

    @Mock private DynamicQueryService dynamicQueryService;

    @InjectMocks private DataQueryProvider provider;

    @Test
    void supports_dataDrivenWidgetTypesOnly() {
        assertThat(provider.supports(WidgetType.DATA_QUERY)).isTrue();
        assertThat(provider.supports(WidgetType.DATA_CHART)).isTrue();
        assertThat(provider.supports(WidgetType.DATA_KPI)).isTrue();
        assertThat(provider.supports(WidgetType.DATA_TABLE)).isTrue();
        assertThat(provider.supports(WidgetType.KPI)).isFalse();
    }

    @Test
    void fetchData_withMapQueryConfig_returnsDynamicQueryResult() {
        DynamicQueryService.QueryResult queryResult = new DynamicQueryService.QueryResult(
                List.of(Map.of("value", 10)),
                List.of(new DynamicQueryService.ColumnMeta("value", "Deger", "NUMBER")),
                Map.of("function", AggregateFunction.SUM),
                1L);
        when(dynamicQueryService.executeQuery(any(QueryConfigDto.class), eq(1L))).thenReturn(queryResult);

        Map<String, Object> data = provider.fetchData(1L, Map.of(
                "queryConfig", Map.of("dataSource", "INVOICE", "limit", 10)));

        assertThat(data)
                .containsEntry("data", queryResult.rows())
                .containsEntry("columns", queryResult.columns())
                .containsEntry("aggregateMeta", queryResult.aggregateMeta())
                .containsEntry("totalCount", 1L);
    }

    @Test
    void fetchData_withJsonQueryConfig_parsesAndPassesItToDynamicQuery() {
        when(dynamicQueryService.executeQuery(any(QueryConfigDto.class), eq(2L)))
                .thenReturn(new DynamicQueryService.QueryResult(List.of(), List.of(), Map.of(), 0L));

        provider.fetchData(2L, Map.of("queryConfigJson", "{\"dataSource\":\"TRANSACTION\",\"limit\":3}"));

        ArgumentCaptor<QueryConfigDto> captor = ArgumentCaptor.forClass(QueryConfigDto.class);
        org.mockito.Mockito.verify(dynamicQueryService).executeQuery(captor.capture(), eq(2L));
        assertThat(captor.getValue().dataSource()).isEqualTo("TRANSACTION");
        assertThat(captor.getValue().limit()).isEqualTo(3);
    }

    @Test
    void fetchData_whenDynamicQueryFails_returnsErrorPayload() {
        when(dynamicQueryService.executeQuery(any(QueryConfigDto.class), eq(3L)))
                .thenThrow(new RuntimeException("Bad query"));

        Map<String, Object> data = provider.fetchData(3L, Map.of("queryConfigJson", "{bad"));

        assertThat(data)
                .containsEntry("error", true)
                .containsEntry("message", "Bad query");
    }
}
