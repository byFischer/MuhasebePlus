package com.MuhasebePlus.demo.admin.controller;

import com.MuhasebePlus.demo.admin.dto.AdminResetPasswordRequestDto;
import com.MuhasebePlus.demo.admin.dto.AdminUserDto;
import com.MuhasebePlus.demo.admin.dto.UpdateUserRoleRequestDto;
import com.MuhasebePlus.demo.admin.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminUserService.getUsers(search, role, status, pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.getUser(userId));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<AdminUserDto> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequestDto dto) {
        return ResponseEntity.ok(adminUserService.updateRole(userId, dto.role()));
    }

    @PutMapping("/{userId}/lock")
    public ResponseEntity<Void> lockUser(@PathVariable Long userId) {
        adminUserService.lockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable Long userId) {
        adminUserService.unlockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long userId) {
        adminUserService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        adminUserService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequestDto dto) {
        adminUserService.resetPassword(userId, dto.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
