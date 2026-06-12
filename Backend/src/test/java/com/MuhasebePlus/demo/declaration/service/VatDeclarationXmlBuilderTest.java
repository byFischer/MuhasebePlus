package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.declaration.entity.VatDeclaration;
import com.MuhasebePlus.demo.declaration.entity.VatDeclarationLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the KDV declaration XML builder: section/field presence,
 * Table I vs Table II line routing, XML escaping (Turkish characters must pass
 * through unescaped) and 2-decimal HALF_UP amount formatting.
 */
class VatDeclarationXmlBuilderTest {

    // ── build: alanların varlığı ──────────────────────────────────────────────

    @Test
    void build_includesXmlHeaderTaxpayerAndPeriodFields() {
        String xml = VatDeclarationXmlBuilder.build(declaration(2026, 5, List.of()), company());

        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<KDVBeyannamesi>").contains("</KDVBeyannamesi>");
        assertThat(xml).contains("<VergiKimlikNo>1234567890</VergiKimlikNo>");
        assertThat(xml).contains("<VergiDairesi>Kadıköy</VergiDairesi>");
        assertThat(xml).contains("<Unvan>Test A.Ş.</Unvan>");
        assertThat(xml).contains("<Yil>2026</Yil>");
        // ay her zaman iki haneli yazılır
        assertThat(xml).contains("<Ay>05</Ay>");
    }

    @Test
    void build_includesSummaryTotalsFromDeclaration() {
        VatDeclaration decl = declaration(2026, 5, List.of());
        decl.setTotalSalesBase(new BigDecimal("1500.00"));
        decl.setTotalSalesVat(new BigDecimal("250.00"));
        decl.setTotalPurchasesVat(new BigDecimal("70.00"));
        decl.setPreviousPeriodCarryover(new BigDecimal("20.00"));
        decl.setPayableVat(new BigDecimal("160.00"));
        decl.setNextPeriodCarryover(BigDecimal.ZERO);

        String xml = VatDeclarationXmlBuilder.build(decl, company());

        assertThat(xml).contains("<ToplamSatisMatrahi>1500.00</ToplamSatisMatrahi>");
        assertThat(xml).contains("<HesaplananKDV>250.00</HesaplananKDV>");
        assertThat(xml).contains("<IndirilecekKDV>70.00</IndirilecekKDV>");
        assertThat(xml).contains("<DevredenKDV>20.00</DevredenKDV>");
        assertThat(xml).contains("<OdenecekKDV>160.00</OdenecekKDV>");
        assertThat(xml).contains("<SonrakiDonemeDevreden>0.00</SonrakiDonemeDevreden>");
    }

    @Test
    void build_routesLinesToTable1AndTable2_andOmitsT3Lines() {
        VatDeclaration decl = declaration(2026, 5, List.of(
                line("39", "%20 oranındaki satışlar", "1000.00", "200.00"),
                line("T2_108", "İndirilecek KDV", "0.00", "70.00"),
                line("T3_999", "Tablo III satırı", "0.00", "5.00")));

        String xml = VatDeclarationXmlBuilder.build(decl, company());

        // Tablo I: T2_/T3_ öneki olmayan satırlar, Matrah + Vergi alanlarıyla
        assertThat(xml).contains("<Satir kod=\"39\">");
        assertThat(xml).contains("<Matrah>1000.00</Matrah>");
        assertThat(xml).contains("<Vergi>200.00</Vergi>");
        // Tablo II: T2_ öneki kırpılır, yalnızca Tutar yazılır
        assertThat(xml).contains("<Satir kod=\"108\">");
        assertThat(xml).contains("<Tutar>70.00</Tutar>");
        assertThat(xml).doesNotContain("T2_108");
        // T3_ satırları hiçbir tabloda yer almaz
        assertThat(xml).doesNotContain("T3_999");
        assertThat(xml).doesNotContain("Tablo III satırı");
    }

    // ── esc: özel karakterler ve Türkçe karakterler ──────────────────────────

