package com.MuhasebePlus.demo.dashboard.dto.query;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FilterOperator {
    EQ, NE, GT, GTE, LT, LTE, BETWEEN, LIKE, IN, IS_NULL, IS_NOT_NULL;

    @JsonCreator
    public static FilterOperator from(String value) {
        return value == null ? null : FilterOperator.valueOf(value.toUpperCase());
    }
}
