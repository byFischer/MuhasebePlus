package com.MuhasebePlus.demo.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.customer.entity.CustomerNote;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    List<CustomerNote> findByCustomerIdAndCompanyCompanyIdOrderByCreatedAtDesc(Long customerId, Long companyId);
    
    Optional<CustomerNote> findByNoteIdAndCompanyCompanyId(Long noteId, Long companyId);
    
    boolean existsByNoteIdAndCompanyCompanyId(Long noteId, Long companyId);
    
    void deleteByCustomerIdAndCompanyCompanyId(Long customerId, Long companyId);

    void deleteByCustomerId(Long customerId);
}
