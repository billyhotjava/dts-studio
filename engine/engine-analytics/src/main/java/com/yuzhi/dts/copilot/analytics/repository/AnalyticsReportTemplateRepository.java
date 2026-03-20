package com.yuzhi.dts.copilot.analytics.repository;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsReportTemplateRepository extends JpaRepository<AnalyticsReportTemplate, Long> {

    List<AnalyticsReportTemplate> findAllByArchivedFalseOrderByUpdatedAtDesc(Pageable pageable);

    @Query("""
            select t
            from AnalyticsReportTemplate t
            where t.archived = false
              and t.published = true
              and lower(t.certificationStatus) = 'certified'
              and (:domain is null or lower(t.domain) = lower(:domain))
              and (:category is null or lower(t.category) = lower(:category))
            order by t.updatedAt desc
            """)
    List<AnalyticsReportTemplate> findCatalogTemplates(
            @Param("domain") String domain,
            @Param("category") String category,
            Pageable pageable);
}
