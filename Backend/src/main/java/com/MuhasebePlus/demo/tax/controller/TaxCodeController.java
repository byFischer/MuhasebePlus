package com.MuhasebePlus.demo.tax.controller;

import com.MuhasebePlus.demo.tax.dto.VatExemptionCodeDto;
import com.MuhasebePlus.demo.tax.dto.WithholdingTaxCodeDto;
import com.MuhasebePlus.demo.tax.repository.VatExemptionCodeRepository;
import com.MuhasebePlus.demo.tax.repository.WithholdingTaxCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tax-codes")
@RequiredArgsConstructor
public class TaxCodeController {

    private final WithholdingTaxCodeRepository withholdingRepo;
    private final VatExemptionCodeRepository exemptionRepo;

    @GetMapping("/withholding")
    public ResponseEntity<List<WithholdingTaxCodeDto>> getWithholdingCodes() {
        List<WithholdingTaxCodeDto> codes = withholdingRepo.findByIsActiveTrueOrderByCodeAsc().stream()
                .map(c -> new WithholdingTaxCodeDto(
                        c.getCodeId(), c.getCode(), c.getDescription(),
                        c.getWithholdingRate(), c.getDenominator(), c.getNumerator(),
                        c.getServiceCategory(), c.isActive()))
                .toList();
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/exemption")
    public ResponseEntity<List<VatExemptionCodeDto>> getExemptionCodes() {
        List<VatExemptionCodeDto> codes = exemptionRepo.findByIsActiveTrueOrderByCodeAsc().stream()
                .map(c -> new VatExemptionCodeDto(
                        c.getCodeId(), c.getCode(), c.getDescription(),
                        c.getCategory(), c.getKdvBeyannamesiSatiri(), c.isActive()))
                .toList();
        return ResponseEntity.ok(codes);
    }
}
