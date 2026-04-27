package com.MuhasebePlus.demo.report.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.report.entity.Report;
import com.MuhasebePlus.demo.report.entity.ReportType;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByReportIdAndCompanyCompanyIdAndIsDeletedFalse(Long reportId, Long companyId);

    Optional<Report> findByReportIdAndCompanyCompanyId(Long reportId, Long companyId);

    List<Report> findByCompanyCompanyIdAndIsDeletedFalseOrderByGeneratedAtDesc(Long companyId);

    List<Report> findByReportTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByGeneratedAtDesc(
            ReportType reportType, Long companyId);

    List<Report> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
