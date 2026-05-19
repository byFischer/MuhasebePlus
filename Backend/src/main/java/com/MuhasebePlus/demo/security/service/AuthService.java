package com.MuhasebePlus.demo.security.service;

import com.MuhasebePlus.demo.security.dto.request.LoginRequestDto;
import com.MuhasebePlus.demo.security.dto.request.PasswordResetRequestDto;
import com.MuhasebePlus.demo.security.dto.response.LoginResponseDto;
import com.MuhasebePlus.demo.security.dto.response.PasswordResetResponseDto;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.service.UserService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;

    public LoginResponseDto login(LoginRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = (User) authentication.getPrincipal();
            userService.resetFailedLoginAttempts(user.getEmail());
            String token = jwtUtil.generateToken(user);
            log.info("User logged in email={}", user.getEmail());
            return new LoginResponseDto(token);
        } catch (BadCredentialsException e) {
            userService.incrementFailedLoginAttempts(request.email());
            log.warn("Failed login attempt email={}", request.email());
            throw e;
        }
    }

    public PasswordResetResponseDto sendPasswordResetTestMail(PasswordResetRequestDto request) {
        JavaMailSender javaMailSender = mailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw new RuntimeException("Mail sender is not configured");
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            String from = environment.getProperty("spring.mail.username");
            if (from != null && !from.isBlank()) {
                message.setFrom(new InternetAddress(from));
            }

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.email()));
            message.setSubject("Muhasebe+ Password Reset Test", StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            message.setHeader("Message-ID", "<" + UUID.randomUUID() + "@muhasebeplus.local>");
            message.setText("""
                    Hello,

                    This is a test email for the Muhasebe+ password reset page.
                    If this reached your inbox, JavaMailSender is configured correctly.

                    Muhasebe+ Team
                    """, StandardCharsets.UTF_8.name());

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Password reset test email could not be prepared", e);
        }
        return new PasswordResetResponseDto("Password reset test email sent successfully.");
    }
}
