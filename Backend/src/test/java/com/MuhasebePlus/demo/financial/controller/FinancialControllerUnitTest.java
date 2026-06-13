package com.MuhasebePlus.demo.financial.controller;

import com.MuhasebePlus.demo.financial.dto.request.BankAccountRequestDto;
import com.MuhasebePlus.demo.financial.dto.request.BudgetRequestDto;
import com.MuhasebePlus.demo.financial.dto.request.ManualMatchRequestDto;
import com.MuhasebePlus.demo.financial.dto.request.TransactionRequestDto;
import com.MuhasebePlus.demo.financial.dto.request.TransactionTemplateRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankAccountResponseDto;
import com.MuhasebePlus.demo.financial.dto.response.BankStatementResponseDto;
import com.MuhasebePlus.demo.financial.dto.response.BudgetResponseDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionResponseDto;
import com.MuhasebePlus.demo.financial.dto.response.TransactionTemplateResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccountType;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.service.BankAccountService;
import com.MuhasebePlus.demo.financial.service.BudgetService;
import com.MuhasebePlus.demo.financial.service.ReconciliationService;
import com.MuhasebePlus.demo.financial.service.TransactionService;
import com.MuhasebePlus.demo.financial.service.TransactionTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialControllerUnitTest {

    @Mock
    private BudgetService budgetService;
    @Mock
    private BankAccountService bankAccountService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private TransactionTemplateService templateService;
    @Mock
    private ReconciliationService reconciliationService;

    @Test
    void budgetController_delegatesCrudEndpoints() {
        BudgetController controller = new BudgetController(budgetService);
        BudgetRequestDto request = budgetRequest();
        BudgetResponseDto response = budgetResponse(1L);
        when(budgetService.getAllBudgets(2026)).thenReturn(List.of(response));
        when(budgetService.createBudget(request)).thenReturn(response);
        when(budgetService.updateBudget(1L, request)).thenReturn(response);

        assertThat(controller.getAll(2026).getBody()).containsExactly(response);
        assertThat(controller.create(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.update(1L, request).getBody()).isSameAs(response);
        assertThat(controller.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(budgetService).deleteBudget(1L);
    }

    @Test
    void bankAccountController_delegatesCrudEndpointsAndBankList() {
        BankAccountController controller = new BankAccountController(bankAccountService);
        BankAccountRequestDto request = new BankAccountRequestDto("Garanti", "TR00", BankAccountType.BANK, Currency.TRY);
        BankAccountResponseDto response = bankAccountResponse(2L);
        when(bankAccountService.createBankAccount(request)).thenReturn(response);
        when(bankAccountService.getAllBankAccounts()).thenReturn(List.of(response));
        when(bankAccountService.getBankAccountById(2L)).thenReturn(response);
        when(bankAccountService.updateBankAccount(2L, request)).thenReturn(response);
        when(bankAccountService.restoreBankAccount(2L)).thenReturn(response);

        assertThat(controller.createBankAccount(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getBankList().getBody()).isNotEmpty();
        assertThat(controller.getAllBankAccounts().getBody()).containsExactly(response);
        assertThat(controller.getBankAccountById(2L).getBody()).isSameAs(response);
        assertThat(controller.updateBankAccount(2L, request).getBody()).isSameAs(response);
        assertThat(controller.deleteBankAccount(2L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.restoreBankAccount(2L).getBody()).isSameAs(response);
        verify(bankAccountService).softDeleteBankAccount(2L);
    }

    @Test
    void transactionController_selectsFilterEndpointByProvidedQuery() {
        TransactionController controller = new TransactionController(transactionService);
        TransactionRequestDto request = transactionRequest();
        TransactionResponseDto response = transactionResponse(3L);
        when(transactionService.createTransaction(request)).thenReturn(response);
        when(transactionService.getTransactionsByAccount(10L)).thenReturn(List.of(response));
        when(transactionService.getTransactionsByType("INCOME")).thenReturn(List.of(response));
        when(transactionService.getTransactionsByDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(response));
        when(transactionService.getAllTransactions()).thenReturn(List.of(response));
        when(transactionService.getTransactionById(3L)).thenReturn(response);
        when(transactionService.updateTransaction(3L, request)).thenReturn(response);
        when(transactionService.restoreTransaction(3L)).thenReturn(response);

        assertThat(controller.createTransaction(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getAllTransactions(10L, null, null, null).getBody()).containsExactly(response);
        assertThat(controller.getAllTransactions(null, "INCOME", null, null).getBody()).containsExactly(response);
        assertThat(controller.getAllTransactions(null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)).getBody())
                .containsExactly(response);
        assertThat(controller.getAllTransactions(null, " ", null, null).getBody()).containsExactly(response);
        assertThat(controller.getTransactionById(3L).getBody()).isSameAs(response);
        assertThat(controller.updateTransaction(3L, request).getBody()).isSameAs(response);
        assertThat(controller.deleteTransaction(3L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.restoreTransaction(3L).getBody()).isSameAs(response);
        verify(transactionService).softDeleteTransaction(3L);
    }

    @Test
    void transactionTemplateController_selectsFilterAndDelegatesCrud() {
        TransactionTemplateController controller = new TransactionTemplateController(templateService);
        TransactionTemplateRequestDto request = new TransactionTemplateRequestDto(
                "Kira",
                new BigDecimal("1000.00"),
                TransactionType.EXPENSE,
                "Aylik kira",
                "Kira"
        );
        TransactionTemplateResponseDto response = templateResponse(4L);
        when(templateService.createTemplate(request)).thenReturn(response);
        when(templateService.getTemplatesByType("EXPENSE")).thenReturn(List.of(response));
        when(templateService.getAllTemplates()).thenReturn(List.of(response));
        when(templateService.getTemplateById(4L)).thenReturn(response);
        when(templateService.updateTemplate(4L, request)).thenReturn(response);
        when(templateService.restoreTemplate(4L)).thenReturn(response);

        assertThat(controller.createTemplate(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getAllTemplates("EXPENSE").getBody()).containsExactly(response);
        assertThat(controller.getAllTemplates(" ").getBody()).containsExactly(response);
        assertThat(controller.getTemplateById(4L).getBody()).isSameAs(response);
        assertThat(controller.updateTemplate(4L, request).getBody()).isSameAs(response);
        assertThat(controller.deleteTemplate(4L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.restoreTemplate(4L).getBody()).isSameAs(response);
        verify(templateService).softDeleteTemplate(4L);
    }

    @Test
    void bankReconciliationController_delegatesEndpoints() {
        BankReconciliationController controller = new BankReconciliationController(reconciliationService);
        BankStatementResponseDto response = bankStatementResponse(5L);
        ManualMatchRequestDto request = new ManualMatchRequestDto(50L, 500L);
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", "date,amount".getBytes());
        when(reconciliationService.getAll()).thenReturn(List.of(response));
        when(reconciliationService.getById(5L)).thenReturn(response);
        when(reconciliationService.importCsv(file, 2L, LocalDate.of(2026, 1, 31), BigDecimal.ZERO, BigDecimal.TEN))
                .thenReturn(response);
        when(reconciliationService.autoMatch(5L)).thenReturn(response);
        when(reconciliationService.manualMatch(request)).thenReturn(response);
        when(reconciliationService.unmatch(50L)).thenReturn(response);

        assertThat(controller.getAll().getBody()).containsExactly(response);
        assertThat(controller.getById(5L).getBody()).isSameAs(response);
        assertThat(controller.importStatement(file, 2L, LocalDate.of(2026, 1, 31), BigDecimal.ZERO, BigDecimal.TEN).getBody())
                .isSameAs(response);
        assertThat(controller.autoMatch(5L).getBody()).isSameAs(response);
        assertThat(controller.manualMatch(request).getBody()).isSameAs(response);
        assertThat(controller.unmatch(50L).getBody()).isSameAs(response);
    }

    private BudgetRequestDto budgetRequest() {
        return new BudgetRequestDto(2026, 5, "Satis", TransactionType.INCOME, new BigDecimal("1000.00"), "Not");
    }

    private BudgetResponseDto budgetResponse(Long id) {
        return new BudgetResponseDto(id, 2026, 5, "Satis", TransactionType.INCOME, new BigDecimal("1000.00"), "Not", null, null);
    }

    private BankAccountResponseDto bankAccountResponse(Long id) {
        return new BankAccountResponseDto(id, "Garanti", "TR00", "CHECKING", "102.01", "TRY", BigDecimal.TEN, false, null, null);
    }

    private TransactionRequestDto transactionRequest() {
        return new TransactionRequestDto(
                10L,
                null,
                null,
                TransactionType.INCOME,
                null,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 1, 10),
                "Tahsilat",
                "Satis",
                false
        );
    }

    private TransactionResponseDto transactionResponse(Long id) {
        return new TransactionResponseDto(id, 10L, null, null, "INCOME", "GENERAL", new BigDecimal("100.00"),
                LocalDate.of(2026, 1, 10), "Tahsilat", "Satis", false, false, null, null);
    }

    private TransactionTemplateResponseDto templateResponse(Long id) {
        return new TransactionTemplateResponseDto(id, "Kira", new BigDecimal("1000.00"), "EXPENSE", "Aylik", "Kira", false, null, null);
    }

    private BankStatementResponseDto bankStatementResponse(Long id) {
        return new BankStatementResponseDto(id, 2L, "Garanti", LocalDate.of(2026, 1, 31),
                BigDecimal.ZERO, BigDecimal.TEN, "IN_PROGRESS", 1, 0, List.of(), null);
    }
}
