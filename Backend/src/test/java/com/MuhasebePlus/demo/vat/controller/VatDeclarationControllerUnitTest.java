package com.MuhasebePlus.demo.vat.controller;

import com.MuhasebePlus.demo.vat.dto.BaBsEntryDto;
import com.MuhasebePlus.demo.vat.dto.VatCalculateRequestDto;
import com.MuhasebePlus.demo.vat.dto.VatDeclarationLineDto;
import com.MuhasebePlus.demo.vat.dto.VatPeriodResponseDto;
import com.MuhasebePlus.demo.vat.service.VatDeclarationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VatDeclarationControllerUnitTest {

    @Mock private VatDeclarationService vatDeclarationService;

    @InjectMocks
    private VatDeclarationController controller;

    @Test
    void endpoints_delegateToVatDeclarationService() {
        VatCalculateRequestDto request = new VatCalculateRequestDto(2026, 5, BigDecimal.ZERO);
        VatPeriodResponseDto period = period();
        BaBsEntryDto baBs = new BaBsEntryDto(10L, "ABC Ltd", "1234567890", new BigDecimal("6000.00"), BigDecimal.ZERO, "BS");
        when(vatDeclarationService.getAll()).thenReturn(List.of(period));
        when(vatDeclarationService.getById(1L)).thenReturn(period);
        when(vatDeclarationService.calculate(request)).thenReturn(period);
        when(vatDeclarationService.lock(1L)).thenReturn(period);
        when(vatDeclarationService.getBaBs(1L)).thenReturn(List.of(baBs));

        assertThat(controller.getAll().getBody()).containsExactly(period);
        assertThat(controller.getById(1L).getBody()).isSameAs(period);
        assertThat(controller.calculate(request).getBody()).isSameAs(period);
        assertThat(controller.lock(1L).getBody()).isSameAs(period);
        assertThat(controller.getBaBs(1L).getBody()).containsExactly(baBs);
    }

    private VatPeriodResponseDto period() {
        return new VatPeriodResponseDto(
                1L,
                2026,
                5,
                "DRAFT",
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("60.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                List.of(new VatDeclarationLineDto("OUTPUT", 20, new BigDecimal("500.00"), new BigDecimal("100.00"))),
                null,
                null
        );
    }
}
