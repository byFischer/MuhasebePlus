package com.MuhasebePlus.demo.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;

import com.MuhasebePlus.demo.security.dto.request.PasswordResetRequestDto;
import com.MuhasebePlus.demo.security.dto.response.PasswordResetResponseDto;

class PasswordResetMailSmokeTest {

    @Test
    void sendsPasswordResetTestMailThroughAuthService() {
        String username = System.getenv("MAIL_USERNAME");
        String password = System.getenv("MAIL_PASSWORD");
        String target = System.getenv("MAIL_TARGET");

        assumeTrue(username != null && !username.isBlank(), "MAIL_USERNAME must be set");
        assumeTrue(password != null && !password.isBlank(), "MAIL_PASSWORD must be set");
        assumeTrue(target != null && !target.isBlank(), "MAIL_TARGET must be set");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(readEnv("MAIL_HOST", "127.0.0.1"));
        sender.setPort(Integer.parseInt(readEnv("MAIL_PORT", "1025")));
        sender.setUsername(username);
        sender.setPassword(password);

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.ssl.trust", readEnv("MAIL_HOST", "127.0.0.1"));
        properties.put("mail.smtp.localhost", readEnv("MAIL_LOCALHOST", "localhost"));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");

        AuthService authService = new AuthService(
                null,
                null,
                null,
                new StaticObjectProvider<>(sender),
                new MockEnvironment().withProperty("spring.mail.username", username)
        );

        PasswordResetResponseDto response = authService.sendPasswordResetTestMail(
                new PasswordResetRequestDto(target)
        );

        assertEquals("Password reset test email sent successfully.", response.message());
    }

    private static String readEnv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record StaticObjectProvider<T>(T value) implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) throws BeansException {
            return value;
        }

        @Override
        public T getIfAvailable() throws BeansException {
            return value;
        }

        @Override
        public T getIfUnique() throws BeansException {
            return value;
        }

        @Override
        public T getObject() throws BeansException {
            return value;
        }
    }
}
