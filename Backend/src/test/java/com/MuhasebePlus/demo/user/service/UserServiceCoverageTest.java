package com.MuhasebePlus.demo.user.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.notification.entity.Notification;
import com.MuhasebePlus.demo.notification.repository.NotificationRepository;
import com.MuhasebePlus.demo.user.dto.request.ChangePasswordRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UpdateProfileRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.dto.response.UserResponseDto;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceCoverageTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private NotificationRepository notificationRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, companyRepository, notificationRepository);
        ReflectionTestUtils.setField(service, "maxLoginAttempts", 2);
    }

    @Test
    void registerUser_whenEmailAndTaxNumberAreUnique_createsCompanyAndUser() {
        UserRequestDto request = userRequest("ada@example.com", "1234567890");
        Company savedCompany = company();
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(companyRepository.existsByTaxNumber("1234567890")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(11L);
            ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 1, 1, 10, 0));
            return user;
        });

        UserResponseDto result = service.registerUser(request);

        assertThat(result.userId()).isEqualTo(11L);
        assertThat(result.companyId()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test
    void registerUser_rejectsDuplicateEmailOrTaxNumber() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(userRequest("ada@example.com", "1234567890")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");

        when(userRepository.existsByEmail("bora@example.com")).thenReturn(false);
        when(companyRepository.existsByTaxNumber("1234567890")).thenReturn(true);

        assertThatThrownBy(() -> service.registerUser(userRequest("bora@example.com", "1234567890")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tax number");
    }

    @Test
    void profileAndEmailOperationsUseCurrentUser() {
        User user = user("ada@example.com");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);

        assertThat(service.getCurrentUserProfile("ada@example.com").email()).isEqualTo("ada@example.com");
        UserResponseDto updated = service.updateOwnProfile(
                "ada@example.com",
                new UpdateProfileRequestDto("Ada", "Yeni", "555", LocalDate.of(1991, 2, 3))
        );
        assertThat(updated.lastName()).isEqualTo("Yeni");

        service.changeEmail("ada@example.com", "new@example.com");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThatThrownBy(() -> service.changeEmail("ada@example.com", "taken@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
        assertThatThrownBy(() -> service.getCurrentUserProfile("missing@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void changePassword_validatesCurrentPasswordAndSavesEncodedPassword() {
        User user = user("ada@example.com");
        user.setPassword("old-encoded");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-encoded")).thenReturn(true);
        when(passwordEncoder.matches("wrong", "new-encoded")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");

        service.changePassword("ada@example.com", new ChangePasswordRequestDto("old", "new-password"));

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(userRepository).save(user);

        assertThatThrownBy(() -> service.changePassword("ada@example.com", new ChangePasswordRequestDto("wrong", "ignored-password")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void failedLoginAttemptOperationsLockResetAndIgnoreMissingUsers() {
        User user = user("ada@example.com");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.incrementFailedLoginAttempts("ada@example.com");
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isLocked()).isFalse();

        service.incrementFailedLoginAttempts("ada@example.com");
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(user.isLocked()).isTrue();

        service.incrementFailedLoginAttempts("missing@example.com");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.argThat(saved -> saved != null && "missing@example.com".equals(saved.getEmail())));
        verify(notificationRepository, times(2)).save(any(Notification.class));

        service.resetFailedLoginAttempts("ada@example.com");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(service.getUserByEmail("ada@example.com")).isSameAs(user);
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);
        assertThat(service.existsByEmail("ada@example.com")).isTrue();
    }

    private UserRequestDto userRequest(String email, String taxNumber) {
        return new UserRequestDto(
                "Ada",
                "Lovelace",
                email,
                "secret123",
                "555",
                LocalDate.of(1990, 1, 1),
                "Ada Yazilim",
                taxNumber
        );
    }

    private User user(String email) {
        User user = new User();
        user.setUserId(7L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(UserRole.USER);
        user.setPhoneNumber("555");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setCompany(company());
        user.setActive(true);
        user.setLocked(false);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 1, 1, 10, 0));
        return user;
    }

    private Company company() {
        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Ada Yazilim");
        company.setTaxNumber("1234567890");
        return company;
    }
}
