package com.MuhasebePlus.demo.invoice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;

public interface EInvoiceProfileRepository extends JpaRepository<EInvoiceProfile, Long> {

    Optional<EInvoiceProfile> findByCompanyCompanyIdAndIsActiveTrue(Long companyId);

    @Query(value = "SELECT profile_id, gib_password FROM einvoice_profile WHERE gib_password NOT LIKE 'ENC:%'", nativeQuery = true)
    List<Object[]> findUnencryptedPasswords();

    @Modifying
    @Query(value = "UPDATE einvoice_profile SET gib_password = :encryptedPassword WHERE profile_id = :profileId", nativeQuery = true)
    void updateGibPasswordRaw(@Param("profileId") Long profileId, @Param("encryptedPassword") String encryptedPassword);
}
