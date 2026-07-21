package com.MuhasebePlus.demo.cheque.repository;

import com.MuhasebePlus.demo.cheque.entity.ChequeMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChequeMovementRepository extends JpaRepository<ChequeMovement, Long> {

    List<ChequeMovement> findByChequeIdAndIsDeletedFalseOrderByMovementDateAsc(Long chequeId);

    List<ChequeMovement> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId);
}
