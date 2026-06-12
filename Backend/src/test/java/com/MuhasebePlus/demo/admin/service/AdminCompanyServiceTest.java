package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.dashboard.entity.AiQuota;
import com.MuhasebePlus.demo.dashboard.repository.AiQuotaRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCompanyServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private AiQuotaRepository aiQuotaRepository;
    @Mock private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminCompanyService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = company(1L, "Test A.S.", true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getCompanies_mapsUserCountsPerCompany() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(companyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(inv -> {
                    exerciseSpec(inv.getArgument(0));
                    return new PageImpl<>(List.of(company), pageable, 1);
                });
        when(userRepository.countByCompanyCompanyId(1L)).thenReturn(3L);

        var result = service.getCompanies("test", true, pageable);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.companyId()).isEqualTo(1L);
            assertThat(dto.companyName()).isEqualTo("Test A.S.");
            assertThat(dto.userCount()).isEqualTo(3L);
            assertThat(dto.isActive()).isTrue();
        });
    }

    @Test
    void getCompanyDetail_includesUsersCountsAndQuota() {
        User admin = user(10L, "admin@test.com", UserRole.ADMIN);
        AiQuota quota = new AiQuota(1L, company, 5000, 1500,
                LocalDateTime.of(2026, 7, 1, 0, 0), null, null);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(userRepository.findByCompanyCompanyId(1L)).thenReturn(List.of(admin));
        when(customerRepository.countByCompanyCompanyIdAndIsDeletedFalse(1L)).thenReturn(11L);
        when(invoiceRepository.countByCompanyCompanyIdAndIsDeletedFalse(1L)).thenReturn(22L);
        when(aiQuotaRepository.findByCompanyCompanyId(1L)).thenReturn(Optional.of(quota));

        var result = service.getCompanyDetail(1L);

        assertThat(result.company().userCount()).isEqualTo(1L);
        assertThat(result.users()).singleElement().satisfies(user -> assertThat(user.email()).isEqualTo("admin@test.com"));
        assertThat(result.customerCount()).isEqualTo(11L);
        assertThat(result.invoiceCount()).isEqualTo(22L);
        assertThat(result.aiQuota().tokensUsedThisMonth()).isEqualTo(1500);
    }

    @Test
    void getCompanyDetail_whenQuotaMissing_returnsNullQuota() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(userRepository.findByCompanyCompanyId(1L)).thenReturn(List.of());
        when(aiQuotaRepository.findByCompanyCompanyId(1L)).thenReturn(Optional.empty());

        var result = service.getCompanyDetail(1L);

        assertThat(result.aiQuota()).isNull();
    }

    @Test
    void getCompanyDetail_whenCompanyMissing_throwsBusinessException() {
        when(companyRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompanyDetail(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("404");
    }

    @Test
    void setActive_updatesCompanyAndAudits() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        service.setActive(1L, false);

        assertThat(company.isActive()).isFalse();
        verify(companyRepository).save(company);
        verify(adminAuditService).logAction(org.mockito.ArgumentMatchers.contains("pasifle"));
    }

    private Company company(Long id, String name, boolean active) {
        return Company.builder()
                .companyId(id)
                .companyName(name)
                .taxNumber("1111111111")
                .taxOffice("Kadikoy")
                .city("Istanbul")
                .phone("555")
                .email("info@test.com")
                .isActive(active)
                .build();
    }

    private User user(Long id, String email, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setCompany(company);
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void exerciseSpec(Specification<Company> spec) {
        spec.toPredicate(
                (Root<Company>) mock(Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
    }
}
