package com.MuhasebePlus.demo.log.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.entity.SystemLog;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByCompanyCompanyIdOrderByTimestampDesc(Long companyId);

    @Query("SELECT l FROM SystemLog l " +
           "WHERE l.company.companyId = :companyId " +
           "AND (:level IS NULL OR l.logLevel = :level) " +
           "AND (:userId IS NULL OR l.userId = :userId) " +
           "AND (:start IS NULL OR l.timestamp >= :start) " +
           "AND (:end IS NULL OR l.timestamp <= :end) " +
           "ORDER BY l.timestamp DESC")
    List<SystemLog> searchLogs(
            @Param("companyId") Long companyId,
            @Param("level") LogLevel level,
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
