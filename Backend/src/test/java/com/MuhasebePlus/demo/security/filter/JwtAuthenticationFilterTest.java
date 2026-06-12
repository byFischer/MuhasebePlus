package com.MuhasebePlus.demo.security.filter;

import com.MuhasebePlus.demo.security.service.CustomUserDetailsService;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService customUserDetailsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_whenAuthorizationHeaderMissing_continuesWithoutAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, customUserDetailsService);
    }

    @Test
    void doFilter_whenBearerTokenIsValid_setsSecurityContextAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user("user@test.com");
        when(jwtUtil.extractUsername("valid-token")).thenReturn("user@test.com");
        when(customUserDetailsService.loadUserByUsername("user@test.com")).thenReturn(user);
        when(jwtUtil.validateToken("valid-token", user)).thenReturn(true);

        filter.doFilter(request, response, (req, res) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("user@test.com");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    void doFilter_whenTokenIsInvalid_doesNotSetAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        User user = user("user@test.com");
        when(jwtUtil.extractUsername("invalid-token")).thenReturn("user@test.com");
        when(customUserDetailsService.loadUserByUsername("user@test.com")).thenReturn(user);
        when(jwtUtil.validateToken("invalid-token", user)).thenReturn(false);

        filter.doFilter(request, response, (req, res) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilter_whenJwtParsingThrows_clearsExistingSecurityContext() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("Authorization", "Bearer broken-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing@test.com", null));
        when(jwtUtil.extractUsername("broken-token")).thenThrow(new IllegalArgumentException("bad token"));

        filter.doFilter(request, response, (req, res) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(customUserDetailsService, never()).loadUserByUsername("existing@test.com");
    }

    @Test
    void doFilter_whenAuthenticationAlreadyExists_skipsUserLookup() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/customers");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var existing = new UsernamePasswordAuthenticationToken("existing@test.com", null);
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("user@test.com");

        filter.doFilter(request, response, (req, res) ->
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing));

        verify(customUserDetailsService, never()).loadUserByUsername("user@test.com");
    }

    private User user(String email) {
        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setPassword("hashed");
        user.setRole(UserRole.USER);
        return user;
    }
}
