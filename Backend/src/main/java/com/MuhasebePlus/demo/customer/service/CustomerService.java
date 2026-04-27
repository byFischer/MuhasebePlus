package com.MuhasebePlus.demo.customer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.dto.request.CustomerNoteRequestDto;
import com.MuhasebePlus.demo.customer.dto.request.CustomerRequestDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerNoteResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerNote;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerNoteRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
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
    private SystemLogService systemLogService;


    // PUBLIC METOTLAR

    public CustomerResponseDto createCustomer(CustomerRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (customerRepository.existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse(dto.taxNumber(), companyId)) {
            throw new RuntimeException("A customer with the same tax number already exists in your company: " + dto.taxNumber());
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

        Customer saved = customerRepository.save(customer);
        systemLogService.log(LogLevel.INFO, "Müşteri oluşturuldu: " + saved.getName() + " (id=" + saved.getCustomerId() + ")");
        return toResponseDto(saved, BigDecimal.ZERO);
    }

    public List<CustomerResponseDto> getAllCustomers() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Customer> customers = customerRepository.findByCompanyCompanyIdAndIsDeletedFalse(companyId);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        return customers.stream()
            .map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO)))
            .toList();
    }

    public CustomerResponseDto getCustomerById(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer c = findCustomerEntityById(id);
        return toResponseDto(c, fetchBalance(id, companyId));
    }

    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = findCustomerEntityById(id);

        if (!customer.getTaxNumber().equals(dto.taxNumber()) &&
                customerRepository.existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse(dto.taxNumber(), companyId)) {
            throw new RuntimeException("A customer with the same tax number already exists in your company: " + dto.taxNumber());
        }

        customer.setName(dto.name());
        customer.setEmail(dto.email());
        customer.setTaxNumber(dto.taxNumber());
        customer.setAddress(dto.address());
        customer.setCity(dto.city());
        customer.setPhoneNumber(dto.phoneNumber());
        customer.setType(dto.type());

        Customer updated = customerRepository.save(customer);
        return toResponseDto(updated, fetchBalance(updated.getCustomerId(), companyId));
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
        return toResponseDto(restored, fetchBalance(restored.getCustomerId(), companyId));
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
        return customers.stream()
            .map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO)))
            .toList();
    }

    public List<CustomerResponseDto> getCustomersByType(String type) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Customer> customers = customerRepository.findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType.valueOf(type), companyId);
        Map<Long, BigDecimal> balances = buildBalanceMap(companyId);
        return customers.stream()
            .map(c -> toResponseDto(c, balances.getOrDefault(c.getCustomerId(), BigDecimal.ZERO)))
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


    // PRIVATE METOTLAR

    private Map<Long, BigDecimal> buildBalanceMap(Long companyId) {
        return invoiceRepository
            .calculateOutstandingBalancesByCompany(companyId, InvoiceType.sale, PaymentStatus.paid)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (BigDecimal) row[1]
            ));
    }

    private BigDecimal fetchBalance(Long customerId, Long companyId) {
        BigDecimal balance = invoiceRepository.calculateOutstandingBalanceForCustomer(
            customerId, companyId, InvoiceType.sale, PaymentStatus.paid);
        return balance != null ? balance : BigDecimal.ZERO;
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

    private CustomerResponseDto toResponseDto(Customer c, BigDecimal balance) {
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
                c.isDeleted(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
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
