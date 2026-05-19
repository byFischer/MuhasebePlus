package com.MuhasebePlus.demo.report.service.excel.builders;

import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.report.service.excel.data.ReportDataFetcher;
import com.MuhasebePlus.demo.report.service.excel.util.ExcelAggregationUtils;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExecutiveSummaryExcelBuilder {

    private final ReportDataFetcher fetcher;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final StockRepository stockRepository;
    private final CustomerRepository customerRepository;

    @SuppressWarnings("null")
    public void build(Workbook wb, Long companyId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();

        List<Invoice> paidSales = fetcher.fetchPaidInvoices(companyId, InvoiceType.sale, start, end);
        List<Invoice> paidPurchases = fetcher.fetchPaidInvoices(companyId, InvoiceType.purchase, start, end);
        List<Transaction> incomes = fetcher.fetchTransactions(companyId, TransactionType.INCOME, start, end);
        List<Transaction> expenses = fetcher.fetchTransactions(companyId, TransactionType.EXPENSE, start, end);
        BigDecimal totalIncome = ExcelAggregationUtils.sumInvoices(paidSales).add(ExcelAggregationUtils.sumTransactions(incomes));
        BigDecimal totalExpense = ExcelAggregationUtils.sumInvoices(paidPurchases).add(ExcelAggregationUtils.sumTransactions(expenses));
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal margin = totalIncome.signum() > 0
                ? netProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue, InvoiceType.sale, companyId));

        BigDecimal arTotal = BigDecimal.ZERO;
        BigDecimal ar90Plus = BigDecimal.ZERO;
        BigDecimal overdueAmt = BigDecimal.ZERO;
        Map<Long, BigDecimal> openByCustomer = new HashMap<>();
        Map<Long, Long> maxAgingByCustomer = new HashMap<>();

        for (Invoice inv : openInvoices) {
            BigDecimal amt = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            arTotal = arTotal.add(amt);
            long days = inv.getDueDate() != null ? java.time.temporal.ChronoUnit.DAYS.between(inv.getDueDate(), today) : 0;
            if (days > 0) overdueAmt = overdueAmt.add(amt);
            if (days > 90) ar90Plus = ar90Plus.add(amt);
            if (inv.getCustomerId() != null) {
                openByCustomer.merge(inv.getCustomerId(), amt, BigDecimal::add);
                maxAgingByCustomer.merge(inv.getCustomerId(), days, (a, b) -> Math.max(a, b));
            }
        }
        BigDecimal overduePct = arTotal.signum() > 0
                ? overdueAmt.multiply(BigDecimal.valueOf(100)).divide(arTotal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Stock> stocks = stockRepository.findActiveStocks(companyId);
        Map<Integer, Product> productMap = fetcher.loadProductMap(companyId, stocks);
        LocalDateTime twelveMonthsAgo = today.minusMonths(12).atStartOfDay();
        List<Object[]> salesRows = invoiceLineItemRepository.sumQuantityByProductLast12Months(companyId, twelveMonthsAgo);
        Map<Integer, BigDecimal> soldQtyMap = new HashMap<>();
        BigDecimal totalSold12M = BigDecimal.ZERO;
        for (Object[] row : salesRows) {
            BigDecimal qty = BigDecimal.valueOf(((Number) row[1]).longValue());
            soldQtyMap.put((Integer) row[0], qty);
            totalSold12M = totalSold12M.add(qty);
        }
        BigDecimal boundCapital = BigDecimal.ZERO;
        long slowMoving = 0;
        List<Object[]> riskProducts = new ArrayList<>();
        for (Stock stock : stocks) {
            Product product = productMap.get(stock.getProductId());
            if (product == null) continue;
            int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
            BigDecimal cost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal bc = cost.multiply(BigDecimal.valueOf(qty));
            boundCapital = boundCapital.add(bc);
            boolean isSlow = !soldQtyMap.containsKey(stock.getProductId())
                    || soldQtyMap.get(stock.getProductId()).compareTo(BigDecimal.valueOf(1)) < 0;
            if (isSlow) {
                slowMoving++;
                if (qty > 0) riskProducts.add(new Object[]{product.getName(), bc, qty});
            }
        }
        BigDecimal avgStockTotal = stocks.stream()
                .map(s -> BigDecimal.valueOf(s.getQuantity() != null ? s.getQuantity() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgTurnover = avgStockTotal.signum() > 0
                ? totalSold12M.divide(avgStockTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        CellStyle bold = ExcelStyleUtils.boldStyle(wb);

        Sheet summary = wb.createSheet("Yönetici Özeti");
        ExcelStyleUtils.writeRow(summary, 0, "Tarih Aralığı:", start + " - " + end);
        ExcelStyleUtils.writeRow(summary, 1, "Rapor Tarihi:", today.toString());

        Row h1 = summary.createRow(3); Cell hc1 = h1.createCell(0); hc1.setCellValue("KARLILıK"); hc1.setCellStyle(bold);
        ExcelStyleUtils.writeRow(summary, 4, "Toplam Gelir", totalIncome.toPlainString());
        ExcelStyleUtils.writeRow(summary, 5, "Toplam Gider", totalExpense.toPlainString());
        ExcelStyleUtils.writeRow(summary, 6, "Net Kar", netProfit.toPlainString());
        ExcelStyleUtils.writeRow(summary, 7, "Kar Marjı %", margin.toPlainString());

        Row h2 = summary.createRow(9); Cell hc2 = h2.createCell(0); hc2.setCellValue("TAHSİLAT"); hc2.setCellStyle(bold);
        ExcelStyleUtils.writeRow(summary, 10, "Toplam Açık AR", arTotal.toPlainString());
        ExcelStyleUtils.writeRow(summary, 11, "Vadesi Geçmiş %", overduePct.toPlainString());
        ExcelStyleUtils.writeRow(summary, 12, "90+ Risk Tutarı", ar90Plus.toPlainString());

        Row h3 = summary.createRow(14); Cell hc3 = h3.createCell(0); hc3.setCellValue("STOK"); hc3.setCellStyle(bold);
        ExcelStyleUtils.writeRow(summary, 15, "Bağlanan Para", boundCapital.toPlainString());
        ExcelStyleUtils.writeRow(summary, 16, "Yavaş Hareket Eden", String.valueOf(slowMoving));
        ExcelStyleUtils.writeRow(summary, 17, "Devir Hızı (12 ay)", avgTurnover.toPlainString());
        ExcelStyleUtils.autoSize(summary, 2);

        Map<Long, String> customerNames = new HashMap<>();
        if (!openByCustomer.isEmpty()) {
            customerRepository.findAllById(openByCustomer.keySet())
                    .forEach(c -> customerNames.put(c.getCustomerId(), c.getName()));
        }
        Sheet custSheet = wb.createSheet("Risk Müşteriler");
        ExcelStyleUtils.writeHeaderRow(custSheet, 0, bold, "Müşteri", "Açık Tutar", "Maks. Gecikme (gün)");
        List<Map.Entry<Long, BigDecimal>> sortedCustomers = new ArrayList<>(openByCustomer.entrySet());
        sortedCustomers.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        int cr = 1;
        for (Map.Entry<Long, BigDecimal> e : sortedCustomers.stream().limit(10).toList()) {
            Row row = custSheet.createRow(cr++);
            row.createCell(0).setCellValue(customerNames.getOrDefault(e.getKey(), "Müşteri #" + e.getKey()));
            row.createCell(1).setCellValue(e.getValue().doubleValue());
            row.createCell(2).setCellValue(maxAgingByCustomer.getOrDefault(e.getKey(), 0L));
        }
        ExcelStyleUtils.autoSize(custSheet, 3);

        riskProducts.sort((a, b) -> ((BigDecimal) b[1]).compareTo((BigDecimal) a[1]));
        Sheet prodSheet = wb.createSheet("Risk Ürünler");
        ExcelStyleUtils.writeHeaderRow(prodSheet, 0, bold, "Ürün", "Bağlanan Para", "Stok Adedi");
        int pr = 1;
        for (Object[] item : riskProducts.stream().limit(10).toList()) {
            Row row = prodSheet.createRow(pr++);
            row.createCell(0).setCellValue(item[0] != null ? (String) item[0] : "");
            row.createCell(1).setCellValue(((BigDecimal) item[1]).doubleValue());
            row.createCell(2).setCellValue((int) item[2]);
        }
        ExcelStyleUtils.autoSize(prodSheet, 3);
    }
}
