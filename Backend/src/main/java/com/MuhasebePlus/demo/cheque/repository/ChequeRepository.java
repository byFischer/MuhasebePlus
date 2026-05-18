package com.MuhasebePlus.demo.cheque.repository;

import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChequeRepository extends JpaRepository<Cheque, Long> {

    Optional<Cheque> findByChequeIdAndCompanyCompanyIdAndIsDeletedFalse(Long chequeId, Long companyId);

    List<Cheque> findByCompanyCompanyIdAndIsDeletedFalseOrderByDueDateAsc(Long companyId);

    List<Cheque> findByCompanyCompanyIdAndStatusAndIsDeletedFalse(Long companyId, ChequeStatus status);

    List<Cheque> findByCompanyCompanyIdAndChequeTypeAndIsDeletedFalse(Long companyId, ChequeType chequeType);

    List<Cheque> findByCompanyCompanyIdAndStatusAndDueDateBeforeAndIsDeletedFalse(
            Long companyId, ChequeStatus status, LocalDate date);

    List<Cheque> findByCompanyCompanyIdAndStatusAndDueDateBetweenAndIsDeletedFalse(
            Long companyId, ChequeStatus status, LocalDate from, LocalDate to);

    boolean existsByChequeNumberAndCompanyCompanyId(String chequeNumber, Long companyId);

    List<Cheque> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
