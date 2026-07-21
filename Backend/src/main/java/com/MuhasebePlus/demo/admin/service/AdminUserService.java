package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.admin.dto.AdminUserDto;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cross-tenant kullanıcı yönetimi. Tüm metotlar şirket (company_id) filtresi
 * OLMADAN çalışır; bu yüzden sadece /api/admin/** altından (ADMIN korumalı)
 * çağrılmalıdır.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyContext companyContext;
    private final AdminAuditService adminAuditService;

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getUsers(String search, String role, String status, Pageable pageable) {
        return userRepository.findAll(buildSpec(search, role, status), pageable)
                .map(AdminUserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUser(Long userId) {
        return AdminUserDto.fromEntity(findUser(userId));
    }

    public AdminUserDto updateRole(Long userId, String newRole) {
        UserRole role = parseRole(newRole);
        User user = findUser(userId);

        requireNotSelf(userId, "Kendi rolünüzü değiştiremezsiniz.");
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN
                && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException("Sistemdeki son yöneticinin rolü düşürülemez.");
        }

        UserRole oldRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);
        adminAuditService.logAction("Kullanıcı rolü değiştirildi: %s (%s -> %s)"
                .formatted(user.getEmail(), oldRole, role));
        return AdminUserDto.fromEntity(user);
    }

    public void lockUser(Long userId) {
        requireNotSelf(userId, "Kendi hesabınızı kilitleyemezsiniz.");
        User user = findUser(userId);
        user.setLocked(true);
        userRepository.save(user);
        adminAuditService.logAction("Kullanıcı kilitlendi: " + user.getEmail());
    }

    public void unlockUser(Long userId) {
        User user = findUser(userId);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        adminAuditService.logAction("Kullanıcı kilidi açıldı: " + user.getEmail());
    }

    public void activateUser(Long userId) {
        User user = findUser(userId);
        user.setActive(true);
        userRepository.save(user);
        adminAuditService.logAction("Kullanıcı aktifleştirildi: " + user.getEmail());
    }

    public void deactivateUser(Long userId) {
        requireNotSelf(userId, "Kendi hesabınızı pasifleştiremezsiniz.");
        User user = findUser(userId);
        user.setActive(false);
        userRepository.save(user);
        adminAuditService.logAction("Kullanıcı pasifleştirildi: " + user.getEmail());
    }

    public void resetPassword(Long userId, String newPassword) {
        User user = findUser(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        adminAuditService.logAction("Kullanıcı şifresi sıfırlandı: " + user.getEmail());
    }

    public void deleteUser(Long userId) {
        requireNotSelf(userId, "Kendi hesabınızı silemezsiniz.");
        User user = findUser(userId);
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException("Sistemdeki son yönetici silinemez.");
        }

        String email = user.getEmail();
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    "Bu kullanıcıya bağlı kayıtlar olduğu için silinemiyor. Bunun yerine hesabı pasifleştirin.");
        }
        adminAuditService.logAction("Kullanıcı silindi: %s (ID %d)".formatted(email, userId));
    }


    // PRIVATE METOTLAR

    private Specification<User> buildSpec(String search, String role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                var company = root.join("company", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(company.get("companyName"), "")), like)
                ));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("role"), parseRole(role)));
            }
            if (status != null && !status.isBlank()) {
                switch (status.toUpperCase(Locale.ROOT)) {
                    case "ACTIVE" -> {
                        predicates.add(cb.isTrue(root.get("isActive")));
                        predicates.add(cb.isFalse(root.get("isLocked")));
                    }
                    case "LOCKED" -> predicates.add(cb.isTrue(root.get("isLocked")));
                    case "PASSIVE" -> predicates.add(cb.isFalse(root.get("isActive")));
                    default -> throw new BusinessException("Geçersiz durum filtresi: " + status);
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Geçersiz rol: " + role);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı: " + userId));
    }

    private void requireNotSelf(Long targetUserId, String message) {
        if (targetUserId.equals(companyContext.getCurrentUserId())) {
            throw new BusinessException(message);
        }
    }
}
