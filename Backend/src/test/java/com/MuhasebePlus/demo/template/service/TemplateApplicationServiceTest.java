package com.MuhasebePlus.demo.template.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.financial.dto.request.TransactionRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.TransactionCategory;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.service.TransactionService;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;
import com.MuhasebePlus.demo.stock.dto.request.StockMovementRequestDto;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.service.StockMovementService;
import com.MuhasebePlus.demo.template.dto.request.TemplateApplyRequestDto;
import com.MuhasebePlus.demo.template.dto.response.TemplateApplyResponseDto;
import com.MuhasebePlus.demo.template.entity.Template;
import com.MuhasebePlus.demo.template.entity.TemplateType;
import com.MuhasebePlus.demo.template.entity.TemplateUsageLog;
import com.MuhasebePlus.demo.template.repository.TemplateRepository;
import com.MuhasebePlus.demo.template.repository.TemplateUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TemplateApplicationService için kapsamlı birim testleri. Her şablon tipi
 * (INCOME/EXPENSE/INVOICE/STOCK_ADJUSTMENT/CUSTOMER_TRANSACTION/BANK_TRANSFER),
 * banka hesabı çözümleme dalları, payload çıkarıcılar ve doğrulama hataları kapsanır.
 */
@ExtendWith(MockitoExtension.class)
class TemplateApplicationServiceTest {

    @Mock private TemplateRepository templateRepository;
    @Mock private TemplateUsageLogRepository usageLogRepository;
    @Mock private CompanyContext companyContext;
    @Mock private TransactionService transactionService;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private StockMovementService stockMovementService;

    @InjectMocks
    private TemplateApplicationService service;

    private static final Long COMPANY_ID = 4L;
    private static final Long USER_ID = 11L;
    private static final Long TEMPLATE_ID = 50L;

    @BeforeEach
    void setUp() {
        lenient().when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(companyContext.getCurrentUserId()).thenReturn(USER_ID);
    }

    private Template template(TemplateType type, Map<String, Object> payload) {
        Template t = new Template();
        t.setTemplateId(TEMPLATE_ID);
        t.setTemplateCode("TPL");
        t.setTemplateName("Şablon");
        t.setDescription("desc");
        t.setTemplateType(type);
        t.setPayload(payload);
        return t;
    }

