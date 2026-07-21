package com.MuhasebePlus.demo.dashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceControllerTest {

    private final DataSourceController controller = new DataSourceController();

    @Test
    void getDataSources_returnsAllMetadataWithSupportedAggregations() {
        var response = controller.getDataSources();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> sources = response.getBody();
        assertThat(sources).isNotNull().hasSize(9);
        assertThat(sources).anySatisfy(source -> {
            assertThat(source).containsEntry("key", "INVOICE");
            assertThat(source.get("supportedAggregations").toString())
                    .contains("COUNT", "SUM", "AVG", "MIN", "MAX");
        });
        assertThat(sources).anySatisfy(source -> {
            assertThat(source).containsEntry("key", "CUSTOMER");
            assertThat(source.get("supportedAggregations").toString()).isEqualTo("[COUNT]");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFields_whenSourceExists_returnsFieldDefinitionsWithFilterOps() {
        var response = controller.getFields("INVOICE");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> fields = response.getBody();
        assertThat(fields).isNotNull();
        assertThat(fields).anySatisfy(field -> {
            assertThat(field).containsEntry("key", "totalAmount");
            assertThat((List<String>) field.get("applicableFilterOps"))
                    .contains("EQ", "NE", "GT", "LT", "GTE", "LTE");
        });
        assertThat(fields).anySatisfy(field -> {
            assertThat(field).containsEntry("key", "paymentStatus");
            assertThat((List<String>) field.get("applicableFilterOps")).contains("IN");
            assertThat((List<String>) field.get("options")).contains("draft", "paid", "overdue");
        });
        assertThat(fields).anySatisfy(field -> {
            assertThat(field).containsEntry("key", "dueDate");
            assertThat((List<String>) field.get("applicableFilterOps")).contains("BETWEEN");
        });
    }

    @Test
    void getFields_whenSourceMissing_returnsNotFound() {
        var response = controller.getFields("UNKNOWN");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
