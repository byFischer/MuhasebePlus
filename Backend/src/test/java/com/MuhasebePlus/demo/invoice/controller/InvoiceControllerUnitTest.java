package com.MuhasebePlus.demo.invoice.controller;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.dto.request.CollectionPromiseRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoicePaymentRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceSeriesRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.CollectionPromiseResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceLineItemResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoicePaymentResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.EInvoiceLog;
import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;
import com.MuhasebePlus.demo.invoice.entity.InvoiceSeries;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.LateFeeConfig;
import com.MuhasebePlus.demo.invoice.entity.PaymentMethod;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.service.EInvoiceService;
import com.MuhasebePlus.demo.invoice.service.InvoicePaymentService;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;
import com.MuhasebePlus.demo.invoice.service.LateFeeService;
import com.MuhasebePlus.demo.invoice.service.PdfInvoiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerUnitTest {

    @Mock private InvoiceService invoiceService;
    @Mock private PdfInvoiceService pdfInvoiceService;
    @Mock private EInvoiceService eInvoiceService;
    @Mock private CompanyContext companyContext;
    @Mock private InvoicePaymentService invoicePaymentService;
    @Mock private LateFeeService lateFeeService;

    @InjectMocks private InvoiceController invoiceController;
    @InjectMocks private InvoicePaymentController paymentController;
    @InjectMocks private LateFeeController lateFeeController;

    @Test
    void invoiceControllerEndpoints_delegateToServices() {
        InvoiceRequestDto request = invoiceRequest();
        InvoiceResponseDto response = invoiceResponse(100L);
        PageRequest pageable = PageRequest.of(0, 20);
        InvoiceSeriesRequestDto seriesRequest = new InvoiceSeriesRequestDto("ARS", InvoiceType.sale, "ARS", 2026, true);
        InvoiceSeries series = InvoiceSeries.builder()
                .seriesId(9L)
                .seriesCode("ARS")
                .invoiceType(InvoiceType.sale)
                .prefix("ARS")
                .year(2026)
                .lastSequenceNumber(12L)
                .isActive(true)
                .build();
        CollectionPromiseRequestDto promiseRequest = new CollectionPromiseRequestDto(
                LocalDate.now().plusDays(1), new BigDecimal("100.00"), "Not");
        CollectionPromiseResponseDto promiseResponse = new CollectionPromiseResponseDto(
                5L, 100L, promiseRequest.promisedDate(), promiseRequest.promisedAmount(), "Not", false, null, null);
        EInvoiceLog eInvoiceLog = new EInvoiceLog();
        EInvoiceProfile profile = EInvoiceProfile.builder().gibAlias("alias").build();
        when(invoiceService.createInvoice(request)).thenReturn(response);
        when(invoiceService.getInvoicesPaged(PaymentStatus.pending, InvoiceType.sale,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "abc", pageable))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 1));
        when(invoiceService.getInvoiceById(100L)).thenReturn(response);
        when(invoiceService.getInvoiceByCustomerId(7L)).thenReturn(List.of(response));
        when(invoiceService.updateInvoice(100L, request)).thenReturn(response);
        when(invoiceService.confirmInvoice(100L)).thenReturn(response);
        when(invoiceService.cancelInvoice(100L, "iptal")).thenReturn(response);
        when(invoiceService.createReturnInvoice(100L, request)).thenReturn(response);
        when(invoiceService.updatePaymentStatus(100L, PaymentStatus.paid)).thenReturn(response);
        when(invoiceService.restoreInvoice(100L)).thenReturn(response);
        when(pdfInvoiceService.generatePdf(100L)).thenReturn(new byte[]{1, 2});
        when(invoiceService.createSeries(seriesRequest)).thenReturn(series);
        when(invoiceService.getSeries()).thenReturn(List.of(series));
        when(invoiceService.assignNextInvoiceNumber(InvoiceType.sale, "ARS")).thenReturn("ARS2026000013");
        when(invoiceService.createCollectionPromise(100L, promiseRequest)).thenReturn(promiseResponse);
        when(invoiceService.getCollectionPromises(100L)).thenReturn(List.of(promiseResponse));
        when(eInvoiceService.sendToGib(100L)).thenReturn(eInvoiceLog);
        when(eInvoiceService.getLogs(100L)).thenReturn(List.of(eInvoiceLog));
        when(eInvoiceService.checkGibStatus(100L)).thenReturn(eInvoiceLog);
        when(eInvoiceService.getProfile()).thenReturn(profile);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(eInvoiceService.saveProfile(1L, "alias", "secret", "ARS")).thenReturn(profile);

        assertThat(invoiceController.createInvoice(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(invoiceController.getInvoices(PaymentStatus.pending, InvoiceType.sale,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "abc", pageable)
                .getBody().getContent()).containsExactly(response);
        assertThat(invoiceController.getInvoiceById(100L).getBody()).isSameAs(response);
        assertThat(invoiceController.getInvoicesByCustomerId(7L).getBody()).containsExactly(response);
        assertThat(invoiceController.updateInvoice(100L, request).getBody()).isSameAs(response);
        assertThat(invoiceController.confirmInvoice(100L).getBody()).isSameAs(response);
        assertThat(invoiceController.cancelInvoice(100L, "iptal").getBody()).isSameAs(response);
        assertThat(invoiceController.createReturnInvoice(100L, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(invoiceController.updatePaymentStatus(100L, PaymentStatus.paid).getBody()).isSameAs(response);
        assertThat(invoiceController.deleteInvoice(100L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(invoiceController.restoreInvoice(100L).getBody()).isSameAs(response);
        assertThat(invoiceController.downloadPdf(100L).getHeaders().getFirst("Content-Type")).isEqualTo("application/pdf");
        assertThat(invoiceController.createSeries(seriesRequest).getBody().seriesCode()).isEqualTo("ARS");
        assertThat(invoiceController.getSeries().getBody().get(0).lastSequenceNumber()).isEqualTo(12L);
        assertThat(invoiceController.getNextInvoiceNumber(InvoiceType.sale, "ARS").getBody()).isEqualTo("ARS2026000013");
        assertThat(invoiceController.createPromise(100L, promiseRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(invoiceController.getPromises(100L).getBody()).containsExactly(promiseResponse);
        assertThat(invoiceController.fulfillPromise(100L, 5L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(invoiceController.sendToGib(100L).getBody()).isSameAs(eInvoiceLog);
        assertThat(invoiceController.getGibLogs(100L).getBody()).containsExactly(eInvoiceLog);
        assertThat(invoiceController.checkGibStatus(100L).getBody()).isSameAs(eInvoiceLog);
        assertThat(invoiceController.getGibProfile().getBody()).isSameAs(profile);
        assertThat(invoiceController.saveGibProfile("alias", "secret", "ARS").getBody()).isSameAs(profile);
        verify(invoiceService).deleteInvoice(100L);
        verify(invoiceService).fulfillCollectionPromise(5L);
    }

    @Test
    void paymentAndLateFeeControllers_delegateToServices() {
        InvoicePaymentRequestDto paymentRequest = new InvoicePaymentRequestDto(
                new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.cash, 3L, "Not", null);
        InvoicePaymentResponseDto paymentResponse = new InvoicePaymentResponseDto(
                8L, 100L, new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.cash, 3L, "Kasa", "Not", null);
        LateFeeConfig config = LateFeeConfig.builder()
                .monthlyRate(new BigDecimal("2.50"))
                .gracePeriodDays(5)
                .isActive(true)
                .build();
        when(invoicePaymentService.createPayment(100L, paymentRequest)).thenReturn(paymentResponse);
        when(invoicePaymentService.getPaymentsByInvoiceId(100L)).thenReturn(List.of(paymentResponse));
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(lateFeeService.calculateLateFee(100L, 1L)).thenReturn(new BigDecimal("12.50"));
        when(lateFeeService.getConfig(1L)).thenReturn(config);
        when(lateFeeService.saveConfig(1L, new BigDecimal("3.00"), 7)).thenReturn(config);

        assertThat(paymentController.createPayment(100L, paymentRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(paymentController.getPayments(100L).getBody()).containsExactly(paymentResponse);
        assertThat(paymentController.deletePayment(100L, 8L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(lateFeeController.getLateFee(100L).getBody()).containsEntry("lateFee", new BigDecimal("12.50"));
        assertThat(lateFeeController.getLateFeeConfig().getBody()).isSameAs(config);
        assertThat(lateFeeController.updateLateFeeConfig(new BigDecimal("3.00"), 7).getBody()).isSameAs(config);
        verify(invoicePaymentService).deletePayment(8L);
    }

    private InvoiceRequestDto invoiceRequest() {
        return new InvoiceRequestDto(
                "FTR-100",
                7L,
                InvoiceType.sale,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 25),
                List.of(new InvoiceLineItemRequestDto(
                        1, 1, null, new BigDecimal("100.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO, null, null, null)),
                Currency.TRY,
                BigDecimal.ONE,
                null,
                BigDecimal.ZERO,
                "Aciklama",
                "Adres"
        );
    }

    private InvoiceResponseDto invoiceResponse(Long id) {
        return new InvoiceResponseDto(
                id,
                "FTR-" + id,
                7L,
                "ABC Ltd",
                "sale",
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 25),
                "pending",
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                new BigDecimal("120.00"),
                List.of(new InvoiceLineItemResponseDto(
                        1, 1, "Urun", "BRC", 1,
                        new BigDecimal("100.00"), new BigDecimal("20.00"),
                        new BigDecimal("120.00"), BigDecimal.ZERO, BigDecimal.ZERO)),
                null,
                null,
                false,
                "TRY",
                BigDecimal.ONE,
                new BigDecimal("120.00"),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Aciklama",
                "Adres",
                false,
                null,
                null,
                "ARS",
                1L
        );
    }
}
