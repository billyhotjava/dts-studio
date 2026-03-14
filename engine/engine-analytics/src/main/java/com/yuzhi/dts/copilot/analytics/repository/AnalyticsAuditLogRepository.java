package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsAuditLogRepository extends JpaRepository<AnalyticsAuditLog, Long> {

    List<AnalyticsAuditLog> findByUserId(Long userId);
}
