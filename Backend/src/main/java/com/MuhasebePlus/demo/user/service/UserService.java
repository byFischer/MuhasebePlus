package com.MuhasebePlus.demo.user.service;

import com.MuhasebePlus.demo.user.dto.request.ChangePasswordRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UpdateProfileRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.dto.response.UserResponseDto;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.notification.entity.NotificationType;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final NotificationRepository notificationRepository;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;


    // Admin işlemleri (listeleme, silme, rol/kilit/durum yönetimi) admin.service.AdminUserService'e taşındı

    public UserResponseDto registerUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("This email is already registered: " + dto.email());
        }

        if (companyRepository.existsByTaxNumber(dto.companyTaxNumber())) {
            throw new RuntimeException("This tax number is already registered: " + dto.companyTaxNumber());
        }

        com.MuhasebePlus.demo.company.entity.Company company = new com.MuhasebePlus.demo.company.entity.Company();
        company.setCompanyName(dto.companyName());
        company.setTaxNumber(dto.companyTaxNumber());
        company.setActive(true);
        company = companyRepository.save(company);

        User user = new User();
        user.setCompany(company);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setPhoneNumber(dto.phoneNumber());
        user.setBirthDate(dto.birthDate());
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);

        return toResponseDto(userRepository.save(user));
    }


    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUserProfile(String email) {
        User user = findUserByEmail(email);
        return toResponseDto(user);
    }


    public UserResponseDto updateOwnProfile(String email, UpdateProfileRequestDto dto) {
        User user = findUserByEmail(email);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setBirthDate(dto.birthDate());
        return toResponseDto(userRepository.save(user));
    }


    public void changePassword(String email, ChangePasswordRequestDto dto) {
        User user = findUserByEmail(email);
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }


    public void changeEmail(String currentEmail, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("This email is already registered: " + newEmail);
        }
        User user = findUserByEmail(currentEmail);
        user.setEmail(newEmail);
        userRepository.save(user);
    }


    public void incrementFailedLoginAttempts(String email) {
        // Kullanıcı yoksa sessizce dön: aksi halde kayıtsız e-posta ile login,
        // user enumeration'a yol açan 500 + e-posta sızıntısı üretir.
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxLoginAttempts) {
                user.setLocked(true);
                log.warn("User account locked after {} failed attempts email={}", attempts, email);
            }
            userRepository.save(user);
            createFailedLoginNotification(user, attempts);
        });
    }


    public void resetFailedLoginAttempts(String email) {
        User user = findUserByEmail(email);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return findUserByEmail(email);
    }


    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }


    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private void createFailedLoginNotification(User user, int attempts) {
        if (user.getCompany() == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setCompany(user.getCompany());
        notification.setUser(user);
        notification.setNotificationType(NotificationType.warning);
        notification.setSubject("Hatalı şifre denemesi");
        notification.setMessage("Hesabınız için hatalı şifre ile giriş denemesi yapıldı. Toplam başarısız deneme: "
                + attempts
                + (user.isLocked() ? ". Güvenlik nedeniyle hesabınız kilitlendi." : "."));
        notification.setSentAt(LocalDateTime.now());
        notification.setRecipientEmail(user.getEmail());
        notificationRepository.save(notification);
    }

    private UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                user.getBirthDate(),
                user.getCreatedAt(),
                user.getCompany() != null ? user.getCompany().getCompanyId() : null,
                user.getCompany() != null ? user.getCompany().getCompanyName() : null
        );
    }
}
