package com.MuhasebePlus.demo.log.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.log.entity.SystemLog;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByCompanyCompanyIdOrderByTimestampDesc(Long companyId);
}
