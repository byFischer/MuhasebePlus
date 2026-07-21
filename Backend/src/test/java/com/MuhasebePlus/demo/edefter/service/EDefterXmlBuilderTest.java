package com.MuhasebePlus.demo.edefter.service;

import com.MuhasebePlus.demo.accounting.entity.ChartOfAccount;
import com.MuhasebePlus.demo.accounting.entity.JournalEntry;
import com.MuhasebePlus.demo.accounting.entity.JournalEntryLine;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import com.MuhasebePlus.demo.company.entity.Company;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDefterXmlBuilder, XBRL-GL biçiminde yevmiye (journal) ve kebir (ledger) XML üreten
 * saf bir yardımcı sınıftır. Bu testler dış bağımlılık olmadan XML çıktısının tüm
 * dallarını (debit/credit seçimi, null hesap/açıklama, özel karakter kaçışı,
 * gruplama, dönem hesaplama vb.) doğrular.
 */
class EDefterXmlBuilderTest {

    private Company company(String taxNumber, String name) {
        return Company.builder()
                .companyId(1L)
                .companyName(name)
                .taxNumber(taxNumber)
                .build();
    }

    private ChartOfAccount account(String code, String name) {
        ChartOfAccount acc = new ChartOfAccount();
        acc.setAccountCode(code);
        acc.setAccountName(name);
        return acc;
    }

    private JournalEntryLine line(ChartOfAccount acc, String debit, String credit, String desc) {
        JournalEntryLine l = new JournalEntryLine();
        l.setAccount(acc);
        l.setDebitAmount(new BigDecimal(debit));
        l.setCreditAmount(new BigDecimal(credit));
        l.setDescription(desc);
        return l;
    }

    private JournalEntry entry(LocalDate date, String number, JournalSourceType type,
                               String description, JournalEntryLine... lines) {
        JournalEntry e = new JournalEntry();
        e.setEntryDate(date);
        e.setEntryNumber(number);
        e.setSourceType(type);
        e.setDescription(description);
        e.setLines(new ArrayList<>(List.of(lines)));
        return e;
    }

    // ── buildJournal ──────────────────────────────────────────────────────────

    @Test
    void buildJournal_writesDeclarationRootAndNamespaces() {
        String xml = EDefterXmlBuilder.buildJournal(company("1234567890", "Test A.Ş."),
                2026, 3, List.of());

        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<gl-cor:accountingEntries");
        assertThat(xml).contains("xmlns:gl-cor=\"http://www.xbrl.org/int/gl/cor/2006-10-06\"");
        assertThat(xml).contains("xmlns:gl-bus=\"http://www.xbrl.org/int/gl/bus/2006-10-06\"");
        assertThat(xml).contains("xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        assertThat(xml).endsWith("</gl-cor:accountingEntries>");
    }

    @Test
    void buildJournal_documentInfoContainsTypePeriodAndCount() {
        ChartOfAccount acc = account("100", "Kasa");
        JournalEntry e = entry(LocalDate.of(2026, 3, 15), "YV-1",
                JournalSourceType.MANUAL, "Açılış",
                line(acc, "100.00", "0.00", "satır"));

        String xml = EDefterXmlBuilder.buildJournal(company("1234567890", "Test A.Ş."),
                2026, 3, List.of(e));

        assertThat(xml).contains("<gl-cor:entriesType>journal</gl-cor:entriesType>");
        // uniqueID = taxNumber-year-month(2 haneli)
        assertThat(xml).contains("<gl-cor:uniqueID>1234567890-2026-03</gl-cor:uniqueID>");
        assertThat(xml).contains("<gl-cor:language>tr</gl-cor:language>");
        assertThat(xml).contains("<gl-cor:entriesCount>1</gl-cor:entriesCount>");
        assertThat(xml).contains("<gl-bus:periodCoveredStart>2026-03-01</gl-bus:periodCoveredStart>");
        // Mart 31 gün
        assertThat(xml).contains("<gl-bus:periodCoveredEnd>2026-03-31</gl-bus:periodCoveredEnd>");
    }

    @Test
    void buildJournal_entityInformationContainsOrganizationIdAndName() {
        String xml = EDefterXmlBuilder.buildJournal(company("9998887776", "Acme Ltd"),
                2026, 1, List.of());

        assertThat(xml).contains("<gl-bus:organizationIdentifier>9998887776</gl-bus:organizationIdentifier>");
        assertThat(xml).contains("<gl-bus:organizationDescription>Acme Ltd</gl-bus:organizationDescription>");
    }

    @Test
    void buildJournal_writesEntryHeaderWithPostedDateNumberTypeAndComment() {
        ChartOfAccount acc = account("100", "Kasa");
        JournalEntry e = entry(LocalDate.of(2026, 2, 10), "YV-42",
                JournalSourceType.INVOICE, "Fatura kaydı",
                line(acc, "250.00", "0.00", null));

        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 2, List.of(e));

        assertThat(xml).contains("<gl-cor:entryHeader>");
        assertThat(xml).contains("<gl-cor:postedDate>2026-02-10</gl-cor:postedDate>");
        assertThat(xml).contains("<gl-cor:entryNumber>YV-42</gl-cor:entryNumber>");
        assertThat(xml).contains("<gl-cor:entryType>INVOICE</gl-cor:entryType>");
        assertThat(xml).contains("<gl-cor:entryComment>Fatura kaydı</gl-cor:entryComment>");
        assertThat(xml).contains("</gl-cor:entryHeader>");
    }

