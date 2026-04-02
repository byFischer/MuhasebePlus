package com.MuhasebePlus.demo.security.service;

import com.MuhasebePlus.demo.security.dto.request.LoginRequestDto;
import com.MuhasebePlus.demo.security.dto.response.LoginResponseDto;
import com.MuhasebePlus.demo.security.util.JwtUtil;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.service.userService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final userService userService;

    public LoginResponseDto login(LoginRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = (User) authentication.getPrincipal();
            userService.resetFailedLoginAttempts(user.getEmail());
            String token = jwtUtil.generateToken(user);
            return new LoginResponseDto(token);
        } catch (BadCredentialsException e) {
            userService.incrementFailedLoginAttempts(request.email());
            throw e;
        }
    }
}
