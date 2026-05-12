package com.MuhasebePlus.demo.customer.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.MuhasebePlus.demo.customer.dto.request.CustomerNoteRequestDto;
import com.MuhasebePlus.demo.customer.dto.request.CustomerRequestDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerNoteResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;
import com.MuhasebePlus.demo.customer.dto.response.ImportResultDto;
import com.MuhasebePlus.demo.customer.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // CUSTOMER CRUD

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Page<CustomerResponseDto>> getAllCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(customerService.searchCustomersPaged(search, pageable));
        }
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(customerService.getCustomersByTypePaged(type, pageable));
        }
        return ResponseEntity.ok(customerService.getAllCustomersPaged(pageable));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequestDto dto) {
        return ResponseEntity.ok(customerService.updateCustomer(customerId, dto));
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        customerService.softDeleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{customerId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponseDto> restoreCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.restoreCustomer(customerId));
    }

    // CUSTOMER NOTES

    @PostMapping("/{customerId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomerNoteResponseDto> addNote(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerNoteRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.addNote(customerId, dto));
    }

    @GetMapping("/{customerId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<CustomerNoteResponseDto>> getNotesByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getNotesByCustomerId(customerId));
    }

    @PutMapping("/{customerId}/notes/{noteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomerNoteResponseDto> updateNote(
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @Valid @RequestBody CustomerNoteRequestDto dto) {
        return ResponseEntity.ok(customerService.updateNote(noteId, dto));
    }

    @DeleteMapping("/{customerId}/notes/{noteId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long customerId,
            @PathVariable Long noteId) {
        customerService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ImportResultDto> importCustomers(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(customerService.importCustomers(file));
    }
}
