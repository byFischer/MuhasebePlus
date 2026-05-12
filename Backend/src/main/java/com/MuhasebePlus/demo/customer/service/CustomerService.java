package com.MuhasebePlus.demo.customer.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.dto.request.CustomerNoteRequestDto;
import com.MuhasebePlus.demo.customer.dto.request.CustomerRequestDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerActivityDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerAgingDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerNoteResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.ImportResultDto;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerNote;
import com.MuhasebePlus.demo.customer.entity.CustomerRole;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerNoteRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoicePayment;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoicePaymentRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;

@Service
@Transactional
public class CustomerService implements HardDeletable {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerNoteRepository customerNoteRepository;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoicePaymentRepository invoicePaymentRepository;

    @Autowired
    private SystemLogService systemLogService;


    // PUBLIC METOTLAR

    public CustomerResponseDto createCustomer(CustomerRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (customerRepository.existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse(dto.taxNumber(), companyId)) {
            throw new RuntimeException("A customer with the same tax number already exists in your company: " + dto.taxNumber());
        }

        if (dto.accountCode() != null && customerRepository.existsByAccountCodeAndCompanyCompanyIdAndIsDeletedFalse(dto.accountCode(), companyId)) {
            throw new RuntimeException("A customer with the same account code already exists in your company: " + dto.accountCode());
        }

        Customer customer = new Customer();
        customer.setCompany(companyRepository.getReferenceById(companyId));
        customer.setName(dto.name());
        customer.setEmail(dto.email());
        customer.setTaxNumber(dto.taxNumber());
        customer.setAddress(dto.address());
        customer.setCity(dto.city());
        customer.setPhoneNumber(dto.phoneNumber());
        customer.setType(dto.type());
        customer.setDeleted(false);

        customer.setAccountCode(dto.accountCode());
        customer.setOpeningBalance(dto.openingBalance() != null ? dto.openingBalance() : BigDecimal.ZERO);
        customer.setOpeningBalanceDate(dto.openingBalanceDate());
        customer.setTaxOffice(dto.taxOffice());
        customer.setIdentityNumber(dto.identityNumber());
        customer.setIban(dto.iban());
        customer.setCurrency(dto.currency());
        customer.setCreditLimit(dto.creditLimit());
        customer.setCustomerRole(dto.customerRole() != null ? dto.customerRole() : com.MuhasebePlus.demo.customer.entity.CustomerRole.BOTH);
        customer.setStatus(dto.status() != null ? dto.status() : com.MuhasebePlus.demo.customer.entity.CustomerStatus.ACTIVE);
        customer.setCustomerGroup(dto.customerGroup());

        Customer saved = customerRepository.save(customer);
        systemLogService.log(LogLevel.INFO, "Müşteri oluşturuldu: " + saved.getName() + " (id=" + saved.getCustomerId() + ")");
        return toResponseDto(saved, BigDecimal.ZERO, false);
    }

