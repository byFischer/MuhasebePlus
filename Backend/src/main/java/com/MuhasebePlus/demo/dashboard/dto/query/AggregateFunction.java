package com.MuhasebePlus.demo.dashboard.dto.query;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AggregateFunction {
    COUNT, SUM, AVG, MIN, MAX;

    @JsonCreator
    public static AggregateFunction from(String value) {
        return value == null ? null : AggregateFunction.valueOf(value.toUpperCase());
    }
}
