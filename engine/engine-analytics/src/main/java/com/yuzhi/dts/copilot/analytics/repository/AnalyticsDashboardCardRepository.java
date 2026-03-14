package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDashboardCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsDashboardCardRepository extends JpaRepository<AnalyticsDashboardCard, Long> {

    List<AnalyticsDashboardCard> findByDashboardId(Long dashboardId);

    void deleteByDashboardId(Long dashboardId);
}
