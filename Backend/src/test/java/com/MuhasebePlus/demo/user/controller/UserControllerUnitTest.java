package com.MuhasebePlus.demo.user.controller;

import com.MuhasebePlus.demo.user.dto.request.ChangePasswordRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UpdateProfileRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.dto.response.UserResponseDto;
import com.MuhasebePlus.demo.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock
    private UserService userService;
    @Mock
    private UserDetails userDetails;

    @Test
    void userController_delegatesRegistrationAndCurrentUserEndpoints() {
        UserController controller = new UserController(userService);
        UserRequestDto register = new UserRequestDto(
                "Ada",
                "Lovelace",
                "ada@example.com",
                "secret123",
                "555",
                LocalDate.of(1990, 1, 1),
                "Ada Yazilim",
                "1234567890"
        );
        UpdateProfileRequestDto profile = new UpdateProfileRequestDto("Ada", "Yeni", "555", LocalDate.of(1991, 1, 1));
        ChangePasswordRequestDto password = new ChangePasswordRequestDto("old", "new-password");
        UserResponseDto response = userResponse();
        when(userDetails.getUsername()).thenReturn("ada@example.com");
        when(userService.registerUser(register)).thenReturn(response);
        when(userService.getCurrentUserProfile("ada@example.com")).thenReturn(response);
        when(userService.updateOwnProfile("ada@example.com", profile)).thenReturn(response);

        assertThat(controller.registerUser(register).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getCurrentUserProfile(userDetails).getBody()).isSameAs(response);
        assertThat(controller.updateOwnProfile(userDetails, profile).getBody()).isSameAs(response);
        assertThat(controller.changePassword(userDetails, password).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.changeEmail(userDetails, "new@example.com").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(userService).changePassword("ada@example.com", password);
        verify(userService).changeEmail("ada@example.com", "new@example.com");
    }

    private UserResponseDto userResponse() {
        return new UserResponseDto(
                7L,
                "Ada",
                "Lovelace",
                "ada@example.com",
                "USER",
                "555",
                LocalDate.of(1990, 1, 1),
                null,
                1L,
                "Ada Yazilim"
        );
    }
}
