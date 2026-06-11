package com.MuhasebePlus.demo.user.repository;

import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    long countByRole(UserRole role);

    long countByIsActiveTrue();

    long countByIsActiveFalse();

    long countByIsLockedTrue();

    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByCompanyCompanyId(Long companyId);

    List<User> findByCompanyCompanyId(Long companyId);
}
