package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.admin.dto.AdminCompanyDetailDto;
import com.MuhasebePlus.demo.admin.dto.AdminCompanyDto;
import com.MuhasebePlus.demo.admin.dto.AdminUserDto;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.dashboard.repository.AiQuotaRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-tenant şirket yönetimi. Normal {@code CompanyService} tenant-scoped
 * çalışır; buradaki sorgular bilinçli olarak tüm şirketleri kapsar ve sadece
 * /api/admin/** altından (ADMIN korumalı) çağrılmalıdır.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminCompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final AiQuotaRepository aiQuotaRepository;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public Page<AdminCompanyDto> getCompanies(String search, Boolean active, Pageable pageable) {
        return companyRepository.findAll(buildSpec(search, active), pageable)
                .map(c -> AdminCompanyDto.fromEntity(c, userRepository.countByCompanyCompanyId(c.getCompanyId())));
    }

    @Transactional(readOnly = true)
    public AdminCompanyDetailDto getCompanyDetail(Long companyId) {
        Company company = findCompany(companyId);

        List<AdminUserDto> users = userRepository.findByCompanyCompanyId(companyId).stream()
                .map(AdminUserDto::fromEntity)
                .toList();

        AdminCompanyDetailDto.AiQuotaDto quota = aiQuotaRepository.findByCompanyCompanyId(companyId)
                .map(q -> new AdminCompanyDetailDto.AiQuotaDto(
                        q.getMonthlyTokenBudget(), q.getTokensUsedThisMonth(), q.getResetAt()))
                .orElse(null);

        return new AdminCompanyDetailDto(
                AdminCompanyDto.fromEntity(company, users.size()),
                users,
                customerRepository.countByCompanyCompanyIdAndIsDeletedFalse(companyId),
                invoiceRepository.countByCompanyCompanyIdAndIsDeletedFalse(companyId),
                quota
        );
    }

    public void setActive(Long companyId, boolean active) {
        Company company = findCompany(companyId);
        company.setActive(active);
        companyRepository.save(company);
        adminAuditService.logAction("Şirket %s: %s (ID %d)"
                .formatted(active ? "aktifleştirildi" : "pasifleştirildi", company.getCompanyName(), companyId));
    }


    // PRIVATE METOTLAR

    private Specification<Company> buildSpec(String search, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("taxNumber"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("city"), "")), like)
                ));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("isActive"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Şirket bulunamadı: " + companyId));
    }
}
