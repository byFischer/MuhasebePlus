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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChartOfAccountService {

    private final ChartOfAccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    public List<ChartOfAccountResponseDto> getAll() {
        Long companyId = companyContext.getCurrentCompanyId();
        return accountRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(companyId)
                .stream().map(this::toResponseDto).toList();
    }

    public ChartOfAccountResponseDto getById(Long accountId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toResponseDto(findByIdOrThrow(companyId, accountId));
    }

    public ChartOfAccountResponseDto create(ChartOfAccountRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        if (accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, dto.accountCode())) {
            throw new BusinessException("Bu hesap kodu zaten kullanılıyor: " + dto.accountCode());
        }
        Company company = companyRepository.getReferenceById(companyId);
        ChartOfAccount account = new ChartOfAccount();
        account.setCompany(company);
        account.setAccountCode(dto.accountCode());
        account.setAccountName(dto.accountName());
        account.setAccountType(dto.accountType());
        account.setParentId(dto.parentId());
        account.setLeaf(dto.isLeaf() != null ? dto.isLeaf() : true);
        account.setSystem(false);
        account.setDescription(dto.description());
        return toResponseDto(accountRepository.save(account));
    }

    public ChartOfAccountResponseDto update(Long accountId, ChartOfAccountRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        ChartOfAccount account = findByIdOrThrow(companyId, accountId);
        if (account.isSystem()) {
            throw new BusinessException("Sistem hesabı güncellenemez: " + account.getAccountCode());
        }
        accountRepository.findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, dto.accountCode())
                .filter(a -> !a.getAccountId().equals(accountId))
                .ifPresent(a -> { throw new BusinessException("Bu hesap kodu zaten kullanılıyor: " + dto.accountCode()); });
        account.setAccountCode(dto.accountCode());
        account.setAccountName(dto.accountName());
        account.setAccountType(dto.accountType());
        account.setParentId(dto.parentId());
        if (dto.isLeaf() != null) account.setLeaf(dto.isLeaf());
        account.setDescription(dto.description());
        return toResponseDto(accountRepository.save(account));
    }

    public void delete(Long accountId) {
        Long companyId = companyContext.getCurrentCompanyId();
        ChartOfAccount account = findByIdOrThrow(companyId, accountId);
        if (account.isSystem()) {
            throw new BusinessException("Sistem hesabı silinemez: " + account.getAccountCode());
        }
        account.setDeleted(true);
        account.setDeletedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    // Called from CompanyService when a new company is created
    public void copyTdhpForCompany(Long companyId) {
        List<ChartOfAccount> templates = accountRepository.findByCompanyCompanyId(0L);
        if (templates.isEmpty()) return;
        Company company = companyRepository.getReferenceById(companyId);
        for (ChartOfAccount t : templates) {
            if (accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, t.getAccountCode())) {
                continue;
            }
            ChartOfAccount copy = new ChartOfAccount();
            copy.setCompany(company);
            copy.setAccountCode(t.getAccountCode());
            copy.setAccountName(t.getAccountName());
            copy.setAccountType(t.getAccountType());
            copy.setLeaf(t.isLeaf());
            copy.setSystem(t.isSystem());
            copy.setDescription(t.getDescription());
            accountRepository.save(copy);
        }
    }

    public Optional<ChartOfAccount> findByCode(Long companyId, String code) {
        return accountRepository.findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, code);
    }

    public Optional<ChartOfAccount> findByAccountId(Long accountId) {
        return accountRepository.findById(accountId).filter(a -> !a.isDeleted());
    }

    public boolean isAccountingSetup(Long companyId) {
        return accountRepository.existsByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, "600");
    }

    // Creates a leaf account under parentCode (e.g. "120") with next available sequence
    public String getOrCreateLeafAccount(Long companyId, String parentCode, String name, AccountType type) {
        List<ChartOfAccount> siblings = accountRepository
                .findByCompanyCompanyIdAndAccountCodeStartingWithAndIsDeletedFalse(companyId, parentCode + ".");
        int nextSeq = siblings.stream()
                .map(ChartOfAccount::getAccountCode)
                .mapToInt(code -> {
                    String[] parts = code.split("\\.");
                    try { return Integer.parseInt(parts[parts.length - 1]); } catch (NumberFormatException e) { return 0; }
                })
                .max().orElse(0) + 1;

        String newCode = parentCode + ".00." + String.format("%03d", nextSeq);
        Long parentId = accountRepository
                .findByCompanyCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, parentCode)
                .map(ChartOfAccount::getAccountId).orElse(null);

        Company company = companyRepository.getReferenceById(companyId);
        ChartOfAccount leaf = new ChartOfAccount();
        leaf.setCompany(company);
        leaf.setAccountCode(newCode);
        leaf.setAccountName(name);
        leaf.setAccountType(type);
        leaf.setParentId(parentId);
        leaf.setLeaf(true);
        leaf.setSystem(false);
        accountRepository.save(leaf);
        return newCode;
    }

    private ChartOfAccount findByIdOrThrow(Long companyId, Long accountId) {
        return accountRepository.findById(accountId)
                .filter(a -> a.getCompany().getCompanyId().equals(companyId) && !a.isDeleted())
                .orElseThrow(() -> new BusinessException("Hesap bulunamadı: " + accountId));
    }

    public ChartOfAccountResponseDto toResponseDto(ChartOfAccount a) {
        String parentCode = null;
        if (a.getParentId() != null) {
            parentCode = accountRepository.findById(a.getParentId())
                    .map(ChartOfAccount::getAccountCode).orElse(null);
        }
        return new ChartOfAccountResponseDto(
                a.getAccountId(), a.getAccountCode(), a.getAccountName(), a.getAccountType(),
                a.getParentId(), parentCode, a.isLeaf(), a.isSystem(), a.getDescription(), a.getCreatedAt());
    }
}
