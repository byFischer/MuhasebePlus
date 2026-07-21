package com.MuhasebePlus.demo.security.controller;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @LocalServerPort int port;

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        userRepository.findByEmail("integration@test.com").ifPresent(userRepository::delete);

        User user = new User();
        user.setEmail("integration@test.com");
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        userRepository.save(user);
    }

    // ── Başarılı giriş ────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returns200AndToken() {
        String body = "{\"email\":\"integration@test.com\",\"password\":\"secret123\"}";

        ResponseEntity<String> response = restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("token");
    }

    // ── Geçersiz şifre ────────────────────────────────────────────────────────

    @Test
    void login_withWrongPassword_returnsClientError() {
        String body = "{\"email\":\"integration@test.com\",\"password\":\"wrong\"}";

        assertThatThrownBy(() -> restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class))
                .isInstanceOf(HttpClientErrorException.class);
    }

    // ── Var olmayan kullanıcı ─────────────────────────────────────────────────

    @Test
    void login_withUnknownEmail_returnsClientError() {
        String body = "{\"email\":\"nobody@test.com\",\"password\":\"pass\"}";

        assertThatThrownBy(() -> restClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class))
                .isInstanceOf(HttpClientErrorException.class);
    }

    // ── Korunan endpoint — token olmadan 401 ──────────────────────────────────

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        assertThatThrownBy(() -> restClient.get()
                .uri("/api/journal-entries")
                .retrieve()
                .toEntity(String.class))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }
}
