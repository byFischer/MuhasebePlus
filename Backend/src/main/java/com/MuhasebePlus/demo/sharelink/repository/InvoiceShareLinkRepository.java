package com.MuhasebePlus.demo.sharelink.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MuhasebePlus.demo.sharelink.entity.InvoiceShareLink;

public interface InvoiceShareLinkRepository extends JpaRepository<InvoiceShareLink, Long> {

    Optional<InvoiceShareLink> findByTokenAndIsActiveTrue(String token);

    Optional<InvoiceShareLink> findByCompanyCompanyIdAndIsActiveTrue(Long companyId);

    @Modifying
    @Query("UPDATE InvoiceShareLink l SET l.lastAccessedAt = :now, l.accessCount = l.accessCount + 1 WHERE l.id = :id")
    void recordAccess(@Param("id") Long id, @Param("now") LocalDateTime now);
}
