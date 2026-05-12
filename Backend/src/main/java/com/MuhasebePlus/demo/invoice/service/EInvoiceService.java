package com.MuhasebePlus.demo.invoice.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.EInvoiceLog;
import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.repository.EInvoiceLogRepository;
import com.MuhasebePlus.demo.invoice.repository.EInvoiceProfileRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final EInvoiceProfileRepository profileRepository;
    private final EInvoiceLogRepository logRepository;
    private final GibClient gibClient;
    private final CompanyContext companyContext;
    private final SystemLogService systemLogService;

    public EInvoiceLog sendToGib(Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        EInvoiceProfile profile = profileRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId)
                .orElseThrow(() -> new RuntimeException("Aktif e-Fatura profili bulunamadı. Önce profil oluşturun."));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyId(invoice.getCustomerId(), companyId)
                .orElse(null);

        List<InvoiceLineItem> lineItems = lineItemRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);

        Map<Integer, Product> productMap = new HashMap<>();
        for (InvoiceLineItem li : lineItems) {
            if (!productMap.containsKey(li.getProductId())) {
                productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(li.getProductId(), companyId)
                        .ifPresent(p -> productMap.put(li.getProductId(), p));
            }
        }

        String ublTrXml = UblTrBuilder.build(invoice, company, customer, lineItems, productMap);

        EInvoiceLog log = new EInvoiceLog();
        log.setCompany(company);
        log.setInvoiceId(invoiceId);
        log.setRequestBody(ublTrXml);
        log.setSentAt(LocalDateTime.now());
        log.setStatus("SENDING");

        log = logRepository.save(log);

        GibClient.GibResponse response = gibClient.sendInvoice(profile, ublTrXml);

        log.setStatus(response.success ? "SENT" : "FAILED");
        log.setUuid(extractUuid(ublTrXml));
        log.setEnvelopeId(response.ettin);
        log.setResponseBody(response.rawResponse);
        log.setErrorMessage(response.success ? null : response.message);

        logRepository.save(log);

        String logMsg = response.success
                ? "E-Arşiv gönderildi: " + invoice.getInvoiceNumber() + " ETTN: " + response.ettin
                : "E-Arşiv HATASI: " + invoice.getInvoiceNumber() + " - " + response.message;
        boolean isDemo = response.message != null && response.message.contains("DEMO");
        systemLogService.log(isDemo ? LogLevel.INFO : (response.success ? LogLevel.INFO : LogLevel.ERROR), logMsg);

        if (!response.success) {
            throw new RuntimeException("GİB gönderim hatası: " + response.message);
        }

        return log;
    }

    public EInvoiceLog checkGibStatus(Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();

        EInvoiceProfile profile = profileRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId)
                .orElseThrow(() -> new RuntimeException("Aktif e-Fatura profili bulunamadı."));

        List<EInvoiceLog> logs = logRepository.findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(invoiceId, companyId);
        if (logs.isEmpty()) throw new RuntimeException("Bu fatura için GİB kaydı bulunamadı.");

        EInvoiceLog lastLog = logs.get(0);
        if (lastLog.getEnvelopeId() == null || lastLog.getEnvelopeId().isBlank()) {
            throw new RuntimeException("Gönderilmiş bir kayıt bulunamadı, önce gönderin.");
        }

        GibClient.GibResponse response = gibClient.checkStatus(profile, lastLog.getEnvelopeId());

        lastLog.setStatus(response.success ? "SENT" : "FAILED");
        lastLog.setResponseBody(response.rawResponse);
        lastLog.setErrorMessage(response.message);
        return logRepository.save(lastLog);
    }

    public List<EInvoiceLog> getLogs(Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return logRepository.findByInvoiceIdAndCompanyCompanyIdOrderByCreatedAtDesc(invoiceId, companyId);
    }

    public EInvoiceProfile saveProfile(Long companyId, String alias, String password, String defaultSeriesCode) {
        profileRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId).ifPresent(p -> {
            p.setActive(false);
            profileRepository.save(p);
        });

        EInvoiceProfile profile = EInvoiceProfile.builder()
                .company(companyRepository.getReferenceById(companyId))
                .gibAlias(alias)
                .gibPassword(password)
                .defaultSeriesCode(defaultSeriesCode)
                .isActive(true)
                .build();
        return profileRepository.save(profile);
    }

    public EInvoiceProfile getProfile() {
        Long companyId = companyContext.getCurrentCompanyId();
        return profileRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId).orElse(null);
    }

    private String extractUuid(String xml) {
        int s = xml.indexOf("<cbc:UUID>");
        if (s < 0) return null;
        s += 11;
        int e = xml.indexOf("</cbc:UUID>", s);
        if (e < 0) return null;
        return xml.substring(s, e);
    }
}
