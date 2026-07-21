package com.MuhasebePlus.demo.security.util;

import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-junit-tests-32chars!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@test.com");
        testUser.setPassword("hashed-password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.USER);
    }

    @Test
    void generateToken_shouldReturnNonNullJwtString() {
        String token = jwtUtil.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken(testUser);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("test@test.com");
    }

    @Test
    void validateToken_withCorrectUser_shouldReturnTrue() {
        String token = jwtUtil.generateToken(testUser);

        boolean valid = jwtUtil.validateToken(token, testUser);

        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_withDifferentUser_shouldReturnFalse() {
        String token = jwtUtil.generateToken(testUser);

        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        otherUser.setRole(UserRole.USER);

        boolean valid = jwtUtil.validateToken(token, otherUser);

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_shouldThrowExpiredJwtException() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expiredToken = jwtUtil.generateToken(testUser);

        assertThatThrownBy(() -> jwtUtil.validateToken(expiredToken, testUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void extractUsername_withInvalidToken_shouldThrowException() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }
}
