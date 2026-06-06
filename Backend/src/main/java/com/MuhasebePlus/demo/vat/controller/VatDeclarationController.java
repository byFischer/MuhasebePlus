package com.MuhasebePlus.demo.vat.controller;

import com.MuhasebePlus.demo.vat.dto.BaBsEntryDto;
import com.MuhasebePlus.demo.vat.dto.VatCalculateRequestDto;
import com.MuhasebePlus.demo.vat.dto.VatPeriodResponseDto;
import com.MuhasebePlus.demo.vat.service.VatDeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vat-declarations")
@RequiredArgsConstructor
public class VatDeclarationController {

    private final VatDeclarationService vatDeclarationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<VatPeriodResponseDto>> getAll() {
        return ResponseEntity.ok(vatDeclarationService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<VatPeriodResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vatDeclarationService.getById(id));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<VatPeriodResponseDto> calculate(@RequestBody VatCalculateRequestDto req) {
        return ResponseEntity.ok(vatDeclarationService.calculate(req));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VatPeriodResponseDto> lock(@PathVariable Long id) {
        return ResponseEntity.ok(vatDeclarationService.lock(id));
    }

    @GetMapping("/{id}/ba-bs")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<BaBsEntryDto>> getBaBs(@PathVariable Long id) {
        return ResponseEntity.ok(vatDeclarationService.getBaBs(id));
    }
}
