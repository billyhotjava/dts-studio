package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsDashboardRepository extends JpaRepository<AnalyticsDashboard, Long> {

    List<AnalyticsDashboard> findByCreatedBy(Long createdBy);
}
