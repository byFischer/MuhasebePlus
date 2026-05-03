package com.MuhasebePlus.demo.template.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.financial.dto.request.TransactionRequestDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.service.TransactionService;

import java.time.LocalDate;
import com.MuhasebePlus.demo.template.dto.request.TemplateApplyRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateApplyResponseDto;
import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import com.MuhasebePlus.demo.template.entity.TemplateUsageLog;
import com.MuhasebePlus.demo.template.repository.TemplateRepository;
import com.MuhasebePlus.demo.template.repository.TemplateUsageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TemplateApplicationService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateUsageLogRepository usageLogRepository;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    public TemplateApplyResponseDto applyTemplate(Long templateId, TemplateApplyRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        Template template = templateRepository
                .findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(templateId, companyId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));

        TemplateType type = template.getTemplateType();
        Map<String, Object> payload = template.getPayload();

        Long targetEntityId = null;
        String targetEntityType = null;
        String message = "";

        switch (type) {
            case INCOME, EXPENSE -> {
                var result = applyTransactionTemplate(template, payload, type, companyId);
                targetEntityId = result;
                targetEntityType = "Transaction";
                message = type == TemplateType.INCOME
                        ? "Gelir işlemi oluşturuldu: " + template.getTemplateName()
                        : "Gider işlemi oluşturuldu: " + template.getTemplateName();
            }
            case INVOICE -> {
                targetEntityId = applyInvoiceTemplate(template, payload);
                targetEntityType = "Invoice";
                message = "Fatura oluşturuldu: " + template.getTemplateName();
            }
            case STOCK_ADJUSTMENT -> {
                targetEntityId = applyStockTemplate(template, payload);
                targetEntityType = "StockAdjustment";
                message = "Stok işlemi oluşturuldu: " + template.getTemplateName();
            }
            case CUSTOMER_TRANSACTION -> {
                targetEntityId = applyCustomerTemplate(template, payload);
                targetEntityType = "CustomerTransaction";
                message = "Cari işlem oluşturuldu: " + template.getTemplateName();
            }
            case BANK_TRANSFER -> {
                targetEntityId = applyBankTemplate(template, payload);
                targetEntityType = "BankTransfer";
                message = "Banka transferi oluşturuldu: " + template.getTemplateName();
            }
        }

        // Kullanım kaydı at
        TemplateUsageLog log = new TemplateUsageLog();
        log.setTemplateId(templateId);
        log.setUserId(userId);
        log.setUsedAt(LocalDateTime.now());
        log.setTargetEntityType(targetEntityType);
        log.setTargetEntityId(targetEntityId);
        usageLogRepository.save(log);

        return new TemplateApplyResponseDto(true, message, targetEntityType, targetEntityId);
    }

    // ============================================================
    // PRIVATE HELPERS — Modül bazlı uygulama
    // ============================================================

    private Long applyTransactionTemplate(Template template, Map<String, Object> payload, TemplateType type, Long companyId) {
        BigDecimal amount = extractAmount(payload);
        Long accountId = resolveAccountId(payload, companyId);
        String category = extractString(payload, "category", template.getTemplateName());
        String description = extractString(payload, "description", template.getDescription());

        TransactionRequestDto txDto = new TransactionRequestDto(
                accountId,
                null,
                type == TemplateType.INCOME ? TransactionType.INCOME : TransactionType.EXPENSE,
                amount,
                LocalDate.now(),
                description != null ? description : template.getTemplateName(),
                category,
                false
        );

        var response = transactionService.createTransaction(txDto);
        return response.transactionId();
    }

    private Long resolveAccountId(Map<String, Object> payload, Long companyId) {
        // 1) Payload'ta accountId varsa doğrudan kullan
        Long explicitId = extractLong(payload, "accountId", null);
        if (explicitId != null) {
            boolean exists = bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(explicitId, companyId).isPresent();
            if (exists) return explicitId;
        }

        // 2) Payload'ta accountName varsa isme göre bul
        String accountName = extractString(payload, "accountName", null);
        if (accountName == null) accountName = extractString(payload, "account", null);
        if (accountName != null && !accountName.isBlank()) {
            List<BankAccount> accounts = bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(companyId);
            for (BankAccount acc : accounts) {
                if (acc.getBankName() != null && acc.getBankName().equalsIgnoreCase(accountName)) {
                    return acc.getAccountId();
                }
            }
        }

        // 3) Şirketteki ilk aktif hesabı al
        List<BankAccount> accounts = bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(companyId);
        if (!accounts.isEmpty()) {
            return accounts.get(0).getAccountId();
        }

        throw new RuntimeException("Şirketinize ait aktif bir banka hesabı bulunamadı. Lütfen önce Banka & Kasa menüsünden bir hesap ekleyin.");
    }

    private Long applyInvoiceTemplate(Template template, Map<String, Object> payload) {
        // Phase 2+ — InvoiceService entegrasyonu buraya eklenecek
        // Şimdilik stub ID dönüyoruz
        return -1L;
    }

    private Long applyStockTemplate(Template template, Map<String, Object> payload) {
        // Phase 2+ — StockService entegrasyonu buraya eklenecek
        return -2L;
    }

    private Long applyCustomerTemplate(Template template, Map<String, Object> payload) {
        // Phase 2+ — CustomerService entegrasyonu buraya eklenecek
        return -3L;
    }

    private Long applyBankTemplate(Template template, Map<String, Object> payload) {
        // Phase 2+ — BankAccountService entegrasyonu buraya eklenecek
        return -4L;
    }

    // ============================================================
    // PAYLOAD EXTRACTORS
    // ============================================================

    @SuppressWarnings("unchecked")
    private BigDecimal extractAmount(Map<String, Object> payload) {
        if (payload == null) return BigDecimal.ZERO;
        Object val = payload.get("amount");
        if (val == null) val = payload.get("defaultAmount");
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (val instanceof String s) {
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException ignored) {}
        }
        return BigDecimal.ZERO;
    }

    private String extractString(Map<String, Object> payload, String key, String defaultValue) {
        if (payload == null) return defaultValue;
        Object val = payload.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private Long extractLong(Map<String, Object> payload, String key, Long defaultValue) {
        if (payload == null) return defaultValue;
        Object val = payload.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
