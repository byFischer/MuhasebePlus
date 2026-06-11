package com.MuhasebePlus.demo.admin.dto;

import com.MuhasebePlus.demo.company.entity.Company;

import java.time.LocalDateTime;

public record AdminCompanyDto(
        Long companyId,
        String companyName,
        String taxNumber,
        String taxOffice,
        String city,
        String phone,
        String email,
        boolean isActive,
        long userCount,
        LocalDateTime createdAt
) {

    public static AdminCompanyDto fromEntity(Company company, long userCount) {
        return new AdminCompanyDto(
                company.getCompanyId(),
                company.getCompanyName(),
                company.getTaxNumber(),
                company.getTaxOffice(),
                company.getCity(),
                company.getPhone(),
                company.getEmail(),
                company.isActive(),
                userCount,
                company.getCreatedAt()
        );
    }
}
