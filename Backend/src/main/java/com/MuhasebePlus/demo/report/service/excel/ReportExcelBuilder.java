package com.MuhasebePlus.demo.report.service.excel;

import com.MuhasebePlus.demo.report.entity.ReportType;
import org.apache.poi.ss.usermodel.Workbook;

import java.time.LocalDate;

public interface ReportExcelBuilder {
    ReportType supports();
    void build(Workbook workbook, Long companyId, LocalDate start, LocalDate end);
}
