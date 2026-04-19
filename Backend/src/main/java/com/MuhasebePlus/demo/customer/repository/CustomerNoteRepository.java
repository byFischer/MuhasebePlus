package com.MuhasebePlus.demo.customer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.MuhasebePlus.demo.customer.entity.CustomerNote;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    List<CustomerNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    void deleteByCustomerId(Long customerId); //HARD DELETE KULLANILIRSA
}
