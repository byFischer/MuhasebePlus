package com.MuhasebePlus.demo.customer.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerExcelService {

    private final CompanyContext companyContext;
    private final CustomerService customerService;

    public byte[] exportToExcel() {
        List<CustomerResponseDto> customers = customerService.getAllCustomers();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Musteri Listesi");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle borderStyle = wb.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setBorderBottom(BorderStyle.THIN);
            moneyStyle.setBorderTop(BorderStyle.THIN);
            moneyStyle.setBorderLeft(BorderStyle.THIN);
            moneyStyle.setBorderRight(BorderStyle.THIN);
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            String[] headers = {"Hesap Kodu", "Ad", "Tip", "VKN/TCKN", "Vergi Dairesi", "TC Kimlik",
                    "Sehir", "Adres", "Telefon", "E-posta", "IBAN", "Doviz", "Acilis Bakiyesi",
                    "Acilis Tarihi", "Kredi Limiti", "Guncel Bakiye", "Kart Turu", "Durum", "Grup",
                    "Vadesi Gecmis"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            for (CustomerResponseDto c : customers) {
                Row row = sheet.createRow(r++);

                set(row, 0, c.accountCode(), borderStyle);
                set(row, 1, c.name(), borderStyle);
                set(row, 2, c.type(), borderStyle);
                set(row, 3, c.taxNumber(), borderStyle);
                set(row, 4, c.taxOffice(), borderStyle);
                set(row, 5, c.identityNumber(), borderStyle);
                set(row, 6, c.city(), borderStyle);
                set(row, 7, c.address(), borderStyle);
                set(row, 8, c.phoneNumber(), borderStyle);
                set(row, 9, c.email(), borderStyle);
                set(row, 10, c.iban(), borderStyle);
                set(row, 11, c.currency(), borderStyle);
                setMoney(row, 12, c.openingBalance(), moneyStyle);
                set(row, 13, c.openingBalanceDate() != null ? c.openingBalanceDate().format(dateFmt) : null, borderStyle);
                setMoney(row, 14, c.creditLimit(), moneyStyle);
                setMoney(row, 15, c.currentBalance(), moneyStyle);
                set(row, 16, c.customerRole(), borderStyle);
                set(row, 17, c.status(), borderStyle);
                set(row, 18, c.customerGroup(), borderStyle);
                set(row, 19, c.hasOverdueInvoices() ? "Evet" : "Hayir", borderStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(w, 12000));
            }

            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel olusturulamadi: " + e.getMessage(), e);
        }
    }

    private void set(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setMoney(Row row, int col, java.math.BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }
}
