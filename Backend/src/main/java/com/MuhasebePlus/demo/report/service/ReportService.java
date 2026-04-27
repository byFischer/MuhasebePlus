package com.MuhasebePlus.demo.report.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.report.dto.request.ReportRequestDto;
import com.MuhasebePlus.demo.report.dto.response.ReportResponseDto;
import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.report.entity.ReportFormat;
import com.MuhasebePlus.demo.report.repository.ReportRepository;

@Service
@Transactional
public class ReportService implements HardDeletable {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportExcelBuilder excelBuilder;

    @Autowired
    private CompanyContext companyContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Value("${app.report.storage-path:./reports/}")
    private String storagePath;


    // PUBLIC METOTLAR

    public ReportResponseDto generateReport(ReportRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        Long userId = companyContext.getCurrentUserId();

        if (dto.startDate().isAfter(dto.endDate())) {
            throw new RuntimeException("Başlangıç tarihi bitiş tarihinden büyük olamaz");
        }

        // Once entity'i kaydedip ID al, sonra dosya adi olustur
        Report report = new Report();
        report.setCompany(companyRepository.getReferenceById(companyId));
        report.setUserId(userId);
        report.setReportType(dto.reportType());
        report.setStartDate(dto.startDate());
        report.setEndDate(dto.endDate());
        report.setFormat(dto.format());
        report.setGeneratedAt(LocalDateTime.now());
        report.setDeleted(false);
        Report saved = reportRepository.save(report);

        // Excel dosyasini uret ve diske yaz
        Path filePath = buildFilePath(companyId, saved.getReportId());
        try {
            Files.createDirectories(filePath.getParent());
            try (OutputStream out = Files.newOutputStream(filePath)) {
                excelBuilder.build(dto.reportType(), companyId, dto.startDate(), dto.endDate(), out);
            }
            saved.setFilePath(filePath.toString());
            saved.setFileSize(Files.size(filePath));
            reportRepository.save(saved);
        } catch (IOException e) {
            throw new RuntimeException("Rapor dosyası oluşturulamadı: " + e.getMessage(), e);
        }

        return toResponseDto(saved);
    }

    public byte[] downloadReport(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Report report = findActiveReportById(reportId, companyId);

        if (report.getFilePath() == null) {
            throw new RuntimeException("Bu raporun dosyası bulunamadı");
        }

        try {
            return Files.readAllBytes(Paths.get(report.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Rapor dosyası okunamadı: " + e.getMessage(), e);
        }
    }

    public List<ReportResponseDto> getAllReports() {
        Long companyId = companyContext.getCurrentCompanyId();
        return reportRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByGeneratedAtDesc(companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public ReportResponseDto getReportById(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return toResponseDto(findActiveReportById(reportId, companyId));
    }

    public void softDeleteReport(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Report report = findActiveReportById(reportId, companyId);
        report.setDeleted(true);
        report.setDeletedAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    public ReportResponseDto restoreReport(Long reportId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Report report = reportRepository.findByReportIdAndCompanyCompanyId(reportId, companyId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));

        report.setDeleted(false);
        report.setDeletedAt(null);
        Report restored = reportRepository.save(report);
        return toResponseDto(restored);
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<Report> expired = reportRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (Report report : expired) {
            // Diskteki dosyayi da sil
            if (report.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(report.getFilePath()));
                } catch (IOException ignored) {
                    // dosya yoksa veya silinemiyorsa devam et
                }
            }
            reportRepository.delete(report);
        }
        return expired.size();
    }


    // PRIVATE METOTLAR

    private Report findActiveReportById(Long reportId, Long companyId) {
        return reportRepository.findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(reportId, companyId)
                .orElseThrow(() -> new RuntimeException("Report not found with id: " + reportId));
    }

    private Path buildFilePath(Long companyId, Long reportId) {
        long timestamp = System.currentTimeMillis();
        String fileName = String.format("report_%d_%d_%d.xlsx", companyId, reportId, timestamp);
        return Paths.get(storagePath, fileName).toAbsolutePath();
    }

    private ReportResponseDto toResponseDto(Report r) {
        return new ReportResponseDto(
                r.getReportId(),
                r.getReportType() != null ? r.getReportType().name() : null,
                r.getFormat() != null ? r.getFormat().name() : ReportFormat.EXCEL.name(),
                r.getStartDate(),
                r.getEndDate(),
                r.getGeneratedAt(),
                r.getFileSize(),
                r.isDeleted(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
