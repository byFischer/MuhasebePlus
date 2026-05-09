package com.MuhasebePlus.demo.dashboard.dto.query;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

public record QueryConfigDto(
        String dataSource,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<FilterClause> filters,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<GroupByClause> groupBy,
        AggregateClause aggregate,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<HavingClause> having,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<SortClause> sort,
        Integer limit
) {}
