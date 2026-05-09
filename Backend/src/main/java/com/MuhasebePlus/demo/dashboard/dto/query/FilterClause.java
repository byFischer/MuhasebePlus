package com.MuhasebePlus.demo.dashboard.dto.query;

public record FilterClause(String field, FilterOperator operator, Object value) {}
