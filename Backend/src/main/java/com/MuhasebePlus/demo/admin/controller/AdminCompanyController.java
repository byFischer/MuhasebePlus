package com.MuhasebePlus.demo.admin.controller;

import com.MuhasebePlus.demo.admin.dto.AdminCompanyDetailDto;
import com.MuhasebePlus.demo.admin.dto.AdminCompanyDto;
import com.MuhasebePlus.demo.admin.service.AdminCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/companies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final AdminCompanyService adminCompanyService;

    @GetMapping
    public ResponseEntity<Page<AdminCompanyDto>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminCompanyService.getCompanies(search, active, pageable));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<AdminCompanyDetailDto> getCompanyDetail(@PathVariable Long companyId) {
        return ResponseEntity.ok(adminCompanyService.getCompanyDetail(companyId));
    }

    @PutMapping("/{companyId}/activate")
    public ResponseEntity<Void> activateCompany(@PathVariable Long companyId) {
        adminCompanyService.setActive(companyId, true);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{companyId}/deactivate")
    public ResponseEntity<Void> deactivateCompany(@PathVariable Long companyId) {
        adminCompanyService.setActive(companyId, false);
        return ResponseEntity.noContent().build();
    }
}
