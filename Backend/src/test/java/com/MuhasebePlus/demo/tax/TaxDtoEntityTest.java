package com.MuhasebePlus.demo.tax;

import com.MuhasebePlus.demo.tax.dto.VatExemptionCodeDto;
import com.MuhasebePlus.demo.tax.dto.WithholdingTaxCodeDto;
import com.MuhasebePlus.demo.tax.entity.VatExemptionCategory;
import com.MuhasebePlus.demo.tax.entity.VatExemptionCode;
import com.MuhasebePlus.demo.tax.entity.WithholdingTaxCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxDtoEntityTest {

    @Test
    void withholdingTaxCodeEntityAndDtoExposeValues() {
        WithholdingTaxCode entity = new WithholdingTaxCode();
        entity.setCodeId(1);
        entity.setCode("601");
        entity.setDescription("Yapim isleri");
        entity.setWithholdingRate(new BigDecimal("0.3000"));
        entity.setDenominator(10);
        entity.setNumerator(3);
        entity.setServiceCategory("Hizmet");
        entity.setActive(true);
        WithholdingTaxCodeDto dto = new WithholdingTaxCodeDto(
                entity.getCodeId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getWithholdingRate(),
                entity.getDenominator(),
                entity.getNumerator(),
                entity.getServiceCategory(),
                entity.isActive()
        );

        assertThat(dto.code()).isEqualTo("601");
        assertThat(dto.withholdingRate()).isEqualByComparingTo("0.3000");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void vatExemptionEntityDtoAndCategoriesExposeValues() {
        VatExemptionCode entity = new VatExemptionCode();
        entity.setCodeId(10);
        entity.setCode("301");
        entity.setDescription("Ihracat");
        entity.setCategory(VatExemptionCategory.IHRACAT);
        entity.setKdvBeyannamesiSatiri("301");
        entity.setActive(true);
        VatExemptionCodeDto dto = new VatExemptionCodeDto(
                entity.getCodeId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getKdvBeyannamesiSatiri(),
                entity.isActive()
        );

        assertThat(dto.category()).isEqualTo(VatExemptionCategory.IHRACAT);
        assertThat(dto.kdvBeyannamesiSatiri()).isEqualTo("301");
        assertThat(VatExemptionCategory.values()).contains(
                VatExemptionCategory.TAM_ISTISNA,
                VatExemptionCategory.KISMI_ISTISNA,
                VatExemptionCategory.IHRACAT,
                VatExemptionCategory.DIPLOMATIK,
                VatExemptionCategory.KDV_DISI
        );
    }
}
