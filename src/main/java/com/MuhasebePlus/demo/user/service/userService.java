package com.MuhasebePlus.demo.user.service;

import com.MuhasebePlus.demo.user.dto.request.ChangePasswordRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UpdateProfileRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.dto.response.UserResponseDto;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class userService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ─── Admin: Tüm kullanıcıları listele ────────────────────────────────────

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // ─── Admin: ID ile kullanıcı getir ───────────────────────────────────────

    public UserResponseDto getUserById(Long userId) {
        User user = findUserById(userId);
        return toResponseDto(user);
    }

    // ─── Admin: Kullanıcıyı tamamen sil ──────────────────────────────────────

    public void deleteUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // ─── AUTH.1: Yeni kullanıcı kaydı ────────────────────────────────────────

    public UserResponseDto registerUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new RuntimeException("Bu e-posta ile hesap mevcut: " + dto.email());
        }

        User user = new User();
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

    // ─── AUTH.1: Aktif kullanıcı profilini getir ─────────────────────────────

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUserProfile(String email) {
        User user = findUserByEmail(email);
        return toResponseDto(user);
    }

    // ─── AUTH.1: Kendi profilini güncelle ────────────────────────────────────

    public UserResponseDto updateOwnProfile(String email, UpdateProfileRequestDto dto) {
        User user = findUserByEmail(email);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setBirthDate(dto.birthDate());
        return toResponseDto(userRepository.save(user));
    }

    // ─── AUTH.1: Şifre değiştir ───────────────────────────────────────────────

    public void changePassword(String email, ChangePasswordRequestDto dto) {
        User user = findUserByEmail(email);
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Mevcut şifre hatalı");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }

    // ─── AUTH.1: Email güncelle ───────────────────────────────────────────────

    public void changeEmail(String currentEmail, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Bu e-posta ile hesap mevcut: " + newEmail);
        }
        User user = findUserByEmail(currentEmail);
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    // ─── Admin: Kullanıcı rolünü güncelle ────────────────────────────────────

    public UserResponseDto updateUserRole(Long userId, String newRole) {
        UserRole role;
        try {
            role = UserRole.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Geçersiz rol: " + newRole);
        }
        User user = findUserById(userId);
        user.setRole(role);
        return toResponseDto(userRepository.save(user));
    }

    // ─── Admin / Güvenlik: Hesap kilitle ─────────────────────────────────────

    public void lockUser(Long userId) {
        User user = findUserById(userId);
        user.setLocked(true);
        userRepository.save(user);
    }

    // ─── Admin: Hesap kilidini aç ─────────────────────────────────────────────

    public void unlockUser(Long userId) {
        User user = findUserById(userId);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    // ─── Güvenlik: Başarısız giriş sayacını artır (Security servisi çağırır) ─

    public void incrementFailedLoginAttempts(String email) {
        User user = findUserByEmail(email);
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= 5) {
            user.setLocked(true);
        }
        userRepository.save(user);
    }

    // ─── Güvenlik: Başarılı girişte sayacı sıfırla ───────────────────────────

    public void resetFailedLoginAttempts(String email) {
        User user = findUserByEmail(email);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    // ─── Soft delete: Kullanıcıyı pasife al ──────────────────────────────────

    public void deactivateUser(Long userId) {
        User user = findUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    // ─── Internal: Email ile kullanıcı getir (Security modülü kullanır) ──────

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return findUserByEmail(email);
    }

    // ─── Internal: Email benzersizlik kontrolü ────────────────────────────────

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ─── Private yardımcı metodlar ────────────────────────────────────────────

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
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
                user.getCreatedAt()
        );
    }
}
