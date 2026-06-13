package com.MuhasebePlus.demo.vat;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.vat.dto.BaBsEntryDto;
import com.MuhasebePlus.demo.vat.dto.VatDeclarationLineDto;
import com.MuhasebePlus.demo.vat.entity.VatDeclarationLine;
import com.MuhasebePlus.demo.vat.entity.VatPeriod;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VatDtoEntityTest {

    @Test
    void baBsAndLineDtosExposeValues() {
        BaBsEntryDto baBs = new BaBsEntryDto(10L, "ABC Ltd", "1234567890", new BigDecimal("6000.00"), BigDecimal.ZERO, "BS");
        VatDeclarationLineDto line = new VatDeclarationLineDto("OUTPUT", 20, new BigDecimal("500.00"), new BigDecimal("100.00"));

        assertThat(baBs.formType()).isEqualTo("BS");
        assertThat(baBs.totalAmount()).isEqualByComparingTo("6000.00");
        assertThat(line.lineType()).isEqualTo("OUTPUT");
        assertThat(line.vatRate()).isEqualTo(20);
    }

    @Test
    void vatPeriodLifecycleCallbacksSetTimestampsAndBuilderDefaults() {
        Company company = new Company();
        company.setCompanyId(1L);
        VatPeriod period = VatPeriod.builder()
                .company(company)
                .year(2026)
                .month(5)
                .build();
        VatDeclarationLine line = VatDeclarationLine.builder()
                .vatPeriod(period)
                .lineType("INPUT")
                .vatRate(10)
                .taxBase(new BigDecimal("100.00"))
                .vatAmount(new BigDecimal("10.00"))
                .build();
        period.setLines(List.of(line));

        ReflectionTestUtils.invokeMethod(period, "onCreate");
        LocalDateTime createdAt = period.getCreatedAt();
        ReflectionTestUtils.invokeMethod(period, "onUpdate");

        assertThat(period.getStatus()).isEqualTo("DRAFT");
        assertThat(period.getTotalOutputVat()).isEqualByComparingTo("0.00");
        assertThat(period.getCreatedAt()).isEqualTo(createdAt);
        assertThat(period.getUpdatedAt()).isNotNull();
        assertThat(period.getLines()).containsExactly(line);
        assertThat(line.getVatAmount()).isEqualByComparingTo("10.00");
    }
}
