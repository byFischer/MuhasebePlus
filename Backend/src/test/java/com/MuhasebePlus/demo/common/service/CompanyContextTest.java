package com.MuhasebePlus.demo.common.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyContextTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CompanyContext companyContext;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── SecurityContext'ten kullanıcı çözümleme ───────────────────────────────

    @Test
    void getCurrentCompanyId_whenPrincipalIsUserWithCompany_returnsCompanyId() {
        User user = userWithCompany(42L, 7L);
        authenticateWithPrincipal(user);

        Long companyId = companyContext.getCurrentCompanyId();

        assertThat(companyId).isEqualTo(7L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentUserId_whenPrincipalIsUser_returnsUserIdWithoutRepositoryLookup() {
        User user = userWithCompany(42L, 7L);
        authenticateWithPrincipal(user);

        Long userId = companyContext.getCurrentUserId();

        assertThat(userId).isEqualTo(42L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentCompanyId_whenUserHasNoCompany_throwsRuntimeException() {
        User user = userWithCompany(42L, null);
        authenticateWithPrincipal(user);

        assertThatThrownBy(() -> companyContext.getCurrentCompanyId())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not associated with any company");
    }

    // ── Kimlik doğrulaması eksik / anonim ─────────────────────────────────────

    @Test
    void getCurrentCompanyId_whenNoAuthenticationInContext_throwsRuntimeException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> companyContext.getCurrentCompanyId())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No authenticated user");
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentCompanyId_whenAnonymousAuthentication_throwsRuntimeException() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThatThrownBy(() -> companyContext.getCurrentCompanyId())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No authenticated user");
        verifyNoInteractions(userRepository);
    }

    // ── E-posta üzerinden depo yedek araması (fallback) ───────────────────────

    @Test
    void getCurrentUserId_whenPrincipalIsEmailString_looksUpUserFromRepository() {
        authenticateWithPrincipal("fallback@test.com");
        User user = userWithCompany(99L, 5L);
        when(userRepository.findByEmail("fallback@test.com")).thenReturn(Optional.of(user));

        Long userId = companyContext.getCurrentUserId();

        assertThat(userId).isEqualTo(99L);
        verify(userRepository).findByEmail("fallback@test.com");
    }

    @Test
    void getCurrentUserId_whenFallbackEmailNotFound_throwsRuntimeException() {
        authenticateWithPrincipal("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyContext.getCurrentUserId())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with email: ghost@test.com");
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    private User userWithCompany(Long userId, Long companyId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("ctx@test.com");
        user.setRole(UserRole.USER);
        if (companyId != null) {
            Company company = new Company();
            company.setCompanyId(companyId);
            user.setCompany(company);
        }
        return user;
    }

    private void authenticateWithPrincipal(Object principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
