package com.MuhasebePlus.demo.report.service.excel;

import com.MuhasebePlus.demo.report.entity.ReportType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportExcelBuilderRegistry {

    private final Map<ReportType, ReportExcelBuilder> byType;

    public ReportExcelBuilderRegistry(List<ReportExcelBuilder> builders) {
        this.byType = builders.stream()
                .collect(Collectors.toMap(ReportExcelBuilder::supports, Function.identity()));
    }

    public ReportExcelBuilder forType(ReportType type) {
        return Optional.ofNullable(byType.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported report type: " + type));
    }
}
