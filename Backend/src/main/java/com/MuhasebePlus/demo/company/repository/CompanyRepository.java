package com.MuhasebePlus.demo.company.repository;

import com.MuhasebePlus.demo.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByTaxNumber(String taxNumber);
}
