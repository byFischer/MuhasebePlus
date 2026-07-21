package com.MuhasebePlus.demo.security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.security.dto.request.LoginRequestDto;
import com.MuhasebePlus.demo.security.dto.request.PasswordResetRequestDto;
import com.MuhasebePlus.demo.security.dto.response.LoginResponseDto;
import com.MuhasebePlus.demo.security.dto.response.PasswordResetResponseDto;
import com.MuhasebePlus.demo.security.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password-reset/test-mail")
    public ResponseEntity<PasswordResetResponseDto> sendPasswordResetTestMail(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        return ResponseEntity.ok(authService.sendPasswordResetTestMail(request));
    }
}
