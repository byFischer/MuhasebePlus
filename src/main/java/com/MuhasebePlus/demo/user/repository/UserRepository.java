package com.MuhasebePlus.demo.user.repository;

import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);
}
