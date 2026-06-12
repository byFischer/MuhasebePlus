package com.MuhasebePlus.demo.admin.bootstrap;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        setCredentials("admin@test.com", "secret123");
    }

    @Test
    void run_whenCredentialsMissing_doesNothing() {
        setCredentials("", "");

        bootstrap.run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(companyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_whenAdminAlreadyExists_doesNothing() {
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        bootstrap.run(null);

        verify(companyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_whenEmailAlreadyExists_doesNotPromoteExistingUser() {
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        bootstrap.run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_whenPasswordTooShort_doesNothing() {
        setCredentials("admin@test.com", "short");
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);

        bootstrap.run(null);

        verify(companyRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_whenValid_createsSystemCompanyAndAdminUser() {
        Company savedCompany = Company.builder()
                .companyId(1L)
                .companyName("Sistem Yonetimi")
                .isActive(true)
                .build();
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(companyRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(savedCompany);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        bootstrap.run(null);

        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getCompany()).isEqualTo(savedCompany);
        assertThat(admin.getEmail()).isEqualTo("admin@test.com");
        assertThat(admin.getPassword()).isEqualTo("encoded");
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isLocked()).isFalse();
    }

    private void setCredentials(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", email);
        ReflectionTestUtils.setField(bootstrap, "adminPassword", password);
    }
}
