package com.MuhasebePlus.demo.accounting.service;

import com.MuhasebePlus.demo.accounting.dto.request.ChartOfAccountRequestDto;
import com.MuhasebePlus.demo.accounting.dto.response.ChartOfAccountResponseDto;
import com.MuhasebePlus.demo.accounting.entity.AccountType;
import com.MuhasebePlus.demo.accounting.entity.ChartOfAccount;
import com.MuhasebePlus.demo.accounting.repository.ChartOfAccountRepository;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
class ChartOfAccountServiceTest {

    @Mock private ChartOfAccountRepository accountRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyContext companyContext;

    @InjectMocks
    private ChartOfAccountService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
    }

    // Tests creating a duplicate account code is blocked.
    @Test
    void create_whenAccountCodeExists_throwsBusinessException() {
        ChartOfAccountRequestDto dto = request("120.001", "Musteri", AccountType.ASSET, null, true);
        when(accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "120.001"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("120.001");

        verify(accountRepository, never()).save(any());
    }

    // Tests account creation defaults leaf flag and marks account as non-system.
    @Test
    void create_whenValid_savesNonSystemLeafAccount() {
        ChartOfAccountRequestDto dto = request("120.001", "Musteri", AccountType.ASSET, 120L, null);
        when(accountRepository.save(any())).thenAnswer(inv -> {
            ChartOfAccount account = inv.getArgument(0);
            account.setAccountId(10L);
            return account;
        });
        when(accountRepository.findById(120L)).thenReturn(Optional.of(account(120L, "120", "Alicilar", AccountType.ASSET, null, false)));

        ChartOfAccountResponseDto result = service.create(dto);

        assertThat(result.accountId()).isEqualTo(10L);
        assertThat(result.parentCode()).isEqualTo("120");
        assertThat(result.isLeaf()).isTrue();
        assertThat(result.isSystem()).isFalse();
    }

    // Tests system accounts cannot be updated.
    @Test
    void update_whenAccountIsSystem_throwsBusinessException() {
        ChartOfAccount account = account(10L, "600", "Yurtici Satislar", AccountType.INCOME, null, true);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.update(10L, request("600", "Satis", AccountType.INCOME, null, false)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sistem");

        verify(accountRepository, never()).save(any());
    }

    // Tests update rejects duplicate account code owned by a different account.
    @Test
    void update_whenCodeBelongsToAnotherAccount_throwsBusinessException() {
        ChartOfAccount account = account(10L, "120.001", "Eski", AccountType.ASSET, null, false);
        ChartOfAccount duplicate = account(11L, "120.002", "Diger", AccountType.ASSET, null, false);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "120.002"))
                .thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> service.update(10L, request("120.002", "Yeni", AccountType.ASSET, null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("120.002");
    }

    // Tests non-system account update persists changed values.
    @Test
    void update_whenValid_updatesMutableFields() {
        ChartOfAccount account = account(10L, "120.001", "Eski", AccountType.ASSET, null, false);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "120.003"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChartOfAccountResponseDto result = service.update(
                10L, request("120.003", "Yeni Musteri", AccountType.ASSET, 120L, true));

        assertThat(result.accountCode()).isEqualTo("120.003");
        assertThat(result.accountName()).isEqualTo("Yeni Musteri");
        assertThat(result.parentId()).isEqualTo(120L);
    }

    // Tests system accounts cannot be deleted.
    @Test
    void delete_whenAccountIsSystem_throwsBusinessException() {
        ChartOfAccount account = account(10L, "600", "Yurtici Satislar", AccountType.INCOME, null, true);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Sistem");

        verify(accountRepository, never()).save(any());
    }

    // Tests non-system account delete performs soft delete.
    @Test
    void delete_whenNonSystem_marksDeleted() {
        ChartOfAccount account = account(10L, "120.001", "Musteri", AccountType.ASSET, null, false);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.delete(10L);

        assertThat(account.isDeleted()).isTrue();
        assertThat(account.getDeletedAt()).isNotNull();
        verify(accountRepository).save(account);
    }

    // Tests TDHP copy skips duplicate templates and copies missing templates.
    @Test
    void copyTdhpForCompany_whenTemplatesExist_skipsExistingAndCopiesMissingAccounts() {
        ChartOfAccount existingTemplate = account(1L, "100", "Kasa", AccountType.ASSET, null, true);
        ChartOfAccount missingTemplate = account(2L, "600", "Satislar", AccountType.INCOME, null, true);
        when(accountRepository.findByCompanyCompanyId(0L)).thenReturn(List.of(existingTemplate, missingTemplate));
        when(accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "100"))
                .thenReturn(true);
        when(accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "600"))
                .thenReturn(false);

        service.copyTdhpForCompany(COMPANY_ID);

        verify(accountRepository).save(org.mockito.ArgumentMatchers.argThat(copy ->
                copy.getCompany().equals(company)
                        && copy.getAccountCode().equals("600")
                        && copy.isSystem()
        ));
    }

    // Tests accounting setup checks presence of standard revenue account 600.
    @Test
    void isAccountingSetup_whenRevenueAccountExists_returnsTrue() {
        when(accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "600"))
                .thenReturn(true);

        assertThat(service.isAccountingSetup(COMPANY_ID)).isTrue();
    }

    // Tests leaf account generation uses next numeric suffix and parent id.
    @Test
    void getOrCreateLeafAccount_whenSiblingsExist_usesNextSequenceAndParentId() {
        ChartOfAccount sibling = account(20L, "120.00.004", "A", AccountType.ASSET, 120L, false);
        ChartOfAccount badSuffix = account(21L, "120.XX", "B", AccountType.ASSET, 120L, false);
        ChartOfAccount parent = account(120L, "120", "Alicilar", AccountType.ASSET, null, true);
        when(accountRepository.findByCompanyCompanyIdAndAccountCodeStartingWithAndIsDeletedFalse(COMPANY_ID, "120."))
                .thenReturn(List.of(sibling, badSuffix));
        when(accountRepository.findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(COMPANY_ID, "120"))
                .thenReturn(Optional.of(parent));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String newCode = service.getOrCreateLeafAccount(COMPANY_ID, "120", "ABC Ltd", AccountType.ASSET);

        assertThat(newCode).isEqualTo("120.00.005");
        verify(accountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                account.getAccountCode().equals("120.00.005")
                        && account.getParentId().equals(120L)
                        && account.isLeaf()
                        && !account.isSystem()
        ));
    }

    // Tests findByAccountId hides soft-deleted accounts.
    @Test
    void findByAccountId_whenAccountIsDeleted_returnsEmpty() {
        ChartOfAccount account = account(10L, "120.001", "Musteri", AccountType.ASSET, null, false);
        account.setDeleted(true);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThat(service.findByAccountId(10L)).isEmpty();
    }

    // Tests response mapping resolves parent account code.
    @Test
    void toResponseDto_whenParentExists_includesParentCode() {
        ChartOfAccount parent = account(120L, "120", "Alicilar", AccountType.ASSET, null, true);
        ChartOfAccount child = account(10L, "120.001", "Musteri", AccountType.ASSET, 120L, false);
        when(accountRepository.findById(120L)).thenReturn(Optional.of(parent));

        ChartOfAccountResponseDto result = service.toResponseDto(child);

        assertThat(result.parentCode()).isEqualTo("120");
    }

    private ChartOfAccountRequestDto request(String code, String name, AccountType type, Long parentId, Boolean leaf) {
        return new ChartOfAccountRequestDto(code, name, type, parentId, leaf, "Aciklama");
    }

    private ChartOfAccount account(Long id, String code, String name, AccountType type, Long parentId, boolean system) {
        ChartOfAccount account = new ChartOfAccount();
        account.setAccountId(id);
        account.setCompany(company);
        account.setAccountCode(code);
        account.setAccountName(name);
        account.setAccountType(type);
        account.setParentId(parentId);
        account.setLeaf(true);
        account.setSystem(system);
        account.setDeleted(false);
        return account;
    }
}
