package com.MuhasebePlus.demo.user;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.user.dto.request.ChangePasswordRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UpdateProfileRequestDto;
import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.dto.response.UserResponseDto;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoEntityTest {

    @Test
    void userRequestRecordsExposeValues() {
        UserRequestDto request = new UserRequestDto(
                "Ada",
                "Lovelace",
                "ada@example.com",
                "secret123",
                "555",
                LocalDate.of(1990, 1, 1),
                "Ada Yazilim",
                "1234567890"
        );
        UpdateProfileRequestDto update = new UpdateProfileRequestDto("Ada", "Yeni", "555", LocalDate.of(1991, 1, 1));
        ChangePasswordRequestDto password = new ChangePasswordRequestDto("old", "new-password");
        UserResponseDto response = new UserResponseDto(
                7L,
                request.firstName(),
                request.lastName(),
                request.email(),
                UserRole.USER.name(),
                request.phoneNumber(),
                request.birthDate(),
                null,
                1L,
                request.companyName()
        );

        assertThat(update.lastName()).isEqualTo("Yeni");
        assertThat(password.newPassword()).isEqualTo("new-password");
        assertThat(response.companyName()).isEqualTo("Ada Yazilim");
    }

    @Test
    void userEntityImplementsUserDetailsFlagsAndAuthorities() {
        Company company = new Company();
        company.setCompanyId(1L);
        User user = new User();
        user.setUserId(7L);
        user.setCompany(company);
        user.setEmail("admin@example.com");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setLocked(false);

        assertThat(user.getUsername()).isEqualTo("admin@example.com");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");

        user.setLocked(true);
        user.setActive(false);
        assertThat(user.isAccountNonLocked()).isFalse();
        assertThat(user.isEnabled()).isFalse();
        assertThat(UserRole.values()).contains(UserRole.USER, UserRole.ADMIN);
    }
}
