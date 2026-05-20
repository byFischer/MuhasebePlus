package com.MuhasebePlus.demo.security.service;

import com.MuhasebePlus.demo.security.dto.request.LoginRequestDto;
import com.MuhasebePlus.demo.security.dto.response.LoginResponseDto;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserService userService;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock private Environment environment;

    @InjectMocks
    private AuthService authService;

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returnsJwtToken() {
        User user = buildUser("test@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token-123");

        LoginResponseDto result = authService.login(new LoginRequestDto("test@test.com", "pass123"));

        assertThat(result.token()).isEqualTo("jwt-token-123");
        verify(userService).resetFailedLoginAttempts("test@test.com");
    }

    @Test
    void login_withInvalidCredentials_throwsBadCredentialsAndIncrementsAttempts() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("wrong@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService).incrementFailedLoginAttempts("wrong@test.com");
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_withValidCredentials_doesNotIncrementFailedAttempts() {
        User user = buildUser("ok@test.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.login(new LoginRequestDto("ok@test.com", "pass"));

        verify(userService, never()).incrementFailedLoginAttempts(any());
    }

    // ── sendPasswordResetTestMail ─────────────────────────────────────────────

    @Test
    void sendPasswordResetTestMail_whenMailSenderNotConfigured_throwsRuntimeException() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> authService.sendPasswordResetTestMail(
                new com.MuhasebePlus.demo.security.dto.request.PasswordResetRequestDto("x@x.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mail sender is not configured");
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    private User buildUser(String email) {
        User u = new User();
        u.setUserId(1L);
        u.setEmail(email);
        u.setRole(UserRole.USER);
        return u;
    }
}
