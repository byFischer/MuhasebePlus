package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StockStatusExcelBuilder {

    private final StockRepository stockRepository;
    private final ReportDataFetcher fetcher;

    public void build(Workbook wb, Long companyId) {
        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = fetcher.loadProductMap(companyId, stocks);

        Sheet sheet = wb.createSheet("Stok Durum");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeHeaderRow(sheet, 0, bold,
                "Ürün Adı", "Barkod", "Birim", "Mevcut Stok", "Min Stok",
                "Durum", "Satış Fiyatı", "Maliyet", "Stok Değeri");

        BigDecimal grandTotal = BigDecimal.ZERO;
        int r = 1;
        for (Stock s : stocks) {
            Product p = productMap.get(s.getProductId());
            int qty = s.getQuantity() != null ? s.getQuantity() : 0;
            int minQty = s.getMinQuantity() != null ? s.getMinQuantity() : 0;
            BigDecimal sale = p != null && p.getSalePrice() != null ? p.getSalePrice() : BigDecimal.ZERO;
            BigDecimal cost = p != null && p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO;
            BigDecimal value = sale.multiply(BigDecimal.valueOf(qty));
            grandTotal = grandTotal.add(value);
            String status = (s.getMinQuantity() != null && qty < minQty) ? "Düşük" : "Normal";

            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(p != null ? ExcelStyleUtils.safeStr(p.getName()) : "Ürün #" + s.getProductId());
            row.createCell(1).setCellValue(p != null ? ExcelStyleUtils.safeStr(p.getBarcode()) : "");
            row.createCell(2).setCellValue(p != null ? ExcelStyleUtils.safeStr(p.getUnit()) : "");
            row.createCell(3).setCellValue(qty);
            row.createCell(4).setCellValue(minQty);
            row.createCell(5).setCellValue(status);
            row.createCell(6).setCellValue(sale.doubleValue());
            row.createCell(7).setCellValue(cost.doubleValue());
            row.createCell(8).setCellValue(value.doubleValue());
        }

        Row totalRow = sheet.createRow(r + 1);
        Cell tc0 = totalRow.createCell(0); tc0.setCellValue("Toplam Stok Değeri"); tc0.setCellStyle(bold);
        Cell tc1 = totalRow.createCell(8); tc1.setCellValue(grandTotal.doubleValue()); tc1.setCellStyle(bold);
        ExcelStyleUtils.autoSize(sheet, 9);
    }
}