    public List<CustomerResponseDto> getAllCustomers() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Customer> customers = customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        Set<Long> overdueIds = buildOverdueCustomerIdSet(companyId);
        return customers.stream()
            .map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO),
                   overdueIds.contains(c.getCustomerId())))
            .toList();
    }

    public CustomerResponseDto getCustomerById(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer c = findCustomerEntityById(id);
        BigDecimal balance = fetchBalance(id, companyId);
        boolean hasOverdue = buildOverdueCustomerIdSet(companyId).contains(id);
        return toResponseDto(c, balance, hasOverdue);
    }

    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = findCustomerEntityById(id);

        if (!customer.getTaxNumber().equals(dto.taxNumber()) &&
                customerRepository.existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse(dto.taxNumber(), companyId)) {
            throw new RuntimeException("A customer with the same tax number already exists in your company: " + dto.taxNumber());
        }

        if (dto.accountCode() != null && !dto.accountCode().equals(customer.getAccountCode()) &&
                customerRepository.existsByAccountCodeAndCompanyCompanyIdAndIsDeletedFalse(dto.accountCode(), companyId)) {
            throw new RuntimeException("A customer with the same account code already exists in your company: " + dto.accountCode());
        }

        customer.setName(dto.name());
        customer.setEmail(dto.email());
        customer.setTaxNumber(dto.taxNumber());
        customer.setAddress(dto.address());
        customer.setCity(dto.city());
        customer.setPhoneNumber(dto.phoneNumber());
        customer.setType(dto.type());

        customer.setAccountCode(dto.accountCode());
        customer.setOpeningBalance(dto.openingBalance() != null ? dto.openingBalance() : BigDecimal.ZERO);
        customer.setOpeningBalanceDate(dto.openingBalanceDate());
        customer.setTaxOffice(dto.taxOffice());
        customer.setIdentityNumber(dto.identityNumber());
        customer.setIban(dto.iban());
        customer.setCurrency(dto.currency());
        customer.setCreditLimit(dto.creditLimit());
        customer.setCustomerRole(dto.customerRole() != null ? dto.customerRole() : customer.getCustomerRole());
        customer.setStatus(dto.status() != null ? dto.status() : customer.getStatus());
        customer.setCustomerGroup(dto.customerGroup());

        Customer updated = customerRepository.save(customer);
        BigDecimal balance = fetchBalance(updated.getCustomerId(), companyId);
        boolean hasOverdue = buildOverdueCustomerIdSet(companyId).contains(updated.getCustomerId());
        return toResponseDto(updated, balance, hasOverdue);
    }

    public void softDeleteCustomer(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoiceRepository.existsByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(id, companyId)) {
            throw new RuntimeException("Bu müşterinin aktif faturaları var, silinemez.");
        }

        Customer customer = findCustomerEntityById(id);
        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    public CustomerResponseDto restoreCustomer(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = findCustomerEntityById(id);
        customer.setDeleted(false);
        customer.setDeletedAt(null);
        Customer restored = customerRepository.save(customer);
        BigDecimal balance = fetchBalance(restored.getCustomerId(), companyId);
        boolean hasOverdue = buildOverdueCustomerIdSet(companyId).contains(restored.getCustomerId());
        return toResponseDto(restored, balance, hasOverdue);
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<Customer> expired = customerRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (Customer customer : expired) {
            customerNoteRepository.deleteByCustomerId(customer.getCustomerId());
            customerRepository.delete(customer);
        }
        return expired.size();
    }

    public List<CustomerResponseDto> searchCustomers(String query) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Customer> customers = customerRepository.searchActive(companyId, query);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        Set<Long> overdueIds = buildOverdueCustomerIdSet(companyId);
        return customers.stream()
            .map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO),
                   overdueIds.contains(c.getCustomerId())))
            .toList();
    }

    public Page<CustomerResponseDto> getAllCustomersPaged(Pageable pageable) {
        Long companyId = companyContext.getCurrentCompanyId();
        Page<Customer> page = customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId, pageable);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        Set<Long> overdueIds = buildOverdueCustomerIdSet(companyId);
        return page.map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO),
                overdueIds.contains(c.getCustomerId())));
    }

    public Page<CustomerResponseDto> searchCustomersPaged(String query, Pageable pageable) {
        Long companyId = companyContext.getCurrentCompanyId();
        Page<Customer> page = customerRepository.searchActivePage(companyId, query, pageable);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        Set<Long> overdueIds = buildOverdueCustomerIdSet(companyId);
        return page.map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO),
                overdueIds.contains(c.getCustomerId())));
    }

    public Page<CustomerResponseDto> getCustomersByTypePaged(String type, Pageable pageable) {
        Long companyId = companyContext.getCurrentCompanyId();
        Page<Customer> page = customerRepository.findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType.valueOf(type), companyId, pageable);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        Set<Long> overdueIds = buildOverdueCustomerIdSet(companyId);
        return page.map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO),
                overdueIds.contains(c.getCustomerId())));
    }

    public List<CustomerResponseDto> getCustomersByType(String type) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Customer> customers = customerRepository.findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType.valueOf(type), companyId);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        Set<Long> overdueIds = buildOverdueCustomerIdSet(companyId);
        return customers.stream()
            .map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO),
                   overdueIds.contains(c.getCustomerId())))
            .toList();
    }

    public List<CustomerActivityDto> getCustomerActivity(Long customerId, LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = findCustomerEntityById(customerId);

        BigDecimal opening = customer.getOpeningBalance() != null ? customer.getOpeningBalance() : BigDecimal.ZERO;

        List<CustomerActivityDto> lines = new java.util.ArrayList<>();

        if (opening.compareTo(BigDecimal.ZERO) != 0) {
            lines.add(new CustomerActivityDto(
                customer.getOpeningBalanceDate(), "ACILIS", "Acilis Bakiyesi", "—",
                opening.compareTo(BigDecimal.ZERO) > 0 ? opening : BigDecimal.ZERO,
                opening.compareTo(BigDecimal.ZERO) < 0 ? opening.negate() : BigDecimal.ZERO,
                opening
            ));
        }

        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .stream()
            .filter(i -> !i.getInvoiceDate().isBefore(startDate) && !i.getInvoiceDate().isAfter(endDate))
            .sorted(Comparator.comparing(Invoice::getInvoiceDate))
            .toList();

        BigDecimal runningBalance = opening;
        for (Invoice inv : invoices) {
            boolean isSale = inv.getInvoiceType() == InvoiceType.sale;
            String desc = isSale ? "Satis Faturasi" : "Alis Faturasi";
            if (inv.isCancelled()) desc += " (IPTAL)";

            if (isSale) runningBalance = runningBalance.add(inv.getTotalAmount());
            else runningBalance = runningBalance.subtract(inv.getTotalAmount());

            lines.add(new CustomerActivityDto(
                inv.getInvoiceDate(), "FATURA", desc, inv.getInvoiceNumber(),
                isSale ? inv.getTotalAmount() : BigDecimal.ZERO,
                !isSale ? inv.getTotalAmount() : BigDecimal.ZERO,
                runningBalance
            ));

            List<InvoicePayment> payments = invoicePaymentRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(inv.getInvoiceId(), companyId);
            for (InvoicePayment pmt : payments) {
                if (pmt.getPaymentDate().isBefore(startDate) || pmt.getPaymentDate().isAfter(endDate)) continue;
                runningBalance = runningBalance.subtract(pmt.getAmount());
                lines.add(new CustomerActivityDto(
                    pmt.getPaymentDate(), "TAHSILAT", "Tahsilat / Odeme", "#PMT" + pmt.getPaymentId(),
                    BigDecimal.ZERO, pmt.getAmount(), runningBalance
                ));
            }
        }

        lines.sort(Comparator.comparing(CustomerActivityDto::date));
        return lines;
    }

    public List<CustomerAgingDto> getCustomerAging() {
        Long companyId = companyContext.getCurrentCompanyId();
        LocalDate today = LocalDate.now();

        List<Invoice> openInvoices = new ArrayList<>();
        openInvoices.addAll(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.pending, InvoiceType.sale, companyId));
        openInvoices.addAll(invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyIdAndIsDeletedFalse(
                PaymentStatus.overdue, InvoiceType.sale, companyId));

        List<Customer> customers = customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId);
        Map<Long, Customer> customerMap = customers.stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c));

        Map<Long, CustomerAgingDto> agingMap = new java.util.HashMap<>();

        for (Invoice inv : openInvoices) {
            if (inv.getCustomerId() == null || inv.getDueDate() == null) continue;
            Customer c = customerMap.get(inv.getCustomerId());
            if (c == null) continue;

            long days = java.time.temporal.ChronoUnit.DAYS.between(inv.getDueDate(), today);
            if (days <= 0) continue;

            CustomerAgingDto dto = agingMap.computeIfAbsent(inv.getCustomerId(), k ->
                new CustomerAgingDto(inv.getCustomerId(), c.getName(), c.getAccountCode(),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            agingMap.put(inv.getCustomerId(),
                new CustomerAgingDto(dto.customerId(), dto.customerName(), dto.accountCode(),
                    dto.totalAR().add(amount),
                    days <= 30 ? dto.bucket0to30().add(amount) : dto.bucket0to30(),
                    days > 30 && days <= 60 ? dto.bucket31to60().add(amount) : dto.bucket31to60(),
                    days > 60 && days <= 90 ? dto.bucket61to90().add(amount) : dto.bucket61to90(),
                    days > 90 ? dto.bucket90plus().add(amount) : dto.bucket90plus()));
        }

        return agingMap.values().stream()
                .sorted(Comparator.comparing(CustomerAgingDto::totalAR).reversed())
                .toList();
    }

    public CustomerNoteResponseDto addNote(Long customerId, CustomerNoteRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (!customerRepository.existsByCustomerIdAndCompanyCompanyId(customerId, companyId)) {
            throw new RuntimeException("Customer not found or access denied for id: " + customerId);
        }

        CustomerNote note = new CustomerNote();
        note.setCompany(companyRepository.getReferenceById(companyId));
        note.setCustomerId(customerId);
        note.setContent(dto.content());

        CustomerNote saved = customerNoteRepository.save(note);
        return toNoteResponseDto(saved);
    }

    public List<CustomerNoteResponseDto> getNotesByCustomerId(Long customerId) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (!customerRepository.existsByCustomerIdAndCompanyCompanyId(customerId, companyId)) {
            throw new RuntimeException("Customer not found or access denied for id: " + customerId);
        }

        List<CustomerNote> notes = customerNoteRepository.findByCustomerIdAndCompanyCompanyIdOrderByCreatedAtDesc(customerId, companyId);
        return notes.stream().map(this::toNoteResponseDto).toList();
    }

    public CustomerNoteResponseDto updateNote(Long noteId, CustomerNoteRequestDto dto) {
        CustomerNote note = findCustomerNoteEntityById(noteId);
        note.setContent(dto.content());
        CustomerNote updated = customerNoteRepository.save(note);
        return toNoteResponseDto(updated);
    }

    public void deleteNote(Long noteId) {
        CustomerNote note = findCustomerNoteEntityById(noteId);
        customerNoteRepository.delete(note);
    }

    public ImportResultDto importCustomers(MultipartFile file) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        String filename = file.getOriginalFilename();

        try {
            if (filename != null && filename.endsWith(".xlsx")) {
                successCount = importFromExcel(file, companyId, errors);
            } else if (filename != null && (filename.endsWith(".csv") || filename.endsWith(".txt"))) {
                successCount = importFromCsv(file, companyId, errors);
            } else {
                errors.add("Unsupported file format. Please upload .csv or .xlsx file.");
            }
        } catch (Exception e) {
            errors.add("File processing error: " + e.getMessage());
        }

        systemLogService.log(LogLevel.INFO, "Toplu müşteri aktarımı: " + successCount + " başarılı, " + errors.size() + " hatalı");
        return new ImportResultDto(successCount, errors.size(), errors);
    }


    // PRIVATE METOTLAR - IMPORT

    private int importFromExcel(MultipartFile file, Long companyId, List<String> errors) throws Exception {
        int count = 0;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    CustomerRequestDto dto = parseExcelRow(row);
                    createCustomer(dto);
                    count++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return count;
    }

    private CustomerRequestDto parseExcelRow(Row row) {
        return new CustomerRequestDto(
            getCellString(row, 0),                    // name
            getCellString(row, 1),                    // taxNumber
            getCellString(row, 2),                    // address
            getCellString(row, 3),                    // city
            getCellString(row, 4),                    // phoneNumber
            parseCustomerType(getCellString(row, 5)), // type
            getCellString(row, 6),                    // email
            getCellString(row, 7),                    // accountCode
            getCellBigDecimal(row, 8),                // openingBalance
            getCellLocalDate(row, 9),                 // openingBalanceDate
            getCellString(row, 10),                   // taxOffice
            getCellString(row, 11),                   // identityNumber
            getCellString(row, 12),                   // iban
            parseCurrency(getCellString(row, 13)),    // currency
            getCellBigDecimal(row, 14),               // creditLimit
            parseCustomerRole(getCellString(row, 15)), // customerRole
            null,                                      // status (defaults in service)
            getCellString(row, 16)                     // customerGroup
        );
    }

    private int importFromCsv(MultipartFile file, Long companyId, List<String> errors) throws Exception {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            reader.readLine(); // skip header
            String line;
            int lineNum = 2;
            while ((line = reader.readLine()) != null) {
                try {
                    CustomerRequestDto dto = parseCsvLine(line);
                    createCustomer(dto);
                    count++;
                } catch (Exception e) {
                    errors.add("Line " + lineNum + ": " + e.getMessage());
                }
                lineNum++;
            }
        }
        return count;
    }

    private CustomerRequestDto parseCsvLine(String line) {
        String[] parts = line.split(",", -1);
        return new CustomerRequestDto(
            getPart(parts, 0),                                                                          // name
            getPart(parts, 1),                                                                          // taxNumber
            getPart(parts, 2),                                                                          // address
            getPart(parts, 3),                                                                          // city
            getPart(parts, 4),                                                                          // phoneNumber
            parseCustomerType(getPart(parts, 5)),                                                       // type
            getPart(parts, 6),                                                                          // email
            getPart(parts, 7),                                                                          // accountCode
            parseBigDecimal(getPart(parts, 8)),                                                         // openingBalance
            parseLocalDate(getPart(parts, 9)),                                                          // openingBalanceDate
            getPart(parts, 10),                                                                         // taxOffice
            getPart(parts, 11),                                                                         // identityNumber
            getPart(parts, 12),                                                                         // iban
            parseCurrency(getPart(parts, 13)),                                                          // currency
            parseBigDecimal(getPart(parts, 14)),                                                        // creditLimit
            parseCustomerRole(getPart(parts, 15)),                                                       // customerRole
            null,                                                                                         // status
            getPart(parts, 16)                                                                            // customerGroup
        );
    }

    private String getCellString(Row row, int index) {
        if (row.getCell(index) == null) return null;
        return row.getCell(index).getStringCellValue().trim();
    }

    private BigDecimal getCellBigDecimal(Row row, int index) {
        if (row.getCell(index) == null) return null;
        try {
            return BigDecimal.valueOf(row.getCell(index).getNumericCellValue());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getCellLocalDate(Row row, int index) {
        if (row.getCell(index) == null) return null;
        try {
            return row.getCell(index).getLocalDateTimeCellValue().toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    private String getPart(String[] parts, int index) {
        return index < parts.length ? parts[index].trim() : null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        return new BigDecimal(value.trim());
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private CustomerType parseCustomerType(String value) {
        if (value == null || value.isBlank()) return CustomerType.CORPORATE;
        try {
            return CustomerType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CustomerType.CORPORATE;
        }
    }

    private CustomerRole parseCustomerRole(String value) {
        if (value == null || value.isBlank()) return CustomerRole.BOTH;
        try {
            return CustomerRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CustomerRole.BOTH;
        }
    }

    private Currency parseCurrency(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Currency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // PRIVATE METOTLAR

    private Map<Long, BigDecimal> buildBalanceMap(Long companyId) {
        Map<Long, BigDecimal> saleBalances = invoiceRepository
            .calculateOutstandingBalancesByCompany(companyId, InvoiceType.sale, PaymentStatus.paid)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (BigDecimal) row[1]
            ));

        Map<Long, BigDecimal> purchaseBalances = invoiceRepository
            .calculateOutstandingBalancesByCompanyPurchase(companyId, InvoiceType.purchase, PaymentStatus.paid)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((BigDecimal) row[1]).negate()
            ));

        List<Customer> allCustomers = customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId);

        Set<Long> allCustomerIds = new HashSet<>();
        allCustomerIds.addAll(saleBalances.keySet());
        allCustomerIds.addAll(purchaseBalances.keySet());
        for (Customer c : allCustomers) allCustomerIds.add(c.getCustomerId());

        Map<Long, BigDecimal> combined = new java.util.HashMap<>();
        for (Long id : allCustomerIds) {
            BigDecimal sale = saleBalances.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal purchase = purchaseBalances.getOrDefault(id, BigDecimal.ZERO);
            Customer c = allCustomers.stream().filter(x -> x.getCustomerId().equals(id)).findFirst().orElse(null);
            BigDecimal opening = (c != null && c.getOpeningBalance() != null) ? c.getOpeningBalance() : BigDecimal.ZERO;
            combined.put(id, sale.add(purchase).add(opening));
        }
        return combined;
    }

    private BigDecimal fetchBalance(Long customerId, Long companyId) {
        Customer c = customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
                .orElse(null);
        BigDecimal opening = (c != null && c.getOpeningBalance() != null) ? c.getOpeningBalance() : BigDecimal.ZERO;

        BigDecimal saleBalance = invoiceRepository.calculateOutstandingBalanceForCustomer(
            customerId, companyId, InvoiceType.sale, PaymentStatus.paid);
        BigDecimal purchaseBalance = invoiceRepository.calculateOutstandingBalanceForCustomerPurchase(
            customerId, companyId, InvoiceType.purchase, PaymentStatus.paid);
        BigDecimal sale = saleBalance != null ? saleBalance : BigDecimal.ZERO;
        BigDecimal purchase = purchaseBalance != null ? purchaseBalance : BigDecimal.ZERO;
        return sale.subtract(purchase).add(opening);
    }

    private Customer findCustomerEntityById(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        if (!customer.getCompany().getCompanyId().equals(companyId)) {
            throw new RuntimeException("Bu kaydı görüntüleme yetkiniz yok");
        }
        return customer;
    }

    private CustomerNote findCustomerNoteEntityById(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        CustomerNote note = customerNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer note not found with id: " + id));

        if (!note.getCompany().getCompanyId().equals(companyId)) {
            throw new RuntimeException("Bu kaydı görüntüleme yetkiniz yok");
        }
        return note;
    }

    private CustomerResponseDto toResponseDto(Customer c, BigDecimal balance, boolean hasOverdueInvoices) {
        return new CustomerResponseDto(
                c.getCustomerId(),
                c.getName(),
                c.getEmail(),
                c.getTaxNumber(),
                c.getAddress(),
                c.getCity(),
                c.getPhoneNumber(),
                c.getType().name(),
                balance,
                hasOverdueInvoices,
                c.isDeleted(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getAccountCode(),
                c.getOpeningBalance(),
                c.getOpeningBalanceDate(),
                c.getTaxOffice(),
                c.getIdentityNumber(),
                c.getIban(),
                c.getCurrency() != null ? c.getCurrency().name() : null,
                c.getCreditLimit(),
                c.getCustomerRole() != null ? c.getCustomerRole().name() : null,
                c.getStatus() != null ? c.getStatus().name() : "ACTIVE",
                c.getCustomerGroup()
        );
    }

    private Set<Long> buildOverdueCustomerIdSet(Long companyId) {
        return new HashSet<>(invoiceRepository.findCustomerIdsWithOverdueInvoices(companyId));
    }

    private CustomerNoteResponseDto toNoteResponseDto(CustomerNote n) {
        return new CustomerNoteResponseDto(
                n.getNoteId(),
                n.getCustomerId(),
                n.getContent(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
