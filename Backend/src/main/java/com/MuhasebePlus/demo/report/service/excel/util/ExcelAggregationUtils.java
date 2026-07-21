package com.MuhasebePlus.demo.report.service.excel.util;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.invoice.entity.Invoice;

import java.math.BigDecimal;
import java.util.List;

public final class ExcelAggregationUtils {

    private ExcelAggregationUtils() {}

    public static BigDecimal sumInvoices(List<Invoice> list) {
        return list.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumInvoiceVat(List<Invoice> list) {
        return list.stream()
                .map(i -> i.getVatAmount() != null ? i.getVatAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumTransactions(List<Transaction> list) {
        return list.stream()
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
