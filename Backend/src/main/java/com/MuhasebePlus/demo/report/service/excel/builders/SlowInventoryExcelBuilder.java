package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelStyleUtils;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import com.MuhasebePlus.demo.report.entity.ReportType;
import com.MuhasebePlus.demo.report.service.excel.ReportExcelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SlowInventoryExcelBuilder implements ReportExcelBuilder {

    private final StockRepository stockRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ReportDataFetcher fetcher;

    @Override
    public ReportType supports() { return ReportType.SLOW_INVENTORY; }

    @Override
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();

        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = fetcher.loadProductMap(companyId, stocks);

        List<Object[]> salesData = invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo);
        Map<Integer, BigDecimal> soldQtyMap = new HashMap<>();
        BigDecimal totalSold12Months = BigDecimal.ZERO;
        for (Object[] row : salesData) {
            Integer productId = (Integer) row[0];
            BigDecimal qty = BigDecimal.valueOf(((Number) row[1]).longValue());
            soldQtyMap.put(productId, qty);
            totalSold12Months = totalSold12Months.add(qty);
        }

        BigDecimal totalBoundCapital = BigDecimal.ZERO;
        long slowMovingCount = 0;
        long b1 = 0, b2 = 0, b3 = 0, b4 = 0;
        List<StockSlowItem> slowItems = new ArrayList<>();

        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product == null) continue;

            Integer qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            BigDecimal costPrice = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal boundCapital = costPrice.multiply(BigDecimal.valueOf(qty));

            Optional<LocalDateTime> lastSaleOpt = invoiceLineItemRepository
                    .findLastSaleDateByProductId(stock.getProductId(), companyId);

            long daysSinceLastSale;
            if (lastSaleOpt.isPresent()) {
                daysSinceLastSale = ChronoUnit.DAYS.between(lastSaleOpt.get().toLocalDate(), today);
            } else {
                daysSinceLastSale = stock.getCreatedAt() != null
                        ? ChronoUnit.DAYS.between(stock.getCreatedAt().toLocalDate(), today)
                        : 9999;
            }

            if (daysSinceLastSale <= 60) b1++;
            else if (daysSinceLastSale <= 90) b2++;
            else if (daysSinceLastSale <= 180) b3++;
            else b4++;

            if (daysSinceLastSale > 90) slowMovingCount++;

            totalBoundCapital = totalBoundCapital.add(boundCapital);

            BigDecimal sold12Months = soldQtyMap.getOrDefault(stock.getProductId(), BigDecimal.ZERO);
            BigDecimal avgStock = BigDecimal.valueOf(qty);
            BigDecimal turnover = avgStock.signum() > 0
                    ? sold12Months.divide(avgStock, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String status = daysSinceLastSale <= 60 ? "Aktif"
                    : daysSinceLastSale <= 90 ? "Dikkat"
                    : daysSinceLastSale <= 180 ? "Yavaş"
                    : "Risksiz";

            slowItems.add(new StockSlowItem(
                    product.getName(),
                    product.getBarcode(),
                    qty,
                    lastSaleOpt.map(LocalDateTime::toLocalDate).orElse(null),
                    daysSinceLastSale,
                    costPrice,
                    boundCapital,
                    turnover,
                    status
            ));
        }

        BigDecimal avgStockTotal = stocks.stream()
                .map(s -> BigDecimal.valueOf(s.getQuantity() != null ? s.getQuantity() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgTurnover = avgStockTotal.signum() > 0
                ? totalSold12Months.divide(avgStockTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Sheet summary = wb.createSheet("Özet");
        CellStyle bold = ExcelStyleUtils.boldStyle(wb);
        ExcelStyleUtils.writeRow(summary, 0, "Rapor Tarihi:", today.toString());
        ExcelStyleUtils.writeRow(summary, 2, "Bağlanan Para", totalBoundCapital.toPlainString());
        ExcelStyleUtils.writeRow(summary, 3, "Yavaş Hareket Eden Ürün", String.valueOf(slowMovingCount));
        ExcelStyleUtils.writeRow(summary, 4, "Stok Devir Hızı (12 ay)", avgTurnover.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Toplam Ürün", String.valueOf(stocks.size()));

        ExcelStyleUtils.writeHeaderRow(summary, 7, bold, "Bucket", "Ürün Sayısı");
        Row r1 = summary.createRow(8);  r1.createCell(0).setCellValue("0-60 gün");   r1.createCell(1).setCellValue(b1);
        Row r2 = summary.createRow(9);  r2.createCell(0).setCellValue("61-90 gün");  r2.createCell(1).setCellValue(b2);
        Row r3 = summary.createRow(10); r3.createCell(0).setCellValue("91-180 gün"); r3.createCell(1).setCellValue(b3);
        Row r4 = summary.createRow(11); r4.createCell(0).setCellValue("180+ gün");   r4.createCell(1).setCellValue(b4);
        ExcelStyleUtils.autoSize(summary, 2);

        Sheet detail = wb.createSheet("Yavaş Stok Detayı");
        ExcelStyleUtils.writeHeaderRow(detail, 0, bold,
                "Ürün", "SKU", "Mevcut Stok", "Son Satış", "Geçen Gün",
                "Maliyet", "Bağlanan Para", "Devir Hızı", "Durum");

        slowItems.sort((a, b) -> Long.compare(b.daysSinceLastSale(), a.daysSinceLastSale()));

        int rowNum = 1;
        for (StockSlowItem item : slowItems) {
            Row row = detail.createRow(rowNum++);
            row.createCell(0).setCellValue(item.productName());
            row.createCell(1).setCellValue(item.sku() != null ? item.sku() : "");
            row.createCell(2).setCellValue(item.quantity());
            row.createCell(3).setCellValue(item.lastSaleDate() != null ? item.lastSaleDate().toString() : "Satış yok");
            row.createCell(4).setCellValue(item.daysSinceLastSale());
            row.createCell(5).setCellValue(item.costPrice().doubleValue());
            row.createCell(6).setCellValue(item.boundCapital().doubleValue());
            row.createCell(7).setCellValue(item.turnover().doubleValue());
            row.createCell(8).setCellValue(item.status());
        }
        ExcelStyleUtils.autoSize(detail, 9);
    }

    private record StockSlowItem(
            String productName,
            String sku,
            int quantity,
            LocalDate lastSaleDate,
            long daysSinceLastSale,
            BigDecimal costPrice,
            BigDecimal boundCapital,
            BigDecimal turnover,
            String status
    ) {}
}
