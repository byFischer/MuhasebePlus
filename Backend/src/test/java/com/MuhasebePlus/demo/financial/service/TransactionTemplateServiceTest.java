package com.MuhasebePlus.demo.financial.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.TransactionTemplateRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionTemplateResponseDto;
import com.MuhasebePlus.demo.financial.entity.TransactionTemplate;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.TransactionTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionTemplateServiceTest {

    @Mock private TransactionTemplateRepository templateRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private TransactionTemplateService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long USER_ID = 9L;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(templateRepository.save(any())).thenAnswer(inv -> {
            TransactionTemplate template = inv.getArgument(0);
            if (template.getTemplateId() == null) {
                template.setTemplateId(10L);
            }
            return template;
        });
    }

    // Rejects duplicate active template names during creation.
    @Test
    void createTemplate_whenNameExists_throwsAndDoesNotSave() {
        when(templateRepository.existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse("Kira", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createTemplate(request("Kira", TransactionType.EXPENSE)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kira");

        verify(templateRepository, never()).save(any());
    }

    // Creates a template for the current company and user.
    @Test
    void createTemplate_whenValid_savesAndMapsResponse() {
        when(templateRepository.existsByTemplateNameAndCompanyCompanyIdAndIsDeletedFalse("Kira", COMPANY_ID))
                .thenReturn(false);

        TransactionTemplateResponseDto result = service.createTemplate(request("Kira", TransactionType.EXPENSE));

        assertThat(result.templateId()).isEqualTo(10L);
        assertThat(result.templateName()).isEqualTo("Kira");
        assertThat(result.transactionType()).isEqualTo("EXPENSE");
        verify(companyRepository).getReferenceById(COMPANY_ID);
    }

    // Lists all active templates ordered by repository result.
    @Test
    void getAllTemplates_mapsRepositoryRows() {
        when(templateRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(COMPANY_ID))
                .thenReturn(List.of(template(2L, "Satis", TransactionType.INCOME), template(1L, "Kira", TransactionType.EXPENSE)));

        List<TransactionTemplateResponseDto> result = service.getAllTemplates();

        assertThat(result).extracting(TransactionTemplateResponseDto::templateName)
                .containsExactly("Satis", "Kira");
    }

    // Converts string type input to enum and filters by transaction type.
    @Test
    void getTemplatesByType_whenLowercaseType_filtersByEnum() {
        when(templateRepository.findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTemplateIdDesc(TransactionType.INCOME, COMPANY_ID))
                .thenReturn(List.of(template(2L, "Satis", TransactionType.INCOME)));

        List<TransactionTemplateResponseDto> result = service.getTemplatesByType("income");

        assertThat(result).singleElement().satisfies(dto -> assertThat(dto.transactionType()).isEqualTo("INCOME"));
    }

    // Returns one active template by id.
    @Test
    void getTemplateById_whenFound_mapsResponse() {
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(template(10L, "Satis", TransactionType.INCOME)));

        TransactionTemplateResponseDto result = service.getTemplateById(10L);

        assertThat(result.templateName()).isEqualTo("Satis");
        assertThat(result.isDeleted()).isFalse();
    }

    // Rejects update when the new name conflicts with another active template.
    @Test
    void updateTemplate_whenNewNameConflicts_throwsAndDoesNotSave() {
        TransactionTemplate template = template(10L, "Kira", TransactionType.EXPENSE);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(template));
        when(templateRepository.existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse("Maas", 10L, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateTemplate(10L, request("Maas", TransactionType.EXPENSE)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Maas");

        verify(templateRepository, never()).save(template);
    }

    // Updates template fields when the name is unique.
    @Test
    void updateTemplate_whenValid_updatesCommercialFields() {
        TransactionTemplate template = template(10L, "Kira", TransactionType.EXPENSE);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(template));
        when(templateRepository.existsByTemplateNameAndTemplateIdNotAndCompanyCompanyIdAndIsDeletedFalse("Kira Yeni", 10L, COMPANY_ID))
                .thenReturn(false);

        TransactionTemplateResponseDto result = service.updateTemplate(10L, request("Kira Yeni", TransactionType.INCOME));

        assertThat(result.templateName()).isEqualTo("Kira Yeni");
        assertThat(result.transactionType()).isEqualTo("INCOME");
        assertThat(result.defaultAmount()).isEqualByComparingTo("150.00");
    }

    // Soft deletes an active template with a deletion timestamp.
    @Test
    void softDeleteTemplate_whenFound_marksDeleted() {
        TransactionTemplate template = template(10L, "Kira", TransactionType.EXPENSE);
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(template));

        service.softDeleteTemplate(10L);

        assertThat(template.isDeleted()).isTrue();
        assertThat(template.getDeletedAt()).isNotNull();
        verify(templateRepository).save(template);
    }

    // Restores a soft-deleted template.
    @Test
    void restoreTemplate_whenFound_clearsDeletedState() {
        TransactionTemplate template = template(10L, "Kira", TransactionType.EXPENSE);
        template.setDeleted(true);
        template.setDeletedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        when(templateRepository.findByTemplateIdAndCompanyCompanyId(10L, COMPANY_ID))
                .thenReturn(Optional.of(template));

        TransactionTemplateResponseDto result = service.restoreTemplate(10L);

        assertThat(result.isDeleted()).isFalse();
        assertThat(template.getDeletedAt()).isNull();
    }

    // Permanently removes expired soft-deleted templates.
    @Test
    void hardDeleteExpired_whenExpiredTemplatesExist_deletesAndReturnsCount() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        TransactionTemplate first = template(1L, "A", TransactionType.INCOME);
        TransactionTemplate second = template(2L, "B", TransactionType.EXPENSE);
        when(templateRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int result = service.hardDeleteExpired(cutoff);

        assertThat(result).isEqualTo(2);
        verify(templateRepository).delete(first);
        verify(templateRepository).delete(second);
    }

    private TransactionTemplateRequestDto request(String name, TransactionType type) {
        return new TransactionTemplateRequestDto(
                name,
                new BigDecimal("150.00"),
                type,
                "Aciklama",
                "Kategori"
        );
    }

    private TransactionTemplate template(Long id, String name, TransactionType type) {
        TransactionTemplate template = new TransactionTemplate();
        template.setTemplateId(id);
        template.setCompany(company);
        template.setUserId(USER_ID);
        template.setTemplateName(name);
        template.setDefaultAmount(new BigDecimal("100.00"));
        template.setTransactionType(type);
        template.setDescription("Aciklama");
        template.setCategory("Kategori");
        template.setDeleted(false);
        return template;
    }
}
