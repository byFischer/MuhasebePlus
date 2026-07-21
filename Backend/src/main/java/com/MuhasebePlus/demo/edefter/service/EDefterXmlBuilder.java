package com.MuhasebePlus.demo.edefter.service;

import com.MuhasebePlus.demo.accounting.entity.ChartOfAccount;
import com.MuhasebePlus.demo.accounting.entity.JournalEntry;
import com.MuhasebePlus.demo.accounting.entity.JournalEntryLine;
import com.MuhasebePlus.demo.company.entity.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EDefterXmlBuilder {

    private EDefterXmlBuilder() {}

    public static String buildJournal(Company company, int year, int month,
                                      List<JournalEntry> entries) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd   = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        StringBuilder sb = new StringBuilder(8192);
        appendXmlDeclaration(sb);
        appendDocumentOpen(sb);
        appendDocumentInfo(sb, company, "journal", year, month, entries.size(), periodStart, periodEnd);
        appendEntityInfo(sb, company);

        for (JournalEntry entry : entries) {
            sb.append("  <gl-cor:entryHeader>\n");
            appendTag(sb, 4, "gl-cor:postedDate", entry.getEntryDate().toString());
            appendTag(sb, 4, "gl-cor:entryNumber", escape(entry.getEntryNumber()));
            appendTag(sb, 4, "gl-cor:entryType", entry.getSourceType().name());
            if (entry.getDescription() != null)
                appendTag(sb, 4, "gl-cor:entryComment", escape(entry.getDescription()));

            int lineOrder = 0;
            for (JournalEntryLine line : entry.getLines()) {
                sb.append("    <gl-cor:entryDetail>\n");
                appendTag(sb, 6, "gl-cor:lineNumber", String.valueOf(++lineOrder));
                ChartOfAccount acc = line.getAccount();
                if (acc != null) {
                    sb.append("      <gl-cor:account>\n");
                    appendTag(sb, 8, "gl-cor:accountMainID", escape(acc.getAccountCode()));
                    appendTag(sb, 8, "gl-cor:accountMainDescription", escape(acc.getAccountName()));
                    sb.append("      </gl-cor:account>\n");
                }
                // Positive amount: debit or credit, sign convention per XBRL-GL
                BigDecimal amount = line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0
                        ? line.getDebitAmount() : line.getCreditAmount();
                String dc = line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 ? "D" : "C";
                appendTag(sb, 6, "gl-cor:amount", amount.toPlainString());
                appendTag(sb, 6, "gl-cor:debitCreditCode", dc);
                if (line.getDescription() != null)
                    appendTag(sb, 6, "gl-cor:detailComment", escape(line.getDescription()));
                sb.append("    </gl-cor:entryDetail>\n");
            }
            sb.append("  </gl-cor:entryHeader>\n");
        }

        sb.append("</gl-cor:accountingEntries>");
        return sb.toString();
    }

    public static String buildLedger(Company company, int year, int month,
                                     List<JournalEntry> entries) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd   = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        // Group lines by account
        Map<String, LedgerAccount> ledgerMap = new TreeMap<>();
        for (JournalEntry entry : entries) {
            for (JournalEntryLine line : entry.getLines()) {
                ChartOfAccount acc = line.getAccount();
                if (acc == null) continue;
                String code = acc.getAccountCode();
                LedgerAccount la = ledgerMap.computeIfAbsent(code, k ->
                        new LedgerAccount(code, acc.getAccountName(),
                                BigDecimal.ZERO, BigDecimal.ZERO));
                la.totalDebit  = la.totalDebit.add(line.getDebitAmount());
                la.totalCredit = la.totalCredit.add(line.getCreditAmount());
            }
        }

        StringBuilder sb = new StringBuilder(4096);
        appendXmlDeclaration(sb);
        appendDocumentOpen(sb);
        appendDocumentInfo(sb, company, "ledger", year, month, ledgerMap.size(), periodStart, periodEnd);
        appendEntityInfo(sb, company);

        for (LedgerAccount la : ledgerMap.values()) {
            sb.append("  <gl-cor:entryHeader>\n");
            appendTag(sb, 4, "gl-cor:entryType", "ledger");
            sb.append("    <gl-cor:entryDetail>\n");
            sb.append("      <gl-cor:account>\n");
            appendTag(sb, 8, "gl-cor:accountMainID", escape(la.code));
            appendTag(sb, 8, "gl-cor:accountMainDescription", escape(la.name));
            sb.append("      </gl-cor:account>\n");
            appendTag(sb, 6, "gl-cor:debitCreditCode", "D");
            appendTag(sb, 6, "gl-cor:amount", la.totalDebit.toPlainString());
            sb.append("    </gl-cor:entryDetail>\n");
            sb.append("    <gl-cor:entryDetail>\n");
            sb.append("      <gl-cor:account>\n");
            appendTag(sb, 8, "gl-cor:accountMainID", escape(la.code));
            appendTag(sb, 8, "gl-cor:accountMainDescription", escape(la.name));
            sb.append("      </gl-cor:account>\n");
            appendTag(sb, 6, "gl-cor:debitCreditCode", "C");
            appendTag(sb, 6, "gl-cor:amount", la.totalCredit.toPlainString());
            sb.append("    </gl-cor:entryDetail>\n");
            sb.append("  </gl-cor:entryHeader>\n");
        }

        sb.append("</gl-cor:accountingEntries>");
        return sb.toString();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static void appendXmlDeclaration(StringBuilder sb) {
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    }

    private static void appendDocumentOpen(StringBuilder sb) {
        sb.append("<gl-cor:accountingEntries\n")
          .append("  xmlns:gl-cor=\"http://www.xbrl.org/int/gl/cor/2006-10-06\"\n")
          .append("  xmlns:gl-bus=\"http://www.xbrl.org/int/gl/bus/2006-10-06\"\n")
          .append("  xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");
    }

    private static void appendDocumentInfo(StringBuilder sb, Company company,
                                           String entriesType, int year, int month,
                                           int count, LocalDate start, LocalDate end) {
        sb.append("  <gl-cor:documentInfo>\n");
        appendTag(sb, 4, "gl-cor:entriesType", entriesType);
        appendTag(sb, 4, "gl-cor:uniqueID",
                escape(company.getTaxNumber() + "-" + year + "-" + String.format("%02d", month)));
        appendTag(sb, 4, "gl-cor:language", "tr");
        appendTag(sb, 4, "gl-cor:creationDate", LocalDate.now().toString());
        appendTag(sb, 4, "gl-cor:entriesCount", String.valueOf(count));
        appendTag(sb, 4, "gl-bus:periodCoveredStart", start.toString());
        appendTag(sb, 4, "gl-bus:periodCoveredEnd",   end.toString());
        sb.append("  </gl-cor:documentInfo>\n");
    }

    private static void appendEntityInfo(StringBuilder sb, Company company) {
        sb.append("  <gl-cor:entityInformation>\n")
          .append("    <gl-bus:organizationIdentifiers>\n");
        appendTag(sb, 6, "gl-bus:organizationIdentifier", escape(company.getTaxNumber()));
        appendTag(sb, 6, "gl-bus:organizationDescription", escape(company.getCompanyName()));
        sb.append("    </gl-bus:organizationIdentifiers>\n")
          .append("  </gl-cor:entityInformation>\n");
    }

    private static void appendTag(StringBuilder sb, int indent, String tag, String value) {
        sb.append(" ".repeat(indent))
          .append('<').append(tag).append('>')
          .append(value)
          .append("</").append(tag).append(">\n");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static final class LedgerAccount {
        final String code;
        final String name;
        BigDecimal totalDebit;
        BigDecimal totalCredit;

        LedgerAccount(String code, String name, BigDecimal totalDebit, BigDecimal totalCredit) {
            this.code = code;
            this.name = name;
            this.totalDebit = totalDebit;
            this.totalCredit = totalCredit;
        }
    }
}
