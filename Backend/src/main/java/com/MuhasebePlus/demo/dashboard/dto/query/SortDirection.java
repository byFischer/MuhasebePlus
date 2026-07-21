package com.MuhasebePlus.demo.dashboard.dto.query;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SortDirection {
    ASC, DESC;

    @JsonCreator
    public static SortDirection from(String value) {
        return value == null ? ASC : SortDirection.valueOf(value.toUpperCase());
    }
}
