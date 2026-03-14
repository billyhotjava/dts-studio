package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsSessionRepository extends JpaRepository<AnalyticsSession, String> {

    List<AnalyticsSession> findByUserId(Long userId);
}
