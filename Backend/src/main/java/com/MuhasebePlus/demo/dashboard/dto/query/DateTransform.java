package com.MuhasebePlus.demo.dashboard.dto.query;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DateTransform {
    MONTH, YEAR;

    @JsonCreator
    public static DateTransform from(String value) {
        return value == null ? null : DateTransform.valueOf(value.toUpperCase());
    }
}
