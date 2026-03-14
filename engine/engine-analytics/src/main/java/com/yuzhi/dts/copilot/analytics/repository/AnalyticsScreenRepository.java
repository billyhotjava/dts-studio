package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsScreenRepository extends JpaRepository<AnalyticsScreen, Long> {

    List<AnalyticsScreen> findByStatus(String status);

    List<AnalyticsScreen> findByCreatedBy(Long createdBy);
}
