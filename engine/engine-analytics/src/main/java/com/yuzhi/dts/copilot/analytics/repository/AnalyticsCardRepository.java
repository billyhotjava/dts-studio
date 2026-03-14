package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsCardRepository extends JpaRepository<AnalyticsCard, Long> {

    List<AnalyticsCard> findByDatabaseId(Long databaseId);

    List<AnalyticsCard> findByCreatedBy(Long createdBy);
}