    @Test
    void buildJournal_omitsEntryCommentWhenDescriptionNull() {
        ChartOfAccount acc = account("100", "Kasa");
        JournalEntry e = entry(LocalDate.of(2026, 2, 10), "YV-1",
                JournalSourceType.MANUAL, null,
                line(acc, "10.00", "0.00", null));

        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 2, List.of(e));

        assertThat(xml).doesNotContain("<gl-cor:entryComment>");
    }

    @Test
    void buildJournal_debitLineWritesDebitCodeAndDebitAmount() {
        ChartOfAccount acc = account("100", "Kasa");
        JournalEntry e = entry(LocalDate.of(2026, 5, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(acc, "1500.75", "0.00", "borç satırı"));

        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 5, List.of(e));

        assertThat(xml).contains("<gl-cor:lineNumber>1</gl-cor:lineNumber>");
        assertThat(xml).contains("<gl-cor:accountMainID>100</gl-cor:accountMainID>");
        assertThat(xml).contains("<gl-cor:accountMainDescription>Kasa</gl-cor:accountMainDescription>");
        assertThat(xml).contains("<gl-cor:amount>1500.75</gl-cor:amount>");
        assertThat(xml).contains("<gl-cor:debitCreditCode>D</gl-cor:debitCreditCode>");
        assertThat(xml).contains("<gl-cor:detailComment>borç satırı</gl-cor:detailComment>");
    }

    @Test
    void buildJournal_creditLineWritesCreditCodeAndCreditAmount() {
        ChartOfAccount acc = account("600", "Yurtiçi Satışlar");
        JournalEntry e = entry(LocalDate.of(2026, 5, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(acc, "0.00", "999.99", null));

        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 5, List.of(e));

        assertThat(xml).contains("<gl-cor:amount>999.99</gl-cor:amount>");
        assertThat(xml).contains("<gl-cor:debitCreditCode>C</gl-cor:debitCreditCode>");
        // Açıklama null olduğunda detailComment yazılmamalı
        assertThat(xml).doesNotContain("<gl-cor:detailComment>");
    }

    @Test
    void buildJournal_incrementsLineNumberPerLine() {
        ChartOfAccount kasa = account("100", "Kasa");
        ChartOfAccount satis = account("600", "Satış");
        JournalEntry e = entry(LocalDate.of(2026, 5, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(kasa, "100.00", "0.00", null),
                line(satis, "0.00", "100.00", null));

        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 5, List.of(e));

        assertThat(xml).contains("<gl-cor:lineNumber>1</gl-cor:lineNumber>");
        assertThat(xml).contains("<gl-cor:lineNumber>2</gl-cor:lineNumber>");
    }

    @Test
    void buildJournal_skipsAccountBlockWhenAccountNull() {
        JournalEntryLine noAccount = line(null, "50.00", "0.00", null);
        JournalEntry e = entry(LocalDate.of(2026, 5, 1), "YV-1",
                JournalSourceType.MANUAL, null, noAccount);

        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 5, List.of(e));

        assertThat(xml).doesNotContain("<gl-cor:account>");
        // Tutar yine de yazılmalı
        assertThat(xml).contains("<gl-cor:amount>50.00</gl-cor:amount>");
    }

    @Test
    void buildJournal_escapesSpecialCharactersInTextValues() {
        ChartOfAccount acc = account("100", "Ar&Ge <Bölüm> \"X\" 'Y'");
        JournalEntry e = entry(LocalDate.of(2026, 5, 1), "YV-1",
                JournalSourceType.MANUAL, "açıklama & <test>",
                line(acc, "1.00", "0.00", null));

        String xml = EDefterXmlBuilder.buildJournal(company("1", "Şirket & Ortakları"),
                2026, 5, List.of(e));

        assertThat(xml).contains("Ar&amp;Ge &lt;Bölüm&gt; &quot;X&quot; &apos;Y&apos;");
        assertThat(xml).contains("açıklama &amp; &lt;test&gt;");
        assertThat(xml).contains("Şirket &amp; Ortakları");
        // Ham özel karakter kalmamalı (kaçış dışında)
        assertThat(xml).doesNotContain("Ar&Ge");
    }

    @Test
    void buildJournal_nullTextValuesEscapedAsEmptyString() {
        // taxNumber ve companyName null → escape(null) "" döndürmeli (NPE olmamalı)
        Company company = Company.builder().companyId(1L).companyName(null).taxNumber(null).build();

        String xml = EDefterXmlBuilder.buildJournal(company, 2026, 1, List.of());

        assertThat(xml).contains("<gl-bus:organizationIdentifier></gl-bus:organizationIdentifier>");
        assertThat(xml).contains("<gl-bus:organizationDescription></gl-bus:organizationDescription>");
    }

    @Test
    void buildJournal_emptyEntriesProducesValidXmlWithZeroCount() {
        String xml = EDefterXmlBuilder.buildJournal(company("1", "C"), 2026, 12, List.of());

        assertThat(xml).contains("<gl-cor:entriesCount>0</gl-cor:entriesCount>");
        assertThat(xml).doesNotContain("<gl-cor:entryHeader>");
        assertThat(xml).contains("<gl-bus:periodCoveredEnd>2026-12-31</gl-bus:periodCoveredEnd>");
    }

    // ── buildLedger ───────────────────────────────────────────────────────────

    @Test
    void buildLedger_groupsLinesByAccountAndSumsDebitCredit() {
        ChartOfAccount kasa = account("100", "Kasa");
        JournalEntry e1 = entry(LocalDate.of(2026, 4, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(kasa, "100.00", "0.00", null));
        JournalEntry e2 = entry(LocalDate.of(2026, 4, 2), "YV-2",
                JournalSourceType.MANUAL, null,
                line(kasa, "50.00", "20.00", null));

        String xml = EDefterXmlBuilder.buildLedger(company("1", "C"), 2026, 4, List.of(e1, e2));

        assertThat(xml).contains("<gl-cor:entriesType>ledger</gl-cor:entriesType>");
        // Tek hesap → entriesCount 1
        assertThat(xml).contains("<gl-cor:entriesCount>1</gl-cor:entriesCount>");
        assertThat(xml).contains("<gl-cor:entryType>ledger</gl-cor:entryType>");
        // Borç toplamı 150.00, alacak toplamı 20.00
        assertThat(xml).contains("<gl-cor:amount>150.00</gl-cor:amount>");
        assertThat(xml).contains("<gl-cor:amount>20.00</gl-cor:amount>");
        assertThat(xml).contains("<gl-cor:accountMainID>100</gl-cor:accountMainID>");
    }

    @Test
    void buildLedger_writesDebitThenCreditEntryDetailPerAccount() {
        ChartOfAccount kasa = account("100", "Kasa");
        JournalEntry e = entry(LocalDate.of(2026, 4, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(kasa, "100.00", "40.00", null));

        String xml = EDefterXmlBuilder.buildLedger(company("1", "C"), 2026, 4, List.of(e));

        int dIdx = xml.indexOf("<gl-cor:debitCreditCode>D</gl-cor:debitCreditCode>");
        int cIdx = xml.indexOf("<gl-cor:debitCreditCode>C</gl-cor:debitCreditCode>");
        assertThat(dIdx).isGreaterThan(0);
        assertThat(cIdx).isGreaterThan(dIdx); // Önce borç, sonra alacak satırı
    }

    @Test
    void buildLedger_sortsAccountsByCodeAscending() {
        ChartOfAccount kasa = account("100", "Kasa");
        ChartOfAccount satis = account("600", "Satış");
        JournalEntry e = entry(LocalDate.of(2026, 4, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(satis, "0.00", "100.00", null),
                line(kasa, "100.00", "0.00", null));

        String xml = EDefterXmlBuilder.buildLedger(company("1", "C"), 2026, 4, List.of(e));

        // İki ayrı hesap
        assertThat(xml).contains("<gl-cor:entriesCount>2</gl-cor:entriesCount>");
        // TreeMap sayesinde 100 hesabı 600'den önce gelmeli
        int idx100 = xml.indexOf("<gl-cor:accountMainID>100</gl-cor:accountMainID>");
        int idx600 = xml.indexOf("<gl-cor:accountMainID>600</gl-cor:accountMainID>");
        assertThat(idx100).isGreaterThan(0);
        assertThat(idx600).isGreaterThan(idx100);
    }

    @Test
    void buildLedger_skipsLinesWithNullAccount() {
        JournalEntryLine noAccount = line(null, "75.00", "0.00", null);
        ChartOfAccount kasa = account("100", "Kasa");
        JournalEntry e = entry(LocalDate.of(2026, 4, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                noAccount,
                line(kasa, "10.00", "0.00", null));

        String xml = EDefterXmlBuilder.buildLedger(company("1", "C"), 2026, 4, List.of(e));

        // Sadece hesabı olan satır gruplanır → tek hesap
        assertThat(xml).contains("<gl-cor:entriesCount>1</gl-cor:entriesCount>");
        assertThat(xml).contains("<gl-cor:amount>10.00</gl-cor:amount>");
        assertThat(xml).doesNotContain("<gl-cor:amount>75.00</gl-cor:amount>");
    }

    @Test
    void buildLedger_emptyEntriesProducesZeroAccounts() {
        String xml = EDefterXmlBuilder.buildLedger(company("1", "C"), 2026, 6, List.of());

        assertThat(xml).contains("<gl-cor:entriesType>ledger</gl-cor:entriesType>");
        assertThat(xml).contains("<gl-cor:entriesCount>0</gl-cor:entriesCount>");
        assertThat(xml).doesNotContain("<gl-cor:entryHeader>");
        assertThat(xml).endsWith("</gl-cor:accountingEntries>");
    }

    @Test
    void buildLedger_escapesSpecialCharactersInAccountName() {
        ChartOfAccount acc = account("100", "Kasa & Banka");
        JournalEntry e = entry(LocalDate.of(2026, 4, 1), "YV-1",
                JournalSourceType.MANUAL, null,
                line(acc, "10.00", "0.00", null));

        String xml = EDefterXmlBuilder.buildLedger(company("1", "C"), 2026, 4, List.of(e));

        assertThat(xml).contains("<gl-cor:accountMainDescription>Kasa &amp; Banka</gl-cor:accountMainDescription>");
    }
}
