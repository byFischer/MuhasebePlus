package com.MuhasebePlus.demo.log;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.log.dto.response.SystemLogResponseDto;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;
import com.MuhasebePlus.demo.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LogDtoEntityTest {

    @Test
    void systemLogEntityAndResponseRecordExposeValues() {
        Company company = new Company();
        company.setCompanyId(1L);
        User user = new User();
        user.setUserId(7L);
        user.setEmail("ada@example.com");
        SystemLog log = new SystemLog();
        log.setLogId(10L);
        log.setCompany(company);
        log.setUserId(7L);
        log.setUser(user);
        log.setLogLevel(LogLevel.ERROR);
        log.setDetails("Hata");
        log.setTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));
        log.setIpAddress("127.0.0.1");
        SystemLogResponseDto response = new SystemLogResponseDto(
                log.getLogId(),
                log.getLogLevel().name(),
                log.getDetails(),
                log.getTimestamp(),
                log.getIpAddress(),
                log.getUserId(),
                log.getUser().getEmail()
        );

        assertThat(response.logLevel()).isEqualTo("ERROR");
        assertThat(response.userEmail()).isEqualTo("ada@example.com");
        assertThat(LogLevel.values()).contains(LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR);
    }
}
