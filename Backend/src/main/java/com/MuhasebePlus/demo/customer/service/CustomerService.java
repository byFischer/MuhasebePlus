package com.MuhasebePlus.demo.customer.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.customer.dto.request.CustomerNoteRequestDto;
import com.MuhasebePlus.demo.customer.dto.request.CustomerRequestDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerNoteResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerNote;
import com.MuhasebePlus.demo.customer.entity.CustomerType;
import com.MuhasebePlus.demo.customer.repository.CustomerNoteRepository;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;

@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerNoteRepository customerNoteRepository;

    //@Autowired private InvoiceRepository invoiceRepository; //Eğer müşteri silinirken faturaları da silinecekse



    //PUBLIC METOTLAR 

    public CustomerResponseDto createCustomer(CustomerRequestDto dto) {
        if (customerRepository.existsByTaxNumberAndIsDeletedFalse(dto.taxNumber())) {
            throw new RuntimeException("A customer with the same tax number already exists: " + dto.taxNumber());
        }
        Customer customer = new Customer();
        customer.setName(dto.name());
        customer.setTaxNumber(dto.taxNumber());
        customer.setAddress(dto.address());
        customer.setCity(dto.city());
        customer.setPhoneNumber(dto.phoneNumber());
        customer.setType(dto.type());
        customer.setCurrentBalance(BigDecimal.ZERO);
        customer.setDeleted(false);

        Customer saved = customerRepository.save(customer);

        return toResponseDto(saved);
    }

    public List<CustomerResponseDto> getAllCustomers() {
        List<Customer> customers = customerRepository.findByIsDeletedFalse();
        return customers.stream().map(this::toResponseDto).toList();
    }

    public CustomerResponseDto getCustomerById(Long id) {
        Customer c = findCustomerEntityById(id);
        return toResponseDto(c);
    }

        public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto dto) {
            Customer customer = findCustomerEntityById(id);

            if (!customer.getTaxNumber().equals(dto.taxNumber()) &&
                    customerRepository.existsByTaxNumberAndIsDeletedFalse(dto.taxNumber())) {
                throw new RuntimeException("A customer with the same tax number already exists: " + dto.taxNumber());
            }

            customer.setName(dto.name());
            customer.setTaxNumber(dto.taxNumber());
            customer.setAddress(dto.address());
            customer.setCity(dto.city());
            customer.setPhoneNumber(dto.phoneNumber());
            customer.setType(dto.type());

            Customer updated = customerRepository.save(customer);

            return toResponseDto(updated);
        }
        public void softDeleteCustomer(Long id){
            Customer customer = findCustomerEntityById(id);
            customer.setDeleted(true);
            customerRepository.save(customer);
        }
        public CustomerResponseDto restoreCustomer(Long id){
            Customer customer = findCustomerEntityById(id);
            customer.setDeleted(false);
            Customer restored = customerRepository.save(customer);
            return toResponseDto(restored);
        }
        public List<CustomerResponseDto> searchCustomers(String query) {
            List<Customer> customers = customerRepository.searchActive(query);
            return customers.stream().map(this::toResponseDto).toList();
        }

        public List<CustomerResponseDto> getCustomersByType(String type) {
            List<Customer> customers = customerRepository.findByTypeAndIsDeletedFalse(CustomerType.valueOf(type));
            return customers.stream().map(this::toResponseDto).toList();
        }

        public CustomerNoteResponseDto addNote(Long customerId, CustomerNoteRequestDto dto){
            if(!customerRepository.existsById(customerId)){
                throw new RuntimeException("Customer not found with id: " + customerId);
            }
            CustomerNote note = new CustomerNote();
            note.setCustomerId(customerId);
            note.setContent(dto.content());
            CustomerNote saved = customerNoteRepository.save(note);
            return toNoteResponseDto(saved);
        }

        public List<CustomerNoteResponseDto> getNotesByCustomerId(Long customerId){
            if(!customerRepository.existsById(customerId)){
                throw new RuntimeException("Customer not found with id: " + customerId);
            }
            List<CustomerNote> notes = customerNoteRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
            return notes.stream().map(this::toNoteResponseDto).toList();
        }

        public CustomerNoteResponseDto updateNote(Long noteId, CustomerNoteRequestDto dto){
            CustomerNote note = findCustomerNoteEntityById(noteId);
            note.setContent(dto.content());
            CustomerNote updated = customerNoteRepository.save(note);
            return toNoteResponseDto(updated);
        }

        public void deleteNote(Long noteId){
            if(!customerNoteRepository.existsById(noteId)){
                throw new RuntimeException("Customer note not found with id: " + noteId);
            }
            customerNoteRepository.deleteById(noteId);
        }


        //PRIVATE METOTLAR

        private Customer findCustomerEntityById(Long id){
            return customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        }

        private CustomerNote findCustomerNoteEntityById(Long id){
            return customerNoteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer note not found with id: " + id));
        }

        private CustomerResponseDto toResponseDto(Customer c){
            return new CustomerResponseDto(
                    c.getCustomerId(),
                    c.getName(),
                    c.getTaxNumber(),
                    c.getAddress(),
                    c.getCity(),
                    c.getPhoneNumber(),
                    c.getType().name(),
                    c.getCurrentBalance(),
                    c.isDeleted(),
                    c.getCreatedAt(),
                    c.getUpdatedAt()
            );
        }

        private CustomerNoteResponseDto toNoteResponseDto(CustomerNote n){
            return new CustomerNoteResponseDto(
                    n.getNoteId(),
                    n.getCustomerId(),
                    n.getContent(),
                    n.getCreatedAt(),
                    n.getUpdatedAt()
            );
        }

        
}
