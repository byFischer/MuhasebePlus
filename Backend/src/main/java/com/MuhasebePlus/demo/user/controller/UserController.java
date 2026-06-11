package com.MuhasebePlus.demo.user.controller;

import com.MuhasebePlus.demo.user.dto.request.ChangePasswordRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UpdateProfileRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.dto.response.UserResponseDto;
import com.MuhasebePlus.demo.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

// Admin endpoint'leri /api/admin/users altına taşındı (bkz. admin.controller.AdminUserController)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //  Registration

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody UserRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(dto));
    }

    //  Current User Endpoints 

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUserProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateOwnProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequestDto dto) {
        return ResponseEntity.ok(userService.updateOwnProfile(userDetails.getUsername(), dto));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto dto) {
        userService.changePassword(userDetails.getUsername(), dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/email")
    public ResponseEntity<Void> changeEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String newEmail) {
        userService.changeEmail(userDetails.getUsername(), newEmail);
        return ResponseEntity.noContent().build();
    }
}
