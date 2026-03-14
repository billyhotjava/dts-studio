package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsCollectionRepository extends JpaRepository<AnalyticsCollection, Long> {

    List<AnalyticsCollection> findByParentId(Long parentId);
}
