package com.MuhasebePlus.demo.dashboard.dto.query;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FilterOperator {
    EQ, NE, GT, GTE, LT, LTE, BETWEEN, LIKE, IN, IS_NULL, IS_NOT_NULL;

    @JsonCreator
    public static FilterOperator from(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        // AI çıktılarında ara sıra görülen eşdeğer operatörleri kabul et
        return switch (normalized) {
            case "BEFORE" -> LT;
            case "AFTER" -> GT;
            default -> FilterOperator.valueOf(normalized);
        };
    }
}
