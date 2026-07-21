package com.MuhasebePlus.demo.dashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WidgetSuggestionControllerTest {

    private final WidgetSuggestionController controller = new WidgetSuggestionController();

    @Test
    void getSuggestions_returnsRichSuggestionsForEveryKnownDataSource() {
        List<String> sources = List.of(
                "INVOICE",
                "TRANSACTION",
                "CUSTOMER",
                "PRODUCT",
                "STOCK",
                "BANK_ACCOUNT",
                "INVOICE_LINE_ITEM",
                "TEMPLATE",
                "REPORT");

        for (String source : sources) {
            var response = controller.getSuggestions(source.toLowerCase());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("title")).isNotNull();
            assertThat((List<?>) body.get("suggestedWidgetTypes")).isNotEmpty();
            assertThat((List<?>) body.get("suggestedAggregates")).isNotEmpty();
            assertThat((List<?>) body.get("suggestedGroupBy")).isNotEmpty();
            assertThat((List<?>) body.get("suggestedFilters")).isNotEmpty();
        }
    }

    @Test
    void getSuggestions_whenUnknown_returnsEmptySuggestionBuckets() {
        var response = controller.getSuggestions("unknown");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("title", "UNKNOWN");
        assertThat((List<?>) body.get("suggestedWidgetTypes")).isEmpty();
        assertThat((List<?>) body.get("suggestedAggregates")).isEmpty();
        assertThat((List<?>) body.get("suggestedGroupBy")).isEmpty();
        assertThat((List<?>) body.get("suggestedFilters")).isEmpty();
    }
}
