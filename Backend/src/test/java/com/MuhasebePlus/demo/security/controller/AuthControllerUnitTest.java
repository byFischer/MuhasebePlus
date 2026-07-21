package com.MuhasebePlus.demo.security.controller;

import com.MuhasebePlus.demo.security.dto.request.LoginRequestDto;
import com.MuhasebePlus.demo.security.dto.request.PasswordResetRequestDto;
import com.MuhasebePlus.demo.security.dto.response.LoginResponseDto;
import com.MuhasebePlus.demo.security.dto.response.PasswordResetResponseDto;
import com.MuhasebePlus.demo.security.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    void login_returnsServiceToken() {
        LoginRequestDto request = new LoginRequestDto("user@test.com", "secret");
        when(authService.login(request)).thenReturn(new LoginResponseDto("jwt-token"));

        var response = controller.login(request);

        assertThat(response.getBody().token()).isEqualTo("jwt-token");
    }

    @Test
    void sendPasswordResetTestMail_returnsServiceMessage() {
        PasswordResetRequestDto request = new PasswordResetRequestDto("user@test.com");
        when(authService.sendPasswordResetTestMail(request))
                .thenReturn(new PasswordResetResponseDto("sent"));

        var response = controller.sendPasswordResetTestMail(request);

        assertThat(response.getBody().message()).isEqualTo("sent");
    }
}
