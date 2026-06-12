package com.MuhasebePlus.demo.config;

import com.MuhasebePlus.demo.common.filter.SecurityHeadersFilter;
import com.MuhasebePlus.demo.common.idempotency.IdempotencyFilter;
import com.MuhasebePlus.demo.common.idempotency.IdempotencyService;
import com.MuhasebePlus.demo.common.ratelimit.RateLimitFilter;
import com.MuhasebePlus.demo.common.sanitizer.XssFilter;
import com.MuhasebePlus.demo.security.filter.JwtAuthenticationFilter;
import com.MuhasebePlus.demo.security.service.CustomUserDetailsService;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = {
        SecurityConfig.class,
        SecurityConfigUnitTest.FilterBeans.class,
        SecurityConfigUnitTest.MvcConfig.class,
        SecurityConfigUnitTest.ProbeController.class
})
@TestPropertySource(properties = {
        "jwt.secret=01234567890123456789012345678901",
        "jwt.expiration=3600000"
})
class SecurityConfigUnitTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext wac;
    @Autowired private CorsConfigurationSource corsConfigurationSource;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationProvider authenticationProvider;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void permitAllEndpoints_areReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/api/system/features")).andExpect(status().isOk());
        mockMvc.perform(get("/api/system/initialize")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/ping")).andExpect(status().isOk());
        mockMvc.perform(get("/error")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/protected/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoint_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_withAdminRole_isAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void protectedEndpoint_withAuthenticatedUser_isAllowed() throws Exception {
        mockMvc.perform(get("/api/protected/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflight_fromAllowedOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/protected/ping")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsConfigurationSource_exposesConfiguredFrontendOriginAndMethods() {
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/protected/ping"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly(ALLOWED_ORIGIN);
        assertThat(configuration.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void authenticationProvider_usesCustomUserDetailsServiceAndPasswordEncoder() {
        when(customUserDetailsService.loadUserByUsername("user@test.com")).thenReturn(
                new User("user@test.com", passwordEncoder.encode("secret"), List.of()));

        var authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken("user@test.com", "secret"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("user@test.com");
        assertThat(passwordEncoder.matches("secret", passwordEncoder.encode("secret"))).isTrue();
        assertThat(authenticationManager).isNotNull();
    }

    @Configuration
    @EnableWebMvc
    static class MvcConfig {
    }

    @Configuration
    static class FilterBeans {
        @Bean
        JwtUtil jwtUtil() {
            return new JwtUtil();
        }

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        IdempotencyService idempotencyService() {
            return mock(IdempotencyService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil,
                                                        CustomUserDetailsService customUserDetailsService) {
            return new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
        }

        @Bean
        IdempotencyFilter idempotencyFilter(IdempotencyService idempotencyService) {
            return new IdempotencyFilter(idempotencyService);
        }

        @Bean
        RateLimitFilter rateLimitFilter() {
            return new RateLimitFilter();
        }

        @Bean
        SecurityHeadersFilter securityHeadersFilter() {
            return new SecurityHeadersFilter();
        }

        @Bean
        XssFilter xssFilter() {
            return new XssFilter();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping({
                "/api/auth/ping",
                "/api/system/features",
                "/api/system/initialize",
                "/actuator/health",
                "/api/public/ping",
                "/api/protected/ping",
                "/api/admin/ping",
                "/error"
        })
        String ping() {
            return "ok";
        }

        @PostMapping("/api/users/register")
        String register() {
            return "ok";
        }
    }
}