    private void stubTemplate(Template t) {
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(TEMPLATE_ID, COMPANY_ID))
                .thenReturn(Optional.of(t));
    }

    private TransactionResponseDto txResponse(Long id) {
        return new TransactionResponseDto(id, null, null, null, null, null,
                null, null, null, null, false, false, null, null);
    }

    private BankAccount account(Long id, String bankName) {
        BankAccount acc = new BankAccount();
        acc.setAccountId(id);
        acc.setBankName(bankName);
        return acc;
    }

    private Map<String, Object> payload(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    // ── INCOME / EXPENSE ────────────────────────────────────────────────────────

    @Test
    void applyTemplate_income_createsTransactionAndLogsUsage() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 1500, "accountId", 10L)));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account(10L, "Ziraat")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(500L));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, new TemplateApplyRequestDto("not"));

        assertThat(result.success()).isTrue();
        assertThat(result.targetEntityType()).isEqualTo("Transaction");
        assertThat(result.targetEntityId()).isEqualTo(500L);
        assertThat(result.message()).contains("Gelir işlemi oluşturuldu");

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(10L);
        assertThat(tx.getValue().transactionType()).isEqualTo(TransactionType.INCOME);
        assertThat(tx.getValue().amount()).isEqualByComparingTo("1500");

        ArgumentCaptor<TemplateUsageLog> log = ArgumentCaptor.forClass(TemplateUsageLog.class);
        verify(usageLogRepository).save(log.capture());
        assertThat(log.getValue().getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(log.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(log.getValue().getTargetEntityType()).isEqualTo("Transaction");
        assertThat(log.getValue().getTargetEntityId()).isEqualTo(500L);
    }

    @Test
    void applyTemplate_expense_messageMentionsGider() {
        stubTemplate(template(TemplateType.EXPENSE, payload("amount", 200)));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(7L, "Garanti")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(501L));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.message()).contains("Gider işlemi oluşturuldu");
        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().transactionType()).isEqualTo(TransactionType.EXPENSE);
        // accountId hesap listesinden çözülmeli
        assertThat(tx.getValue().accountId()).isEqualTo(7L);
    }

    // ── resolveAccountId dalları ──────────────────────────────────────────────

    @Test
    void applyTemplate_explicitAccountIdNotFound_fallsBackToFirstAccount() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 50, "accountId", 99L)));
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(99L, COMPANY_ID))
                .thenReturn(Optional.empty());
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(3L, "Akbank")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(3L);
    }

    @Test
    void applyTemplate_resolvesAccountByName() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 50, "accountName", "ziraat")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(8L, "Garanti"), account(9L, "Ziraat")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        // İsim eşleşmesi büyük/küçük harf duyarsız → "Ziraat" (id 9)
        assertThat(tx.getValue().accountId()).isEqualTo(9L);
    }

    @Test
    void applyTemplate_noBankAccount_throws() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 50)));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aktif bir banka hesabı bulunamadı");
    }

    // ── payload extractor dalları ─────────────────────────────────────────────

    @Test
    void applyTemplate_amountAsString_isParsed() {
        stubTemplate(template(TemplateType.EXPENSE, payload("amount", "250.50")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(1L, "X")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().amount()).isEqualByComparingTo("250.50");
    }

    @Test
    void applyTemplate_invalidAmountString_defaultsToZero() {
        stubTemplate(template(TemplateType.EXPENSE, payload("defaultAmount", "not-a-number")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(1L, "X")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().amount()).isEqualByComparingTo("0");
    }

    @Test
    void applyTemplate_nullPayload_usesDefaults() {
        stubTemplate(template(TemplateType.INCOME, null));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(2L, "X")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().amount()).isEqualByComparingTo("0");
    }

    // ── INVOICE ───────────────────────────────────────────────────────────────

    @Test
    void applyTemplate_invoice_createsInvoice() {
        Map<String, Object> item = payload("productId", 1, "quantity", 2);
        stubTemplate(template(TemplateType.INVOICE,
                payload("customerId", 20L, "invoiceType", "purchase",
                        "dueDate", "2026-08-01", "lineItems", List.of(item))));
        InvoiceResponseDto invoice = mock(InvoiceResponseDto.class);
        when(invoice.invoiceId()).thenReturn(800L);
        when(invoiceService.createInvoice(any())).thenReturn(invoice);

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.targetEntityType()).isEqualTo("Invoice");
        assertThat(result.targetEntityId()).isEqualTo(800L);
        assertThat(result.message()).contains("Fatura oluşturuldu");
    }

    @Test
    void applyTemplate_invoice_missingCustomerId_throws() {
        stubTemplate(template(TemplateType.INVOICE, payload("lineItems", List.of())));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("customerId zorunludur");
    }

    @Test
    void applyTemplate_invoice_noValidLineItems_throws() {
        // quantity eksik kalemler filtrelenir → boş liste → hata
        Map<String, Object> badItem = payload("productId", 1);
        stubTemplate(template(TemplateType.INVOICE,
                payload("customerId", 20L, "lineItems", List.of(badItem))));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("en az bir kalem");
    }

    // ── STOCK_ADJUSTMENT ──────────────────────────────────────────────────────

    @Test
    void applyTemplate_stockAdjustmentOut_createsMovement() {
        stubTemplate(template(TemplateType.STOCK_ADJUSTMENT,
                payload("productId", 5, "quantity", 3, "direction", "OUT")));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.targetEntityType()).isEqualTo("StockAdjustment");
        assertThat(result.targetEntityId()).isEqualTo(5L);
        ArgumentCaptor<StockMovementRequestDto> mv = ArgumentCaptor.forClass(StockMovementRequestDto.class);
        verify(stockMovementService).createManualMovement(mv.capture());
        assertThat(mv.getValue().movementType()).isEqualTo(MovementType.ADJUSTMENT_OUT);
    }

    @Test
    void applyTemplate_stockAdjustment_defaultDirectionIn() {
        stubTemplate(template(TemplateType.STOCK_ADJUSTMENT,
                payload("productId", 5, "quantity", 3)));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<StockMovementRequestDto> mv = ArgumentCaptor.forClass(StockMovementRequestDto.class);
        verify(stockMovementService).createManualMovement(mv.capture());
        assertThat(mv.getValue().movementType()).isEqualTo(MovementType.ADJUSTMENT_IN);
    }

    @Test
    void applyTemplate_stock_missingProductId_throws() {
        stubTemplate(template(TemplateType.STOCK_ADJUSTMENT, payload("quantity", 3)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("productId zorunludur");
    }

    @Test
    void applyTemplate_stock_nonPositiveQuantity_throws() {
        stubTemplate(template(TemplateType.STOCK_ADJUSTMENT, payload("productId", 5, "quantity", 0)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("quantity 0'dan büyük");
    }

    // ── CUSTOMER_TRANSACTION ──────────────────────────────────────────────────

    @Test
    void applyTemplate_customerCollection_isIncome() {
        stubTemplate(template(TemplateType.CUSTOMER_TRANSACTION,
                payload("amount", 300, "direction", "COLLECTION")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(1L, "X")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(600L));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.targetEntityType()).isEqualTo("CustomerTransaction");
        assertThat(result.targetEntityId()).isEqualTo(600L);
        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().transactionType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void applyTemplate_customerPayment_isExpense() {
        stubTemplate(template(TemplateType.CUSTOMER_TRANSACTION,
                payload("amount", 300, "direction", "PAYMENT")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(1L, "X")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(601L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(tx.getValue().transactionCategory()).isEqualTo(TransactionCategory.GENERAL);
    }

    // ── BANK_TRANSFER ─────────────────────────────────────────────────────────

    @Test
    void applyTemplate_bankTransfer_createsTransfer() {
        stubTemplate(template(TemplateType.BANK_TRANSFER,
                payload("fromAccountId", 1L, "toAccountId", 2L, "amount", 500)));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(700L));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.targetEntityType()).isEqualTo("BankTransfer");
        assertThat(result.targetEntityId()).isEqualTo(700L);
        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(1L);
        assertThat(tx.getValue().transferAccountId()).isEqualTo(2L);
        assertThat(tx.getValue().transactionCategory()).isEqualTo(TransactionCategory.TRANSFER);
    }

    @Test
    void applyTemplate_bankTransfer_missingAccounts_throws() {
        stubTemplate(template(TemplateType.BANK_TRANSFER, payload("amount", 500)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fromAccountId ve toAccountId zorunludur");
    }

    @Test
    void applyTemplate_bankTransfer_sameAccount_throws() {
        stubTemplate(template(TemplateType.BANK_TRANSFER,
                payload("fromAccountId", 1L, "toAccountId", 1L, "amount", 500)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("aynı olamaz");
    }

    @Test
    void applyTemplate_bankTransfer_nonPositiveAmount_throws() {
        stubTemplate(template(TemplateType.BANK_TRANSFER,
                payload("fromAccountId", 1L, "toAccountId", 2L, "amount", 0)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("sıfırdan büyük");
    }

    // ── Payload çıkarıcı String-parse ve doğrulama dalları ────────────────────

    @Test
    void applyTemplate_stringAccountIdAndNullDescription() {
        Template t = template(TemplateType.INCOME, payload("amount", "100", "accountId", "10"));
        t.setDescription(null); // description çözümlemesinin null dalını tetikler
        stubTemplate(t);
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(10L, COMPANY_ID))
                .thenReturn(Optional.of(account(10L, "Ziraat")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(10L);   // String "10" parse edildi
        assertThat(tx.getValue().amount()).isEqualByComparingTo("100");
        assertThat(tx.getValue().description()).isEqualTo("Şablon"); // null → templateName
    }

    @Test
    void applyTemplate_invalidStringAccountId_fallsBackToFirstAccount() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 50, "accountId", "xyz")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(4L, "Akbank")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(4L);
    }

    @Test
    void applyTemplate_blankAccountName_fallsBackToFirstAccount() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 50, "accountName", "   ")));
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(account(6L, "Akbank")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(6L);
    }

    @Test
    void applyTemplate_accountNameSkipsNullBankNameAndMatchesNext() {
        stubTemplate(template(TemplateType.INCOME, payload("amount", 50, "account", "Vakif")));
        BankAccount nullName = account(1L, null); // bankName null → atlanmalı
        when(bankAccountRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByAccountIdDesc(COMPANY_ID))
                .thenReturn(List.of(nullName, account(2L, "Vakif")));
        when(transactionService.createTransaction(any())).thenReturn(txResponse(1L));

        service.applyTemplate(TEMPLATE_ID, null);

        ArgumentCaptor<TransactionRequestDto> tx = ArgumentCaptor.forClass(TransactionRequestDto.class);
        verify(transactionService).createTransaction(tx.capture());
        assertThat(tx.getValue().accountId()).isEqualTo(2L);
    }

    @Test
    void applyTemplate_stock_stringProductIdAndQuantity() {
        stubTemplate(template(TemplateType.STOCK_ADJUSTMENT,
                payload("productId", "5", "quantity", "3")));

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.targetEntityId()).isEqualTo(5L);
        ArgumentCaptor<StockMovementRequestDto> mv = ArgumentCaptor.forClass(StockMovementRequestDto.class);
        verify(stockMovementService).createManualMovement(mv.capture());
        assertThat(mv.getValue().productId()).isEqualTo(5);
        assertThat(mv.getValue().quantity()).isEqualTo(3);
    }

    @Test
    void applyTemplate_stock_nullQuantity_throws() {
        stubTemplate(template(TemplateType.STOCK_ADJUSTMENT, payload("productId", 5)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("quantity 0'dan büyük");
    }

    @Test
    void applyTemplate_invoice_stringLineItemValuesAndInvalidDate() {
        Map<String, Object> stringItem = payload("productId", "1", "quantity", "2");
        // Map olmayan bir öğe de eklenir → filtrelenmeli
        stubTemplate(template(TemplateType.INVOICE,
                payload("customerId", "20", "dueDate", "gecersiz-tarih",
                        "lineItems", List.of("not-a-map", stringItem))));
        InvoiceResponseDto invoice = mock(InvoiceResponseDto.class);
        when(invoice.invoiceId()).thenReturn(801L);
        when(invoiceService.createInvoice(any())).thenReturn(invoice);

        TemplateApplyResponseDto result = service.applyTemplate(TEMPLATE_ID, null);

        assertThat(result.targetEntityId()).isEqualTo(801L);
    }

    @Test
    void applyTemplate_invoice_lineItemsNotAList_throws() {
        stubTemplate(template(TemplateType.INVOICE,
                payload("customerId", 20L, "lineItems", "duz-metin")));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("en az bir kalem");
    }

    @Test
    void applyTemplate_bankTransfer_toAccountNull_throws() {
        // fromAccountId mevcut, toAccountId yok → ikinci koşul tetiklenir
        stubTemplate(template(TemplateType.BANK_TRANSFER,
                payload("fromAccountId", 1L, "amount", 500)));

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fromAccountId ve toAccountId zorunludur");
    }

    // ── Template bulunamadı ───────────────────────────────────────────────────

    @Test
    void applyTemplate_templateNotFound_throws() {
        when(templateRepository.findByTemplateIdAndCompanyCompanyIdAndIsDeletedFalse(TEMPLATE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyTemplate(TEMPLATE_ID, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template not found");
        verify(usageLogRepository, org.mockito.Mockito.never()).save(any());
    }
}
