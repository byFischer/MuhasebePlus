package com.MuhasebePlus.demo.log.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.log.entity.SystemLog;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {

    List<SystemLog> findByCompanyCompanyIdOrderByTimestampDesc(Long companyId);

    Page<SystemLog> findByCompanyCompanyId(Long companyId, Pageable pageable);
}
