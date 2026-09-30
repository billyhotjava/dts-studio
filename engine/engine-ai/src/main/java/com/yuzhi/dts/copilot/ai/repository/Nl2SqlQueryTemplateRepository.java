package com.yuzhi.dts.copilot.ai.repository;

import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Nl2SqlQueryTemplateRepository extends JpaRepository<Nl2SqlQueryTemplate, Long> {

    List<Nl2SqlQueryTemplate> findByIsActiveTrueOrderByPriorityDesc();

    List<Nl2SqlQueryTemplate> findByDomainAndIsActiveTrue(String domain);

    Optional<Nl2SqlQueryTemplate> findByTemplateCode(String templateCode);

    /** Version ownership is immutable, even after activation replaces template rows. */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT v.id AS versionId, p.name AS packName, v.version AS packVersion
            FROM copilot_ai.studio_pack_version v JOIN copilot_ai.studio_pack p ON p.id=v.pack_id
            WHERE v.id IN (:versionIds) ORDER BY p.name, v.version
            """, nativeQuery = true)
    List<TemplatePackSource> findPackSources(
            @org.springframework.data.repository.query.Param("versionIds") java.util.Collection<Long> versionIds);

    interface TemplatePackSource {
        Long getVersionId();
        String getPackName();
        String getPackVersion();
    }
}
