package com.MuhasebePlus.demo.dashboard.dto.query;

public record AggregateClause(AggregateFunction function, String field, String alias) {}