    @Test
    void build_escapesXmlSpecialCharactersInCompanyAndLineFields() {
        Company company = company();
        company.setCompanyName("Şen & Oğulları <Ltd> \"Şti\"");
        VatDeclaration decl = declaration(2026, 5, List.of(
                line("1", "Mal & hizmet <satışı>", "100.00", "20.00")));

        String xml = VatDeclarationXmlBuilder.build(decl, company);

        assertThat(xml).contains("<Unvan>Şen &amp; Oğulları &lt;Ltd&gt; &quot;Şti&quot;</Unvan>");
        assertThat(xml).contains("<Aciklama>Mal &amp; hizmet &lt;satışı&gt;</Aciklama>");
    }

    @Test
    void build_keepsTurkishCharactersUnescaped() {
        Company company = company();
        company.setCompanyName("ĞÜŞİÖÇ ğüşıöç A.Ş.");
        company.setTaxOffice("Çankaya");

        String xml = VatDeclarationXmlBuilder.build(declaration(2026, 12, List.of()), company);

        assertThat(xml).contains("<Unvan>ĞÜŞİÖÇ ğüşıöç A.Ş.</Unvan>");
        assertThat(xml).contains("<VergiDairesi>Çankaya</VergiDairesi>");
        assertThat(xml).doesNotContain("&#");
    }

    // ── fmt: tutar biçimlendirme ──────────────────────────────────────────────

    @Test
    void build_formatsAmountsWithTwoDecimalsUsingHalfUpRounding() {
        VatDeclaration decl = declaration(2026, 5, List.of(
                line("1", "Yuvarlama testi", "1234.567", "0.005")));
        decl.setTotalSalesBase(new BigDecimal("10"));

        String xml = VatDeclarationXmlBuilder.build(decl, company());

        // 1234.567 -> HALF_UP -> 1234.57 ; 0.005 -> 0.01 ; 10 -> 10.00
        assertThat(xml).contains("<Matrah>1234.57</Matrah>");
        assertThat(xml).contains("<Vergi>0.01</Vergi>");
        assertThat(xml).contains("<ToplamSatisMatrahi>10.00</ToplamSatisMatrahi>");
    }

    @Test
    void build_whenAmountsAndCompanyFieldsAreNull_rendersZeroAmountsAndEmptyStrings() {
        Company company = new Company(); // taxNumber/taxOffice/companyName null
        VatDeclaration decl = declaration(2026, 1, List.of());
        // builder default'ı dışındaki tüm tutar alanları null bırakıldı

        String xml = VatDeclarationXmlBuilder.build(decl, company);

        assertThat(xml).contains("<VergiKimlikNo></VergiKimlikNo>");
        assertThat(xml).contains("<VergiDairesi></VergiDairesi>");
        assertThat(xml).contains("<Unvan></Unvan>");
        assertThat(xml).contains("<Ay>01</Ay>");
        assertThat(xml).contains("<OdenecekKDV>0.00</OdenecekKDV>");
        assertThat(xml).contains("<HesaplananKDV>0.00</HesaplananKDV>");
        assertThat(xml).contains("<SonrakiDonemeDevreden>0.00</SonrakiDonemeDevreden>");
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private Company company() {
        Company company = new Company();
        company.setCompanyName("Test A.Ş.");
        company.setTaxNumber("1234567890");
        company.setTaxOffice("Kadıköy");
        return company;
    }

    private VatDeclaration declaration(int year, int month, List<VatDeclarationLine> lines) {
        return VatDeclaration.builder()
                .year(year)
                .month(month)
                .lines(lines)
                .build();
    }

    private VatDeclarationLine line(String code, String description, String base, String vat) {
        return VatDeclarationLine.builder()
                .lineCode(code)
                .description(description)
                .baseAmount(base != null ? new BigDecimal(base) : null)
                .vatAmount(vat != null ? new BigDecimal(vat) : null)
                .lineOrder(0)
                .build();
    }
}
