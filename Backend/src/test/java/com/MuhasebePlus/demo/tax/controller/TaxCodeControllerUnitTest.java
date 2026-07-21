package com.MuhasebePlus.demo.tax.controller;

import com.MuhasebePlus.demo.tax.dto.VatExemptionCodeDto;
import com.MuhasebePlus.demo.tax.dto.WithholdingTaxCodeDto;
import com.MuhasebePlus.demo.tax.entity.VatExemptionCategory;
import com.MuhasebePlus.demo.tax.entity.VatExemptionCode;
import com.MuhasebePlus.demo.tax.entity.WithholdingTaxCode;
import com.MuhasebePlus.demo.tax.repository.VatExemptionCodeRepository;
import com.MuhasebePlus.demo.tax.repository.WithholdingTaxCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxCodeControllerUnitTest {

    @Mock
    private WithholdingTaxCodeRepository withholdingRepo;
    @Mock
    private VatExemptionCodeRepository exemptionRepo;

    @Test
    void getWithholdingCodes_mapsActiveCodesToDtos() {
        TaxCodeController controller = new TaxCodeController(withholdingRepo, exemptionRepo);
        WithholdingTaxCode code = new WithholdingTaxCode(
                1,
                "601",
                "Yapim isleri",
                new BigDecimal("0.3000"),
                10,
                3,
                "Hizmet",
                true
        );
        when(withholdingRepo.findByIsActiveTrueOrderByCodeAsc()).thenReturn(List.of(code));

        ResponseEntity<List<WithholdingTaxCodeDto>> result = controller.getWithholdingCodes();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).singleElement().satisfies(dto -> {
            assertThat(dto.codeId()).isEqualTo(1);
            assertThat(dto.code()).isEqualTo("601");
            assertThat(dto.withholdingRate()).isEqualByComparingTo("0.3000");
            assertThat(dto.denominator()).isEqualTo(10);
            assertThat(dto.numerator()).isEqualTo(3);
            assertThat(dto.serviceCategory()).isEqualTo("Hizmet");
            assertThat(dto.isActive()).isTrue();
        });
    }

    @Test
    void getExemptionCodes_mapsActiveCodesToDtos() {
        TaxCodeController controller = new TaxCodeController(withholdingRepo, exemptionRepo);
        VatExemptionCode code = new VatExemptionCode(
                10,
                "301",
                "Ihracat teslimleri",
                VatExemptionCategory.IHRACAT,
                "301",
                true
        );
        when(exemptionRepo.findByIsActiveTrueOrderByCodeAsc()).thenReturn(List.of(code));

        ResponseEntity<List<VatExemptionCodeDto>> result = controller.getExemptionCodes();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).singleElement().satisfies(dto -> {
            assertThat(dto.codeId()).isEqualTo(10);
            assertThat(dto.code()).isEqualTo("301");
            assertThat(dto.category()).isEqualTo(VatExemptionCategory.IHRACAT);
            assertThat(dto.kdvBeyannamesiSatiri()).isEqualTo("301");
            assertThat(dto.isActive()).isTrue();
        });
    }
}
