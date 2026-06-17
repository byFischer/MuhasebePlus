package com.MuhasebePlus.demo.log.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.dto.response.SystemLogResponseDto;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;
import com.MuhasebePlus.demo.log.repository.SystemLogRepository;
import com.MuhasebePlus.demo.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemLogServiceTest {

    @Mock
    private SystemLogRepository systemLogRepository;
    @Mock
    private CompanyContext companyContext;
    @Mock
    private CompanyRepository companyRepository;

    private SystemLogService service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new SystemLogService(systemLogRepository, companyContext, companyRepository);
        company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void log_whenContextAndForwardedIpExist_savesCompanyUserAndFirstForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyContext.getCurrentUserId()).thenReturn(7L);
        when(companyRepository.getReferenceById(1L)).thenReturn(company);

        service.log(LogLevel.INFO, "Fatura olusturuldu");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());
        SystemLog saved = captor.getValue();
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getLogLevel()).isEqualTo(LogLevel.INFO);
        assertThat(saved.getDetails()).isEqualTo("Fatura olusturuldu");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void log_whenCompanyContextMissing_savesLogWithoutCompanyAndUsesRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(companyContext.getCurrentCompanyId()).thenThrow(new RuntimeException("No context"));

        service.log(LogLevel.ERROR, "Context yok");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());
        SystemLog saved = captor.getValue();
        assertThat(saved.getCompany()).isNull();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.10");
        verify(companyRepository, never()).getReferenceById(any());
    }

    @Test
    void log_whenUserContextAndRequestMissing_handlesNullsGracefully() {
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyContext.getCurrentUserId()).thenThrow(new RuntimeException("No user"));
        when(companyRepository.getReferenceById(1L)).thenReturn(company);

        service.log(LogLevel.WARNING, "Kullanici yok");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
        assertThat(captor.getValue().getIpAddress()).isNull();
    }

    @Test
    void getLogs_mapsRepositoryPageToResponseDtos() {
        User user = new User();
        user.setUserId(7L);
        user.setEmail("ada@example.com");
        SystemLog log = log(1L, LogLevel.WARNING, "Detay", "10.0.0.1", 7L, user);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(systemLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<SystemLogResponseDto> result = service.getLogs(
                LogLevel.WARNING,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                7L,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.logId()).isEqualTo(1L);
            assertThat(dto.logLevel()).isEqualTo("WARNING");
            assertThat(dto.userEmail()).isEqualTo("ada@example.com");
        });
    }

    @Test
    void exportLogsAsExcel_buildsWorkbookWithHeaderAndRows_withoutRequiringDates() throws Exception {
        User user = new User();
        user.setEmail("ada@example.com");
        SystemLog first = log(1L, LogLevel.INFO, "Virgullu, detay", "10.0.0.1", 7L, user);
        SystemLog second = log(2L, null, "Satir\nsonu ve \"tırnak\"", null, null, null);
        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(systemLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(first, second));

        // Tarih verilmeden de çalışmalı (Sistem Logları ekranında tarih filtresi yok)
        byte[] xlsx = service.exportLogsAsExcel(LogLevel.INFO, null, null, 7L);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = wb.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Log ID");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Seviye");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("Mesaj");

            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("INFO");
            assertThat(row1.getCell(3).getStringCellValue()).isEqualTo("10.0.0.1");
            assertThat(row1.getCell(4).getStringCellValue()).isEqualTo("ada@example.com");
            assertThat(row1.getCell(5).getStringCellValue()).isEqualTo("Virgullu, detay");

            Row row2 = sheet.getRow(2);
            assertThat(row2.getCell(1).getStringCellValue()).isEmpty();
            assertThat(row2.getCell(4).getStringCellValue()).isEmpty();
            assertThat(row2.getCell(5).getStringCellValue()).isEqualTo("Satir\nsonu ve \"tırnak\"");
        }
    }

    private SystemLog log(Long id, LogLevel level, String details, String ipAddress, Long userId, User user) {
        SystemLog log = new SystemLog();
        log.setLogId(id);
        log.setLogLevel(level);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setUserId(userId);
        log.setUser(user);
        log.setTimestamp(LocalDateTime.of(2026, 1, 10, 12, 0));
        log.setCompany(company);
        return log;
    }
}
