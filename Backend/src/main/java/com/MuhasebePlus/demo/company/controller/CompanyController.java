package com.MuhasebePlus.demo.company.controller;

import com.MuhasebePlus.demo.company.dto.request.CompanyRequestDto;
import com.MuhasebePlus.demo.company.dto.response.CompanyResponseDto;
import com.MuhasebePlus.demo.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Cross-tenant admin işlemleri /api/admin/companies altına taşındı (bkz. admin.controller.AdminCompanyController)
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CompanyResponseDto> getCompanyById(@PathVariable Long id) {
        CompanyResponseDto response = companyService.getCompanyById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CompanyResponseDto> updateCompany(
            @PathVariable Long id, 
            @Valid @RequestBody CompanyRequestDto requestDto) {
        CompanyResponseDto response = companyService.updateCompany(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CompanyResponseDto> getCurrentCompany() {
        return ResponseEntity.ok(companyService.getCurrentCompany());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CompanyResponseDto> updateCurrentCompany(@Valid @RequestBody CompanyRequestDto requestDto) {
        return ResponseEntity.ok(companyService.updateCurrentCompany(requestDto));
    }
}
