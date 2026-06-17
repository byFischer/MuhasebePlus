package com.MuhasebePlus.demo.log.controller;

import com.MuhasebePlus.demo.log.dto.response.SystemLogResponseDto;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemLogControllerUnitTest {

    @Mock
    private SystemLogService systemLogService;

    @Test
    void getLogs_delegatesToService() {
        SystemLogController controller = new SystemLogController(systemLogService);
        PageRequest pageable = PageRequest.of(0, 10);
        SystemLogResponseDto dto = response();
        when(systemLogService.getLogs(LogLevel.INFO, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 7L, pageable))
                .thenReturn(new PageImpl<>(List.of(dto)));

        ResponseEntity<Page<SystemLogResponseDto>> result = controller.getLogs(
                LogLevel.INFO,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                7L,
                pageable
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getContent()).containsExactly(dto);
    }

    @Test
    void exportLogs_setsXlsxHeadersAndBody() {
        SystemLogController controller = new SystemLogController(systemLogService);
        byte[] body = {1, 2, 3, 4};
        when(systemLogService.exportLogsAsExcel(LogLevel.ERROR, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null))
                .thenReturn(body);

        ResponseEntity<byte[]> result = controller.exportLogs(
                LogLevel.ERROR,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        assertThat(result.getHeaders().getContentDisposition().getFilename()).isEqualTo("system_logs.xlsx");
        assertThat(result.getHeaders().getContentLength()).isEqualTo(body.length);
        assertThat(result.getBody()).isEqualTo(body);
    }

    private SystemLogResponseDto response() {
        return new SystemLogResponseDto(
                1L,
                LogLevel.INFO.name(),
                "Detay",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                "10.0.0.1",
                7L,
                "ada@example.com"
        );
    }
}
