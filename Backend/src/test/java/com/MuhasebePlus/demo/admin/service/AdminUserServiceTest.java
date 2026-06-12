package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.admin.dto.AdminUserDto;
import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CompanyContext companyContext;
    @Mock private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminUserService service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .companyId(1L)
                .companyName("Test A.S.")
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_mapsRepositoryPage() {
        PageRequest pageable = PageRequest.of(0, 20);
        User user = user(10L, "admin@test.com", UserRole.ADMIN);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(inv -> {
                    exerciseSpec(inv.getArgument(0));
                    return new PageImpl<>(List.of(user), pageable, 1);
                });

        var result = service.getUsers("admin", "ADMIN", "ACTIVE", pageable);

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.userId()).isEqualTo(10L);
            assertThat(dto.role()).isEqualTo("ADMIN");
            assertThat(dto.companyName()).isEqualTo("Test A.S.");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_statusFiltersExerciseLockedPassiveAndInvalidBranches() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenAnswer(inv -> {
                    exerciseSpec(inv.getArgument(0));
                    return new PageImpl<>(List.of(), pageable, 0);
                });

        service.getUsers(null, null, "LOCKED", pageable);
        service.getUsers(null, null, "PASSIVE", pageable);

        assertThatThrownBy(() -> service.getUsers(null, null, "UNKNOWN", pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void getUser_whenUserExists_returnsDto() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, "user@test.com", UserRole.USER)));

        AdminUserDto result = service.getUser(10L);

        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test
    void getUser_whenMissing_throwsBusinessException() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(404L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("404");
    }

    @Test
    void updateRole_whenValid_updatesRoleAndAudits() {
        User user = user(10L, "user@test.com", UserRole.USER);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(companyContext.getCurrentUserId()).thenReturn(99L);
        when(userRepository.save(user)).thenReturn(user);

        AdminUserDto result = service.updateRole(10L, " admin ");

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.role()).isEqualTo("ADMIN");
        verify(adminAuditService).logAction(org.mockito.ArgumentMatchers.contains("user@test.com"));
    }

    @Test
    void updateRole_whenInvalidRole_throwsBusinessException() {
        assertThatThrownBy(() -> service.updateRole(10L, "owner"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void updateRole_whenChangingOwnRole_throwsBusinessException() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, "admin@test.com", UserRole.ADMIN)));
        when(companyContext.getCurrentUserId()).thenReturn(10L);

        assertThatThrownBy(() -> service.updateRole(10L, "USER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kendi");
    }

    @Test
    void updateRole_whenLastAdminWouldBeDemoted_throwsBusinessException() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, "admin@test.com", UserRole.ADMIN)));
        when(companyContext.getCurrentUserId()).thenReturn(99L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.updateRole(10L, "USER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("son");

        verify(userRepository, never()).save(any());
    }

    @Test
    void lockUser_whenNotSelf_locksUser() {
        User user = user(10L, "user@test.com", UserRole.USER);
        when(companyContext.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.lockUser(10L);

        assertThat(user.isLocked()).isTrue();
        verify(userRepository).save(user);
        verify(adminAuditService).logAction(org.mockito.ArgumentMatchers.contains("kilitlendi"));
    }

    @Test
    void lockUser_whenSelf_throwsBusinessException() {
        when(companyContext.getCurrentUserId()).thenReturn(10L);

        assertThatThrownBy(() -> service.lockUser(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kendi");
    }

    @Test
    void unlockUser_resetsLockAndFailedAttempts() {
        User user = user(10L, "user@test.com", UserRole.USER);
        user.setLocked(true);
        user.setFailedLoginAttempts(3);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.unlockUser(10L);

        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(user);
    }

    @Test
    void activateAndDeactivateUser_toggleActiveState() {
        User user = user(10L, "user@test.com", UserRole.USER);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.activateUser(10L);
        assertThat(user.isActive()).isTrue();

        when(companyContext.getCurrentUserId()).thenReturn(99L);
        service.deactivateUser(10L);
        assertThat(user.isActive()).isFalse();
        verify(userRepository, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    void deactivateUser_whenSelf_throwsBusinessException() {
        when(companyContext.getCurrentUserId()).thenReturn(10L);

        assertThatThrownBy(() -> service.deactivateUser(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Kendi");
    }

    @Test
    void resetPassword_encodesPasswordAndUnlocksUser() {
        User user = user(10L, "user@test.com", UserRole.USER);
        user.setLocked(true);
        user.setFailedLoginAttempts(4);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");

        service.resetPassword(10L, "secret123");

        assertThat(user.getPassword()).isEqualTo("encoded");
        assertThat(user.isLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_whenValid_deletesAndAudits() {
        User user = user(10L, "user@test.com", UserRole.USER);
        when(companyContext.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        service.deleteUser(10L);

        verify(userRepository).delete(user);
        verify(userRepository).flush();
        verify(adminAuditService).logAction(org.mockito.ArgumentMatchers.contains("ID 10"));
    }

    @Test
    void deleteUser_whenLastAdmin_throwsBusinessException() {
        User admin = user(10L, "admin@test.com", UserRole.ADMIN);
        when(companyContext.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteUser(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("son");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_whenIntegrityViolation_throwsFriendlyBusinessException() {
        User user = user(10L, "user@test.com", UserRole.USER);
        when(companyContext.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        doThrow(new DataIntegrityViolationException("fk")).when(userRepository).flush();

        assertThatThrownBy(() -> service.deleteUser(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pasifle");
    }

    private User user(Long id, String email, UserRole role) {
        User user = new User();
        user.setUserId(id);
        user.setCompany(company);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword("old");
        user.setRole(role);
        user.setPhoneNumber("555");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setActive(true);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        return user;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void exerciseSpec(Specification<User> spec) {
        spec.toPredicate(
                (Root<User>) mock(Root.class, org.mockito.Mockito.RETURNS_DEEP_STUBS),
                mock(CriteriaQuery.class),
                mock(CriteriaBuilder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
    }
}
